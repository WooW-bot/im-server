package com.pd.im.service.message.service.check;

import com.pd.im.common.ResponseVO;

/**
 * @author Parker
 * @date 12/7/25
 */
public interface CheckSendMessageService {

    /**
     * 检查发送人是否被禁言或者是禁用
     *
     * @param fromId
     * @param appId
     * @return
     */
    ResponseVO checkSenderForbidAndMute(String fromId, Integer appId);

    /**
     * 检查好友关系链
     *
     * @param fromId 己方
     * @param toId   对方
     * @param appId  平台 SDK
     * @return
     */
    ResponseVO checkFriendShip(String fromId, String toId, Integer appId);

    /**
     * 检查群组是否能发送消息
     *
     * @param fromId
     * @param groupId
     * @param appId
     * @return
     */
    ResponseVO checkGroupMessage(String fromId, String groupId, Integer appId);
}
