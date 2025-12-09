package com.pd.im.common.enums.group;

/**
 * @author Parker
 * @date 12/8/25
 */
public enum GroupType {
    /**
     * 群类型 1私有群（类似微信） 2公开群(类似qq）
     */
    PRIVATE(1),

    PUBLIC(2),

    ;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static GroupType getEnum(Integer ordinal) {

        if(ordinal == null){
            return null;
        }

        for (int i = 0; i < GroupType.values().length; i++) {
            if (GroupType.values()[i].getCode() == ordinal) {
                return GroupType.values()[i];
            }
        }
        return null;
    }

    private int code;

    GroupType(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
