package com.pd.im.service.conversation.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.common.model.message.MessageReadContent;
import com.pd.im.service.conversation.model.*;

/**
 * 会话服务接口
 * 参考腾讯云文档: https://cloud.tencent.com/document/product/269/62116
 *
 * @author Parker
 * @date 12/5/25
 */
public interface ConversationService {
    static String convertConversationId(Integer type, String fromId, String toId) {
        return type + "_" + fromId + "_" + toId;
    }

    /**
     * 创建会话
     *
     * @param req CreateConversationReq
     * @return ResponseVO
     */
    ResponseVO createConversation(CreateConversationReq req);

    /**
     * 标记用户已读消息情况，记录 Seq 消息偏序
     *
     * @param messageReadContent
     */
    void messageMarkRead(MessageReadContent messageReadContent);

    /**
     * 删除会话
     * 参考: https://cloud.tencent.com/document/product/269/62119
     *
     * @param req DeleteConversationReq
     * @return ResponseVO
     */
    ResponseVO deleteConversation(DeleteConversationReq req);

    /**
     * 更新会话: 置顶、免打扰
     * 对应 SDK 的 setConversationDraft/pinConversation 等操作
     *
     * @param req UpdateConversationReq
     * @return ResponseVO
     */
    ResponseVO updateConversation(UpdateConversationReq req);

    /**
     * 同步客户端本地 Seq 与服务端最大 Seq (拉取会话列表)
     * 参考: https://cloud.tencent.com/document/product/269/62118
     *
     * @param req 数据结构为{客户端最大 Seq，服务端一次响应的最大次数}
     * @return ResponseVO
     */
    ResponseVO syncConversationSet(SyncRequest req);
}
