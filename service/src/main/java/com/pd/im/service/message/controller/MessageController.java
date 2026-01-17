package com.pd.im.service.message.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.common.model.message.CheckSendMessageReq;
import com.pd.im.service.message.model.req.SendMessageReq;
import com.pd.im.service.message.service.GroupMessageService;
import com.pd.im.service.message.service.sync.MessageSyncService;
import com.pd.im.service.message.service.P2PMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台发送消息控制层
 *
 * @author Parker
 * @date 12/7/25
 */
@RestController
@RequestMapping("v1/message")
public class MessageController {
    @Autowired
    P2PMessageService p2PMessageService;

    @Autowired
    MessageSyncService messageSyncService;

    @Autowired
    GroupMessageService groupMessageService;

    /**
     * 后台消息发送接口
     * 参考: https://cloud.tencent.com/document/product/269/2282
     *
     * @param req SendMessageReq
     * @return ResponseVO
     */
    @RequestMapping("/send")
    public ResponseVO send(@RequestBody @Validated SendMessageReq req) {
        return ResponseVO.successResponse(p2PMessageService.send(req));
    }

    /**
     * Feign RPC 调用 [P2P] 内部接口
     *
     * @param req
     * @return
     */
    @RequestMapping("/p2pCheckSend")
    public ResponseVO checkP2PSend(@RequestBody @Validated CheckSendMessageReq req) {
        return p2PMessageService.serverPermissionCheck(req.getFromId(), req.getToId(), req.getAppId());
    }

    /**
     * Feign RPC 调用 [GROUP] 内部接口
     *
     * @param req
     * @return
     */
    @RequestMapping("/groupCheckSend")
    public ResponseVO checkGroupSend(@RequestBody @Validated CheckSendMessageReq req) {
        return groupMessageService.serverPermissionCheck(req.getFromId(), req.getToId(), req.getAppId());
    }

    /**
     * 同步离线消息
     * 参考: https://cloud.tencent.com/document/product/269/42794
     *
     * @param req SyncRequest
     * @return ResponseVO
     */
    @RequestMapping("/syncOfflineMessageList")
    public ResponseVO syncP2POfflineMessageList(@RequestBody @Validated SyncRequest req) {
        return messageSyncService.syncOfflineMessage(req);
    }
}
