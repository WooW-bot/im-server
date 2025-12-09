package com.pd.im.service.friendship.model.callback;

import com.pd.im.service.friendship.model.req.FriendDto;
import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class AddFriendAfterCallbackDto {

    private String fromId;

    private FriendDto toItem;
}
