package com.pd.im.message.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Parker
 * @date 12/8/25
 */
@Configuration
public class BeanConfig {
    @Bean
    public EasySqlInjector easySqlInjector () {
        return new EasySqlInjector();
    }
}
