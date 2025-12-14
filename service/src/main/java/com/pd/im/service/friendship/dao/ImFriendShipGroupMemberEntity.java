package com.pd.im.service.friendship.dao;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
@TableName("im_friendship_group_member")
public class ImFriendShipGroupMemberEntity {
    // 使用组合主键：group_id + to_id
    private Long groupId;
    private String toId;
}
