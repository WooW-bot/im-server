package com.pd.im.common.enums.conversation;

import com.pd.im.common.exception.ApplicationExceptionEnum;

/**
 * @author Parker
 * @date 12/9/25
 */
public enum ConversationErrorCode implements ApplicationExceptionEnum {

    CONVERSATION_UPDATE_PARAM_ERROR(50000,"會話修改參數錯誤"),
    CONVERSATION_CREATE_FAIL(50001, "会话创建失败"),
    ;

    private int code;
    private String error;

    ConversationErrorCode(int code, String error){
        this.code = code;
        this.error = error;
    }
    public int getCode() {
        return this.code;
    }

    public String getError() {
        return this.error;
    }
}
