package com.pd.im.common.route.algorithm.loop;

import com.pd.im.common.BaseErrorCode;
import com.pd.im.common.exception.ApplicationException;
import com.pd.im.common.route.RouteHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Parker
 * @date 12/8/25
 */
public class LoopHandler implements RouteHandler {
    private AtomicLong index = new AtomicLong();

    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if (size == 0) {
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR);
        }
        Long l = index.incrementAndGet() % size;
        if (l < 0) {
            l = 0L;
        }
        return values.get(l.intValue());
    }
}
