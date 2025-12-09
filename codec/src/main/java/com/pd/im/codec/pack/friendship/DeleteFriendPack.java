package com.pd.im.codec.pack.friendship;

import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class DeleteFriendPack {
    private String fromId;
    private String toId;
    private Long sequence;
}
