package com.pd.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.BaseErrorCode;
import com.pd.im.common.enums.GatewayErrorCode;
import com.pd.im.common.enums.user.UserType;
import com.pd.im.common.exception.ApplicationExceptionEnum;
import com.pd.im.common.util.SigAPI;
import com.pd.im.service.app.service.ImAppService;
import com.pd.im.service.user.dao.ImUserDataEntity;
import com.pd.im.service.user.service.ImUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 用户身份验证工具类
 * 负责验证UserSig签名的有效性
 *
 * @author Parker
 * @date 12/9/25
 */
@Slf4j
@Component
public class IdentityCheck {

    private final ImUserService imUserService;
    private final ImAppService imAppService;
    private final StringRedisTemplate stringRedisTemplate;

    public IdentityCheck(ImUserService imUserService,
                         ImAppService imAppService,
                         StringRedisTemplate stringRedisTemplate) {
        this.imUserService = imUserService;
        this.imAppService = imAppService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 验证用户签名
     *
     * @param identifier 用户标识
     * @param appId      应用ID
     * @param userSig    用户签名
     * @return 验证结果
     */
    public ApplicationExceptionEnum checkUserSig(String identifier, String appId, String userSig) {
        // 参数校验
        if (StringUtils.isAnyBlank(identifier, appId, userSig)) {
            return GatewayErrorCode.USERSIGN_IS_ERROR;
        }

        log.info("========== UserSign 验证开始 ==========");
        log.info("客户端参数: appId={}, identifier={}", appId, identifier);
        log.info("客户端 userSign: {}", userSig);

        // 检查缓存，格式: appId:userSign:identifier#userSig
        String cacheKey = buildCacheKey(appId, identifier, userSig);
        String cachedExpireTime = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cachedExpireTime)) {
            try {
                long expireTime = Long.parseLong(cachedExpireTime);
                long currentTime = System.currentTimeMillis() / 1000;
                if (expireTime > currentTime) {
                    log.info("缓存命中，签名有效");
                    setIsAdmin(identifier, Integer.parseInt(appId));
                    return BaseErrorCode.SUCCESS;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid cached expire time for identifier={}, appId={}", identifier, appId);
            }
        }

        // 从数据库获取应用私钥
        String privateKey = imAppService.getPrivateKey(Integer.parseInt(appId));
        if (StringUtils.isBlank(privateKey)) {
            log.error("Failed to get private key for appId={}, app may not exist or is inactive", appId);
            return GatewayErrorCode.USERSIGN_IS_ERROR;
        }

        log.info("服务端 privateKey: {}", privateKey);

        // 创建SigAPI
        SigAPI sigAPI = new SigAPI(Long.parseLong(appId), privateKey);

        // 解密UserSig
        JSONObject sigDoc = SigAPI.decodeUserSig(userSig);
        if (sigDoc == null || sigDoc.isEmpty()) {
            log.error("Failed to decode userSig for identifier={}, appId={}", identifier, appId);
            return GatewayErrorCode.USERSIGN_IS_ERROR;
        }
        // 重新生成签名并比对 HMAC (TLS.sig)
        String generatedSig = sigAPI.genUserSig(identifier, sigDoc.getLongValue("TLS.expire"), sigDoc.getLongValue("TLS.time"), null);
        JSONObject generatedSigDoc = SigAPI.decodeUserSig(generatedSig);
        String generatedHMAC = generatedSigDoc.getString("TLS.sig");
        String originalHMAC = sigDoc.getString("TLS.sig");

        if (!originalHMAC.equals(generatedHMAC)) {
            log.warn("Signature verification failed for identifier={}, appId={}", identifier, appId);
            return GatewayErrorCode.USERSIGN_IS_ERROR;
        }

        log.info("解析后的签名文档: {}", sigDoc.toJSONString());

        // 解析签名文档
        String decoderAppId;
        String decoderIdentifier;
        long time;
        long expireSec;
        long expireTime;

        try {
            decoderAppId = sigDoc.getString("TLS.appId");
            decoderIdentifier = sigDoc.getString("TLS.identifier");
            time = sigDoc.getLongValue("TLS.time");
            expireSec = sigDoc.getLongValue("TLS.expire");
            expireTime = time + expireSec;

            log.info("签名文档字段:");
            log.info("  - TLS.appId: {}", decoderAppId);
            log.info("  - TLS.identifier: {}", decoderIdentifier);
            log.info("  - TLS.time: {}", time);
            log.info("  - TLS.expire: {} 秒", expireSec);
            log.info("  - expireTime: {}", expireTime);
        } catch (Exception e) {
            log.error("Failed to parse userSig fields for identifier={}, appId={}", identifier, appId, e);
            return GatewayErrorCode.USERSIGN_IS_ERROR;
        }

        // 验证标识符
        if (!identifier.equals(decoderIdentifier)) {
            log.warn("标识符不匹配: 期望={}, 实际={}", identifier, decoderIdentifier);
            return GatewayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }

        // 验证AppId
        if (!appId.equals(decoderAppId)) {
            log.warn("AppId不匹配: 期望={}, 实际={}", appId, decoderAppId);
            return GatewayErrorCode.USERSIGN_IS_ERROR;
        }

        // 验证过期时间
        if (expireSec <= 0) {
            log.warn("无效的过期时间: {}", expireSec);
            return GatewayErrorCode.USERSIGN_IS_EXPIRED;
        }

        long currentTime = System.currentTimeMillis() / 1000;
        if (expireTime < currentTime) {
            log.warn("UserSig已过期: expireTime={}, currentTime={}, 差值={} 秒",
                    expireTime, currentTime, (currentTime - expireTime));
            return GatewayErrorCode.USERSIGN_IS_EXPIRED;
        }


        // 签名验证成功，缓存结果
        long remainingTime = expireTime - currentTime;
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                String.valueOf(expireTime),
                remainingTime,
                TimeUnit.SECONDS
        );

        log.info("签名验证成功！缓存有效期: {} 秒", remainingTime);
        log.info("========== UserSign 验证结束 (成功) ==========");

        setIsAdmin(identifier, Integer.parseInt(appId));
        return BaseErrorCode.SUCCESS;
    }

    /**
     * 构建缓存Key
     *
     * @param appId      应用ID
     * @param identifier 用户标识
     * @param userSig    用户签名
     * @return 缓存key
     */
    private String buildCacheKey(String appId, String identifier, String userSig) {
        return appId + Constants.RedisConstants.USER_SIGN + identifier + "#" + userSig;
    }

    /**
     * 根据appId和identifier判断是否为App管理员，并设置到RequestHolder
     *
     * @param identifier 用户标识
     * @param appId      应用ID
     */
    private void setIsAdmin(String identifier, Integer appId) {
        ResponseVO<ImUserDataEntity> userInfoResponse = imUserService.getSingleUserInfo(identifier, appId);
        boolean isAdmin = userInfoResponse.isSuccess()
                && userInfoResponse.getData() != null
                && userInfoResponse.getData().getUserType() == UserType.APP_ADMIN.getCode();
        RequestHolder.set(isAdmin);
    }
}
