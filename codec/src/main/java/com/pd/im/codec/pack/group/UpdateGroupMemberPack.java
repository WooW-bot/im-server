package com.pd.im.codec.pack.group;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class UpdateGroupMemberPack {

    private String groupId;

    private String memberId;

    private String alias;

    private String extra;
}
