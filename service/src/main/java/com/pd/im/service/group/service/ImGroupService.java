package com.pd.im.service.group.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.service.group.dao.ImGroupEntity;
import com.pd.im.service.group.model.req.*;

/**
 * @author Parker
 * @date 12/7/25
 */
public interface ImGroupService {
    /**
     * 导入群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1615
     *
     * @param req ImportGroupReq
     * @return ResponseVO
     */
    ResponseVO importGroup(ImportGroupReq req);

    /**
     * 创建群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1615
     *
     * @param req CreateGroupReq
     * @return ResponseVO
     */
    ResponseVO createGroup(CreateGroupReq req);

    /**
     * 修改群组基础资料
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1620
     *
     * @param req UpdateGroupReq
     * @return ResponseVO
     */
    ResponseVO updateBaseGroupInfo(UpdateGroupReq req);

    /**
     * 获取用户所加入的群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1625
     *
     * @param req GetJoinedGroupReq
     * @return ResponseVO
     */
    ResponseVO getJoinedGroup(GetJoinedGroupReq req);

    /**
     * 解散群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1624
     *
     * @param req DestroyGroupReq
     * @return ResponseVO
     */
    ResponseVO destroyGroup(DestroyGroupReq req);

    /**
     * 转让群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1633
     *
     * @param req TransferGroupReq
     * @return ResponseVO
     */
    ResponseVO transferGroup(TransferGroupReq req);

    /**
     * 获取群组详细资料 (Internal Use Only)
     *
     * @param groupId String
     * @param appId   Integer
     * @return ResponseVO
     */
    ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId);

    /**
     * 获取群组详细资料
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1616
     *
     * @param req GetGroupReq
     * @return ResponseVO
     */
    ResponseVO getGroup(GetGroupReq req);

    /**
     * 批量禁言和取消禁言
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1627
     *
     * @param req MuteGroupReq
     * @return ResponseVO
     */
    ResponseVO muteGroup(MuteGroupReq req);

    ResponseVO syncJoinedGroupList(SyncRequest req);

    Long getUserGroupMaxSeq(String userId, Integer appId);
}
