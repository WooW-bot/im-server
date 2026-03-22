package com.pd.im.service.friendship.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.pd.im.codec.pack.friendship.*;
import com.pd.im.common.enums.BaseErrorCode;
import com.pd.im.codec.pack.user.UserSnapshotPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.FriendshipEventCommand;
import com.pd.im.common.enums.friend.AllowType;
import com.pd.im.common.enums.friend.CheckFriendshipType;
import com.pd.im.common.enums.friend.FriendshipErrorCode;
import com.pd.im.common.enums.friend.FriendshipStatus;
import com.pd.im.common.model.RequestBase;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.common.model.SyncResponse;
import com.pd.im.service.callback.CallbackService;
import com.pd.im.service.friendship.dao.ImFriendShipEntity;
import com.pd.im.service.friendship.dao.mapper.ImFriendShipMapper;
import com.pd.im.service.friendship.model.callback.AddFriendAfterCallbackDto;
import com.pd.im.service.friendship.model.callback.AddFriendBlackAfterCallbackDto;
import com.pd.im.service.friendship.model.callback.DeleteFriendAfterCallbackDto;
import com.pd.im.service.friendship.model.req.*;
import com.pd.im.service.friendship.model.resp.*;
import com.pd.im.service.friendship.service.ImFriendService;
import com.pd.im.service.friendship.service.ImFriendShipRequestService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.user.dao.ImUserDataEntity;
import com.pd.im.common.model.UserProfileDto;
import com.pd.im.service.user.model.req.GetUserInfoReq;
import com.pd.im.service.user.model.resp.GetUserInfoResp;
import com.pd.im.service.user.model.resp.ImUserDataVO;
import com.pd.im.service.user.service.ImUserService;
import com.pd.im.service.utils.MessageProducer;
import com.pd.im.service.utils.UserSequenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IM好友服务实现类
 * <p>
 * 腾讯云好友管理设计：https://cloud.tencent.com/document/product/269/1643
 *
 * @author Parker
 * @date 12/7/25
 */
@Service
@Slf4j
public class ImFriendServiceImpl implements ImFriendService {

  @Autowired
  private ImFriendShipMapper imFriendShipMapper;

  @Autowired
  private ImUserService imUserService;

  @Autowired
  private AppConfig appConfig;

  @Autowired
  private CallbackService callbackService;

  @Autowired
  private MessageProducer messageProducer;

  @Autowired
  private ImFriendShipRequestService imFriendShipRequestService;

  @Autowired
  private RedisSequence redisSequence;

  @Autowired
  private UserSequenceRepository userSequenceRepository;

  @Override
  @Transactional
  public ResponseVO<?> importFriendShip(ImportFriendShipReq req) {
    // 检查批量导入数量限制
    if (req.getFriendItem().size() > appConfig.getFriendShipMaxImportSize()) {
      return ResponseVO.errorResponse(FriendshipErrorCode.OP_SIZE_BEYOND);
    }

    // 验证发起方用户是否存在
    ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(),
        req.getAppId());
    if (!fromInfo.isSuccess()) {
      return ResponseVO.errorResponse(fromInfo.getCode(), fromInfo.getMsg());
    }

    ImportFriendShipResp resp = new ImportFriendShipResp();
    List<String> successIds = new ArrayList<>();
    List<String> errorIds = new ArrayList<>();

