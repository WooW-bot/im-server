package com.pd.im.common.model.message;

import lombok.Builder;
import lombok.Data;

/**
 * 这个 Controller 层的请求体放在 Common 层，供 TCP 层调度
 * @author Parker
 * @date 12/4/25
 */
@Data
@Builder
public class CheckSendMessageReq {
    private String fromId;
    /**
     * [P2P] toId 为目标用户
     * [GROUP] toId 为目标群组
     */
    private String toId;
    private Integer appId;
    private Integer command;
}
