package com.pd.im.tcp.zookeeper;

import com.pd.im.codec.config.ImBootstrapConfig;
import com.pd.im.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Parker
 * @date 12/5/25
 */
@Slf4j
public class RegistryZK implements Runnable {
    private ZKit zKit;
    private String ip;
    private ImBootstrapConfig.TcpConfig tcpConfig;

    public RegistryZK(ZKit zKit, String ip, ImBootstrapConfig.TcpConfig tcpConfig) {
        this.zKit = zKit;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }

    @Override
    public void run() {
        zKit.createRootNode();
        String tcpPath = Constants.ZKConstants.ImCoreZkRoot + Constants.ZKConstants.ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();
        zKit.createNode(tcpPath);
        log.info("Registry zookeeper tcpPath success, msg=[{}]", tcpPath);

        String webPath = Constants.ZKConstants.ImCoreZkRoot + Constants.ZKConstants.ImCoreZkRootWeb + "/" + ip + ":" + tcpConfig.getWebSocketPort();
        zKit.createNode(webPath);
        log.info("Registry zookeeper webPath success, msg=[{}]", tcpPath);
    }
}
