package com.pd.im.common.enums.friend;

/**
 * @author Parker
 * @date 12/9/25
 */
public enum CheckFriendshipType {
    /**
     * 1 单方校验；
     * 2 双方校验。
     */
    SINGLE(1),

    BOTH(2),
    ;

    private int type;

    CheckFriendshipType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
