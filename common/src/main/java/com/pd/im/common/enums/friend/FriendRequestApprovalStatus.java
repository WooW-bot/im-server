package com.pd.im.common.enums.friend;

import lombok.Getter;

/**
 * @author Parker
 * @date 12/9/25
 */
@Getter
public enum FriendRequestApprovalStatus {
    /**
     * 默认状态
     */
    NORMAL(0),

    /**
     * 已同意
     */
    AGREE(1),

    /**
     * 已拒绝
     */
    REJECT(2),
    ;

    private final int code;

    FriendRequestApprovalStatus(int code) {
        this.code = code;
    }

    /**
     * 匹配判断（类似你之前的 isMe 逻辑）
     */
    public boolean isMe(Integer code) {
        return code != null && this.code == code;
    }
}
