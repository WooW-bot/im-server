package com.pd.im.service.friendship.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer appId;

    // 使用组合主键：group_id + to_id
    private Long groupId;
    @TableField(value = "to_id")
    private String toId;
}
