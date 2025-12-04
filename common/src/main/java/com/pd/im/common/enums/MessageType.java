package com.pd.im.common.enums;

/**
 * @author Parker
 * @date 12/3/25
 */
public enum MessageType implements CodeAdapter {
    // 0x0. json、 0x1. protobuf、 0x2. xml
    DATA_TYPE_JSON(0x0),
    DATA_TYPE_PROTOBUF(0x1),
    DATA_TYPE_XML(0x2);

    private Integer msgType;

    MessageType(Integer msgType) {
        this.msgType = msgType;
    }

    @Override
    public Integer getCode() {
        return msgType;
    }
}
