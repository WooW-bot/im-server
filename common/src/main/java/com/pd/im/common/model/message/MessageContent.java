package com.pd.im.common.model.message;

import com.pd.im.common.model.ClientInfo;
import lombok.Data;

/**
 * clientType、imei、appId 来自于原始消息头，是在tcp层组装进MessageContent的。
 * command 一般是来自于原始消息头，也是在tcp层组装进MessageContent的。
 *
 * @author Parker
 * @date 12/5/25
 */
@Data
public class MessageContent extends ClientInfo {
    private String messageId;

    private String fromId;

    private String toId;

    private String messageBody;

    private Long messageTime;

    private String extra;

    private Long messageKey;

    private Integer messageRandom;

    private long messageSequence;
}
