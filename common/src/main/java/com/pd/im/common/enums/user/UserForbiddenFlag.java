package com.pd.im.common.enums.user;

/**
 * @author Parker
 * @date 12/7/25
 */
public enum UserForbiddenFlag {
    /**
     * 0 正常；1 禁用。
     */
    NORMAL(0),

    FORBIBBEN(1),
    ;

    private int code;

    UserForbiddenFlag(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
