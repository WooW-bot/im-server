package com.pd.im.codec.proto.group;

import lombok.Data;

import java.util.List;

/**
 * 群内添加群成员通知报文
 *
 * @author Parker
 * @date 12/8/25
 */
@Data
public class AddGroupMemberPack {
    private String groupId;
    private List<String> members;
}
