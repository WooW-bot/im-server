package com.pd.im.tcp.rabbitmq.process;

import com.pd.im.codec.proto.MessagePack;
import com.pd.im.tcp.utils.UserChannelRepository;
import io.netty.channel.Channel;

/**
 * @author Parker
 * @date 12/5/25
 */
public abstract class BaseProcess {

    public void process(MessagePack messagePack) {
        processBefore();

        Channel userChannel = UserChannelRepository.getUserChannel(messagePack.getAppId(),
                messagePack.getToId(), messagePack.getClientType(), messagePack.getImei());
        if (userChannel != null) {
            // 数据通道写入消息内容
            userChannel.writeAndFlush(messagePack);
        }

        processAfter();
    }

    /**
     * 流程执行前的定制化处理
     */
    public abstract void processBefore();

    /**
     * 流程执行后的定制化处理
     */
    public abstract void processAfter();
}
