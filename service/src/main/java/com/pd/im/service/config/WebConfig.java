package com.pd.im.service.config;

import com.pd.im.service.interceptor.GateWayInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Parker
 * @date 12/3/25
 * @description WebConfig类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    GateWayInterceptor gateWayInterceptor;
}
