package com.pd.im.codec.pack.message;

import lombok.Data;

/**
 * @author Parker
 * @date 12/4/25
 */
@Data
public class ChatMessageAck {
    private String messageId;
    private Long messageSequence;

    public ChatMessageAck(String messageId) {
        this.messageId = messageId;
    }

    public ChatMessageAck(String messageId, Long messageSequence) {
        this.messageId = messageId;
        this.messageSequence = messageSequence;
    }
}
