package com.pd.im.common.enums.user;

/**
 * @author Parker
 * @date 12/7/25
 */
public enum UserSilentFlagEnum {
    /**
     * 0 正常；1 禁言。
     */
    NORMAL(0),

    MUTE(1),
    ;

    private int code;

    UserSilentFlagEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
