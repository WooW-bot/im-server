package com.pd.im.common.enums.group;

/**
 * @author Parker
 * @date 12/8/25
 */
public enum GroupStatus {
    /**
     * 1正常 2解散 其他待定比如封禁...
     */
    NORMAL(1),

    DESTROY(2),

    ;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static GroupStatus getEnum(Integer ordinal) {

        if(ordinal == null){
            return null;
        }

        for (int i = 0; i < GroupStatus.values().length; i++) {
            if (GroupStatus.values()[i].getCode() == ordinal) {
                return GroupStatus.values()[i];
            }
        }
        return null;
    }

    private int code;

    GroupStatus(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
