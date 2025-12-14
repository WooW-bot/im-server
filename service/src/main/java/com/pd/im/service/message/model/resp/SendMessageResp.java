package com.pd.im.service.message.model.resp;

import lombok.Data;

/**
 * @author Parker
 * @date 12/7/25
 */
@Data
public class SendMessageResp {
    private String messageId;
    private Long messageKey;
    private Long messageTime;
}
