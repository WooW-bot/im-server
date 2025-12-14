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
 * @author Parker
 * @date 12/9/25
 */
@RestController
@RequestMapping("v1/conversation")
public class ConversationController {
    @Autowired
    ConversationService conversationService;

    @RequestMapping("/deleteConversation")
    public ResponseVO deleteConversation(@RequestBody @Validated DeleteConversationReq req) {
        return conversationService.deleteConversation(req);
    }

    @RequestMapping("/updateConversation")
    public ResponseVO updateConversation(@RequestBody @Validated UpdateConversationReq req) {
        return conversationService.updateConversation(req);
    }

    @RequestMapping("/syncConversationList")
    public ResponseVO syncConversationList(@RequestBody @Validated SyncRequest req) {
        return conversationService.syncConversationSet(req);
    }
}
