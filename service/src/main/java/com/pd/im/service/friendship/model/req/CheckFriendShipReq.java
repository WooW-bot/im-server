package com.pd.im.service.friendship.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class CheckFriendShipReq extends RequestBase {

    @NotBlank(message = "fromId不能为空")
    private String fromId;

    @NotEmpty(message = "toIds不能为空")
    private List<String> toIds;

    @NotNull(message = "checkType不能为空")
    private Integer checkType;
}
