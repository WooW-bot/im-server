package com.pd.im.service.user.service.impl;

import com.alibaba.fastjson.JSONObject;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pd.im.codec.pack.user.UserModifyPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.DeleteFlag;
import com.pd.im.common.enums.command.Command;
import com.pd.im.common.enums.command.UserEventCommand;
import com.pd.im.common.enums.user.UserErrorCode;
import com.pd.im.common.enums.device.ClientType;
import com.pd.im.common.exception.ApplicationException;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.common.route.RouteHandler;
import com.pd.im.common.util.RouteInfoParser;
import com.pd.im.common.route.RouteInfo;
import com.pd.im.service.callback.CallbackService;
import com.pd.im.service.group.service.ImGroupService;
import com.pd.im.service.user.dao.ImUserDataEntity;
import com.pd.im.service.user.dao.mapper.ImUserDataMapper;
import com.pd.im.service.user.model.req.*;
import com.pd.im.service.user.model.resp.GetUserInfoResp;
import com.pd.im.service.user.model.resp.ImportUserResp;
import com.pd.im.service.user.model.resp.ImUserDataVO;
import com.pd.im.service.user.model.resp.LoginResp;
import com.pd.im.service.user.service.ImUserService;
import com.pd.im.service.utils.MessageProducer;
import com.pd.im.service.utils.ZKit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Parker
 * @date 12/3/25
 * @description ImUserviceImpl类
 */
@Slf4j
@Service
public class ImUserServiceImpl implements ImUserService {
    @Autowired
    ImUserDataMapper imUserDataMapper;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    AppConfig appConfig;

    @Autowired
    CallbackService callbackService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ImGroupService imGroupService;

    @Autowired
    RouteHandler routeHandler;

    @Autowired
    ZKit zKit;

    @Override
    public ResponseVO importUser(ImportUserReq req) {
        if (req.getUserData().size() > 100) {
            return ResponseVO.errorResponse(UserErrorCode.IMPORT_SIZE_BEYOND);
        }
        ImportUserResp resp = new ImportUserResp();
        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();

        for (ImUserDataEntity data : req.getUserData()) {
            data.setAppId(req.getAppId());
            try {
                data.setAppId(req.getAppId());
                int insert = imUserDataMapper.insert(data);
                if (insert == 1) {
                    successId.add(data.getUserId());
                }
            } catch (Exception e) {
                log.error("Import User failed: {}", data.getUserId(), e);
                errorId.add(data.getUserId());
            }
        }

        resp.setErrorId(errorId);
        resp.setSuccessId(successId);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req) {
        LambdaQueryWrapper<ImUserDataEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImUserDataEntity::getAppId, req.getAppId())
                .in(ImUserDataEntity::getUserId, req.getUserIds())
                .eq(ImUserDataEntity::getDelFlag, DeleteFlag.NORMAL.getCode());

        List<ImUserDataEntity> userDataEntities = imUserDataMapper.selectList(queryWrapper);
        HashMap<String, ImUserDataEntity> map = new HashMap<>();

        for (ImUserDataEntity data : userDataEntities) {
            map.put(data.getUserId(), data);
        }

        List<String> failUser = new ArrayList<>();
        for (String uid : req.getUserIds()) {
            if (!map.containsKey(uid)) {
                failUser.add(uid);
            }
        }

        List<ImUserDataVO> voList = new ArrayList<>();
        for (ImUserDataEntity entity : userDataEntities) {
            ImUserDataVO vo = new ImUserDataVO();
            BeanUtils.copyProperties(entity, vo);
            voList.add(vo);
        }

        GetUserInfoResp resp = new GetUserInfoResp();
        resp.setUserDataItem(voList);
        resp.setFailUser(failUser);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO deleteUser(DeleteUserReq req) {
        ImUserDataEntity entity = new ImUserDataEntity();
        entity.setDelFlag(DeleteFlag.DELETE.getCode());

        List<String> errorId = new ArrayList<>();
        List<String> successId = new ArrayList<>();

        for (String userId : req.getUserId()) {
            LambdaQueryWrapper<ImUserDataEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ImUserDataEntity::getAppId, req.getAppId())
                    .eq(ImUserDataEntity::getUserId, userId)
                    .eq(ImUserDataEntity::getDelFlag, DeleteFlag.NORMAL.getCode());
            int update = 0;
            try {
                update = imUserDataMapper.update(entity, wrapper);
                if (update > 0) {
                    successId.add(userId);
                } else {
                    errorId.add(userId);
                }
            } catch (Exception e) {
                log.error("Delete User failed: {}", userId, e);
                errorId.add(userId);
            }
        }

