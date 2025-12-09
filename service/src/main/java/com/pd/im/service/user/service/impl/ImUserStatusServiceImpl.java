package com.pd.im.service.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.pack.user.UserCustomStatusChangeNotifyPack;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.UserEventCommand;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.common.model.UserSession;
import com.pd.im.service.friendship.service.ImFriendService;
import com.pd.im.service.user.model.req.PullFriendOnlineStatusReq;
import com.pd.im.service.user.model.req.PullUserOnlineStatusReq;
import com.pd.im.service.user.model.req.SetUserCustomerStatusReq;
import com.pd.im.service.user.model.req.SubscribeUserOnlineStatusReq;
import com.pd.im.service.user.model.resp.UserOnlineStatusResp;
import com.pd.im.service.user.service.ImUserStatusService;
import com.pd.im.service.utils.MessageProducer;
import com.pd.im.service.utils.UserSessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Parker
 * @date 12/8/25
 */
@Service
public class ImUserStatusServiceImpl implements ImUserStatusService {
    @Autowired
    UserSessionUtils userSessionUtils;
    @Autowired
    MessageProducer messageProducer;
    @Autowired
    ImFriendService imFriendService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public void subscribeUserOnlineStatus(SubscribeUserOnlineStatusReq req) {
        Long subExpireTime = 0L;
        if (req != null && req.getSubTime() > 0) {
            subExpireTime = System.currentTimeMillis() + req.getSubTime();
        }

        for (String beSubUserId : req.getSubUserId()) {
            String userKey = req.getAppId() + Constants.RedisConstants.SUBSCRIBE + beSubUserId;
            stringRedisTemplate.opsForHash().put(userKey, req.getOperator(), subExpireTime.toString());
        }
    }

    @Override
    public void setUserCustomerStatus(SetUserCustomerStatusReq req) {
        UserCustomStatusChangeNotifyPack userCustomStatusChangeNotifyPack = new UserCustomStatusChangeNotifyPack();
        userCustomStatusChangeNotifyPack.setCustomStatus(req.getCustomStatus());
        userCustomStatusChangeNotifyPack.setCustomText(req.getCustomText());
        userCustomStatusChangeNotifyPack.setUserId(req.getUserId());
        stringRedisTemplate.opsForValue().set(
                req.getAppId() + Constants.RedisConstants.USER_CUSTOMER_STATUS + req.getUserId(),
                JSONObject.toJSONString(userCustomStatusChangeNotifyPack));

        syncSender(userCustomStatusChangeNotifyPack,
                req.getUserId(), new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        dispatcher(userCustomStatusChangeNotifyPack, req.getUserId(), req.getAppId());
    }

    @Override
    public Map<String, UserOnlineStatusResp> queryFriendOnlineStatus(PullFriendOnlineStatusReq req) {
        List<String> friendIds = imFriendService.getFriendIds(req.getOperator(), req.getAppId());
        return getUserOnlineStatus(friendIds, req.getAppId());
    }

    @Override
    public Map<String, UserOnlineStatusResp> queryUserOnlineStatus(PullUserOnlineStatusReq req) {
        return getUserOnlineStatus(req.getUserList(), req.getAppId());
    }

    private Map<String, UserOnlineStatusResp> getUserOnlineStatus(List<String> userId, Integer appId) {
        Map<String, UserOnlineStatusResp> result = new HashMap<>(userId.size());
        for (String uid : userId) {
            UserOnlineStatusResp resp = new UserOnlineStatusResp();
            List<UserSession> userSession = userSessionUtils.getUserSession(appId, uid);
            resp.setSession(userSession);
            String userKey = appId + Constants.RedisConstants.USER_CUSTOMER_STATUS + uid;
            String s = stringRedisTemplate.opsForValue().get(userKey);
            if (StringUtils.isNotBlank(s)) {
                JSONObject parse = (JSONObject) JSON.parse(s);
                resp.setCustomText(parse.getString("customText"));
                resp.setCustomStatus(parse.getInteger("customStatus"));
            }
            result.put(uid, resp);
        }
        return result;
    }

    private void syncSender(Object pack, String userId, ClientInfo clientInfo) {
        messageProducer.sendToOtherClients(userId,
                UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC,
                pack, clientInfo);
    }

    private void dispatcher(Object pack, String userId, Integer appId) {
        List<String> friendIds = imFriendService.getFriendIds(userId, appId);
        for (String fid : friendIds) {
            messageProducer.sendToAllClients(fid, UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY, pack, appId);
        }

        String userKey = appId + Constants.RedisConstants.SUBSCRIBE + userId;
        Set<Object> keys = stringRedisTemplate.opsForHash().keys(userKey);
        for (Object key : keys) {
            String filed = (String) key;
            Long expire = Long.valueOf((String) stringRedisTemplate.opsForHash().get(userKey, filed));
            if (expire > 0 && expire > System.currentTimeMillis()) {
                messageProducer.sendToAllClients(filed, UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY, pack, appId);
            } else {
                stringRedisTemplate.opsForHash().delete(userKey, filed);
            }
        }
    }
}
