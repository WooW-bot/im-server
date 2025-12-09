package com.pd.im.codec.proto.group;

import lombok.Data;

/**
 * 踢人出群通知报文
 * @author Parker
 * @date 12/8/25
 */
@Data
public class RemoveGroupMemberPack {
    private String groupId;
    private String member;
}
