package com.pd.im.codec.pack.message;

import lombok.Data;

/**
 * @author Parker
 * @date 12/5/25
 */
@Data
public class MessageReceiveServerAckPack {
    private Long messageKey;
    private String fromId;
    private String toId;
    private Long messageSequence;
    private Boolean serverSend;
}
