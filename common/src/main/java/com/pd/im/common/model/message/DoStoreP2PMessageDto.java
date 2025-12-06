package com.pd.im.common.model.message;

import com.pd.im.common.model.message.content.MessageBody;
import lombok.Data;

/**
 * P2P消息存储DTO
 * <p>
 * 用于MQ传输，包含完整的消息内容和消息体
 * 消息内容(MessageContent)包含业务字段，消息体(MessageBody)用于持久化
 *
 * @author Parker
 * @date 12/5/25
 */
@Data
public class DoStoreP2PMessageDto {

    /**
     * P2P消息内容（包含fromId、toId等业务字段）
     */
    private MessageContent messageContent;

    /**
     * 消息体（用于数据库持久化的核心字段）
     */
    private MessageBody messageBody;
}
