package com.pd.im.common.model.message;

import com.pd.im.common.model.ClientInfo;
import lombok.Data;

/**
 * @author Parker
 * @date 12/5/25
 */
@Data
public class RecallMessageContent extends ClientInfo {
    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageTime;

    private Long messageSequence;

    private Integer conversationType;
}
