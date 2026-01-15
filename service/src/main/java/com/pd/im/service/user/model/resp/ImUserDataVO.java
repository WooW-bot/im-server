package com.pd.im.service.user.model.resp;

import lombok.Data;

/**
 * 用户资料VO - 仅包含公开信息
 * 参考 OpenIM 和 Tencent IM 字段设计
 *
 * @author Parker
 * @date 1/15/26
 */
@Data
public class ImUserDataVO {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String nickName;

    /**
     * 地址
     */
    private String location;

    /**
     * 生日 (YYYY-MM-DD)
     */
    private String birthday;

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
