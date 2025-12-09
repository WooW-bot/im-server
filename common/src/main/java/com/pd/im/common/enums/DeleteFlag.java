package com.pd.im.common.enums;

public enum DeleteFlag {

    /**
     * 0 正常；1 删除。
     */
    NORMAL(0),

    DELETE(1),
    ;

    private int code;

    DeleteFlag(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
