package com.pd.im.common.route.algorithm.random;

import com.pd.im.common.enums.user.UserErrorCode;
import com.pd.im.common.exception.ApplicationException;
import com.pd.im.common.route.RouteHandler;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机负载均衡
 *
 * @author Parker
 * @date 12/8/25
 */
public class RandomHandler implements RouteHandler {

    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if (size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        int i = ThreadLocalRandom.current().nextInt(size);
        return values.get(i);
    }
}
