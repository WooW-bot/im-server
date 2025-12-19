package com.pd.im.service.friendship.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.RequestBase;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.service.friendship.model.req.*;

import java.util.List;

/**
 * IM好友服务接口
 *
 * <p>提供好友关系管理的核心功能:
 * <ul>
 *   <li>好友关系: 导入、添加、更新、删除、查询</li>
 *   <li>黑名单管理: 添加、删除、校验</li>
 *   <li>关系校验: 单向/双向好友关系和黑名单校验</li>
 *   <li>增量同步: 好友列表增量拉取</li>
 * </ul>
 *
 * @author Parker
 * @date 12/7/25
 */
public interface ImFriendService {

    /**
     * 批量导入好友关系
     * 用于数据迁移或批量初始化场景
     * https://cloud.tencent.com/document/product/269/8301
     *
     * @param req 导入请求,包含批量好友数据
     * @return 导入结果, 包含成功和失败的ID列表
     */
    ResponseVO importFriendShip(ImportFriendShipReq req);

    /**
     * 添加好友
     * 支持两种模式:
     * 1. 对方无需验证: 直接建立好友关系
     * 2. 对方需要验证: 创建好友申请记录
     * https://cloud.tencent.com/document/product/269/1643
     *
     * @param req 添加好友请求
     * @return 操作结果
     */
    ResponseVO addFriend(AddFriendReq req);

    /**
     * 更新好友信息
     * 可更新备注、来源、扩展字段等
     * https://cloud.tencent.com/document/product/269/12525
     *
     * @param req 更新请求
     * @return 操作结果
     */
    ResponseVO updateFriend(UpdateFriendReq req);

    /**
     * 删除好友
     * 软删除,修改关系状态为已删除
     * https://cloud.tencent.com/document/product/269/1644
     *
     * @param req 删除请求,包含fromId和toId
     * @return 操作结果
     */
    ResponseVO deleteFriend(DeleteFriendReq req);

    /**
     * 删除所有好友
     * 批量软删除当前用户的所有好友关系
     * https://cloud.tencent.com/document/product/269/1645
     *
     * @param req 删除请求,包含fromId
     * @return 操作结果
     */
    ResponseVO deleteAllFriend(DeleteFriendReq req);

    /**
     * 获取用户的所有好友关系
     * 包含已删除和黑名单的好友
     *
     * @param req 查询请求,包含fromId
     * @return 好友关系列表
     */
    ResponseVO getAllFriendShip(GetAllFriendShipReq req);

    /**
     * 查询指定的好友关系
     * 检查数据库中是否存在fromId到toId的关系记录
     *
     * @param req 查询请求,包含fromId和toId
     * @return 好友关系实体
     */
    ResponseVO getRelation(GetRelationReq req);

    /**
     * 执行添加好友操作
     * 内部方法,直接建立双向好友关系,不进行验证流程
     *
     * @param requestBase 请求基础信息
     * @param fromId      发起方用户ID
     * @param dto         好友信息DTO
     * @param appId       应用ID
     * @return 操作结果
     */
    ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId);

    /**
     * 校验好友关系
     * 支持单向和双向校验
     * https://cloud.tencent.com/document/product/269/1646
     *
     * @param req 校验请求,包含checkType、fromId、toIds
     * @return 校验结果列表
     */
    ResponseVO checkFriendship(CheckFriendShipReq req);

    /**
     * 添加黑名单
     * 将目标用户加入黑名单
     *
     * @param req 添加黑名单请求
     * @return 操作结果
     */
    ResponseVO addBlack(AddFriendShipBlackReq req);

    /**
     * 移除黑名单
     * 将目标用户从黑名单移除
     *
     * @param req 删除黑名单请求
     * @return 操作结果
     */
    ResponseVO deleteBlack(DeleteBlackReq req);

    /**
     * 校验黑名单关系
     * 支持单向和双向校验
     *
     * @param req 校验请求,包含checkType、fromId、toIds
     * @return 校验结果列表
     */
    ResponseVO checkBlack(CheckFriendShipReq req);

    /**
     * 增量同步好友列表
     * 根据序列号拉取增量好友数据
     *
     * @param req 同步请求,包含lastSequence和maxLimit
     * @return 同步响应, 包含数据列表和是否完成标志
     */
    ResponseVO syncFriendshipList(SyncRequest req);

    /**
     * 获取用户的好友ID列表
     * 仅返回正常状态且未拉黑的好友ID
     *
     * @param userId 用户ID
     * @param appId  应用ID
     * @return 好友ID列表
     */
    List<String> getFriendIds(String userId, Integer appId);
}
