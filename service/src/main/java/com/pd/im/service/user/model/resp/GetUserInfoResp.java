package com.pd.im.service.user.model.resp;

import com.pd.im.service.user.dao.ImUserDataEntity;
import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class GetUserInfoResp {
    private List<ImUserDataEntity> userDataItem;
    private List<String> failUser;
}
