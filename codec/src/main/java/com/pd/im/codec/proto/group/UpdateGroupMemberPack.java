package com.pd.im.codec.proto.group;

import lombok.Data;

/**
 * 修改群成员通知报文
 *
 * @author Parker
 * @date 12/8/25
 */
@Data
public class UpdateGroupMemberPack {
    private String groupId;
    private String memberId;
    private String alias;
    private String extra;
}
