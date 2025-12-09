package com.pd.im.service.group.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncReq;
import com.pd.im.service.group.dao.ImGroupEntity;
import com.pd.im.service.group.model.req.*;

/**
 * @author Parker
 * @date 12/7/25
 */
public interface ImGroupService {
    ResponseVO importGroup(ImportGroupReq req);

    ResponseVO createGroup(CreateGroupReq req);

    ResponseVO updateBaseGroupInfo(UpdateGroupReq req);

    ResponseVO getJoinedGroup(GetJoinedGroupReq req);

    ResponseVO destroyGroup(DestroyGroupReq req);

    ResponseVO transferGroup(TransferGroupReq req);

    ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId);

    ResponseVO getGroup(GetGroupReq req);

    ResponseVO muteGroup(MuteGroupReq req);

    ResponseVO syncJoinedGroupList(SyncReq req);

    Long getUserGroupMaxSeq(String userId, Integer appId);
}
