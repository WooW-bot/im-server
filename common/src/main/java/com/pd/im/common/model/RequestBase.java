package com.pd.im.common.model;

import lombok.Data;

/**
 * @author Parker
 * @date 12/7/25
 */
@Data
public class RequestBase {
    private Integer appId;
    private String operator;
    private Integer clientType;
    private String imei;
}
