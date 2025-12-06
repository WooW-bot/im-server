package com.pd.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.device.ConnectState;
import com.pd.im.common.model.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Parker
 * @date 12/5/25
 */
@Component
public class UserSessionUtils {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public List<UserSession> getUserSession(Integer appId, String userId) {
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(userSessionKey);
        List<UserSession> list = new ArrayList<>();
        Collection<Object> values = entries.values();
        for (Object o : values) {
            String str = (String) o;
            UserSession session = JSONObject.parseObject(str, UserSession.class);
            if (session.getConnectState() == ConnectState.CONNECT_STATE_ONLINE.getCode()) {
                list.add(session);
            }
        }
        return list;
    }

    public UserSession getUserSession(Integer appId, String userId, Integer clientType, String imei) {
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        String hashKey = clientType + ":" + imei;
        Object o = stringRedisTemplate.opsForHash().get(userSessionKey, hashKey);
        UserSession session = JSONObject.parseObject(o.toString(), UserSession.class);
        return session;
    }
}
