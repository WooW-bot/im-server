package com.pd.im.message.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author Parker
 * @date 12/6/25
 */
@Data
@TableName("im_group_message_history")
public class ImGroupMessageHistoryEntity {
    private Integer appId;

    private String fromId;

    private String groupId;

    /** messageBodyId*/
    private Long messageKey;
    /** 序列号*/
    private Long sequence;

    private Integer messageRandom;

    private Long messageTime;

    private Long createTime;
}
