package com.pd.im.service.callback.impl;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.util.HttpRequestUtils;
import com.pd.im.service.callback.CallbackService;
import com.pd.im.service.utils.ShareThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Parker
 * @date 12/5/25
 */
@Slf4j
@Component
public class CallbackServiceImpl implements CallbackService {

    @Autowired
    HttpRequestUtils httpRequestUtils;

    @Autowired
    AppConfig appConfig;

    @Autowired
    ShareThreadPool shareThreadPool;

    @Override
    public ResponseVO beforeCallback(Integer appId, String callbackCommand, String jsonBody) {
        try {
            ResponseVO responseVO = httpRequestUtils.doPost(
                    // 方法回调地址
                    // TODO 目前只是将回调地址存储在配置文件中，后续要将其存放在表里持久化
                    appConfig.getCallbackUrl(),
                    // 指定返回值类型
                    ResponseVO.class,
                    // 请求参数，内部集成了 appId 和 callbackCommand
                    builderUrlParams(appId, callbackCommand),
                    // 回调内容
                    jsonBody,
                    // 指定字符集，为 null 默认 UTF8
                    null
            );
            return responseVO;
        } catch (Exception e) {
            log.error("Callback 回调 {} : {} 出现异常 : {} ", callbackCommand, appId, e.getMessage());
            // 回调失败也需要放行，避免阻碍正常程序执行，运维通过最高级别日志快速定位问题所在
            return ResponseVO.successResponse();
        }
    }

    @Override
    public void afterCallback(Integer appId, String callbackCommand, String jsonBody) {
        shareThreadPool.submit(() -> {
            try {
                httpRequestUtils.doPost(appConfig.getCallbackUrl(), Object.class, builderUrlParams(appId, callbackCommand),
                        jsonBody, null);
            } catch (Exception e) {
                log.error("callback 回调{} : {}出现异常 ： {} ", callbackCommand, appId, e.getMessage());
            }
        });
    }

    public Map builderUrlParams(Integer appId, String command) {
        Map map = new HashMap();
        map.put("appId", appId);
        map.put("command", command);
        return map;
    }
}
