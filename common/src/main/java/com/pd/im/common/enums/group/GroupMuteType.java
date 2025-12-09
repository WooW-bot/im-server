package com.pd.im.common.enums.group;

/**
 * @author Parker
 * @date 12/7/25
 */
public enum GroupMuteType {
    /**
     * 是否全员禁言，0 不禁言；1 全员禁言。
     */
    NOT_MUTE(0),

    MUTE(1),

    ;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     *
     * @param ordinal
     * @return
     */
    public static GroupMuteType getEnum(Integer ordinal) {
        if (ordinal == null) {
            return null;
        }
        for (int i = 0; i < GroupMuteType.values().length; i++) {
            if (GroupMuteType.values()[i].getCode().equals(ordinal)) {
                return GroupMuteType.values()[i];
            }
        }
        return null;
    }

    private Integer code;

    GroupMuteType(int code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
