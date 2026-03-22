package com.pd.im.service.friendship.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pd.im.service.friendship.dao.ImFriendShipRequestEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author Parker
 * @date 12/9/25
 */
@Mapper
public interface ImFriendShipRequestMapper extends BaseMapper<ImFriendShipRequestEntity> {

  /**
   * 获取用户收到的好友申请的最大序列号
   *
   * @param appId  应用ID
   * @param userId 用户ID (接收人)
   * @return 最大序列号
   */
  @Select("SELECT IFNULL(MAX(sequence), 0) FROM im_friendship_request " +
      "WHERE app_id = #{appId} AND (to_id = #{userId} OR from_id = #{userId})")
  Long getFriendShipRequestMaxSeq(@Param("appId") Integer appId, @Param("userId") String userId);
}
