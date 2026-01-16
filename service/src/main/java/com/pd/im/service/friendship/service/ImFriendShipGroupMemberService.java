package com.pd.im.service.friendship.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.friendship.model.req.*;

/**
 * 好友分组成员服务接口
 * 参考腾讯云文档: https://cloud.tencent.com/document/product/269/10107
 *
 * @author Parker
 * @date 12/9/25
 */
public interface ImFriendShipGroupMemberService {
    /**
     * 添加分组成员
     * 参考: https://cloud.tencent.com/document/product/269/10107
     *
     * @param req 添加成员请求
     * @return 操作结果
     */
    ResponseVO addGroupMember(AddFriendShipGroupMemberReq req);

    /**
     * 删除分组成员
     * 参考: https://cloud.tencent.com/document/product/269/10108
     *
     * @param req 删除成员请求
     * @return 操作结果
     */
    ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req);

    /**
     * 执行添加分组成员(内部调用)
     *
     * @param groupId 分组ID
     * @param toId    目标用户ID
     * @return 影响行数
     */
    int doAddGroupMember(Long groupId, String toId);

    /**
     * 清空分组成员(内部调用)
     *
     * @param groupId 分组ID
     * @return 影响行数
     */
    int clearGroupMember(Long groupId);
}
