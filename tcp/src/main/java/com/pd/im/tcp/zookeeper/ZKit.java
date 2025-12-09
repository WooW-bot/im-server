package com.pd.im.tcp.zookeeper;

import com.pd.im.common.constant.Constants;
import org.I0Itec.zkclient.ZkClient;

/**
 * @author Parker
 * @date 12/5/25
 */
public class ZKit {
    private ZkClient zkClient;

    public ZKit(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    //im-coreRoot/tcp/ip:port
    public void createRootNode() {
        boolean exists = zkClient.exists(Constants.ZKConstants.IM_CORE_ZK_ROOT);
        if (!exists) {
            zkClient.createPersistent(Constants.ZKConstants.IM_CORE_ZK_ROOT);
        }
        String tcpParentPath = Constants.ZKConstants.IM_CORE_ZK_ROOT + Constants.ZKConstants.IM_CORE_ZK_ROOT_TCP;
        boolean tcpExists = zkClient.exists(tcpParentPath);
        if (!tcpExists) {
            zkClient.createPersistent(tcpParentPath);
        }
        String webParentPath = Constants.ZKConstants.IM_CORE_ZK_ROOT + Constants.ZKConstants.IM_CORE_ZK_ROOT_WEB;
        boolean webExists = zkClient.exists(webParentPath);
        if (!webExists) {
            zkClient.createPersistent(webParentPath);
        }
    }

    //ip+port
    public void createNode(String path) {
        if (!zkClient.exists(path)) {
            zkClient.createEphemeral(path);
        }
    }
}
