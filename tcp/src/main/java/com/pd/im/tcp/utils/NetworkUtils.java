package com.pd.im.tcp.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 网络工具类
 * 用于获取本机的局域网IP地址
 *
 * @author AI Assistant
 * @date 2026-01-14
 */
@Slf4j
public class NetworkUtils {

    /**
     * 获取本机的局域网IP地址
     * <p>
     * 优先返回非回环地址的IPv4地址
     * 适用于Mac、Linux、Windows等系统
     *
     * @return 局域网IP地址，如果获取失败则返回127.0.0.1
     */
    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    // 只处理IPv4地址，跳过回环地址
                    if (!address.isLoopbackAddress() &&
                            !address.isLinkLocalAddress() &&
                            address.getHostAddress().indexOf(':') == -1) {

                        String ip = address.getHostAddress();
                        log.info("检测到局域网IP地址: {}", ip);
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            log.error("获取局域网IP地址失败", e);
        }

        // 如果无法获取，返回127.0.0.1
        log.warn("无法获取局域网IP地址，使用回环地址 127.0.0.1");
        return "127.0.0.1";
    }

    /**
     * 获取本机的所有IP地址（用于调试）
     */
    public static void printAllIpAddresses() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            log.info("========== 本机所有网络接口 ==========");
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                log.info("接口: {} ({})",
                        networkInterface.getDisplayName(),
                        networkInterface.getName());
                log.info("  状态: up={}, loopback={}",
                        networkInterface.isUp(),
                        networkInterface.isLoopback());

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    log.info("  地址: {} (IPv{})",
                            address.getHostAddress(),
                            address.getHostAddress().indexOf(':') > -1 ? "6" : "4");
                }
            }
            log.info("====================================");
        } catch (SocketException e) {
            log.error("获取网络接口信息失败", e);
        }
    }
}
