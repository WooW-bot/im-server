package com.pd.im.service.friendship.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class AddFriendShipGroupReq extends RequestBase {

    @NotBlank(message = "fromId不能为空")
    public String fromId;

    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    private List<String> toIds;
}
