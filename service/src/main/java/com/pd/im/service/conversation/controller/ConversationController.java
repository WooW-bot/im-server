package com.pd.im.service.conversation.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.service.conversation.model.*;
import com.pd.im.service.conversation.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话控制器
 * 参考腾讯云文档: https://cloud.tencent.com/document/product/269/62116
 *
 * @author Parker
 * @date 12/9/25
 */
@RestController
@RequestMapping("v1/conversation")
public class ConversationController {
    @Autowired
    ConversationService conversationService;

    /**
     * 删除会话
     * 参考: https://cloud.tencent.com/document/product/269/62119
     *
     * @param req DeleteConversationReq
     * @return ResponseVO
     */
    @RequestMapping("/deleteConversation")
    public ResponseVO deleteConversation(@RequestBody @Validated DeleteConversationReq req, Integer appId) {
        req.setAppId(appId);
        return conversationService.deleteConversation(req);
    }

    /**
     * 更新会话 (置顶/免打扰/草稿)
     * 对应 SDK 的 setConversationDraft/pinConversation 等操作
     *
     * @param req UpdateConversationReq
     * @return ResponseVO
     */
    @RequestMapping("/updateConversation")
    public ResponseVO updateConversation(@RequestBody @Validated UpdateConversationReq req, Integer appId) {
        req.setAppId(appId);
        return conversationService.updateConversation(req);
    }

    /**
     * 同步/获取会话列表
     * 参考: https://cloud.tencent.com/document/product/269/62118
     *
     * @param req SyncRequest
     * @return ResponseVO
     */
    @RequestMapping("/syncConversationList")
    public ResponseVO syncConversationList(@RequestBody @Validated SyncRequest req, Integer appId) {
        req.setAppId(appId);
        return conversationService.syncConversationSet(req);
    }
}
