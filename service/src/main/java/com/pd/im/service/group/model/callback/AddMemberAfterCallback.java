package com.pd.im.service.group.model.callback;

import com.pd.im.service.group.model.resp.AddMemberResp;
import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class AddMemberAfterCallback {
    private String groupId;
    private Integer groupType;
    private String operator;
    private List<AddMemberResp> memberId;
}
