package com.pd.im.codec.pack.friendship;

import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class AddFriendGroupMemberPack {
    public String fromId;
    private String groupName;
    private List<String> toIds;
    /** 序列号*/
    private Long sequence;
}
