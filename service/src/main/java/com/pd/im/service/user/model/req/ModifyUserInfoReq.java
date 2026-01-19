package com.pd.im.service.user.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class ModifyUserInfoReq extends RequestBase {
    @NotEmpty(message = "用户id不能为空")
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

    /**
     * 扩展字段 (JSON)
     */
    private String extra;
}
