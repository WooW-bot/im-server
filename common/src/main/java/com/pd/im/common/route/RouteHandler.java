package com.pd.im.common.route;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
public interface RouteHandler {
    String routeServer(List<String> values, String key);
}
