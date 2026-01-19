package com.pd.im.message.config;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
public class EasySqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(org.apache.ibatis.session.Configuration configuration,
            Class<?> mapperClass, com.baomidou.mybatisplus.core.metadata.TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(configuration, mapperClass, tableInfo);
        methodList.add(new InsertBatchSomeColumn()); // 添加InsertBatchSomeColumn方法
        return methodList;
    }
}
