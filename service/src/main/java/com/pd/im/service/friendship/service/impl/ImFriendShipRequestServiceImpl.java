package com.pd.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pd.im.codec.pack.friendship.FriendRequestNotifyPack;
import com.pd.im.codec.pack.friendship.ReadAllFriendRequestNotifyPack;
import com.pd.im.codec.pack.user.UserSnapshotPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.RequestBase;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.FriendshipEventCommand;
import com.pd.im.common.enums.friend.FriendRequestApprovalStatus;
import com.pd.im.common.enums.friend.FriendshipErrorCode;
import com.pd.im.common.exception.ApplicationException;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.service.friendship.dao.ImFriendShipRequestEntity;
import com.pd.im.service.friendship.dao.mapper.ImFriendShipRequestMapper;
import com.pd.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.pd.im.service.friendship.model.req.FriendDto;
import com.pd.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.pd.im.service.friendship.service.ImFriendService;
import com.pd.im.service.friendship.service.ImFriendShipRequestService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.utils.MessageProducer;
import com.pd.im.service.utils.UserSequenceRepository;
import com.pd.im.service.user.model.req.GetUserInfoReq;
import com.pd.im.service.user.model.resp.GetUserInfoResp;
import com.pd.im.service.user.model.resp.ImUserDataVO;
import com.pd.im.service.user.service.ImUserService;
import com.pd.im.service.friendship.model.resp.ImFriendShipRequestDto;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.pd.im.common.model.SyncRequest;
import com.pd.im.common.model.SyncResponse;
import com.pd.im.common.config.AppConfig;
import org.springframework.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Parker
 * @date 12/9/25
 */
@Slf4j
@Service
public class ImFriendShipRequestServiceImpl implements ImFriendShipRequestService {

  @Autowired
  ImFriendShipRequestMapper imFriendShipRequestMapper;

  @Autowired
  ImFriendService imFriendShipService;

  @Autowired
  MessageProducer messageProducer;

  @Autowired
  RedisSequence redisSequence;

  @Autowired
  UserSequenceRepository userSequenceRepository;

  @Autowired
  ImUserService imUserService;

  @Autowired
  AppConfig appConfig;

  /**
   * 发送好友申请逻辑 包含：自添加校验、双向互加检测（双向奔赴）、申请记录维护、多端同步通知
   */
  @Override
  @Transactional
  public ResponseVO<?> addFriendshipRequest(RequestBase requestBase, String fromId, FriendDto dto,
      Integer appId) {
    // 1. 兜底校验：禁止添加自己为好友
    if (fromId.equals(dto.getToId())) {
      return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR.getCode(),
          "不能添加自己为好友");
    }

    // 2. 核心优化：检测“双向互加”场景
    // 如果 A 之前申请过 B，此时 B 也主动加 A，则不再创建新申请，而是直接通过 doAddFriend 建立好友关系。
    LambdaQueryWrapper<ImFriendShipRequestEntity> reverseQuery = new LambdaQueryWrapper<>();
    reverseQuery.eq(ImFriendShipRequestEntity::getAppId, appId);
    reverseQuery.eq(ImFriendShipRequestEntity::getFromId, dto.getToId());
    reverseQuery.eq(ImFriendShipRequestEntity::getToId, fromId);
    reverseQuery.eq(ImFriendShipRequestEntity::getApproveStatus,
        FriendRequestApprovalStatus.NORMAL.getCode());
    ImFriendShipRequestEntity reverseRequest = imFriendShipRequestMapper.selectOne(reverseQuery);

