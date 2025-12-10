package com.pd.im.service.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.pd.im.common.constant.Constants.ZKConstants.*;

/**
 * @author Parker
 * @date 12/8/25
 */
@Slf4j
@Component
public class ZKit {
    @Autowired
    private ZkClient zkClient;

    /**
     * 从 Zk 获取所有 TCP 服务节点地址
     *
     * @return
     */
    public List<String> getAllTcpNode() {
        List<String> children = zkClient.getChildren(IM_CORE_ZK_ROOT + IM_CORE_ZK_ROOT_TCP);
        log.info("Query all [TCP] node =[{}] success.", JSON.toJSONString(children));
        return children;
    }

    /**
     * 从 Zk 获取所有 WEB 服务节点地址
     *
     * @return
     */
    public List<String> getAllWebNode() {
        List<String> children = zkClient.getChildren(IM_CORE_ZK_ROOT + IM_CORE_ZK_ROOT_WEB);
        log.info("Query all [WEB] node =[{}] success.", JSON.toJSONString(children));
        return children;
    }
}
