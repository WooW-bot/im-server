package com.pd.im.common.util;

import com.pd.im.common.enums.BaseErrorCode;
import com.pd.im.common.exception.ApplicationException;
import com.pd.im.common.route.RouteInfo;

/**
 * @author Parker
 * @date 12/8/25
 */
public class RouteInfoParser {
    public static RouteInfo parse(String info) {
        try {
            String[] serverInfo = info.split(":");
            RouteInfo routeInfo = new RouteInfo(serverInfo[0], Integer.parseInt(serverInfo[1]));
            return routeInfo;
        } catch (Exception e) {
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR);
        }
    }
}
