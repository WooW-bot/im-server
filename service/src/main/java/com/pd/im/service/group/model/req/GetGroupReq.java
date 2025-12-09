package com.pd.im.service.group.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class GetGroupReq extends RequestBase {

    private String groupId;
}
