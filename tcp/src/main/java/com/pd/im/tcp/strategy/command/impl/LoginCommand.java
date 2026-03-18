package com.pd.im.tcp.strategy.command.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.proto.Message;
import com.pd.im.codec.proto.MessagePack;
import com.pd.im.codec.proto.generated.LoginAckPack;
import com.pd.im.codec.proto.generated.LoginPack;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.SystemCommand;
import com.pd.im.common.enums.command.UserEventCommand;
import com.pd.im.common.enums.device.ConnectState;
import com.pd.im.common.model.UserClientDto;
import com.pd.im.common.model.UserSession;
import com.pd.im.tcp.rabbitmq.publish.MqMessageProducer;
import com.pd.im.tcp.redis.RedissonManager;
import com.pd.im.tcp.strategy.command.CommandStrategy;
import com.pd.im.tcp.strategy.command.model.CommandContext;
import com.pd.im.tcp.utils.UserChannelRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

/**
 * 用户登录命令
 * <p>
 * 处理用户登录逻辑：
 * 1. 绑定用户与Channel
 * 2. 存储用户Session到Redis
 * 3. 发布用户上线通知
 * 4. 发送在线状态变更消息到MQ
 * 5. 返回登录成功响应
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class LoginCommand implements CommandStrategy {

    @Override
    public void execute(CommandContext context) {
        try {
            // 参数校验
            if (context == null || context.getMsg() == null || context.getCtx() == null) {
                log.error("登录命令执行失败：参数为空");
                return;
            }

            Message msg = context.getMsg();
            Integer brokeId = context.getBrokeId();

            // 1. 解析登录请求
            LoginPack loginPack = parseLoginPack(msg);
            if (loginPack == null || loginPack.getUserId() == null || loginPack.getUserId().trim().isEmpty()) {
                log.error("登录失败：用户ID为空");
                sendLoginFailResponse(context, "用户ID不能为空");
                return;
            }

            // 1.1 校验 Ticket (对齐腾讯云优化)
            if (!verifyLoginTicket(loginPack, msg)) {
                log.error("登录失败：Ticket 校验不通过, userId={}", loginPack.getUserId());
                sendLoginFailResponse(context, "登录票据无效或已过期");
                return;
            }

            // 校验消息头
            if (!validateMessageHeader(msg.getMessageHeader())) {
                log.error("登录失败：消息头参数不完整");
                sendLoginFailResponse(context, "消息头参数不完整");
                return;
            }

            UserClientDto userClientDto = new UserClientDto();
            userClientDto.setUserId(loginPack.getUserId());
            userClientDto.setAppId(msg.getMessageHeader().getAppId());
            userClientDto.setClientType(msg.getMessageHeader().getClientType());
            userClientDto.setImei(msg.getMessageHeader().getImei());

            // 2. 双向绑定用户与Channel
            UserChannelRepository.bind(userClientDto, context.getCtx().channel());

            // 3. 构建用户Session并存储到Redis
            UserSession userSession = buildUserSession(loginPack, msg, brokeId);
            saveUserSession(userSession, msg);

            // 4. 发布用户上线通知（用于多端登录策略处理）
            publishUserLoginEvent(userClientDto);

            // 5. 发送用户在线状态变更消息到MQ
            sendUserStatusChangeMessage(loginPack, msg);

            // 6. 返回登录成功响应
            sendLoginAckResponse(context, loginPack, msg);

            log.info("用户登录成功: appId={}, userId={}, clientType={}, imei={}, brokerId={}",
                    msg.getMessageHeader().getAppId(), loginPack.getUserId(),
                    msg.getMessageHeader().getClientType(), msg.getMessageHeader().getImei(), brokeId);

        } catch (Exception e) {
            log.error("登录命令执行异常", e);
            sendLoginFailResponse(context, "登录失败：" + e.getMessage());
        }
    }

    private LoginPack parseLoginPack(Message msg) {
        try {
            Object pack = msg.getMessagePack();
            if (pack instanceof LoginPack) {
                return (LoginPack) pack;
            }
            return null;
        } catch (Exception e) {
            log.error("解析登录数据包失败", e);
            return null;
        }
    }

    /**
     * 校验消息头参数
     */
    private boolean validateMessageHeader(com.pd.im.codec.proto.MessageHeader header) {
        return header != null
                && header.getAppId() != null
                && header.getClientType() != null
                && header.getImei() != null
                && !header.getImei().trim().isEmpty();
    }

    /**
     * 发送登录失败响应
     */
    private void sendLoginFailResponse(CommandContext context, String errorMsg) {
        try {
            // LoginAckPack in Protobuf
            LoginAckPack loginAckPack = LoginAckPack.newBuilder()
                    .setUserId("")
                    .build();

            MessagePack<LoginAckPack> loginFail = new MessagePack<>();
            loginFail.setCommand(SystemCommand.LOGINACK.getCommand());
            loginFail.setData(loginAckPack);
            loginFail.setTimestamp(System.currentTimeMillis());

            context.getCtx().channel().writeAndFlush(loginFail);
        } catch (Exception e) {
            log.error("发送登录失败响应异常", e);
        }
    }

    /**
     * 构建用户Session对象
     */
    private UserSession buildUserSession(LoginPack loginPack, Message msg, Integer brokeId) {
        UserSession userSession = new UserSession();
        userSession.setUserId(loginPack.getUserId());
        userSession.setAppId(msg.getMessageHeader().getAppId());
        userSession.setClientType(msg.getMessageHeader().getClientType());
        userSession.setConnectState(ConnectState.CONNECT_STATE_ONLINE.getCode());
        userSession.setImei(msg.getMessageHeader().getImei());
        userSession.setBrokerId(brokeId);

        try {
            // 使用NetworkUtils获取局域网IP地址
            String hostAddress = com.pd.im.tcp.utils.NetworkUtils.getLocalIpAddress();
            userSession.setBrokerHost(hostAddress);
        } catch (Exception e) {
            log.error("获取本机IP地址失败", e);
        }

        return userSession;
    }

    /**
     * 保存用户Session到Redis
     */
    private void saveUserSession(UserSession userSession, Message msg) {
        RedissonClient redissonClient = RedissonManager.getRedissonClient();
        String mapKey = msg.getMessageHeader().getAppId()
                + Constants.RedisConstants.USER_SESSION
                + userSession.getUserId();
        RMap<String, String> map = redissonClient.getMap(mapKey);

        String sessionKey = msg.getMessageHeader().getClientType() + ":" + msg.getMessageHeader().getImei();
        map.put(sessionKey, JSONObject.toJSONString(userSession));
    }

    /**
     * 发布用户上线通知
     */
    private void publishUserLoginEvent(UserClientDto userClientDto) {
        RedissonClient redissonClient = RedissonManager.getRedissonClient();
        RTopic topic = redissonClient.getTopic(Constants.RedisConstants.USER_LOGIN_CHANNEL);
        topic.publish(JSONObject.toJSONString(userClientDto));
    }

    /**
     * 发送用户在线状态变更消息到MQ
     */
    private void sendUserStatusChangeMessage(LoginPack loginPack, Message msg) {
        com.pd.im.codec.pack.user.UserStatusChangeNotifyPack userStatusChangeNotifyPack = new com.pd.im.codec.pack.user.UserStatusChangeNotifyPack();
        userStatusChangeNotifyPack.setAppId(msg.getMessageHeader().getAppId());
        userStatusChangeNotifyPack.setUserId(loginPack.getUserId());
        userStatusChangeNotifyPack.setStatus(ConnectState.CONNECT_STATE_ONLINE.getCode());

        MqMessageProducer.sendMessage(
                userStatusChangeNotifyPack,
                msg.getMessageHeader(),
                UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand()
        );
    }

    /**
     * 校验登录票据
     */
    private boolean verifyLoginTicket(LoginPack loginPack, Message msg) {
        String ticket = loginPack.getTicket();
        if (ticket == null || ticket.trim().isEmpty()) {
            log.error("登录失败：Ticket 为空, userId={}", loginPack.getUserId());
            return false;
        }

        RedissonClient redissonClient = RedissonManager.getRedissonClient();
        String key = msg.getMessageHeader().getAppId() 
                + Constants.RedisConstants.USER_LOGIN_TICKET 
                + loginPack.getUserId() + ":" 
                + msg.getMessageHeader().getClientType() + ":" 
                + msg.getMessageHeader().getImei();
        
        Object ticketObj = redissonClient.getBucket(key).get();
        if (ticketObj == null) {
            log.error("登录失败：Redis 中未找到 Ticket, userId={}, key={}", loginPack.getUserId(), key);
            return false;
        }
        String cachedTicket = ticketObj.toString();
        log.info("Ticket 校验详情: userId={}, key={}, inputTicket={}, cachedTicket={}", 
                loginPack.getUserId(), key, ticket, cachedTicket);
        
        // 校验成功后立即删除 Ticket (一次性使用)
        if (ticket.equals(cachedTicket)) {
            redissonClient.getBucket(key).delete();
            return true;
        }
        
        log.error("登录失败：Ticket 不匹配, userId={}, input={}, cached={}", 
                loginPack.getUserId(), ticket, cachedTicket);
        return false;
    }

    /**
     * 发送登录成功响应
     */
    private void sendLoginAckResponse(CommandContext context, LoginPack loginPack, Message msg) {
        LoginAckPack loginAckPack = LoginAckPack.newBuilder()
                .setUserId(loginPack.getUserId())
                .build();

        MessagePack<LoginAckPack> loginSuccess = new MessagePack<>();
        loginSuccess.setCommand(SystemCommand.LOGINACK.getCommand());
        loginSuccess.setData(loginAckPack);
        loginSuccess.setTimestamp(System.currentTimeMillis());
        loginSuccess.setImei(msg.getMessageHeader().getImei());
        loginSuccess.setAppId(msg.getMessageHeader().getAppId());

        context.getCtx().channel().writeAndFlush(loginSuccess);
    }
}
