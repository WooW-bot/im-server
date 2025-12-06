package com.pd.im.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Parker
 * @date 12/5/25
 */
@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class AppConfig {
    /**
     * 回调地址
     */
    private String callbackUrl;

    private boolean sendMessageBeforeCallback;//发送单聊消息之前

    private boolean sendMessageAfterCallback; //发送单聊消息之后

    private Integer offlineMessageCount; //离线消息最大条数
}