        ImportUserResp resp = new ImportUserResp();
        resp.setSuccessId(successId);
        resp.setErrorId(errorId);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO modifyUserInfo(ModifyUserInfoReq req) {
        LambdaQueryWrapper<ImUserDataEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImUserDataEntity::getAppId, req.getAppId())
                .eq(ImUserDataEntity::getUserId, req.getUserId())
                .eq(ImUserDataEntity::getDelFlag, DeleteFlag.NORMAL.getCode());
        ImUserDataEntity user = imUserDataMapper.selectOne(query);
        if (user == null) {
            throw new ApplicationException(UserErrorCode.USER_IS_NOT_EXIST);
        }

        ImUserDataEntity update = new ImUserDataEntity();
        BeanUtils.copyProperties(req, update);

        update.setAppId(null);
        update.setUserId(null);
        int update1 = imUserDataMapper.update(update, query);

        if (update1 == 1) {
            UserModifyPack pack = new UserModifyPack();
            BeanUtils.copyProperties(req, pack);

            messageProducer.sendToClients(req.getUserId(), UserEventCommand.USER_MODIFY, pack, req.getAppId(),
                    req.getClientType(), req.getImei());

            if (appConfig.isModifyUserAfterCallback()) {
                callbackService.afterCallback(req.getAppId(),
                        Constants.CallbackCommand.MODIFY_USER_AFTER,
                        JSONObject.toJSONString(req));
            }
            return ResponseVO.successResponse();
        }
        throw new ApplicationException(UserErrorCode.MODIFY_USER_ERROR);
    }

    @Override
    public ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId) {
        LambdaQueryWrapper<ImUserDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImUserDataEntity::getAppId, appId)
                .eq(ImUserDataEntity::getUserId, userId)
                .eq(ImUserDataEntity::getDelFlag, DeleteFlag.NORMAL.getCode());

        ImUserDataEntity imUserDataEntity = imUserDataMapper.selectOne(wrapper);
        if (imUserDataEntity == null) {
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }

        return ResponseVO.successResponse(imUserDataEntity);
    }

    @Override
    public ResponseVO login(LoginReq req) {
        // 1. 生成临时票据 Ticket
        String ticket = UUID.randomUUID().toString().replace("-", "");

        // 2. 存储到 Redis，设置 2 分钟过期
        String key = req.getAppId() + Constants.RedisConstants.USER_LOGIN_TICKET
                + req.getUserId() + ":" + req.getClientType() + ":" + req.getImei();
        stringRedisTemplate.opsForValue().set(key, ticket, 2, TimeUnit.MINUTES);
        log.info("Ticket 已写入 Redis: userId={}, key={}, ticket={}", req.getUserId(), key, ticket);

        // 3.获取 IM 地址 (路由逻辑从 Controller 迁移至此)
        List<String> allNode;
        if (ClientType.WEB.getCode().equals(req.getClientType())) {
            allNode = zKit.getAllWebNode();
        } else {
            allNode = zKit.getAllTcpNode();
        }
        String s = routeHandler.routeServer(allNode, req.getUserId());
        RouteInfo routeInfo = RouteInfoParser.parse(s);

        // 4. 返回 Ticket 和 RouteInfo 给 SDK
        LoginResp resp = new LoginResp();
        resp.setTicket(ticket);
        resp.setIp(routeInfo.getIp());
        resp.setPort(routeInfo.getPort());
        
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO getUserSequence(GetUserSequenceReq req) {
        String key = req.getAppId() + Constants.RedisConstants.SEQ_PREFIX + req.getUserId();
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(key);
        Long groupSeq = imGroupService.getUserGroupMaxSeq(req.getUserId(), req.getAppId());
        map.put(Constants.SeqConstants.GROUP_SEQ, groupSeq);
        return ResponseVO.successResponse(map);
    }
}
