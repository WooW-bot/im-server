package com.pd.im.service.user.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author BanTanger 半糖
 */
@Data
public class DeleteUserReq extends RequestBase {
    @NotEmpty(message = "用户id不能为空")
    private List<String> userId;
}
