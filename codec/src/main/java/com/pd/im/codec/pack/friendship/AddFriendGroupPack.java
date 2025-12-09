package com.pd.im.codec.pack.friendship;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class AddFriendGroupPack {
    public String fromId;
    private String groupName;
    /** 序列号*/
    private Long sequence;
}
