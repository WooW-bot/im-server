package com.pd.im.service.user.model.resp;

import com.pd.im.common.model.UserSession;
import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class UserOnlineStatusResp {
    private List<UserSession> session;
    private String customText;
    private Integer customStatus;
}
