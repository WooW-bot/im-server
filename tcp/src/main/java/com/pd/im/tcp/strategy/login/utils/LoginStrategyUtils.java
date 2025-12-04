package com.pd.im.tcp.strategy.login.utils;

import com.pd.im.codec.proto.MessagePack;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.SystemCommand;
import com.pd.im.common.enums.device.ClientType;
import com.pd.im.common.model.UserClientDto;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 登录策略工具类
 * <p>
 * 提供登录策略相关的工具方法
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class LoginStrategyUtils {

    /**
     * 客户端类型映射表
     */
    private static final Map<Integer, String> CLIENT_TYPE_MAP;

    static {
        Map<Integer, String> map = new java.util.HashMap<>();
        map.put(ClientType.ANDROID.getCode(), ClientType.ANDROID.getInfo());
        map.put(ClientType.WEB.getCode(), ClientType.WEB.getInfo());
        map.put(ClientType.IOS.getCode(), ClientType.IOS.getInfo());
        map.put(ClientType.WINDOWS.getCode(), ClientType.WINDOWS.getInfo());
        map.put(ClientType.MAC.getCode(), ClientType.MAC.getInfo());
        map.put(ClientType.WEBAPI.getCode(), ClientType.WEBAPI.getInfo());
        CLIENT_TYPE_MAP = java.util.Collections.unmodifiableMap(map);
    }

    /**
     * 发送互踢登录消息
     * <p>
     * 向旧设备发送MUTALOGIN消息，告知客户端"你被其他设备踢下线了"
     * 注意：此方法只发送通知消息，不负责关闭连接和删除Session
     *
     * @param oldChannel    旧设备的 channel
     * @param oldClientType 旧设备的客户端类型
     * @param oldImei       旧设备的 IMEI
     * @param newDto        新登录的设备信息
     */
    public static void sendMutualLoginMsg(Channel oldChannel, Integer oldClientType, String oldImei, UserClientDto newDto) {
        // 构建设备标识
        String oldDevice = parseClientType(oldClientType) + ":" + oldImei;
        String newDevice = parseClientType(newDto.getClientType()) + ":" + newDto.getImei();

        // 只有不同设备才需要踢出
        if (oldDevice.equals(newDevice)) {
            return;
        }

        // 记录互踢日志
        log.info("appId=[{}] userId=[{}] 从新端 [{}] 登录，旧端 [{}] 下线",
                newDto.getAppId(), newDto.getUserId(), newDevice, oldDevice);

        // 发送下线通知
        MessagePack<Object> pack = new MessagePack<>();
        pack.setToId((String) oldChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.UserId)).get());
        pack.setUserId((String) oldChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.UserId)).get());
        pack.setCommand(SystemCommand.MUTALOGIN.getCommand());
        oldChannel.writeAndFlush(pack);
    }

    /**
     * 解析客户端类型为字符串
     *
     * @param clientType 客户端类型代码
     * @return 客户端类型名称，未知类型返回 "Unknown"
     */
    public static String parseClientType(Integer clientType) {
        return CLIENT_TYPE_MAP.getOrDefault(clientType, "Unknown");
    }

    /**
     * 判断两个设备是否是同一个设备
     *
     * @param dto1 设备1信息
     * @param dto2 设备2信息
     * @return true-同一设备，false-不同设备
     */
    public static boolean isSameDevice(UserClientDto dto1, UserClientDto dto2) {
        if (dto1 == null || dto2 == null) {
            return false;
        }
        return dto1.getClientType().equals(dto2.getClientType())
                && dto1.getImei().equals(dto2.getImei());
    }

    private LoginStrategyUtils() {
        // 工具类，禁止实例化
    }
}
