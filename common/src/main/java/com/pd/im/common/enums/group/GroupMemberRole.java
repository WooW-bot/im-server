package com.pd.im.common.enums.group;

/**
 * @author Parker
 * @date 12/7/25
 */
public enum GroupMemberRole {
    /**
     * 普通成员
     */
    ORDINARY(0),

    /**
     * 管理员
     */
    MANAGER(1),

    /**
     * 群主
     */
    OWNER(2),

    /**
     * 离开
     */
    LEAVE(3);;


    private Integer code;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     *
     * @param ordinal
     * @return
     */
    public static GroupMemberRole getItem(int ordinal) {
        for (int i = 0; i < GroupMemberRole.values().length; i++) {
            if (GroupMemberRole.values()[i].getCode() == ordinal) {
                return GroupMemberRole.values()[i];
            }
        }
        return null;
    }

    GroupMemberRole(int code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
