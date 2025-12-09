package com.pd.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.BaseErrorCode;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.GateWayErrorCode;
import com.pd.im.common.enums.user.UserTypeEnum;
import com.pd.im.common.exception.ApplicationExceptionEnum;
import com.pd.im.common.utils.SigAPI;
import com.pd.im.service.user.dao.ImUserDataEntity;
import com.pd.im.service.user.service.ImUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author Parker
 * @date 12/9/25
 */
@Slf4j
@Component
public class IdentityCheck {
    @Autowired
    ImUserService imUserService;
    @Autowired
    AppConfig appConfig;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public ApplicationExceptionEnum checkUserSig(String identifier, String appId, String userSig) {
        // 10001:userSign:bantangereJyrVgrxCdZLrSjILEpVsjI0gAIdsHBiQYFnCkTUUAdJYUhmLkixmYWBiYm5sSVULjMlNa8kMy0ztUjJSikpMa8kMS8dyIZIFmemA0XziiJzQs0tvVLK-IPLjE1K9XOTMs3KyiL9w0qTwkMCsg1CLI2Kg-wiqyIibZVqAQHCL7I_
        String cacheUserSig = stringRedisTemplate.opsForValue().get(appId + Constants.RedisConstants.UserSign + identifier + userSig);
        if (!StringUtils.isBlank(cacheUserSig) && Long.valueOf(cacheUserSig) > System.currentTimeMillis() / 1000) {
            this.setIsAdmin(identifier, Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }

        //获取秘钥
        String privateKey = appConfig.getPrivateKey();

        // TODO 这一段逻辑需要更改，服务端生成密钥提供给客户端，而不是直接嵌套在这
        // 根据 appId + 密钥创建 sigApi(加密 token)
        SigAPI sigAPI = new SigAPI(Long.parseLong(appId), privateKey);

        // 调用 sigApi 对 userSig 解密
        JSONObject jsonObject = sigAPI.decodeUserSig(userSig);

        //取出解密后的 appid 和 操作人 和 过期时间做匹配，不通过则提示错误
        Long expireTime = 0L;
        Long expireSec = 0L;
        Long time = 0L;
        String decoderAppId = "";
        String decoderIdentifier = "";

        try {
            decoderAppId = jsonObject.getString("TLS.appId");
            decoderIdentifier = jsonObject.getString("TLS.identifier");
            String expireStr = jsonObject.get("TLS.expire").toString();
            String expireTimeStr = jsonObject.get("TLS.expireTime").toString();
            time = Long.valueOf(expireTimeStr);
            expireSec = Long.valueOf(expireStr);
            expireTime = Long.valueOf(expireTimeStr) + expireSec;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("checkUserSig-error:{}", e.getMessage());
        }

        if (!decoderIdentifier.equals(identifier)) {
            return GateWayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }

        if (!decoderAppId.equals(appId)) {
            return GateWayErrorCode.USERSIGN_IS_ERROR;
        }

        if (expireSec == 0L) {
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        if (expireTime < System.currentTimeMillis() / 1000) {
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        //appid + "xxx" + userId + sign
        String genSig = sigAPI.genUserSig(identifier, expireSec, time, null);
        if (genSig.toLowerCase().equals(userSig.toLowerCase())) {
            String key = appId + Constants.RedisConstants.UserSign + identifier + "#" + userSig;

            Long etime = expireTime - System.currentTimeMillis() / 1000;
            stringRedisTemplate.opsForValue().set(key, expireTime.toString(), etime, TimeUnit.SECONDS);
            this.setIsAdmin(identifier, Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }

        return GateWayErrorCode.USERSIGN_IS_ERROR;
    }

    /**
     * 根据appid,identifier判断是否App管理员,并设置到RequestHolder
     *
     * @param identifier
     * @param appId
     * @return
     */
    public void setIsAdmin(String identifier, Integer appId) {
        //去DB或Redis中查找, 后面写
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(identifier, appId);
        if (singleUserInfo.isOk()) {
            RequestHolder.set(singleUserInfo.getData().getUserType() == UserTypeEnum.APP_ADMIN.getCode());
        } else {
            RequestHolder.set(false);
        }
    }
}
