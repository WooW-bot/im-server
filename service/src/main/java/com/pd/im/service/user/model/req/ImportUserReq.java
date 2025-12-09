package com.pd.im.service.user.model.req;

import com.pd.im.common.model.RequestBase;
import com.pd.im.service.user.dao.ImUserDataEntity;
import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class ImportUserReq extends RequestBase {
    private List<ImUserDataEntity> userData;
}
