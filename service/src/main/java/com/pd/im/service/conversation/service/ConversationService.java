package com.pd.im.service.conversation.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncReq;
import com.pd.im.common.model.message.MessageReadContent;
import com.pd.im.service.conversation.model.*;

/**
 * @author Parker
 * @date 12/5/25
 */
public interface ConversationService {
    static String convertConversationId(Integer type, String fromId, String toId) {
        return type + "_" + fromId + "_" + toId;
    }

    ResponseVO createConversation(CreateConversationReq req);

    /**
     * 标记用户已读消息情况，记录 Seq 消息偏序
     *
     * @param messageReadContent
     */
    void messageMarkRead(MessageReadContent messageReadContent);

    /**
     * 删除会话
     *
     * @param req
     * @return
     */
    ResponseVO deleteConversation(DeleteConversationReq req);

    /**
     * 更新会话: 置顶、免打扰
     *
     * @param req
     * @return
     */
    ResponseVO updateConversation(UpdateConversationReq req);

    /**
     * 同步客户端本地 Seq 与服务端最大 Seq
     *
     * @param req 数据结构为{客户端最大 Seq，服务端一次响应的最大次数}
     * @return
     */
    ResponseVO syncConversationSet(SyncReq req);
}
