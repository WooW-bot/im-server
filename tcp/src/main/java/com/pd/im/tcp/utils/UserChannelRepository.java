package com.pd.im.tcp.utils;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.pack.user.UserStatusChangeNotifyPack;
import com.pd.im.codec.proto.MessageHeader;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.UserEventCommand;
import com.pd.im.common.enums.device.ConnectState;
import com.pd.im.common.model.UserClientDto;
import com.pd.im.common.model.UserSession;
import com.pd.im.tcp.rabbitmq.publish.MqMessageProducer;
import com.pd.im.tcp.redis.RedissonManager;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户Channel管理仓库
 * <p>
 * 负责用户与Channel的绑定、解绑、状态管理
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class UserChannelRepository {
    /**
     * 所有的Channel集合
     */
    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 用户与Channel的映射关系
     * Key: UserClientDto (userId + appId + clientType + imei)
     * Value: Channel
     */
    private static final Map<UserClientDto, Channel> USER_CHANNEL = new ConcurrentHashMap<>();

    private static final Object BIND_LOCK = new Object();
    private static final Object UNBIND_LOCK = new Object();

    // ==================== 公共方法 ====================

    /**
     * 添加Channel到Channel组
     * <p>
     * 在channelActive时调用，此时Channel还未绑定用户信息
     *
     * @param channel 新建立的Channel
     */
    public static void add(Channel channel) {
        CHANNEL_GROUP.add(channel);
    }

    /**
     * 绑定用户与Channel
     * <p>
     * 在用户登录成功后调用，建立用户与Channel的双向绑定关系
     * 如果同一设备已有连接，会先关闭旧连接
     *
     * @param userClientDto 用户客户端信息
     * @param channel       Channel
     */
    public static void bind(UserClientDto userClientDto, Channel channel) {
        synchronized (BIND_LOCK) {
            // 检查是否已经有同设备的连接
            if (USER_CHANNEL.containsKey(userClientDto)) {
                Channel oldChannel = USER_CHANNEL.get(userClientDto);
                log.info("同设备重复登录，关闭旧连接: appId={}, userId={}, clientType={}, imei={}",
                        userClientDto.getAppId(), userClientDto.getUserId(),
                        userClientDto.getClientType(), userClientDto.getImei());

                // 先从映射中移除
                USER_CHANNEL.remove(userClientDto);
                CHANNEL_GROUP.remove(oldChannel);

                // 设置标志位，避免在channelInactive中重复处理
                oldChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.CLOSING_BY_REBIND)).set(true);

                // 关闭旧连接
                oldChannel.close();
            }

            // 双向绑定：channel -> user properties
            channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.USER_ID)).set(userClientDto.getUserId());
            channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.APP_ID)).set(userClientDto.getAppId());
            channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.CLIENT_TYPE)).set(userClientDto.getClientType());
            channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.IMEI)).set(userClientDto.getImei());
            channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.CLIENT_IMEI)).set(userClientDto.getClientType() + ":" + userClientDto.getImei());

            // userClientDto -> channel
            USER_CHANNEL.put(userClientDto, channel);
        }
    }

    /**
     * 用户离线（保留Session）
     * <p>
     * 用于被动断开场景：心跳超时、网络异常、连接断开等
     * Session保留在Redis并标记为OFFLINE状态，客户端可以重新连接恢复
     * <p>
     * 使用场景：
     * - 心跳超时
     * - 网络中断
     * - APP被杀死（客户端可选择是否自动重连）
     * - 连接异常
     *
     * @param channel 需要离线的Channel
     */
    public static void setOffline(Channel channel) {
        doCleanup(channel, "用户离线", false);
    }

    /**
     * 用户登出（删除Session）
     * <p>
     * 用于主动退出场景：用户点击"退出登录"
     * Session从Redis中完全删除，需要重新登录
     * <p>
     * 使用场景：
     * - 用户主动点击"退出登录"
     * - 多设备冲突时服务端踢出旧设备
     * - 账号被封禁等安全原因
     *
     * @param channel 需要登出的Channel
     */
    public static void logout(Channel channel) {
        doCleanup(channel, "用户登出", true);
    }

    public static Channel getUserChannel(Integer appId, String userId, Integer clientType, String imei) {
        UserClientDto userClientDto = UserClientDto.builder()
                .userId(userId)
                .appId(appId)
                .clientType(clientType)
                .imei(imei)
                .build();
        return getChannel(userClientDto);
    }

    /**
     * 判断用户是否在线
     *
     * @param userClientDto 用户客户端信息
     * @return Channel 在线返回Channel，离线返回null
     */
    public static Channel getChannel(UserClientDto userClientDto) {
        Channel channel = USER_CHANNEL.get(userClientDto);
        if (channel == null) {
            return null;
        }
        // 双重检查：确保Channel在ChannelGroup中
        return CHANNEL_GROUP.find(channel.id());
    }

    /**
     * 获取用户的所有在线Channel
     *
     * @param appId  应用ID
     * @param userId 用户ID
     * @return 该用户的所有在线Channel列表
     */
    public static List<Channel> getUserChannels(Integer appId, String userId) {
        List<Channel> channels = new ArrayList<>();
        Set<UserClientDto> allUsers = USER_CHANNEL.keySet();

        for (UserClientDto user : allUsers) {
            if (appId.equals(user.getAppId()) && userId.equals(user.getUserId())) {
                Channel channel = USER_CHANNEL.get(user);
                if (channel != null) {
                    channels.add(channel);
                }
            }
        }
        return channels;
    }

    /**
     * 从Channel获取用户信息
     *
     * @param channel Channel
     * @return 用户客户端信息，未绑定返回null
     */
    public static UserClientDto getUserInfo(Channel channel) {
        String userId = (String) channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.USER_ID)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.APP_ID)).get();
        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.CLIENT_TYPE)).get();
        String imei = (String) channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.IMEI)).get();

        if (userId == null || appId == null || clientType == null || imei == null) {
            return null;
        }

        return UserClientDto.builder()
                .userId(userId)
                .appId(appId)
                .clientType(clientType)
                .imei(imei)
                .build();
    }

    // ==================== 内部方法 ====================

    /**
     * 统一的清理方法
     * <p>
     * 处理离线和登出的通用逻辑，使用标志位避免重复处理
     *
     * @param channel       需要处理的Channel
     * @param reason        处理原因（用于日志）
     * @param removeSession true-删除Session（登出），false-保留Session（离线）
     */
    private static void doCleanup(Channel channel, String reason, boolean removeSession) {
        synchronized (UNBIND_LOCK) {
            // 检查是否已经处理过
            Boolean alreadyProcessed = (Boolean) channel.attr(
                    AttributeKey.valueOf(Constants.ChannelConstants.CLOSING_BY_CLEANUP)).get();
            Boolean closingByRebind = (Boolean) channel.attr(
                    AttributeKey.valueOf(Constants.ChannelConstants.CLOSING_BY_REBIND)).get();

            if (Boolean.TRUE.equals(alreadyProcessed) || Boolean.TRUE.equals(closingByRebind)) {
                log.debug("Channel已被处理，跳过: reason={}", reason);
                return;
            }

            // 设置标志位，防止重复处理
            channel.attr(AttributeKey.valueOf(Constants.ChannelConstants.CLOSING_BY_CLEANUP)).set(true);

            // 获取用户信息
            UserClientDto userInfo = getUserInfo(channel);
            if (userInfo == null) {
                log.debug("Channel未绑定用户信息，仅关闭连接: reason={}", reason);
                CHANNEL_GROUP.remove(channel);
                if (channel.isActive()) {
                    channel.close();
                }
                return;
            }

            log.info("{}: appId={}, userId={}, clientType={}, imei={}, removeSession={}",
                    reason, userInfo.getAppId(), userInfo.getUserId(),
                    userInfo.getClientType(), userInfo.getImei(), removeSession);

            // 1. 处理Redis Session
            if (removeSession) {
                deleteSession(userInfo);
            } else {
                markSessionOffline(userInfo);
            }

            // 2. 发送用户状态变更通知到MQ
            sendStatusChangeNotification(userInfo, ConnectState.CONNECT_STATE_OFFLINE.getCode());

            // 3. 从本地映射中移除
            USER_CHANNEL.remove(userInfo);
            CHANNEL_GROUP.remove(channel);

            // 4. 关闭Channel
            if (channel.isActive()) {
                channel.close();
            }
        }
    }

    /**
     * 标记Session为离线状态（保留Session）
     */
    private static void markSessionOffline(UserClientDto userInfo) {
        try {
            RedissonClient redissonClient = RedissonManager.getRedissonClient();
            String mapKey = userInfo.getAppId() + Constants.RedisConstants.USER_SESSION + userInfo.getUserId();
            RMap<String, String> map = redissonClient.getMap(mapKey);

            String sessionKey = userInfo.getClientType() + ":" + userInfo.getImei();
            String sessionValue = map.get(sessionKey);

            if (!StringUtils.isBlank(sessionValue)) {
                UserSession userSession = JSONObject.parseObject(sessionValue, UserSession.class);
                userSession.setConnectState(ConnectState.CONNECT_STATE_OFFLINE.getCode());
                map.put(sessionKey, JSONObject.toJSONString(userSession));
                log.debug("Session标记为离线: userId={}, clientType={}, imei={}",
                        userInfo.getUserId(), userInfo.getClientType(), userInfo.getImei());
            }
        } catch (Exception e) {
            log.error("标记Session离线失败", e);
        }
    }

    /**
     * 删除Session（完全登出）
     */
    private static void deleteSession(UserClientDto userInfo) {
        try {
            RedissonClient redissonClient = RedissonManager.getRedissonClient();
            String mapKey = userInfo.getAppId() + Constants.RedisConstants.USER_SESSION + userInfo.getUserId();
            RMap<String, String> map = redissonClient.getMap(mapKey);

            String sessionKey = userInfo.getClientType() + ":" + userInfo.getImei();
            map.remove(sessionKey);
            log.debug("Session已删除: userId={}, clientType={}, imei={}",
                    userInfo.getUserId(), userInfo.getClientType(), userInfo.getImei());
        } catch (Exception e) {
            log.error("删除Session失败", e);
        }
    }

    /**
     * 发送用户状态变更通知到MQ
     */
    private static void sendStatusChangeNotification(UserClientDto userInfo, Integer status) {
        try {
            MessageHeader messageHeader = new MessageHeader();
            messageHeader.setAppId(userInfo.getAppId());
            messageHeader.setImei(userInfo.getImei());
            messageHeader.setClientType(userInfo.getClientType());

            UserStatusChangeNotifyPack notifyPack = new UserStatusChangeNotifyPack();
            notifyPack.setAppId(userInfo.getAppId());
            notifyPack.setUserId(userInfo.getUserId());
            notifyPack.setStatus(status);

            MqMessageProducer.sendMessage(notifyPack, messageHeader,
                    UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand());
        } catch (Exception e) {
            log.error("发送用户状态变更通知失败", e);
        }
    }
}
