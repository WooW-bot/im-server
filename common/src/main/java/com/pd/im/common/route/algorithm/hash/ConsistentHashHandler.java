package com.pd.im.common.route.algorithm.hash;

import com.pd.im.common.route.RouteHandler;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
public class ConsistentHashHandler implements RouteHandler {
    private AbstractConsistentHash hash;

    public void setHash(AbstractConsistentHash hash) {
        this.hash = hash;
    }

    @Override
    public String routeServer(List<String> values, String key) {
        return hash.process(values, key);
    }
}
