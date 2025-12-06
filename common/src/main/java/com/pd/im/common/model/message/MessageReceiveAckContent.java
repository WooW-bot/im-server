package com.pd.im.common.model.message;

import com.pd.im.common.model.ClientInfo;
import lombok.Data;

/**
 * 消息确认收到 ACK
 *
 * @author Parker
 * @date 12/5/25
 */
@Data
public class MessageReceiveAckContent extends ClientInfo {
    /**
     * 消息唯一标识
     */
    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageSequence;
}
