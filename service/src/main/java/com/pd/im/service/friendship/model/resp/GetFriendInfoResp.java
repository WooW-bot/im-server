package com.pd.im.service.friendship.model.resp;

import com.pd.im.service.friendship.dao.ImFriendShipEntity;
import lombok.Data;

/**
 * 获取好友信息响应
 * 
 * @author Parker
 * @date 2026-01-27
 */
@Data
public class GetFriendInfoResp {
    /**
     * 目标用户ID
     */
    private String toId;

    /**
     * 结果码: 0-成功, -1-好友关系不存在
     */
    private Integer resultCode;

    /**
     * 结果信息
     */
    private String resultInfo;

    /**
     * 好友信息（当resultCode=0时有值）
     */
    private ImFriendShipEntity friendInfo;
}
