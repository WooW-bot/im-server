package com.pd.im.codec.proto;

import lombok.Data;

/**
 * @author Parker
 * @date 12/7/25
 */
@Data
public class MessageReadPack {
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
