package com.pd.im.common.model.message;

import com.pd.im.common.model.ClientInfo;
import lombok.Data;

/**
 * 消息已读数据包
 *
 * @author Parker
 * @date 12/5/25
 */
@Data
public class MessageReadContent extends ClientInfo {
    /**
     * 消息已读偏序
     */
    private long messageSequence;

    /**
     * 要么 fromId + toId
     */
    private String fromId;
    private String toId;

    /**
     * 要么 groupId
     */
    private String groupId;

    /**
     * 标识消息来源于单聊还是群聊
     */
    private Integer conversationType;
}
