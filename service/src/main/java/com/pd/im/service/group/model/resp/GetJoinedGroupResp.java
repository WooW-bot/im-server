package com.pd.im.service.group.model.resp;

import com.pd.im.service.group.dao.ImGroupEntity;
import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class GetJoinedGroupResp {

    private Integer totalCount;

    private List<ImGroupEntity> groupList;
}
