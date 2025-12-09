package com.pd.im.codec.pack.group;

import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class AddGroupMemberPack {
    private String groupId;
    private List<String> members;
}
