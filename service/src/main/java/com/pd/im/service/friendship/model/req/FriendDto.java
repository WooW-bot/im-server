package com.pd.im.service.friendship.model.req;

import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class FriendDto {

    private String toId;

    private String remark;

    private String addSource;

    private String extra;

    private String addWording;
}
