package com.pd.im.service.friendship.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 移除黑名单请求（支持批量）
 *
 * @author Parker
 * @date 12/8/25
 */
@Data
public class DeleteBlackReq extends RequestBase {

    @NotBlank(message = "用户id不能为空")
    private String fromId;

    @NotEmpty(message = "目标用户列表不能为空")
    private List<String> toIds;
}
