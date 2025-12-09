package com.pd.im.codec.pack.group;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class GroupMemberSpeakPack {
    private String groupId;
    private String memberId;
    private Long speakDate;
}
