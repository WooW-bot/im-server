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
        boolean exists = zkClient.exists(Constants.ZKConstants.ImCoreZkRoot);
        if (!exists) {
            zkClient.createPersistent(Constants.ZKConstants.ImCoreZkRoot);
        }
        String tcpParentPath = Constants.ZKConstants.ImCoreZkRoot + Constants.ZKConstants.ImCoreZkRootTcp;
        boolean tcpExists = zkClient.exists(tcpParentPath);
        if (!tcpExists) {
            zkClient.createPersistent(tcpParentPath);
        }
        String webParentPath = Constants.ZKConstants.ImCoreZkRoot + Constants.ZKConstants.ImCoreZkRootWeb;
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
