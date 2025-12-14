package com.pd.im.service.conversation.model;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class UpdateConversationReq extends RequestBase {

    @NotBlank(message = "会话id不能为空")
    private String conversationId;

    private Integer isMute;

    private Integer isTop;

    @NotBlank(message = "fromId不能为空")
    private String fromId;
}
