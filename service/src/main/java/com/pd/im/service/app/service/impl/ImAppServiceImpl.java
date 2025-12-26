package com.pd.im.service.app.service.impl;

import com.pd.im.common.util.EncryptionUtil;
import com.pd.im.service.app.dao.ImAppEntity;
import com.pd.im.service.app.dao.mapper.ImAppMapper;
import com.pd.im.service.app.service.ImAppService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * IM应用服务实现类
 * 使用基于应用属性的密钥派生方式，无需额外管理加密密钥
 *
 * @author Parker
 * @date 12/24/25
 */
@Slf4j
@Service
public class ImAppServiceImpl implements ImAppService {

    private static final int APP_STATUS_NORMAL = 1;

    private final ImAppMapper imAppMapper;

    public ImAppServiceImpl(ImAppMapper imAppMapper) {
        this.imAppMapper = imAppMapper;
    }



    @Override
    @Cacheable(value = "imApp", key = "#appId", unless = "#result == null")
    public ImAppEntity getAppById(Integer appId) {
        return imAppMapper.selectById(appId);
    }

    @Override
    @Cacheable(value = "imAppPrivateKey", key = "#appId", unless = "#result == null")
    public String getPrivateKey(Integer appId) {
        ImAppEntity app = getAppById(appId);
        if (app == null) {
            log.warn("Application not found: appId={}", appId);
            return null;
        }

        if (app.getAppStatus() == null || app.getAppStatus() != APP_STATUS_NORMAL) {
            log.warn("Application is not active: appId={}, status={}", appId, app.getAppStatus());
            return null;
        }

        // 检查应用是否过期
        if (app.getExpireTime() != null && app.getExpireTime() < System.currentTimeMillis()) {
            log.warn("Application has expired: appId={}, expireTime={}", appId, app.getExpireTime());
            return null;
        }

        // 解密私钥
        String encryptedPrivateKey = app.getPrivateKey();
        String encryptionKeyWrapper = app.getEncryptionKey();

        if (StringUtils.isBlank(encryptedPrivateKey)) {
            log.warn("Private key is empty for appId={}", appId);
            return null;
        }

        try {
            // New Strategy: Key Wrapping
            if (StringUtils.isNotBlank(encryptionKeyWrapper)) {
                // 1. 解码（解密）应用密钥，使用 Global Master Secret (Internal)
                String appKey = EncryptionUtil.decodeKey(encryptionKeyWrapper);
                // 2. 使用应用密钥解密私钥
                return EncryptionUtil.decrypt(encryptedPrivateKey, appKey);
            } else {
                log.error("Missing encryption key for appId={}", appId);
                return null;
            }

        } catch (Exception e) {
            log.error("Failed to decrypt private key for appId={}", appId, e);
            return null;
        }
    }


    // getAppByKey method removed


    @Override
    public boolean isAppValid(Integer appId) {
        ImAppEntity app = getAppById(appId);
        if (app == null) {
            return false;
        }

        if (app.getAppStatus() == null || app.getAppStatus() != APP_STATUS_NORMAL) {
            return false;
        }

        // 检查应用是否过期
        if (app.getExpireTime() != null && app.getExpireTime() < System.currentTimeMillis()) {
            return false;
        }

        return true;
    }
}
