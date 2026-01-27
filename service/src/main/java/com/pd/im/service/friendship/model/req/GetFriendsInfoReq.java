package com.pd.im.service.friendship.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 拉取指定好友请求
 * 参考腾讯IM: https://cloud.tencent.com/document/product/269/8609
 * 
 * @author Parker
 * @date 2026-01-27
 */
@Data
public class GetFriendsInfoReq extends RequestBase {
    @NotBlank(message = "fromId不能为空")
    private String fromId;

    @NotEmpty(message = "toIds不能为空")
    private List<String> toIds;
}
