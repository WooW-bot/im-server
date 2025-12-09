package com.pd.im.common.enums.command;

/**
 * @author Parker
 * @date 12/9/25
 */
public enum ConversationEventCommand implements Command {

    //删除会话 5000 -> 0x1388
    CONVERSATION_DELETE(0x1388),

    //更新会话 5001 -> 0x1389
    CONVERSATION_UPDATE(0x1389),

    ;

    private Integer command;

    ConversationEventCommand(Integer command) {
        this.command = command;
    }

    @Override
    public Integer getCommand() {
        return command;
    }
}
