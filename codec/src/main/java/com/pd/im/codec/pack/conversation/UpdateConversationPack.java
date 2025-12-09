package com.pd.im.codec.pack.conversation;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class UpdateConversationPack {
    private String conversationId;
    private Integer isMute;
    private Integer isTop;
    private Integer conversationType;
    private Long sequence;
}
