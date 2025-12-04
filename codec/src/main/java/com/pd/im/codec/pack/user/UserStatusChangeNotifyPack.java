package com.pd.im.codec.pack.user;

import com.pd.im.common.model.UserSession;
import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/4/25
 */
@Data
public class UserStatusChangeNotifyPack {
    private Integer appId;
    private String userId;
    private Integer status;
    private List<UserSession> client;
}
