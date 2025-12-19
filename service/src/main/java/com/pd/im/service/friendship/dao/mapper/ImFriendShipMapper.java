package com.pd.im.service.friendship.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pd.im.service.friendship.dao.ImFriendShipEntity;
import com.pd.im.service.friendship.model.req.CheckFriendShipReq;
import com.pd.im.service.friendship.model.resp.CheckFriendShipResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友关系Mapper接口
 *
 * <p>功能说明:
 * <ul>
 *   <li>单向校验好友关系: 仅检查 A->B 的关系</li>
 *   <li>双向校验好友关系: 检查 A<->B 的双向关系</li>
 * </ul>
 *
 * <p>参考文档:
 * <a href="https://cloud.tencent.com/document/product/269/1501">腾讯云好友关系链</a>
 *
 * @author Parker
 * @date 12/7/25
 */
@Mapper
public interface ImFriendShipMapper extends BaseMapper<ImFriendShipEntity> {

    /**
     * 单向校验好友关系
     * 检查fromId到toIds的单向好友关系状态
     *
     * @param req 校验请求,包含fromId、toIds、appId
     * @return 校验结果列表,status: 1-是好友 0-不是好友
     */
    @Select("<script>" +
            "SELECT from_id AS fromId, to_id AS toId, IF(status = 1, 1, 0) AS status " +
            "FROM im_friendship " +
            "WHERE from_id = #{fromId} " +
            "  AND to_id IN " +
            "  <foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('> " +
            "    #{id}" +
            "  </foreach>" +
            "  AND app_id = #{appId} " +
            "</script>")
    List<CheckFriendShipResp> checkFriendShip(CheckFriendShipReq req);

    /**
     * 双向校验好友关系
     * 检查fromId与toIds之间的双向好友关系状态
     *
     * @param req 校验请求,包含fromId、toIds、appId
     * @return 校验结果列表,status: 1-双向好友 2-A单向B 3-B单向A 4-无关系
     */
    @Select("<script>" +
            "SELECT a.fromId, a.toId, " +
            "  (CASE " +
            "    WHEN a.status = 1 AND b.status = 1 THEN 1 " +
            "    WHEN a.status = 1 AND b.status != 1 THEN 2 " +
            "    WHEN a.status != 1 AND b.status = 1 THEN 3 " +
            "    WHEN a.status != 1 AND b.status != 1 THEN 4 " +
            "  END) AS status " +
            "FROM " +
            "  (SELECT from_id AS fromId, to_id AS toId, IF(status = 1, 1, 0) AS status " +
            "   FROM im_friendship " +
            "   WHERE app_id = #{appId} AND from_id = #{fromId} AND to_id IN " +
            "   <foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            "     #{id}" +
            "   </foreach>" +
            "  ) AS a " +
            "INNER JOIN " +
            "  (SELECT from_id AS fromId, to_id AS toId, IF(status = 1, 1, 0) AS status " +
            "   FROM im_friendship " +
            "   WHERE app_id = #{appId} AND to_id = #{fromId} AND from_id IN " +
            "   <foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            "     #{id}" +
            "   </foreach>" +
            "  ) AS b " +
            "ON a.fromId = b.toId AND b.fromId = a.toId " +
            "</script>")
    List<CheckFriendShipResp> checkFriendShipBoth(CheckFriendShipReq req);

    /**
     * 单向校验黑名单关系
     * 检查fromId是否将toIds加入黑名单
     *
     * @param req 校验请求,包含fromId、toIds、appId
     * @return 校验结果列表,status: 1-已拉黑 0-未拉黑
     */
    @Select("<script>" +
            "SELECT from_id AS fromId, to_id AS toId, IF(black = 1, 1, 0) AS status " +
            "FROM im_friendship " +
            "WHERE app_id = #{appId} " +
            "  AND from_id = #{fromId} " +
            "  AND to_id IN " +
            "  <foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            "    #{id}" +
            "  </foreach>" +
            "</script>")
    List<CheckFriendShipResp> checkFriendShipBlack(CheckFriendShipReq req);

    /**
     * 双向校验黑名单关系
     * 检查fromId与toIds之间的双向黑名单状态
     *
     * @param req 校验请求,包含fromId、toIds、appId
     * @return 校验结果列表,status: 1-互相拉黑 2-A拉黑B 3-B拉黑A 4-无拉黑
     */
    @Select("<script>" +
            "SELECT a.fromId, a.toId, " +
            "  (CASE " +
            "    WHEN a.black = 1 AND b.black = 1 THEN 1 " +
            "    WHEN a.black = 1 AND b.black != 1 THEN 2 " +
            "    WHEN a.black != 1 AND b.black = 1 THEN 3 " +
            "    WHEN a.black != 1 AND b.black != 1 THEN 4 " +
            "  END) AS status " +
            "FROM " +
            "  (SELECT from_id AS fromId, to_id AS toId, IF(black = 1, 1, 0) AS black " +
            "   FROM im_friendship " +
            "   WHERE app_id = #{appId} AND from_id = #{fromId} AND to_id IN " +
            "   <foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            "     #{id}" +
            "   </foreach>" +
            "  ) AS a " +
            "INNER JOIN " +
            "  (SELECT from_id AS fromId, to_id AS toId, IF(black = 1, 1, 0) AS black " +
            "   FROM im_friendship " +
            "   WHERE app_id = #{appId} AND to_id = #{fromId} AND from_id IN " +
            "   <foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            "     #{id}" +
            "   </foreach>" +
            "  ) AS b " +
            "ON a.fromId = b.toId AND b.fromId = a.toId " +
            "</script>")
    List<CheckFriendShipResp> checkFriendShipBlackBoth(CheckFriendShipReq req);

    /**
     * 获取用户的最大好友序列号
     * 用于增量同步
     *
     * @param appId 应用ID
     * @param userId 用户ID
     * @return 最大序列号
     */
    @Select("SELECT MAX(friend_sequence) FROM im_friendship " +
            "WHERE app_id = #{appId} AND from_id = #{userId}")
    Long getFriendShipMaxSeq(@Param("appId") Integer appId, @Param("userId") String userId);

    /**
     * 获取用户的好友ID列表
     * 仅返回正常状态且未拉黑的好友
     *
     * @param userId 用户ID
     * @param appId 应用ID
     * @return 好友ID列表
     */
    @Select("SELECT to_id FROM im_friendship " +
            "WHERE from_id = #{userId} " +
            "  AND app_id = #{appId} " +
            "  AND status = 1 " +
            "  AND (black != 1 OR black IS NULL)")
    List<String> getFriendIds(@Param("userId") String userId, @Param("appId") Integer appId);
}
