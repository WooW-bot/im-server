package com.pd.im.codec.pack.friendship;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class ReadAllFriendRequestPack {

    private String fromId;

    private Long sequence;
}
