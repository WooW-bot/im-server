package com.pd.im.service.conversation.model;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class CreateConversationReq extends RequestBase {

    /**
     * 会话类型
     */
    @NotBlank(message = "会话类型不能为空")
    private Integer conversationType;

    @NotBlank(message = "fromId 不能为空")
    private String fromId;

    /**
     * 目标对象 Id 或者群组 Id
     */
    @NotBlank(message = "toId 不能为空")
    private String toId;
}
