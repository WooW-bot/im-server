package com.pd.im.service.user.model.req;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * @author Parker
 * @date 12/2/25
 * @description LoginReq类
 */
@Data
public class LoginReq {
    @NotNull(message = "appId不能为空")
    private Integer appId;
    @NotNull(message = "用户id不能位空")
    private String userId;
    private Integer clientType;
    private String imei;
    private String operator;
}
