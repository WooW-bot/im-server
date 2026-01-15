package com.pd.im.codec.pack.user;

import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class UserModifyPack {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String nickName;

    /**
     * 密码
     */
    private String password;

    /**
     * 头像URL
     */
    private String faceUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 个性签名
     */
    private String selfSignature;

    /**
     * 加好友验证类型
     * 1: 需要验证
     */
    private Integer friendAllowType;
}
