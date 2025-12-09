package com.pd.im.service.group.model.resp;

import lombok.Data;

/**
 * @author Parker
 * @date 12/7/25
 */
@Data
public class GetRoleInGroupResp {
    private Long groupMemberId;
    private String memberId;
    private Integer role;
    private Long speakDate;
}
