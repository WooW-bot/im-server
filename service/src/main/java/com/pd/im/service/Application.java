package com.pd.im.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Parker
 * @date 12/2/25
 * @description Application类
 */
@SpringBootApplication(scanBasePackages = {"com.pd.im.service", "com.pd.im.common"})
@MapperScan("com.pd.im.service.*.dao.mapper")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
