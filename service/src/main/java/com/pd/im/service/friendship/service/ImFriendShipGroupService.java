package com.pd.im.service.friendship.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.friendship.dao.ImFriendShipGroupEntity;
import com.pd.im.service.friendship.model.req.*;

/**
 * 好友分组服务接口
 * 参考腾讯云文档: https://cloud.tencent.com/document/product/269/10107
 *
 * @author Parker
 * @date 12/9/25
 */
public interface ImFriendShipGroupService {
    /**
     * 添加好友分组
     * 参考: https://cloud.tencent.com/document/product/269/10107
     *
     * @param req 添加分组请求
     * @return 操作结果
     */
    ResponseVO addGroup(AddFriendShipGroupReq req);

    /**
     * 删除好友分组
     * 参考: https://cloud.tencent.com/document/product/269/10108
     *
     * @param req 删除分组请求
     * @return 操作结果
     */
    ResponseVO deleteGroup(DeleteFriendShipGroupReq req);

    /**
     * 获取好友分组信息
     *
     * @param fromId    所属用户ID
     * @param groupName 分组名称
     * @param appId     应用ID
     * @return 分组实体
     */
    ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId, String groupName, Integer appId);

    /**
     * 更新分组序列号
     *
     * @param fromId    所属用户ID
     * @param groupName 分组名称
     * @param appId     应用ID
     * @return 新的序列号
     */
    Long updateSeq(String fromId, String groupName, Integer appId);
}
