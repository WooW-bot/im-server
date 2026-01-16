package com.pd.im.service.group.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pd.im.service.group.dao.ImGroupMemberEntity;
import com.pd.im.service.group.model.req.GroupMemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author Parker
 * @date 12/6/25
 */
@Mapper
public interface ImGroupMemberMapper extends BaseMapper<ImGroupMemberEntity> {

    @Select("select group_id from im_group_member where app_id = #{appId} AND member_id = #{memberId} ")
    public List<String> getJoinedGroupIds(Integer appId, String memberId);

    @Select("select group_id from im_group_member where app_id = #{appId} AND member_id = #{memberId} and role != #{role}")
    public List<String> syncJoinedGroupIds(Integer appId, String memberId, int role);

    @Results({
            @Result(column = "member_id", property = "memberId"),
//            @Result(column = "mute_flag", property = "muteFlag"),
            @Result(column = "mute_end_time", property = "muteEndTime"),
            @Result(column = "role", property = "role"),
            @Result(column = "alias", property = "alias"),
            @Result(column = "join_time", property = "joinTime"),
            @Result(column = "join_type", property = "joinType")
    })
    @Select("select " +
            " member_id, " +
//            " mute_flag,  " +
            " mute_end_time,  " +
            " role, " +
            " alias, " +
            " join_time ," +
            " join_type " +
            " from im_group_member where app_id = #{appId} AND group_id = #{groupId} ")
    public List<GroupMemberDto> getGroupMembers(Integer appId, String groupId);

    @Select("select " +
            " member_id " +
            " from im_group_member where app_id = #{appId} AND group_id = #{groupId} and role != 3")
    public List<String> getGroupMemberIds(Integer appId, String groupId);

    @Results({
            @Result(column = "member_id", property = "memberId"),
//            @Result(column = "mute_flag", property = "muteFlag"),
            @Result(column = "role", property = "role")
//            @Result(column = "alias", property = "alias"),
//            @Result(column = "join_time", property = "joinTime"),
//            @Result(column = "join_type", property = "joinType")
    })
    @Select("select " +
            " member_id, " +
//            " mute_flag,  " +
            " role " +
//            " alias, " +
//            " join_time ," +
//            " join_type " +
            " from im_group_member where app_id = #{appId} AND group_id = #{groupId} and role in (1,2) ")
    public List<GroupMemberDto> getGroupManagers(String groupId, Integer appId);
}
