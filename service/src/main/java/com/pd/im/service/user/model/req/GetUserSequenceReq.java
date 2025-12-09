package com.pd.im.service.user.model.req;

import com.pd.im.common.model.RequestBase;
import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class GetUserSequenceReq extends RequestBase {
    private String userId;
}
