package com.pd.im.service.group.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pd.im.service.group.dao.ImGroupMemberEntity;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Parker
 * @date 12/6/25
 */
@Repository
public interface ImGroupMemberMapper extends BaseMapper<ImGroupMemberEntity> {

    @Select("select " +
            " member_id " +
            " from im_group_member where app_id = #{appId} AND group_id = #{groupId} and role != 3")
    public List<String> getGroupMemberIds(Integer appId, String groupId);
}
