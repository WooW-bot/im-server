package com.pd.im.common.model.message;

import lombok.Data;

/**
 * @author Parker
 * @date 12/5/25
 */
@Data
public class MessageBody {
    private Integer appId;

    /** messageBodyId*/
    private Long messageKey;

    /** messageBody*/
    private String messageBody;

    private String securityKey;

    private Long messageTime;

    private Long createTime;

    private String extra;

    private Integer delFlag;
}
