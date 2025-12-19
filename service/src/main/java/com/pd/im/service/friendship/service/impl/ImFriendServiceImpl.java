package com.pd.im.service.friendship.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.pd.im.codec.pack.friendship.*;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.FriendshipEventCommand;
import com.pd.im.common.enums.friend.AllowFriendType;
import com.pd.im.common.enums.friend.CheckFriendshipType;
import com.pd.im.common.enums.friend.FriendshipErrorCode;
import com.pd.im.common.enums.friend.FriendshipStatus;
import com.pd.im.common.exception.ApplicationException;
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
import com.pd.im.service.friendship.model.resp.CheckFriendShipResp;
import com.pd.im.service.friendship.model.resp.ImportFriendShipResp;
import com.pd.im.service.friendship.service.ImFriendService;
import com.pd.im.service.friendship.service.ImFriendShipRequestService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.user.dao.ImUserDataEntity;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    public ResponseVO importFriendShip(ImportFriendShipReq req) {
        // 检查批量导入数量限制
        if (req.getFriendItem().size() > appConfig.getFriendShipMaxImportSize()) {
            return ResponseVO.errorResponse(FriendshipErrorCode.IMPORT_SIZE_BEYOND);
        }

        // 验证发起方用户是否存在
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isSuccess()) {
            return fromInfo;
        }

        ImportFriendShipResp resp = new ImportFriendShipResp();
        List<String> successIds = new ArrayList<>();
        List<String> errorIds = new ArrayList<>();

        for (ImportFriendShipReq.ImportFriendDto dto : req.getFriendItem()) {
            try {
                // 验证目标用户是否存在
                ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(dto.getToId(), req.getAppId());
                if (!toInfo.isSuccess()) {
                    log.warn("导入好友失败: 目标用户不存在, toId={}", dto.getToId());
                    errorIds.add(dto.getToId());
                    continue;
                }

                // 生成序列号
                long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);

                // ========== 处理 A -> B 的好友关系 ==========
                LambdaQueryWrapper<ImFriendShipEntity> fromQuery = buildFriendshipQuery(req.getAppId(), req.getFromId(), dto.getToId());
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
                    userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP, seq);
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
                    userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP, seq);
                }

                // ========== 处理 B -> A 的反向好友关系（建立双向关系） ==========
                LambdaQueryWrapper<ImFriendShipEntity> toQuery = buildFriendshipQuery(req.getAppId(), dto.getToId(), req.getFromId());
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
                    userSequenceRepository.writeUserSeq(req.getAppId(), dto.getToId(), Constants.SeqConstants.FRIENDSHIP, seq);
                } else {
                    // 已存在，仅更新序列号和状态（确保是正常状态）
                    ImFriendShipEntity update = new ImFriendShipEntity();
                    update.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
                    update.setFriendSequence(seq);

                    imFriendShipMapper.update(update, toQuery);
                    userSequenceRepository.writeUserSeq(req.getAppId(), dto.getToId(), Constants.SeqConstants.FRIENDSHIP, seq);
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

    @Override
    public ResponseVO addFriend(AddFriendReq req) {
        // 校验发起方用户是否存在
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isSuccess()) {
            return fromInfo;
        }

        // 校验目标方用户是否存在
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if (!toInfo.isSuccess()) {
            return toInfo;
        }

        // 添加好友前回调(是否允许添加好友动作)
        if (appConfig.isAddFriendBeforeCallback()) {
            ResponseVO callbackResp = callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.ADD_FRIEND_BEFORE, JSONObject.toJSONString(req));
            if (!callbackResp.isSuccess()) {
                return callbackResp;
            }
        }

        ImUserDataEntity toUser = toInfo.getData();

        // 判断是否需要验证
        if (toUser.getFriendAllowType() != null && toUser.getFriendAllowType() == AllowFriendType.NOT_NEED.getCode()) {
            // 无需验证,直接添加好友
            return doAddFriend(req, req.getFromId(), req.getToItem(), req.getAppId());
        }

        // 需要验证,先检查双向好友关系
        LambdaQueryWrapper<ImFriendShipEntity> fromQuery = buildFriendshipQuery(req.getAppId(), req.getFromId(),
                req.getToItem().getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(fromQuery);

        LambdaQueryWrapper<ImFriendShipEntity> toQuery = buildFriendshipQuery(req.getAppId(), req.getToItem().getToId(),
                req.getFromId());
        ImFriendShipEntity toItem = imFriendShipMapper.selectOne(toQuery);

        // 检查双向关系状态
        boolean fromIsNormal = fromItem != null && fromItem.getStatus().equals(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
        boolean toIsNormal = toItem != null && toItem.getStatus().equals(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());

        if (fromIsNormal && toIsNormal) {
            // 双向都是好友,返回已经是好友错误
            return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_YOUR_FRIEND);
        }

        // 其他情况(单向好友或无好友关系),创建好友申请
        return imFriendShipRequestService.addFriendshipRequest(req.getFromId(), req.getToItem(), req.getAppId());
    }

    @Override
    @Transactional
    public ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId) {
        // 检查双向好友关系是否已存在
        LambdaQueryWrapper<ImFriendShipEntity> fromQuery = buildFriendshipQuery(appId, fromId, dto.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(fromQuery);

        LambdaQueryWrapper<ImFriendShipEntity> toQuery = buildFriendshipQuery(appId, dto.getToId(), fromId);
        ImFriendShipEntity toItem = imFriendShipMapper.selectOne(toQuery);

        // 检查双向关系状态
        boolean fromIsNormal = fromItem != null && fromItem.getStatus().equals(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
        boolean toIsNormal = toItem != null && toItem.getStatus().equals(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());

        if (fromIsNormal && toIsNormal) {
            // 双向都已经是好友,返回错误
            return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_YOUR_FRIEND);
        }

        // 生成序列号,用于双向关系同步
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
            userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FRIENDSHIP, seq);
        } else if (!fromIsNormal) {
            // 已存在关系但状态不正常,更新为正常状态
            ImFriendShipEntity update = new ImFriendShipEntity();
            if (StringUtils.isNotBlank(dto.getAddSource())) {
                update.setAddSource(dto.getAddSource());
            }
            if (StringUtils.isNotBlank(dto.getRemark())) {
                update.setRemark(dto.getRemark());
            }
            if (StringUtils.isNotBlank(dto.getExtra())) {
                update.setExtra(dto.getExtra());
            }
            update.setFriendSequence(seq);
            update.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());

            int result = imFriendShipMapper.update(update, fromQuery);
            if (result != 1) {
                return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR);
            }
            userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FRIENDSHIP, seq);
            fromItem = imFriendShipMapper.selectOne(fromQuery);
        } else {
            // A->B 已经是好友,也需要更新序列号以便通知客户端双向关系已建立
            ImFriendShipEntity update = new ImFriendShipEntity();
            update.setFriendSequence(seq);
            imFriendShipMapper.update(update, fromQuery);
            userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FRIENDSHIP, seq);
            fromItem.setFriendSequence(seq);
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
            userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FRIENDSHIP, seq);
        } else if (!toIsNormal) {
            // 已存在关系但状态不正常,更新为正常状态
            ImFriendShipEntity update = new ImFriendShipEntity();
            update.setFriendSequence(seq);
            update.setStatus(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode());
            imFriendShipMapper.update(update, toQuery);
            userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FRIENDSHIP, seq);
        } else {
            // B->A 已经是好友,也需要更新序列号以便通知客户端双向关系已建立
            ImFriendShipEntity update = new ImFriendShipEntity();
            update.setFriendSequence(seq);
            imFriendShipMapper.update(update, toQuery);
            userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FRIENDSHIP, seq);
            toItem.setFriendSequence(seq);
        }

        // ========== 发送TCP通知 ==========
        AddFriendPack fromPack = new AddFriendPack();
        BeanUtils.copyProperties(fromItem, fromPack);
        fromPack.setSequence(seq);
        if (requestBase != null) {
            messageProducer.sendToClients(fromId, FriendshipEventCommand.FRIEND_ADD, fromPack,
                    requestBase.getAppId(), requestBase.getClientType(), requestBase.getImei());
        } else {
            messageProducer.sendToAllClients(fromId, FriendshipEventCommand.FRIEND_ADD, fromPack, appId);
        }

        AddFriendPack toPack = new AddFriendPack();
        BeanUtils.copyProperties(toItem, toPack);
        toPack.setSequence(seq);
        messageProducer.sendToAllClients(dto.getToId(), FriendshipEventCommand.FRIEND_ADD, toPack, appId);

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
    private LambdaQueryWrapper<ImFriendShipEntity> buildFriendshipQuery(Integer appId, String fromId, String toId) {
        LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipEntity::getAppId, appId)
                .eq(ImFriendShipEntity::getFromId, fromId)
                .eq(ImFriendShipEntity::getToId, toId);
        return query;
    }

    @Override
    @Transactional
    public ResponseVO updateFriend(UpdateFriendReq req) {
        // 校验用户是否存在
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isSuccess()) {
            return fromInfo;
        }

        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if (!toInfo.isSuccess()) {
            return toInfo;
        }

        // 执行更新
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
        userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP, seq);

        // 发送TCP通知
        UpdateFriendPack updateFriendPack = new UpdateFriendPack();
        updateFriendPack.setRemark(req.getToItem().getRemark());
        updateFriendPack.setToId(req.getToItem().getToId());
        messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_UPDATE, updateFriendPack,
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
    public ResponseVO deleteFriend(DeleteFriendReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(req.getAppId(), req.getFromId(), req.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);

        if (fromItem == null) {
            return ResponseVO.errorResponse(FriendshipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        }

        if (fromItem.getStatus() == null || !fromItem.getStatus().equals(FriendshipStatus.FRIEND_STATUS_NORMAL.getCode())) {
            return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_DELETED);
        }

        // 更新状态为已删除
        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);
        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setFriendSequence(seq);
        update.setStatus(FriendshipStatus.FRIEND_STATUS_DELETE.getCode());
        imFriendShipMapper.update(update, query);
        userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP, seq);

        // 发送TCP通知
        DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
        deleteFriendPack.setFromId(req.getFromId());
        deleteFriendPack.setSequence(seq);
        deleteFriendPack.setToId(req.getToId());
        messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_DELETE, deleteFriendPack,
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
    public ResponseVO deleteAllFriend(DeleteFriendReq req) {
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
        messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_ALL_DELETE, deleteFriendPack,
                req.getAppId(), req.getClientType(), req.getImei());

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getAllFriendShip(GetAllFriendShipReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipEntity::getAppId, req.getAppId())
                .eq(ImFriendShipEntity::getFromId, req.getFromId());
        return ResponseVO.successResponse(imFriendShipMapper.selectList(query));
    }

    @Override
    public ResponseVO getRelation(GetRelationReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(
                req.getAppId(), req.getFromId(), req.getToId());

        ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);
        if (entity == null) {
            return ResponseVO.errorResponse(FriendshipErrorCode.REPEATSHIP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public ResponseVO checkFriendship(CheckFriendShipReq req) {
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
     * 填充缺失的toId记录
     * 对于没有查询到关系的toId,创建状态为0的记录
     */
    private void fillMissingToIds(CheckFriendShipReq req, List<CheckFriendShipResp> resp) {
        Map<String, Integer> existingToIds = resp.stream()
                .collect(Collectors.toMap(CheckFriendShipResp::getToId, CheckFriendShipResp::getStatus));

        for (String toId : req.getToIds()) {
            if (!existingToIds.containsKey(toId)) {
                CheckFriendShipResp checkResp = new CheckFriendShipResp();
                checkResp.setFromId(req.getFromId());
                checkResp.setToId(toId);
                checkResp.setStatus(0);
                resp.add(checkResp);
            }
        }
    }

    @Override
    public ResponseVO addBlack(AddFriendShipBlackReq req) {
        // 校验用户是否存在
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isSuccess()) {
            return fromInfo;
        }
        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToId(), req.getAppId());
        if (!toInfo.isSuccess()) {
            return toInfo;
        }

        LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(req.getAppId(), req.getFromId(), req.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);
        long seq;

        if (fromItem == null) {
            // 不存在关系,新增并设置为拉黑
            seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);
            fromItem = new ImFriendShipEntity();
            fromItem.setFromId(req.getFromId());
            fromItem.setToId(req.getToId());
            fromItem.setFriendSequence(seq);
            fromItem.setAppId(req.getAppId());
            fromItem.setBlack(FriendshipStatus.BLACK_STATUS_BLACKED.getCode());
            fromItem.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(fromItem);
            if (insert != 1) {
                return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR);
            }
            userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP, seq);
        } else {
            // 已存在关系,检查黑名单状态
            if (fromItem.getBlack() != null && fromItem.getBlack().equals(FriendshipStatus.BLACK_STATUS_BLACKED.getCode())) {
                return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_BLACK);
            }
            seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);
            ImFriendShipEntity update = new ImFriendShipEntity();
            update.setFriendSequence(seq);
            update.setBlack(FriendshipStatus.BLACK_STATUS_BLACKED.getCode());
            int result = imFriendShipMapper.update(update, query);
            if (result != 1) {
                return ResponseVO.errorResponse(FriendshipErrorCode.ADD_BLACK_ERROR);
            }
            userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP, seq);
        }

        // 发送TCP通知
        AddFriendBlackPack pack = new AddFriendBlackPack();
        pack.setFromId(req.getFromId());
        pack.setSequence(seq);
        pack.setToId(req.getToId());
        messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_BLACK_ADD, pack,
                req.getAppId(), req.getClientType(), req.getImei());

        // 添加黑名单后回调
        if (appConfig.isAddFriendShipBlackAfterCallback()) {
            AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
            callbackDto.setFromId(req.getFromId());
            callbackDto.setToId(req.getToId());
            callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.ADD_BLACK_AFTER,
                    JSONObject.toJSONString(callbackDto));
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteBlack(DeleteBlackReq req) {
        LambdaQueryWrapper<ImFriendShipEntity> query = buildFriendshipQuery(
                req.getAppId(), req.getFromId(), req.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);

        // 检查是否存在且是否已拉黑
        if (fromItem == null || fromItem.getBlack() == null ||
                !fromItem.getBlack().equals(FriendshipStatus.BLACK_STATUS_BLACKED.getCode())) {
            throw new ApplicationException(FriendshipErrorCode.FRIEND_IS_NOT_YOUR_BLACK);
        }

        // 移除黑名单，设置为正常状态
        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP);
        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setFriendSequence(seq);
        update.setBlack(FriendshipStatus.BLACK_STATUS_NORMAL.getCode());

        int updateResult = imFriendShipMapper.update(update, query);
        if (updateResult != 1) {
            return ResponseVO.errorResponse();
        }
        userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP, seq);

        // 发送TCP通知
        DeleteBlackPack pack = new DeleteBlackPack();
        pack.setFromId(req.getFromId());
        pack.setSequence(seq);
        pack.setToId(req.getToId());
        messageProducer.sendToClients(req.getFromId(), FriendshipEventCommand.FRIEND_BLACK_DELETE, pack,
                req.getAppId(), req.getClientType(), req.getImei());

        // 删除黑名单后回调
        if (appConfig.isAddFriendShipBlackAfterCallback()) {
            AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
            callbackDto.setFromId(req.getFromId());
            callbackDto.setToId(req.getToId());
            callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.DELETE_BLACK,
                    JSONObject.toJSONString(callbackDto));
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO checkBlack(CheckFriendShipReq req) {
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
    public ResponseVO syncFriendshipList(SyncRequest req) {
        // 限制单次拉取数量
        if (req.getMaxLimit() > appConfig.getFriendShipMaxCount()) {
            req.setMaxLimit(appConfig.getFriendShipMaxCount());
        }

        SyncResponse<ImFriendShipEntity> resp = new SyncResponse<>();

        // 查询增量数据: sequence > lastSequence
        LambdaQueryWrapper<ImFriendShipEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipEntity::getFromId, req.getOperator())
                .eq(ImFriendShipEntity::getAppId, req.getAppId())
                .gt(ImFriendShipEntity::getFriendSequence, req.getLastSequence())
                .orderByAsc(ImFriendShipEntity::getFriendSequence)
                .last("limit " + req.getMaxLimit());

        List<ImFriendShipEntity> list = imFriendShipMapper.selectList(query);

        if (!CollectionUtils.isEmpty(list)) {
            // 获取最大序列号
            Long maxSeq = imFriendShipMapper.getFriendShipMaxSeq(req.getAppId(), req.getOperator());
            ImFriendShipEntity lastEntity = list.get(list.size() - 1);

            resp.setDataList(list);
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
}
