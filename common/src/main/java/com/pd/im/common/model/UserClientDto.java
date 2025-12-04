package com.pd.im.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Parker
 * @date 12/3/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserClientDto {
    private Integer appId;
    private Integer clientType;
    private String userId;
    private String imei;
}
