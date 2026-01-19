package com.pd.im.service.conversation.model;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class CreateConversationReq extends RequestBase {

    /**
     * 会话类型
     */
    @NotNull(message = "会话类型不能为空")
    private Integer conversationType;

    @NotBlank(message = "fromId 不能为空")
    private String fromId;

    /**
     * 目标对象 Id 或者群组 Id
     */
    @NotBlank(message = "toId 不能为空")
    private String toId;
}
