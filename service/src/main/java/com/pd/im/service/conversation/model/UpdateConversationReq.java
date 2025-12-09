package com.pd.im.service.conversation.model;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class UpdateConversationReq extends RequestBase {

    private String conversationId;

    private Integer isMute;

    private Integer isTop;

    private String fromId;
}
