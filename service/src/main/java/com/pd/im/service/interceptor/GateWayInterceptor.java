package com.pd.im.service.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.enums.BaseErrorCode;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.enums.GatewayErrorCode;
import com.pd.im.common.exception.ApplicationExceptionEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * @author Parker
 * @date 12/3/25
 * @description GateWayInterceptor类
 */
@Component
@Slf4j
public class GateWayInterceptor implements HandlerInterceptor {

    @Autowired
    IdentityCheck identityCheck;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取appId 操作人 userSign
        String appIdStr = request.getParameter("appId");
        if (StringUtils.isBlank(appIdStr)) {
            resp(ResponseVO.errorResponse(GatewayErrorCode.APPID_NOT_EXIST), response);
            return false;
        }

        String identifier = request.getParameter("identifier");
        if (StringUtils.isBlank(identifier)) {
            resp(ResponseVO.errorResponse(GatewayErrorCode.OPERATER_NOT_EXIST), response);
            return false;
        }

        String userSign = request.getParameter("userSign");
        if (StringUtils.isBlank(userSign)) {
            resp(ResponseVO.errorResponse(GatewayErrorCode.USERSIGN_NOT_EXIST), response);
            return false;
        }

        // 签名和操作人和appid是否匹配
        ApplicationExceptionEnum result = identityCheck.checkUserSig(identifier, appIdStr, userSign);
        if (result != BaseErrorCode.SUCCESS) {
            resp(ResponseVO.errorResponse(result), response);
            return false;
        }

        return true;
    }

    private void resp(ResponseVO<?> respVo, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        // CORS headers - ideally handled globally, but kept for compatibility if no global config exists
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "*");
        response.setHeader("Access-Control-Allow-Headers", "*");
        response.setHeader("Access-Control-Max-Age", "3600");

        try (PrintWriter writer = response.getWriter()) {
            String resp = JSONObject.toJSONString(respVo);
            writer.write(resp);
            writer.flush();
        } catch (Exception e) {
            log.error("GateWayInterceptor Response Write Exception", e);
        }
    }
}
