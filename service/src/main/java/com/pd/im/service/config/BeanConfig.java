package com.pd.im.service.config;

import com.pd.im.common.config.AppConfig;
import com.pd.im.common.enums.route.RouteHashMethod;
import com.pd.im.common.enums.route.UrlRouteMode;
import com.pd.im.common.route.RouteHandler;
import com.pd.im.common.route.algorithm.hash.AbstractConsistentHash;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * @author Parker
 * @date 12/8/25
 */
@Configuration
public class BeanConfig {
    @Autowired
    AppConfig appConfig;

    @Bean
    public ZkClient buildZKClient() {
        return new ZkClient(appConfig.getZkAddr(),
                appConfig.getZkConnectTimeOut());
    }

    @Bean
    public RouteHandler routeHandle() throws Exception {
        Integer imRouteModel = appConfig.getImRouteModel();
        String routeModel = "";

        // 配置文件指定使用哪种路由策略
        UrlRouteMode handler = UrlRouteMode.getHandler(imRouteModel);
        routeModel = handler.getClazz();

        // 反射机制调用具体的类对象执行对应方法
        RouteHandler routeHandler = (RouteHandler) Class.forName(routeModel).newInstance();
        // 特判，一致性哈希可以指定底层数据结构
        if (UrlRouteMode.HASH.equals(handler)) {
            Method setHash = Class.forName(routeModel).getMethod("setHash", AbstractConsistentHash.class);
            Integer consistentHashModel = appConfig.getConsistentHashModel();
            String hashModel = "";

            RouteHashMethod hashHandler = RouteHashMethod.getHandler(consistentHashModel);
            hashModel = hashHandler.getClazz();
            AbstractConsistentHash consistentHash = (AbstractConsistentHash) Class.forName(hashModel).newInstance();
            setHash.invoke(routeHandler, consistentHash);
        }
        return routeHandler;
    }

    @Bean
    public EasySqlInjector easySqlInjector() {
        return new EasySqlInjector();
    }
}
