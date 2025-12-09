package com.pd.im.common.enums.friend;

/**
 * @author Parker
 * @date 12/9/25
 */
public enum FriendRequestApprovalStatus {
    /**
     * 默认状态
     */
    NORMAL(0),

    /**
     * 1 同意；2 拒绝。
     */
    AGREE(1),

    REJECT(2),
    ;

    private int code;

    FriendRequestApprovalStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
