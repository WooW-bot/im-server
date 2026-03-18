package com.pd.im.service.user.model.resp;

import lombok.Data;

/**
 * @author Parker
 * @date 3/18/26
 */
@Data
public class LoginResp {
    private String ticket;
    private String ip;
    private Integer port;
}
