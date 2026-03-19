package com.pd.im.service.conversation.dao;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
@TableName("im_conversation_set")
public class ImConversationSetEntity {
    /** 会话id 0_fromId_toId */
    @TableId(value = "conversation_id")
    private String conversationId;

    /** 会话类型 */
    @TableField("conversation_type")
    private Integer conversationType;

    @TableField("from_id")
    private String fromId;

    /** 目标对象 Id 或者群组 Id */
    @TableField("to_id")
    private String toId;

    /** 是否禁言 */
    @TableField("is_mute")
    private Integer isMute;

    /** 是否置顶消息 */
    @TableField("is_top")
    private Integer isTop;

    @TableField("sequence")
    private Long sequence;

    /** 消息已读偏序 */
    @TableField("read_sequence")
    private Long readSequence;

    @TableField("app_id")
    private Integer appId;

    @TableField("create_time")
    private Long createTime;

    @TableField("update_time")
    private Long updateTime;
}
