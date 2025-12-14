package com.pd.im.common.enums.command;

/**
 * @author Parker
 * @date 12/3/25
 */
public enum MessageCommand implements Command {
    //单聊消息 1103
    MSG_P2P(0x44F),  // 1103

    //单聊消息 ACK 1046
    MSG_ACK(0x416),  // 1046

    //消息收到 ACK 1107
    MSG_RECEIVE_ACK(0x453),  // 1107

    //发送消息已读 1106
    MSG_READ(0x452),  // 1106

    //消息已读通知给同步端 1053
    MSG_READ_NOTIFY(0x41D),  // 1053

    //消息已读回执，给原消息发送方 1054
    MSG_READ_RECEIPT(0x41E),  // 1054

    //消息撤回 1050
    MSG_RECALL(0x41A),  // 1050

    //消息撤回通知 1052
    MSG_RECALL_NOTIFY(0x41C),  // 1052

    //消息撤回回报 1051
    MSG_RECALL_ACK(0x41B);  // 1051

    private Integer command;

    MessageCommand(Integer command) {
        this.command = command;
    }


    @Override
    public Integer getCommand() {
        return command;
    }
}
