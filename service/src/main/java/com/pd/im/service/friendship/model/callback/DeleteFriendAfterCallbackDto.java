package com.pd.im.service.friendship.model.callback;

import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class DeleteFriendAfterCallbackDto {

    private String fromId;

    private String toId;
}
