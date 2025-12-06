package com.pd.im.common.model.message;

import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/5/25
 */
@Data
public class GroupChatMessageContent extends MessageContent {
    private String groupId;

    private List<String> memberIds;
}
