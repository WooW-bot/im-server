package com.pd.im.service.support.ids;

/**
 * @author Parker
 * @date 12/5/25
 */
public class ConversationIdGenerate {
    /**
     * 小的 id 放前面
     *
     * @param fromId
     * @param toId
     * @return
     */
    public static String generateP2PId(String fromId, String toId) {
        int i = fromId.compareTo(toId);
        if (i < 0) {
            return toId + "|" + fromId;
        } else if (i > 0) {
            return fromId + "|" + toId;
        }

        throw new RuntimeException("");
    }
}
