package com.pd.im.common.enums.user;

/**
 * @author Parker
 * @date 12/9/25
 */
public enum UserTypeEnum {

    IM_USER(1),

    APP_ADMIN(100),
    ;

    private int code;

    UserTypeEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