    // 获取好友申请 Timeline 的序列号
    long seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FRIENDSHIP_REQUEST);

    if (reverseRequest != null) {
      // “双向奔赴”：直接尝试建立好友
      log.info("检测到双向申请: fromId={}, toId={}, 尝试直接建立好友关系", fromId, dto.getToId());
      ResponseVO<?> responseVO = imFriendShipService.doAddFriend(requestBase, fromId, dto, appId);

      if (responseVO.isSuccess()) {
        // 如果建立好友成功，则同步更新对方的申请状态为“已同意”
        reverseRequest.setApproveStatus(FriendRequestApprovalStatus.AGREE.getCode());
        reverseRequest.setUpdateTime(System.currentTimeMillis());
        reverseRequest.setSequence(seq);
        imFriendShipRequestMapper.updateById(reverseRequest);

        // 发送序列号更新
        userSequenceRepository.writeUserSeq(appId, fromId,
            Constants.SeqConstants.FRIENDSHIP_REQUEST, seq);

        // 分别为申请人和审批人组装“对端资料”快照的通知包
        FriendRequestNotifyPack approvePackForFrom = buildFriendRequestNotifyPack(
            reverseRequest, seq, appId, reverseRequest.getFromId());
        FriendRequestNotifyPack approvePackForTo = buildFriendRequestNotifyPack(
            reverseRequest, seq, appId, reverseRequest.getToId());

        // 更新双方的序列号 (Timeline 并行)
        userSequenceRepository.writeUserSeq(appId, fromId,
            Constants.SeqConstants.FRIENDSHIP_REQUEST, seq);
        userSequenceRepository.writeUserSeq(appId, dto.getToId(),
            Constants.SeqConstants.FRIENDSHIP_REQUEST, seq);

        // 发送给处理人（同意者）
        messageProducer.sendToAllClients(reverseRequest.getToId(),
            FriendshipEventCommand.FRIEND_REQUEST_APPROVE, approvePackForTo, appId);
        // 发送给申请人
        messageProducer.sendToAllClients(reverseRequest.getFromId(),
            FriendshipEventCommand.FRIEND_REQUEST_APPROVE, approvePackForFrom, appId);
      }
      return responseVO;
    }

    // 3. 正常申请流程
    LambdaQueryWrapper<ImFriendShipRequestEntity> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(ImFriendShipRequestEntity::getAppId, appId);
    queryWrapper.eq(ImFriendShipRequestEntity::getFromId, fromId);
    queryWrapper.eq(ImFriendShipRequestEntity::getToId, dto.getToId());
    ImFriendShipRequestEntity request = imFriendShipRequestMapper.selectOne(queryWrapper);

    if (request == null) {
      request = new ImFriendShipRequestEntity();
      request.setAppId(appId);
      request.setFromId(fromId);
      request.setToId(dto.getToId());
      request.setApproveStatus(FriendRequestApprovalStatus.NORMAL.getCode());
      request.setCreateTime(System.currentTimeMillis());
    }

    // 更新/设置内容
    if (StringUtils.isNotBlank(dto.getAddSource())) {
      request.setAddSource(dto.getAddSource());
    }
    if (StringUtils.isNotBlank(dto.getRemark())) {
      request.setRemark(dto.getRemark());
    }
    if (StringUtils.isNotBlank(dto.getAddWording())) {
      request.setAddWording(dto.getAddWording());
    }
    request.setSequence(seq);
    request.setApproveStatus(FriendRequestApprovalStatus.NORMAL.getCode());
    request.setUpdateTime(System.currentTimeMillis());

    if (request.getId() == null) {
      imFriendShipRequestMapper.insert(request);
    } else {
      imFriendShipRequestMapper.updateById(request);
    }

    // 使用 JIT (Just-In-Time) 模式组装统一通知包 (针对对端进行资料补偿)
    FriendRequestNotifyPack packForTo = buildFriendRequestNotifyPack(request, seq, appId,
        request.getToId());
    FriendRequestNotifyPack packForFrom = buildFriendRequestNotifyPack(request, seq, appId,
        request.getFromId());

    // 更新接收方的序列号
    userSequenceRepository.writeUserSeq(appId, dto.getToId(),
        Constants.SeqConstants.FRIENDSHIP_REQUEST, seq);

    // 1. 发送给接收方的所有端
    messageProducer.sendToAllClients(dto.getToId(), FriendshipEventCommand.FRIEND_REQUEST,
        packForTo, appId);

    // 2. 发送给发起方的所有端（多端同步：告知其它端我也发了申请）
    messageProducer.sendToOtherClients(fromId, FriendshipEventCommand.FRIEND_REQUEST, packForFrom,
        new ClientInfo(appId, requestBase.getClientType(), requestBase.getImei()));

    return ResponseVO.successResponse();
  }

  @Override
  @Transactional
  public ResponseVO<?> approveFriendRequest(ApproveFriendRequestReq req) {
    ImFriendShipRequestEntity imFriendShipRequestEntity = imFriendShipRequestMapper.selectById(
        req.getId());

    if (imFriendShipRequestEntity == null) {
      throw new ApplicationException(FriendshipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
    }

    if (!FriendRequestApprovalStatus.NORMAL.isMe(imFriendShipRequestEntity.getApproveStatus())) {
      // 如果已经是同意或拒绝状态，原则上就不再重复处理，除非是“由拒绝转同意”
      if (imFriendShipRequestEntity.getApproveStatus().equals(req.getStatus())) {
        return ResponseVO.successResponse(); // 状态一致，幂等返回
      }
    }

    long seq = redisSequence.doGetSeq(
        req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP_REQUEST);

    if (FriendRequestApprovalStatus.AGREE.isMe(req.getStatus())) {
      //同意 ===> 去执行添加好友逻辑
      FriendDto dto = new FriendDto();
      dto.setAddSource(imFriendShipRequestEntity.getAddSource());
      dto.setAddWording(imFriendShipRequestEntity.getAddWording());
      dto.setRemark(imFriendShipRequestEntity.getRemark());
      dto.setToId(imFriendShipRequestEntity.getToId());
      ResponseVO<?> responseVO = imFriendShipService.doAddFriend(req,
          imFriendShipRequestEntity.getFromId(), dto, req.getAppId());

      if (!responseVO.isSuccess()
          && responseVO.getCode() != FriendshipErrorCode.TO_IS_YOUR_FRIEND.getCode()) {
        // 如果添加好友失败（如黑名单拦截），则不更新申请状态，直接返回错误
        return responseVO;
      }
    }

    // 执行到这里说明：要么是拒绝，要么是同意且添加好友成功
    imFriendShipRequestEntity.setApproveStatus(req.getStatus());
    imFriendShipRequestEntity.setUpdateTime(System.currentTimeMillis());
    imFriendShipRequestEntity.setSequence(seq);
    imFriendShipRequestMapper.updateById(imFriendShipRequestEntity);

    // 审批通过/拒绝后，也要通知发起方（即申请人）
    // 为双方生成包含“对端资料”的通知包
    FriendRequestNotifyPack approvePackForTo = buildFriendRequestNotifyPack(
        imFriendShipRequestEntity, seq, req.getAppId(), imFriendShipRequestEntity.getToId());
    FriendRequestNotifyPack approvePackForFrom = buildFriendRequestNotifyPack(
        imFriendShipRequestEntity, seq, req.getAppId(), imFriendShipRequestEntity.getFromId());

    // 为操作者（审批人）记录序列号 (多端同步：我的申请已读/已处理)
    userSequenceRepository.writeUserSeq(req.getAppId(), req.getOperator(),
        Constants.SeqConstants.FRIENDSHIP_REQUEST,
        seq);

    // 为申请人（发起方）记录序列号 (离线拉取：我的申请被同意/拒绝了)
    userSequenceRepository.writeUserSeq(req.getAppId(), imFriendShipRequestEntity.getFromId(),
        Constants.SeqConstants.FRIENDSHIP_REQUEST,
        seq);

    messageProducer.sendToOtherClients(imFriendShipRequestEntity.getToId(),
        FriendshipEventCommand.FRIEND_REQUEST_APPROVE, approvePackForTo,
        new ClientInfo(req.getAppId(),
            req.getClientType(), req.getImei()));

    messageProducer.sendToAllClients(imFriendShipRequestEntity.getFromId(),
        FriendshipEventCommand.FRIEND_REQUEST_APPROVE, approvePackForFrom, req.getAppId());
    return ResponseVO.successResponse();
  }

  /**
   * 将好友申请标记为已读
   */
  @Override
  @Transactional
  public ResponseVO<?> readFriendShipRequestReq(ReadFriendShipRequestReq req) {
    // 1. 确定本次要标记的“已读水位线”
    Long readSeq = req.getSequence();
    if (readSeq == null || readSeq <= 0) {
      // 兜底：如果客户端没传，则取当前模块的最大序列号（这是全读模式）
      readSeq = imFriendShipRequestMapper.getFriendShipRequestMaxSeq(req.getAppId(),
          req.getFromId());
    }

    // 2. 获取旧水位线，防止水位线倒退
    Long oldSeq = userSequenceRepository.getUserSeq(req.getAppId(), req.getFromId(),
        Constants.SeqConstants.FRIENDSHIP_REQUEST_READ);
    if (readSeq <= oldSeq) {
      return ResponseVO.successResponse(); // 已经是读过的位置，直接返回
    }

    // 3. 更新用户的“已读水位线”
    userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
        Constants.SeqConstants.FRIENDSHIP_REQUEST_READ,
        readSeq);

    // 4. TCP 通知：告知其它端水位线已更新（多端同步）
    ReadAllFriendRequestNotifyPack readPack = ReadAllFriendRequestNotifyPack.builder()
        .fromId(req.getFromId())
        .sequence(readSeq)
        .build();

    messageProducer.sendToOtherClients(req.getFromId(), FriendshipEventCommand.FRIEND_REQUEST_READ,
        readPack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

    return ResponseVO.successResponse();
  }

  /**
   * 获取用户接收到的好友申请列表
   */
  @Override
  public ResponseVO<List<ImFriendShipRequestEntity>> getFriendRequest(String fromId,
      Integer appId) {
    LambdaQueryWrapper<ImFriendShipRequestEntity> query = new LambdaQueryWrapper<>();
    query.eq(ImFriendShipRequestEntity::getAppId, appId);
    query.eq(ImFriendShipRequestEntity::getToId, fromId);

    List<ImFriendShipRequestEntity> requestList = imFriendShipRequestMapper.selectList(query);

    return ResponseVO.successResponse(requestList);
  }

  @Override
  public ResponseVO<?> syncFriendshipRequestList(SyncRequest req) {
    // 1. 限制单次拉取数量
    if (req.getMaxLimit() > appConfig.getFriendShipRequestMaxCount()) {
      req.setMaxLimit(appConfig.getFriendShipRequestMaxCount());
    }

    SyncResponse<ImFriendShipRequestDto> resp = new SyncResponse<>();
    List<ImFriendShipRequestEntity> list;

    // 2. 获取当前系统最新序号
    Long currentMaxSeq = imFriendShipRequestMapper.getFriendShipRequestMaxSeq(req.getAppId(),
        req.getOperator());

    // 3. 判断同步模式：全量快照(0或Gap过大) vs 增量补齐
    // 定义 Gap 阈值（工业级实践通常为 100-200），超过此值认为增量补齐成本过高
    boolean isSnapshotMode =
        req.getLastSequence() == 0 || (currentMaxSeq - req.getLastSequence() > 100);

    if (isSnapshotMode) {
      // --- 快照模式 (Snapshot Mode) ---
      // 解决新登录或长期下线导致的拉取积压

      // 1. 获取所有待处理 (业务核心待办，不可遗漏)
      LambdaQueryWrapper<ImFriendShipRequestEntity> pendingQuery = new LambdaQueryWrapper<>();
      pendingQuery.eq(ImFriendShipRequestEntity::getAppId, req.getAppId())
          .and(wq -> wq.eq(ImFriendShipRequestEntity::getToId, req.getOperator())
              .or()
              .eq(ImFriendShipRequestEntity::getFromId, req.getOperator()))
          .eq(ImFriendShipRequestEntity::getApproveStatus,
              FriendRequestApprovalStatus.NORMAL.getCode()); // 严格等于 0
      List<ImFriendShipRequestEntity> pendingList = imFriendShipRequestMapper.selectList(
          pendingQuery);

      // 2. 获取最近 N 条已处理 (用于回顾近期动态)
      LambdaQueryWrapper<ImFriendShipRequestEntity> handledQuery = new LambdaQueryWrapper<>();
      handledQuery.eq(ImFriendShipRequestEntity::getAppId, req.getAppId())
          .and(wq -> wq.eq(ImFriendShipRequestEntity::getToId, req.getOperator())
              .or()
              .eq(ImFriendShipRequestEntity::getFromId, req.getOperator()))
          .ne(ImFriendShipRequestEntity::getApproveStatus,
              FriendRequestApprovalStatus.NORMAL.getCode()) // 严格不等于 0
          .orderByDesc(ImFriendShipRequestEntity::getSequence)
          .last("limit " + req.getMaxLimit());
      List<ImFriendShipRequestEntity> handledList = imFriendShipRequestMapper.selectList(
          handledQuery);
      // 3. 聚合并去重
      LinkedHashSet<ImFriendShipRequestEntity> combinedSet = new LinkedHashSet<>(pendingList);
      combinedSet.addAll(handledList);

      // 4. 对快照结果进行最终排序 (统一按 sequence 正序 ASC 返回)
      // 理由：增量同步和快照同步在协议层保持一致的“正序”流，更方便 SDK 进行游标更新和去重处理。
      // 提示：客户端 UI 展示时，请自行在本地使用 ORDER BY sequence DESC 进行倒序展示。
      list = combinedSet.stream()
          .sorted(Comparator.comparing(ImFriendShipRequestEntity::getSequence))
          .collect(Collectors.toList());
    } else {
      // --- 增量模式 (Incremental Mode) ---
      // 标准 Timeline 补齐逻辑: sequence > lastSequence ASC
      LambdaQueryWrapper<ImFriendShipRequestEntity> query = new LambdaQueryWrapper<>();
      query.eq(ImFriendShipRequestEntity::getAppId, req.getAppId())
          .and(wq -> wq.eq(ImFriendShipRequestEntity::getToId, req.getOperator())
              .or()
              .eq(ImFriendShipRequestEntity::getFromId, req.getOperator()))
          .gt(ImFriendShipRequestEntity::getSequence, req.getLastSequence())
          .orderByAsc(ImFriendShipRequestEntity::getSequence)
          .last("limit " + req.getMaxLimit());
      list = imFriendShipRequestMapper.selectList(query);
    }

    if (!CollectionUtils.isEmpty(list)) {
      // 3. 批量获取申请涉及的“对方”（Counterparty）基本信息
      // 逻辑：如果我是发送者，对方是 toId；如果我是接收者，对方是 fromId。
      Set<String> counterpartyIds = list.stream()
          .map(entity -> entity.getFromId().equals(req.getOperator()) ? entity.getToId()
              : entity.getFromId())
          .collect(Collectors.toSet());

      GetUserInfoReq userInfoReq = new GetUserInfoReq();
      userInfoReq.setUserIds(new ArrayList<>(counterpartyIds));
      userInfoReq.setAppId(req.getAppId());
      ResponseVO<GetUserInfoResp> userInfoRes = imUserService.getUserInfo(userInfoReq);

      List<ImUserDataVO> userDataItems = new ArrayList<>();
      if (userInfoRes.isSuccess() && userInfoRes.getData() != null) {
        userDataItems = userInfoRes.getData().getUserDataItem();
      }
      Map<String, ImUserDataVO> userMap = userDataItems.stream()
          .collect(Collectors.toMap(ImUserDataVO::getUserId, u -> u));

      // 4. 封装成带有对端用户信息的 DTO 列表返回
      List<ImFriendShipRequestDto> dtoList = list.stream().map(entity -> {
        ImFriendShipRequestDto dto = new ImFriendShipRequestDto();
        BeanUtils.copyProperties(entity, dto);

        // 确定对方 ID
        String counterpartyId =
            entity.getFromId().equals(req.getOperator()) ? entity.getToId() : entity.getFromId();
        ImUserDataVO user = userMap.get(counterpartyId);

        if (user != null) {
          dto.setUserProfile(UserSnapshotPack.builder()
              .nickName(user.getNickName())
              .faceUrl(user.getFaceUrl())
              .gender(user.getGender())
              .build());
        }
        return dto;
      }).collect(Collectors.toList());

      // 获取数据库中最新的序列号
      Long maxSeq = imFriendShipRequestMapper.getFriendShipRequestMaxSeq(req.getAppId(),
          req.getOperator());

      // 获取用户已读的水位线
      Long readSeq = userSequenceRepository.getUserSeq(req.getAppId(), req.getOperator(),
          Constants.SeqConstants.FRIENDSHIP_REQUEST_READ);

      resp.setDataList(dtoList);
      resp.setMaxSequence(maxSeq);

      // 使用 extras 扩展字段下发游标，保持 SyncResponse 的通用性
      Map<String, Object> extras = new HashMap<>();
      extras.put(Constants.SeqConstants.FRIENDSHIP_REQUEST_READ, readSeq);
      resp.setExtras(extras);

      // 5. 判定同步是否“完成” (数据已覆盖到最新游标)
      // 无论是快照同步（取最新 N 条）还是增量补齐（按 sequence 截取），
      // 只要结果集中的最大序列号达到或超过了服务器全局 maxSeq，则视为已完成。
      ImFriendShipRequestEntity lastEntity = list.get(list.size() - 1);
      resp.setCompleted(lastEntity.getSequence() >= maxSeq);
    } else {
      // 没有增量数据
      resp.setCompleted(true);
    }

    return ResponseVO.successResponse(resp);
  }

  /**
   * JIT (Just-In-Time) 模式组装统一的好友申请通知包 从 User Service 实时抓取最新的昵称、头像、性别，确保“胖通知”包含完整快照
   */
  private FriendRequestNotifyPack buildFriendRequestNotifyPack(ImFriendShipRequestEntity request,
      long seq, Integer appId, String targetUserId) {
    FriendRequestNotifyPack.FriendRequestNotifyPackBuilder builder = FriendRequestNotifyPack.builder()
        .id(request.getId())
        .fromId(request.getFromId())
        .toId(request.getToId())
        .addSource(request.getAddSource() != null ? request.getAddSource() : "")
        .addWording(request.getAddWording() != null ? request.getAddWording() : "")
        .approveStatus(request.getApproveStatus())
        .createTime(request.getCreateTime())
        .updateTime(request.getUpdateTime())
        .sequence(seq);

    // 确定对方（Counterparty）ID：相对于接收消息的 targetUserId 而言
    String counterpartyId =
        request.getFromId().equals(targetUserId) ? request.getToId() : request.getFromId();

    GetUserInfoReq userInfoReq = new GetUserInfoReq();
    userInfoReq.setUserIds(List.of(counterpartyId));
    userInfoReq.setAppId(appId);
    ResponseVO<GetUserInfoResp> userInfoRes = imUserService.getUserInfo(userInfoReq);

    if (userInfoRes.isSuccess() && userInfoRes.getData() != null && !userInfoRes.getData()
        .getUserDataItem().isEmpty()) {
      ImUserDataVO user = userInfoRes.getData().getUserDataItem().get(0);
      builder.userProfile(UserSnapshotPack.builder()
          .nickName(user.getNickName() != null ? user.getNickName() : "")
          .faceUrl(user.getFaceUrl() != null ? user.getFaceUrl() : "")
          .gender(user.getGender() != null ? user.getGender() : 0)
          .build());
    }

    return builder.build();
  }
}
