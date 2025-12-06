package com.pd.im.service.conversation.service;

/**
 * @author Parker
 * @date 12/5/25
 */
public interface ConversationService {
    static String convertConversationId(Integer type, String fromId, String toId) {
        return type + "_" + fromId + "_" + toId;
    }
}
