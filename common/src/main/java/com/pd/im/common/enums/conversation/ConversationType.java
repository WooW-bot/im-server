package com.pd.im.common.enums.conversation;

/**
 * @author Parker
 * @date 12/5/25
 */
public enum ConversationType {
    /**
     * 0 单聊
     * 1 群聊
     * 2 机器人
     * 3 公众号
     */
    P2P(0),

    GROUP(1),

    ROBOT(2),
    ;

    private Integer code;

    ConversationType(int code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
