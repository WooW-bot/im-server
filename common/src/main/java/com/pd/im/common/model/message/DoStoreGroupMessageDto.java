package com.pd.im.common.model.message;

import com.pd.im.common.model.message.content.MessageBody;
import lombok.Data;

/**
 * 群消息存储DTO
 * <p>
 * 用于MQ传输，包含完整的群消息内容和消息体
 * 消息内容(GroupChatMessageContent)包含业务字段，消息体(MessageBody)用于持久化
 *
 * @author Parker
 * @date 12/6/25
 */
@Data
public class DoStoreGroupMessageDto {

    /**
     * 群消息内容（包含fromId、groupId等业务字段）
     */
    private GroupChatMessageContent messageContent;

    /**
     * 消息体（用于数据库持久化的核心字段）
     */
    private MessageBody messageBody;
}
