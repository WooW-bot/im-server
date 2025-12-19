package com.pd.im.service.friendship.dao;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.jeffreyning.mybatisplus.anno.AutoMap;
import com.pd.im.common.enums.friend.FriendshipStatus;
import lombok.Data;

/**
 * IM好友关系实体类
 * 对应数据库表: im_friendship
 * 索引说明:
 * - 联合主键: app_id + from_id + to_id
 * - 索引: idx_app_from (app_id, from_id)
 * - 索引: idx_app_to (app_id, to_id)
 *
 * @author Parker
 * @date 12/7/25
 */
@Data
@TableName("im_friendship")
@AutoMap
public class ImFriendShipEntity {

    /**
     * 应用ID
     */
    @TableField(value = "app_id")
    private Integer appId;

    /**
     * 用户ID
     */
    @TableField(value = "from_id")
    private String fromId;

    /**
     * 好友用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 好友备注名
     */
    private String remark;

    /**
     * 好友关系状态
     * 0-未添加 1-正常 2-已删除
     * @see FriendshipStatus
     */
    private Integer status;

    /**
     * 黑名单状态
     * 0-正常(未拉黑) 1-已拉黑
     * @see FriendshipStatus
     */
    private Integer black;

    /**
     * 创建时间(时间戳,毫秒)
     */
    private Long createTime;

    /**
     * 好友关系序列号,用于增量同步
     */
    private Long friendSequence;

    /**
     * 黑名单序列号,用于增量同步
     */
    private Long blackSequence;

    /**
     * 好友来源渠道
     */
    private String addSource;

    /**
     * 扩展字段,JSON格式
     */
    private String extra;
}
