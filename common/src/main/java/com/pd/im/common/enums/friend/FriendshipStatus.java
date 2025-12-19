package com.pd.im.common.enums.friend;

/**
 * @author Parker
 * @date 12/7/25
 */
public enum FriendshipStatus {
    /**
     * 好友关系状态
     * 0-未添加 1-正常 2-已删除
     */
    FRIEND_STATUS_NO_FRIEND(0),

    FRIEND_STATUS_NORMAL(1),

    FRIEND_STATUS_DELETE(2),

    /**
     * 黑名单状态
     * 0-正常(未拉黑) 1-已拉黑
     */
    BLACK_STATUS_NORMAL(0),

    BLACK_STATUS_BLACKED(1),
    ;

    private Integer code;

    FriendshipStatus(int code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
