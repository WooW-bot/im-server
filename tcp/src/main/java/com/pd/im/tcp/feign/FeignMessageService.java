package com.pd.im.tcp.feign;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.message.CheckSendMessageReq;
import feign.Headers;
import feign.RequestLine;

/**
 * @author Parker
 * @date 12/3/25
 */
public interface FeignMessageService {
    /**
     * RPC 调度业务层的接口，接口职责为检查 [P2P] 发送方是否有权限
     *
     * @param o
     * @return
     */
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @RequestLine("POST /message/p2pCheckSend")
    ResponseVO checkP2PSendMessage(CheckSendMessageReq o);

    /**
     * RPC 调度业务层的接口，接口职责为检查 [GROUP] 发送方是否有权限
     *
     * @param o
     * @return
     */
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @RequestLine("POST /message/groupCheckSend")
    ResponseVO checkGroupSendMessage(CheckSendMessageReq o);
}
