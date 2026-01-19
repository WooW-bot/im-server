package com.pd.im.service.group.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class TransferGroupReq extends RequestBase {

    @NotNull(message = "群id不能为空")
    private String groupId;

    private String ownerId;
}