    for (ImportFriendShipReq.ImportFriendDto dto : req.getFriendItem()) {
      try {
        // 验证目标用户是否存在
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(dto.getToId(),
            req.getAppId());
        if (!toInfo.isSuccess()) {
          log.warn("导入好友失败: 目标用户不存在, toId={}", dto.getToId());
          errorIds.add(dto.getToId());
          continue;
        }

        // 生成全局递增序列号，确保导入数据的 Timeline 记录一致性
        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);

        // ========== 处理 A -> B 的好友关系 ==========
        LambdaQueryWrapper<ImFriendShipEntity> fromQuery = buildFriendshipQuery(req.getAppId(),
            req.getFromId(),
            dto.getToId());
        ImFriendShipEntity fromEntity = imFriendShipMapper.selectOne(fromQuery);

        if (fromEntity == null) {
          // 不存在，新增
          fromEntity = new ImFriendShipEntity();
          fromEntity.setAppId(req.getAppId());
          fromEntity.setFromId(req.getFromId());
          fromEntity.setToId(dto.getToId());
          fromEntity.setRemark(dto.getRemark());
          fromEntity.setAddSource(dto.getAddSource());
          fromEntity.setStatus(dto.getStatus());
          fromEntity.setBlack(dto.getBlack());
          fromEntity.setFriendSequence(seq);
          fromEntity.setCreateTime(System.currentTimeMillis());

          int insert = imFriendShipMapper.insert(fromEntity);
          if (insert != 1) {
            errorIds.add(dto.getToId());
            continue;
          }
          userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
              Constants.SeqConstants.FRIENDSHIP, seq);
        } else {
          // 已存在，更新（导入操作通常会覆盖原有数据）
          ImFriendShipEntity update = new ImFriendShipEntity();
          if (StringUtils.isNotBlank(dto.getRemark())) {
            update.setRemark(dto.getRemark());
          }
          if (StringUtils.isNotBlank(dto.getAddSource())) {
            update.setAddSource(dto.getAddSource());
          }
          if (dto.getStatus() != null) {
            update.setStatus(dto.getStatus());
          }
          if (dto.getBlack() != null) {
            update.setBlack(dto.getBlack());
          }
          update.setFriendSequence(seq);

          int result = imFriendShipMapper.update(update, fromQuery);
          if (result != 1) {
            errorIds.add(dto.getToId());
            continue;
          }
          userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
              Constants.SeqConstants.FRIENDSHIP, seq);
        }

        // ========== 处理 B -> A 的反向好友关系（建立双向关系） ==========
        LambdaQueryWrapper<ImFriendShipEntity> toQuery = buildFriendshipQuery(req.getAppId(),
            dto.getToId(),
            req.getFromId());
        ImFriendShipEntity toEntity = imFriendShipMapper.selectOne(toQuery);

        if (toEntity == null) {
          // 不存在，新增反向关系（最小字段）
          toEntity = new ImFriendShipEntity();
          toEntity.setAppId(req.getAppId());
          toEntity.setFromId(dto.getToId());
          toEntity.setToId(req.getFromId());
          toEntity.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
          toEntity.setBlack(FriendshipStatus.BLACK_STATUS_NORMAL.getCode());
          toEntity.setFriendSequence(seq);
          toEntity.setCreateTime(System.currentTimeMillis());

          imFriendShipMapper.insert(toEntity);
          userSequenceRepository.writeUserSeq(req.getAppId(), dto.getToId(),
              Constants.SeqConstants.FRIENDSHIP, seq);
        } else {
          // 已存在，仅更新序列号和状态（确保是正常状态）
          ImFriendShipEntity update = new ImFriendShipEntity();
          update.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
          update.setFriendSequence(seq);

          imFriendShipMapper.update(update, toQuery);
          userSequenceRepository.writeUserSeq(req.getAppId(), dto.getToId(),
              Constants.SeqConstants.FRIENDSHIP, seq);
        }

        successIds.add(dto.getToId());

      } catch (Exception e) {
        log.error("导入好友关系失败: toId={}", dto.getToId(), e);
        errorIds.add(dto.getToId());
      }
    }

    resp.setErrorId(errorIds);
    resp.setSuccessId(successIds);

    return ResponseVO.successResponse(resp);
  }

  /**
   * 添加好友入口 逻辑步骤： 1. 参数与权限准备 2. 基础校验（自添加、用户存在性、Callback预检查） 3. 目标用户隐私设置校验 (AllowType) 4.
   * 黑名单关系校验（互相拉黑拦截） 5. 根据验证方式路由：直接添加 (doAddFriend) 或 发送申请 (addFriendshipRequest)
   */
  @Override
  public ResponseVO<?> addFriend(AddFriendReq req) {
    // 1. 权限校验：操作人必须是发起人（或管理员，此处简化为发起人）
    if (StringUtils.isNotBlank(req.getOperator()) && !req.getOperator().equals(req.getFromId())) {
      return ResponseVO.errorResponse(BaseErrorCode.PARAMETER_ERROR.getCode(), "操作人权限不足");
    }

    // 2. 校验点：禁止添加自己为好友
    if (req.getFromId().equals(req.getToItem().getToId())) {
      return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR.getCode(),
          "不能添加自己为好友");
    }

    // 校验发起方用户是否存在
    ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(),
        req.getAppId());
    if (!fromInfo.isSuccess()) {
      return ResponseVO.errorResponse(fromInfo.getCode(), fromInfo.getMsg());
    }

    // 校验目标方用户是否存在
    ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToItem().getToId(),
        req.getAppId());
    if (!toInfo.isSuccess()) {
      return ResponseVO.errorResponse(toInfo.getCode(), toInfo.getMsg());
    }

    // 添加好友前回调(是否允许添加好友动作)
    if (appConfig.isAddFriendBeforeCallback()) {
      ResponseVO callbackResp = callbackService.beforeCallback(req.getAppId(),
          Constants.CallbackCommand.ADD_FRIEND_BEFORE, JSONObject.toJSONString(req));
      if (!callbackResp.isSuccess()) {
        return ResponseVO.errorResponse(callbackResp.getCode(), callbackResp.getMsg());
      }
    }

    ImUserDataEntity toUser = toInfo.getData();

    if (AllowType.DENY_ANY.isMe(toUser.getAllowType())) {
      // 拒绝任何人添加
      return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_SHIP_REQUEST_REFUSED);
    }

    // 2. 优化：合并查询双向好友关系（及互相拉黑状态）
    // 这样做可以减少一次数据库查询，同时在内存中判断更加高效。
    LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
    query.eq(ImFriendShipEntity::getAppId, req.getAppId())
        .and(wrapper -> wrapper
            .and(w1 -> w1.eq(ImFriendShipEntity::getFromId, req.getFromId())
                .eq(ImFriendShipEntity::getToId, req.getToItem().getToId()))
            .or(w2 -> w2.eq(ImFriendShipEntity::getFromId, req.getToItem().getToId())
                .eq(ImFriendShipEntity::getToId, req.getFromId()))
        );
    List<ImFriendShipEntity> friendshipList = imFriendShipMapper.selectList(query);

    // 分离出 A->B 和 B->A 的记录
    ImFriendShipEntity fromItem = friendshipList.stream()
        .filter(i -> i.getFromId().equals(req.getFromId()))
        .findFirst().orElse(null);
    ImFriendShipEntity toItem = friendshipList.stream()
        .filter(i -> i.getFromId().equals(req.getToItem().getToId()))
        .findFirst().orElse(null);

    // 3. 校验点：黑名单双向拦截
    // 规则：只要有一方把对方拉黑了，就不能直接跨过申请流程（或直接建立好友关系）
    if (toItem != null && FriendshipStatus.BLACK_STATUS_BLACKED.isMe(toItem.getBlack())) {
      return ResponseVO.errorResponse(FriendshipErrorCode.TARGET_IS_BLACK_YOU);
    }
    if (fromItem != null && FriendshipStatus.BLACK_STATUS_BLACKED.isMe(fromItem.getBlack())) {
      return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_BLACK);
    }

    // 处理“允许任何人添加”的情况
    if (AllowType.ALLOW_ANY.isMe(toUser.getAllowType())) {
      // 允许任何人添加,直接添加好友
      return doAddFriend(req, req.getFromId(), req.getToItem(), req.getAppId());
    }

    // 需要验证, 检查双向好友关系状态
    boolean fromIsNormal =
        fromItem != null && FriendshipStatus.FRIEND_STATUS_NORMAL.isMe(fromItem.getStatus());
    boolean toIsNormal =
        toItem != null && FriendshipStatus.FRIEND_STATUS_NORMAL.isMe(toItem.getStatus());

    if (fromIsNormal && toIsNormal) {
      // 双向都是好友,返回已经是好友错误
      return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_YOUR_FRIEND);
    }

    // 其他情况(单向好友或无好友关系),创建好友申请
    return imFriendShipRequestService.addFriendshipRequest(req, req.getFromId(), req.getToItem(),
        req.getAppId());
  }

  /**
   * 执行实际的物理/逻辑好友添加动作 关键点：双向关系同步更新、Timeline序列号一致性、TCP多端通知、操作后回调
   */
  @Override
  @Transactional
  public ResponseVO<?> doAddFriend(RequestBase requestBase, String fromId, FriendDto dto,
      Integer appId) {
    // 1. 优化：合并查询双向好友关系
    LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
    query.eq(ImFriendShipEntity::getAppId, appId)
        .and(wrapper -> wrapper
            .and(w1 -> w1.eq(ImFriendShipEntity::getFromId, fromId)
                .eq(ImFriendShipEntity::getToId, dto.getToId()))
            .or(w2 -> w2.eq(ImFriendShipEntity::getFromId, dto.getToId())
                .eq(ImFriendShipEntity::getToId, fromId))
        );
    List<ImFriendShipEntity> friendshipList = imFriendShipMapper.selectList(query);

    ImFriendShipEntity fromItem = friendshipList.stream()
        .filter(i -> i.getFromId().equals(fromId))
        .findFirst().orElse(null);
    ImFriendShipEntity toItem = friendshipList.stream()
        .filter(i -> i.getFromId().equals(dto.getToId()))
        .findFirst().orElse(null);

    // 如果该接口被单独调用（如审批通过时调用），需要进行兜底黑名单校验
    // A拉黑了B，B审批通过了A的好友请求，此时应该提示“好友已被拉黑”
    if (fromItem != null && FriendshipStatus.BLACK_STATUS_BLACKED.getCode()
        .equals(fromItem.getBlack())) {
      return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_BLACK);
    }
    if (toItem != null && FriendshipStatus.BLACK_STATUS_BLACKED.getCode()
        .equals(toItem.getBlack())) {
      return ResponseVO.errorResponse(FriendshipErrorCode.TARGET_IS_BLACK_YOU);
    }

    // 检查双向关系状态
    boolean fromIsNormal = fromItem != null
        && FriendshipStatus.FRIEND_STATUS_NORMAL.getCode().equals(fromItem.getStatus());
    boolean toIsNormal = toItem != null
        && FriendshipStatus.FRIEND_STATUS_NORMAL.getCode().equals(toItem.getStatus());

    if (fromIsNormal && toIsNormal) {
      // 双向都已经是好友,返回错误
      return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_YOUR_FRIEND);
    }

    // 生成全局递增序列号, 用于双向关系同步
    // 保证 A->B 和 B->A 在这一刻生成的快照序列号是一致的，便于客户端排序和同步
    long seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FRIENDSHIP);

    // ========== 处理 A -> B 的好友关系 ==========
    if (fromItem == null) {
      // 不存在关系,执行新增
      fromItem = new ImFriendShipEntity();
      fromItem.setAppId(appId);
      fromItem.setFromId(fromId);
      fromItem.setFriendSequence(seq);
      BeanUtils.copyProperties(dto, fromItem);
      fromItem.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
      fromItem.setCreateTime(System.currentTimeMillis());

      int insert = imFriendShipMapper.insert(fromItem);
      if (insert != 1) {
        return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR);
      }
      // 写入 A 的 Timeline 序列号
      userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FRIENDSHIP, seq);
    } else {
      // 已存在关系, 更新为正常状态并更新序列号
      ImFriendShipEntity update = new ImFriendShipEntity();
      if (!fromIsNormal) {
        if (StringUtils.isNotBlank(dto.getAddSource())) {
          update.setAddSource(dto.getAddSource());
        }
        if (StringUtils.isNotBlank(dto.getRemark())) {
          update.setRemark(dto.getRemark());
        }
        if (StringUtils.isNotBlank(dto.getExtra())) {
          update.setExtra(dto.getExtra());
        }
        update.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
      }
      update.setFriendSequence(seq);

      LambdaQueryWrapper<ImFriendShipEntity> fromQuery = buildFriendshipQuery(appId, fromId,
          dto.getToId());
      int result = imFriendShipMapper.update(update, fromQuery);
      if (result != 1) {
        return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR);
      }
      userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FRIENDSHIP, seq);
      fromItem.setFriendSequence(seq);
      fromItem.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
    }

    // ========== 处理 B -> A 的好友关系(反向) ==========
    if (toItem == null) {
      // 创建反向关系
      toItem = new ImFriendShipEntity();
      toItem.setAppId(appId);
      toItem.setFromId(dto.getToId());
      toItem.setToId(fromId);
      toItem.setFriendSequence(seq);
      toItem.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
      toItem.setCreateTime(System.currentTimeMillis());
      imFriendShipMapper.insert(toItem);
      // 写入 B 的 Timeline 序列号，确保 B 的设备也能感知到好友增加
      userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FRIENDSHIP,
          seq);
    } else {
      // 已存在关系, 更新为正常状态并更新序列号
      ImFriendShipEntity update = new ImFriendShipEntity();
      if (!toIsNormal) {
        update.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
      }
      update.setFriendSequence(seq);

      LambdaQueryWrapper<ImFriendShipEntity> toQuery = buildFriendshipQuery(appId, dto.getToId(),
          fromId);
      imFriendShipMapper.update(update, toQuery);
      userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FRIENDSHIP,
          seq);
      toItem.setFriendSequence(seq);
      toItem.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
    }

    // ========== 发送TCP通知 ==========
    // 使用 JIT (Just-In-Time) 模式组装通知包快照
    FriendInfoNotifyPack fromPack = buildFriendInfoNotifyPack(fromId, dto.getToId(), fromItem, seq,
        appId);

    if (requestBase != null) {
      messageProducer.sendToClients(fromId, FriendshipEventCommand.FRIEND_ADD, fromPack,
          requestBase.getAppId(), requestBase.getClientType(), requestBase.getImei());
    } else {
      messageProducer.sendToAllClients(fromId, FriendshipEventCommand.FRIEND_ADD, fromPack, appId);
    }

    // 为 toId 端构建通知（带上 fromId 的资料）
    FriendInfoNotifyPack toPack = buildFriendInfoNotifyPack(dto.getToId(), fromId, toItem, seq,
        appId);

    messageProducer.sendToAllClients(dto.getToId(), FriendshipEventCommand.FRIEND_ADD, toPack,
        appId);

    // ========== 添加好友后回调 ==========
    if (appConfig.isAddFriendAfterCallback()) {
      AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
      callbackDto.setFromId(fromId);
      callbackDto.setToItem(dto);
      callbackService.afterCallback(appId, Constants.CallbackCommand.ADD_FRIEND_AFTER,
          JSONObject.toJSONString(callbackDto));
    }

    return ResponseVO.successResponse();
  }

  /**
   * 构建好友关系查询条件
   */
  private LambdaQueryWrapper<ImFriendShipEntity> buildFriendshipQuery(Integer appId, String fromId,
      String toId) {
    LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
    query.eq(ImFriendShipEntity::getAppId, appId)
        .eq(ImFriendShipEntity::getFromId, fromId)
        .eq(ImFriendShipEntity::getToId, toId);
    return query;
  }

  @Override
  @Transactional
  public ResponseVO<?> updateFriend(UpdateFriendReq req) {
    // 校验用户是否存在
    ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(),
        req.getAppId());
    if (!fromInfo.isSuccess()) {
      return ResponseVO.errorResponse(fromInfo.getCode(), fromInfo.getMsg());
    }

    ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToItem().getToId(),
        req.getAppId());
    if (!toInfo.isSuccess()) {
      return ResponseVO.errorResponse(toInfo.getCode(), toInfo.getMsg());
    }

    // 执行更新动作（备注、来源、扩展字段）
    long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);

    UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
    updateWrapper.lambda()
        .set(ImFriendShipEntity::getAddSource, req.getToItem().getAddSource())
        .set(ImFriendShipEntity::getExtra, req.getToItem().getExtra())
        .set(ImFriendShipEntity::getFriendSequence, seq)
        .set(ImFriendShipEntity::getRemark, req.getToItem().getRemark())
        .eq(ImFriendShipEntity::getAppId, req.getAppId())
        .eq(ImFriendShipEntity::getToId, req.getToItem().getToId())
        .eq(ImFriendShipEntity::getFromId, req.getFromId());

    int update = imFriendShipMapper.update(null, updateWrapper);
    if (update != 1) {
      return ResponseVO.errorResponse();
    }
    userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
        Constants.SeqConstants.FRIENDSHIP, seq);

    // 发送TCP通知
    UpdateFriendPack updateFriendPack = new UpdateFriendPack();
    updateFriendPack.setRemark(req.getToItem().getRemark());
    updateFriendPack.setToId(req.getToItem().getToId());
    messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_UPDATE,
        updateFriendPack,
        req.getAppId(), req.getClientType(), req.getImei());

    // 更新好友后回调
    if (appConfig.isModifyFriendAfterCallback()) {
      AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
      callbackDto.setFromId(req.getFromId());
      callbackDto.setToItem(req.getToItem());
      callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.UPDATE_FRIEND_AFTER,
          JSONObject.toJSONString(callbackDto));
    }

    return ResponseVO.successResponse();
  }

  @Override
  @Transactional
  public ResponseVO<?> deleteFriend(DeleteFriendReq req) {
    LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(req.getAppId(),
        req.getFromId(),
        req.getToId());
    ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);

    if (fromItem == null) {
      return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_NOT_YOUR_FRIEND);
    }

    if (fromItem.getStatus() == null
        || !fromItem.getStatus().equals(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode())) {
      return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_DELETED);
    }

    // 执行逻辑删除 (软删除)
    long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);
    ImFriendShipEntity update = new ImFriendShipEntity();
    update.setFriendSequence(seq);
    update.setStatus(FriendshipStatus.FRIEND_STATUS_DELETE.getCode());
    imFriendShipMapper.update(update, query);
    userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
        Constants.SeqConstants.FRIENDSHIP, seq);

    // 发送TCP通知
    DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
    deleteFriendPack.setFromId(req.getFromId());
    deleteFriendPack.setSequence(seq);
    deleteFriendPack.setToId(req.getToId());
    messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_DELETE,
        deleteFriendPack,
        req.getAppId(), req.getClientType(), req.getImei());

    // 删除好友后回调
    if (appConfig.isDeleteFriendAfterCallback()) {
      DeleteFriendAfterCallbackDto callbackDto = new DeleteFriendAfterCallbackDto();
      callbackDto.setFromId(req.getFromId());
      callbackDto.setToId(req.getToId());
      callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.DELETE_FRIEND_AFTER,
          JSONObject.toJSONString(callbackDto));
    }

    return ResponseVO.successResponse();
  }

  @Override
  @Transactional
  public ResponseVO<?> deleteAllFriend(DeleteFriendReq req) {
    LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
    query.eq(ImFriendShipEntity::getAppId, req.getAppId())
        .eq(ImFriendShipEntity::getFromId, req.getFromId())
        .eq(ImFriendShipEntity::getStatus, FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());

    ImFriendShipEntity update = new ImFriendShipEntity();
    update.setStatus(FriendshipStatus.FRIEND_STATUS_DELETE.getCode());
    imFriendShipMapper.update(update, query);

    // 发送TCP通知
    DeleteAllFriendPack deleteFriendPack = new DeleteAllFriendPack();
    deleteFriendPack.setFromId(req.getFromId());
    messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_ALL_DELETE,
        deleteFriendPack,
        req.getAppId(), req.getClientType(), req.getImei());

    return ResponseVO.successResponse();
  }

  @Override
  public ResponseVO<List<ImFriendShipEntity>> getAllFriendShip(GetAllFriendShipReq req) {
    LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
    query.eq(ImFriendShipEntity::getAppId, req.getAppId())
        .eq(ImFriendShipEntity::getFromId, req.getFromId());
    return ResponseVO.successResponse(imFriendShipMapper.selectList(query));
  }

  @Override
  public ResponseVO<List<GetFriendInfoResp>> getFriendsInfo(GetFriendsInfoReq req) {
    log.info("[getFriendsInfo] fromId={}, toIds={}", req.getFromId(), req.getToIds());

    // 限制单次查询数量
    if (req.getToIds().size() > 100) {
      return ResponseVO.errorResponse(FriendshipErrorCode.OP_SIZE_BEYOND);
    }

    // 校验发起方用户是否存在
    ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(),
        req.getAppId());
    if (!fromInfo.isSuccess()) {
      return ResponseVO.errorResponse(fromInfo.getCode(), fromInfo.getMsg());
    }

    // 批量查询好友关系，并为每个 toId 构建结果
    List<GetFriendInfoResp> results = new ArrayList<>();

    for (String toId : req.getToIds()) {
      GetFriendInfoResp result = new GetFriendInfoResp();
      result.setToId(toId);

      LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(
          req.getAppId(), req.getFromId(), toId);
      ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);

      if (entity != null) {
        // 找到好友关系
        result.setResultCode(0);
        result.setResultInfo("成功");
        result.setFriendInfo(entity);
      } else {
        // 未找到好友关系
        result.setResultCode(-1);
        result.setResultInfo("好友关系不存在");
        result.setFriendInfo(null);
      }

      results.add(result);
    }

    log.info("[getFriendsInfo] 返回 {} 个结果，其中 {} 个找到好友关系",
        results.size(),
        results.stream().filter(r -> r.getResultCode() == 0).count());

    return ResponseVO.successResponse(results);
  }

  @Override
  public ResponseVO<ImFriendShipEntity> getRelation(GetRelationReq req) {
    LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(
        req.getAppId(), req.getFromId(), req.getToId());

    ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);
    if (entity == null) {
      return ResponseVO.errorResponse(FriendshipErrorCode.REPEATSHIP_IS_NOT_EXIST);
    }
    return ResponseVO.successResponse(entity);
  }

  @Override
  public ResponseVO<?> checkFriendship(CheckFriendShipReq req) {
    List<CheckFriendShipResp> resp;

    // 根据校验类型调用不同的方法
    if (req.getCheckType() == CheckFriendshipType.SINGLE.getType()) {
      resp = imFriendShipMapper.checkFriendShip(req);
    } else {
      resp = imFriendShipMapper.checkFriendShipBoth(req);
    }

    // 填充缺失的toId,状态设为0(无关系)
    fillMissingToIds(req, resp);

    return ResponseVO.successResponse(resp);
  }

  /**
   * 填充缺失的toId记录 对于没有查询到关系的toId,创建状态为0的记录
   */
  private void fillMissingToIds(CheckFriendShipReq req, List<CheckFriendShipResp> resp) {
    Map<String, Integer> existingToIds = resp.stream()
        .collect(Collectors.toMap(CheckFriendShipResp::getToId, CheckFriendShipResp::getStatus));

    for (String toId : req.getToIds()) {
      if (!existingToIds.containsKey(toId)) {
        CheckFriendShipResp checkResp = new CheckFriendShipResp();
        checkResp.setFromId(req.getFromId());
        checkResp.setToId(toId);

        // 单向校验：0-不是好友，双向校验：4-无关系
        int status = 0;
        if (req.getCheckType() != null
            && req.getCheckType() == CheckFriendshipType.BOTH.getType()) {
          status = 4;
        }
        checkResp.setStatus(status);

        resp.add(checkResp);
      }
    }
  }

  @Override
  @Transactional
  public ResponseVO<?> addBlackList(AddFriendShipBlackReq req) {
    log.info("[addBlackList] fromId={}, toIds={}", req.getFromId(), req.getToIds());

    // 限制单次操作数量
    if (req.getToIds().size() > 100) {
      return ResponseVO.errorResponse(FriendshipErrorCode.OP_SIZE_BEYOND);
    }

    // 校验发起方用户是否存在
    ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(),
        req.getAppId());
    if (!fromInfo.isSuccess()) {
      return ResponseVO.errorResponse(fromInfo.getCode(), fromInfo.getMsg());
    }

    // 批量处理每个用户
    List<BlackListOperationResp> results = new ArrayList<>();

    for (String toId : req.getToIds()) {
      BlackListOperationResp result = new BlackListOperationResp();
      result.setToId(toId);

      try {
        // 校验目标用户是否存在
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
        if (!toInfo.isSuccess()) {
          result.setResultCode(toInfo.getCode());
          result.setResultInfo(toInfo.getMsg());
          results.add(result);
          continue;
        }

        LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(req.getAppId(),
            req.getFromId(),
            toId);
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);
        long seq;

        if (fromItem == null) {
          // 不存在关系,新增并设置为拉黑
          seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);
          fromItem = new ImFriendShipEntity();
          fromItem.setFromId(req.getFromId());
          fromItem.setToId(toId);
          fromItem.setFriendSequence(seq);
          fromItem.setAppId(req.getAppId());
          fromItem.setBlack(FriendshipStatus.BLACK_STATUS_BLACKED.getCode());
          fromItem.setCreateTime(System.currentTimeMillis());
          int insert = imFriendShipMapper.insert(fromItem);
          if (insert != 1) {
            result.setResultCode(FriendshipErrorCode.ADD_FRIEND_ERROR.getCode());
            result.setResultInfo(FriendshipErrorCode.ADD_FRIEND_ERROR.getError());
            results.add(result);
            continue;
          }
          userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
              Constants.SeqConstants.FRIENDSHIP, seq);
        } else {
          // 已存在关系,检查黑名单状态
          if (fromItem.getBlack() != null
              && fromItem.getBlack().equals(FriendshipStatus.BLACK_STATUS_BLACKED.getCode())) {
            result.setResultCode(FriendshipErrorCode.FRIEND_IS_BLACK.getCode());
            result.setResultInfo(FriendshipErrorCode.FRIEND_IS_BLACK.getError());
            results.add(result);
            continue;
          }
          seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);
          ImFriendShipEntity update = new ImFriendShipEntity();
          update.setFriendSequence(seq);
          update.setBlack(FriendshipStatus.BLACK_STATUS_BLACKED.getCode());
          int updateResult = imFriendShipMapper.update(update, query);
          if (updateResult != 1) {
            result.setResultCode(FriendshipErrorCode.ADD_BLACK_ERROR.getCode());
            result.setResultInfo(FriendshipErrorCode.ADD_BLACK_ERROR.getError());
            results.add(result);
            continue;
          }
          userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
              Constants.SeqConstants.FRIENDSHIP, seq);
        }

        // 发送TCP通知
        AddFriendBlackPack pack = new AddFriendBlackPack();
        pack.setFromId(req.getFromId());
        pack.setSequence(seq);
        pack.setToId(toId);
        messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_BLACK_ADD,
            pack,
            req.getAppId(), req.getClientType(), req.getImei());

        // 添加黑名单后回调
        if (appConfig.isAddFriendShipBlackAfterCallback()) {
          AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
          callbackDto.setFromId(req.getFromId());
          callbackDto.setToId(toId);
          callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.ADD_BLACK_AFTER,
              JSONObject.toJSONString(callbackDto));
        }

        result.setResultCode(0);
        result.setResultInfo("成功");
      } catch (Exception e) {
        log.error("[addBlackList] 添加黑名单失败 toId={}", toId, e);
        result.setResultCode(-1);
        result.setResultInfo("操作失败: " + e.getMessage());
      }

      results.add(result);
    }

    log.info("[addBlackList] 完成 - 成功: {}/{}",
        results.stream().filter(r -> r.getResultCode() == 0).count(),
        results.size());

    return ResponseVO.successResponse(results);
  }

  @Override
  @Transactional
  public ResponseVO<?> deleteBlackList(DeleteBlackReq req) {
    log.info("[deleteBlackList] fromId={}, toIds={}", req.getFromId(), req.getToIds());

    // 限制单次操作数量
    if (req.getToIds().size() > 100) {
      return ResponseVO.errorResponse(FriendshipErrorCode.OP_SIZE_BEYOND);
    }

    // 校验发起方用户是否存在
    ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(),
        req.getAppId());
    if (!fromInfo.isSuccess()) {
      return fromInfo;
    }

    // 批量处理每个用户
    List<BlackListOperationResp> results = new ArrayList<>();

    for (String toId : req.getToIds()) {
      BlackListOperationResp result = new BlackListOperationResp();
      result.setToId(toId);

      try {
        LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(req.getAppId(),
            req.getFromId(),
            toId);
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);

        // 检查是否存在且是否已拉黑
        if (fromItem == null || fromItem.getBlack() == null ||
            !fromItem.getBlack().equals(FriendshipStatus.BLACK_STATUS_BLACKED.getCode())) {
          result.setResultCode(FriendshipErrorCode.FRIEND_IS_NOT_YOUR_BLACK.getCode());
          result.setResultInfo(FriendshipErrorCode.FRIEND_IS_NOT_YOUR_BLACK.getError());
          results.add(result);
          continue;
        }

        // 移除黑名单，设置为正常状态
        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);
        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setFriendSequence(seq);
        update.setBlack(FriendshipStatus.BLACK_STATUS_NORMAL.getCode());

        int updateResult = imFriendShipMapper.update(update, query);
        if (updateResult != 1) {
          result.setResultCode(FriendshipErrorCode.ADD_BLACK_ERROR.getCode());
          result.setResultInfo(FriendshipErrorCode.ADD_BLACK_ERROR.getError());
          results.add(result);
          continue;
        }
        userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
            Constants.SeqConstants.FRIENDSHIP,
            seq);

        // 发送TCP通知
        DeleteBlackPack pack = new DeleteBlackPack();
        pack.setFromId(req.getFromId());
        pack.setSequence(seq);
        pack.setToId(toId);
        messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_BLACK_DELETE,
            pack,
            req.getAppId(), req.getClientType(), req.getImei());

        // 删除黑名单后回调
        if (appConfig.isAddFriendShipBlackAfterCallback()) {
          AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
          callbackDto.setFromId(req.getFromId());
          callbackDto.setToId(toId);
          callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.DELETE_BLACK,
              JSONObject.toJSONString(callbackDto));
        }

        result.setResultCode(0);
        result.setResultInfo("成功");
      } catch (Exception e) {
        log.error("[deleteBlackList] 移除黑名单失败 toId={}", toId, e);
        result.setResultCode(-1);
        result.setResultInfo("操作失败: " + e.getMessage());
      }

      results.add(result);
    }

    log.info("[deleteBlackList] 完成 - 成功: {}/{}",
        results.stream().filter(r -> r.getResultCode() == 0).count(),
        results.size());

    return ResponseVO.successResponse(results);
  }

  @Override
  public ResponseVO<?> checkBlackList(CheckFriendShipReq req) {
    List<CheckFriendShipResp> result;

    // 根据校验类型调用不同的方法
    if (req.getCheckType() == CheckFriendshipType.SINGLE.getType()) {
      result = imFriendShipMapper.checkFriendShipBlack(req);
    } else {
      result = imFriendShipMapper.checkFriendShipBlackBoth(req);
    }

    // 填充缺失的toId
    fillMissingToIds(req, result);

    return ResponseVO.successResponse(result);
  }

  @Override
  public ResponseVO<?> syncFriendshipList(SyncRequest req) {
    // 限制单次拉取数量
    if (req.getMaxLimit() > appConfig.getFriendShipMaxCount()) {
      req.setMaxLimit(appConfig.getFriendShipMaxCount());
    }

    SyncResponse<ImFriendDto> resp = new SyncResponse<>();

    // 查询增量数据: sequence > lastSequence
    LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
    query.eq(ImFriendShipEntity::getFromId, req.getOperator())
        .eq(ImFriendShipEntity::getAppId, req.getAppId())
        .gt(ImFriendShipEntity::getFriendSequence, req.getLastSequence())
        .orderByAsc(ImFriendShipEntity::getFriendSequence)
        .last("limit " + req.getMaxLimit());

    List<ImFriendShipEntity> list = imFriendShipMapper.selectList(query);

    if (!CollectionUtils.isEmpty(list)) {
      // 批量获取好友的资料信息
      List<String> toIds = list.stream().map(ImFriendShipEntity::getToId)
          .collect(Collectors.toList());
      GetUserInfoReq userInfoReq = new GetUserInfoReq();
      userInfoReq.setUserIds(toIds);
      userInfoReq.setAppId(req.getAppId());
      ResponseVO<GetUserInfoResp> userInfoResp = imUserService.getUserInfo(userInfoReq);

      Map<String, ImUserDataVO> userMap = new HashMap<>();
      if (userInfoResp.isSuccess() && userInfoResp.getData() != null) {
        userMap = userInfoResp.getData().getUserDataItem().stream()
            .collect(Collectors.toMap(ImUserDataVO::getUserId, u -> u));
      }

      final Map<String, ImUserDataVO> finalUserMap = userMap;
      List<ImFriendDto> dtoList = list.stream().map(entity -> {
        ImFriendDto dto = new ImFriendDto();
        BeanUtils.copyProperties(entity, dto);

        ImUserDataVO userData = finalUserMap.get(entity.getToId());
        if (userData != null) {
          UserProfileDto profile = new UserProfileDto();
          BeanUtils.copyProperties(userData, profile);
          dto.setUserProfile(profile);
        }
        return dto;
      }).collect(Collectors.toList());

      // 获取最大序列号
      Long maxSeq = imFriendShipMapper.getFriendShipMaxSeq(req.getAppId(), req.getOperator());
      ImFriendShipEntity lastEntity = list.get(list.size() - 1);

      resp.setDataList(dtoList);
      resp.setMaxSequence(maxSeq);
      // 判断是否拉取完毕
      resp.setCompleted(lastEntity.getFriendSequence() >= maxSeq);
    } else {
      // 没有增量数据
      resp.setCompleted(true);
    }

    return ResponseVO.successResponse(resp);
  }

  @Override
  public List<String> getFriendIds(String userId, Integer appId) {
    return imFriendShipMapper.getFriendIds(userId, appId);
  }

  /**
   * JIT (Just-In-Time) 模式组装好友信息通知包 从 User Service 实时抓取最新的昵称、头像、性别
   */
  private FriendInfoNotifyPack buildFriendInfoNotifyPack(String fromId, String toId,
      ImFriendShipEntity item, long seq, Integer appId) {
    FriendInfoNotifyPack pack = FriendInfoNotifyPack.builder()
        .appId(appId)
        .fromId(fromId)
        .toId(toId)
        .remark(item.getRemark())
        .status(item.getStatus())
        .black(item.getBlack())
        .createTime(item.getCreateTime())
        .friendSequence(seq)
        .addSource(item.getAddSource())
        .extra(item.getExtra())
        .build();

    ResponseVO<ImUserDataEntity> userInfo = imUserService.getSingleUserInfo(toId, appId);
    if (userInfo.isSuccess()) {
      ImUserDataEntity user = userInfo.getData();
      pack.setUserProfile(UserSnapshotPack.builder()
          .nickName(user.getNickName())
          .faceUrl(user.getFaceUrl())
          .gender(user.getGender())
          .build());
    }
    return pack;
  }
}
