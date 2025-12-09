package com.pd.im.service.user.service;

import com.pd.im.service.user.model.req.PullFriendOnlineStatusReq;
import com.pd.im.service.user.model.req.PullUserOnlineStatusReq;
import com.pd.im.service.user.model.req.SetUserCustomerStatusReq;
import com.pd.im.service.user.model.req.SubscribeUserOnlineStatusReq;
import com.pd.im.service.user.model.resp.UserOnlineStatusResp;

import java.util.Map;

/**
 * @author Parker
 * @date 12/8/25
 */
public interface ImUserStatusService {
    // void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content);
    void subscribeUserOnlineStatus(SubscribeUserOnlineStatusReq req);

    void setUserCustomerStatus(SetUserCustomerStatusReq req);

    Map<String, UserOnlineStatusResp> queryFriendOnlineStatus(PullFriendOnlineStatusReq req);

    Map<String, UserOnlineStatusResp> queryUserOnlineStatus(PullUserOnlineStatusReq req);
}
