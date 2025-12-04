package com.pd.im.common.model;

import lombok.Data;

/**
 * @author Parker
 * @date 12/4/25
 */
@Data
public class UserSession {
    private String userId;
    private Integer appId;
    /**
     * 端标识：web端、pc端、移动端
     */
    private Integer clientType;
    /**
     * SDK 版本号，对接前端传入的版本号做后端相应逻辑
     */
    private Integer version;
    /**
     * 连接状态【1.在线、2.离线】
     */
    private Integer connectState;
    private Integer brokerId;
    private String brokerHost;
    private String imei;
}
