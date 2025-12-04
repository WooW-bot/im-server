package com.pd.im.common.enums.device;

import com.pd.im.common.enums.CodeAdapter;

/**
 * @author Parker
 * @date 12/4/25
 */
public enum ConnectState implements CodeAdapter {
    // 1.在线 2.离线
    CONNECT_STATE_ONLINE(1),
    CONNECT_STATE_OFFLINE(2),
    ;

    private Integer state;

    ConnectState(Integer state) {
        this.state = state;
    }

    @Override
    public Integer getCode() {
        return state;
    }
}
