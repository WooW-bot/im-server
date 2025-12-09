package com.pd.im.service.group.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class GetJoinedGroupReq extends RequestBase {
    @NotBlank(message = "用户id不能为空")
    private String memberId;
    //群类型
    private List<Integer> groupType;
    //单次拉取的群组数量，如果不填代表所有群组
    private Integer limit;
    //第几页
    private Integer offset;
}
