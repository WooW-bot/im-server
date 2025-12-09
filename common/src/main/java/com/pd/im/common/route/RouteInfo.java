package com.pd.im.common.route;

import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public final class RouteInfo {
    private String ip;
    private Integer port;

    public RouteInfo(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }
}
