package com.pd.im.codec.pack.friendship;

import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class UpdateFriendPack {
    public String fromId;
    private String toId;
    private String remark;
    private Long sequence;
}
