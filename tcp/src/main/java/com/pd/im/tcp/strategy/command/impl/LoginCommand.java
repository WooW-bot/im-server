package com.pd.im.tcp.strategy.command.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.pd.im.codec.pack.user.LoginAckPack;
import com.pd.im.codec.pack.user.LoginPack;
import com.pd.im.codec.pack.user.UserStatusChangeNotifyPack;
import com.pd.im.codec.proto.Message;
import com.pd.im.codec.proto.MessagePack;
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

import java.net.InetAddress;

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

    /**
     * 解析登录数据包
     */
    private LoginPack parseLoginPack(Message msg) {
        try {
            return JSON.parseObject(
                    JSONObject.toJSONString(msg.getMessagePack()),
                    new TypeReference<LoginPack>() {
                    }.getType()
            );
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
            LoginAckPack loginAckPack = new LoginAckPack();
            loginAckPack.setUserId("");

            MessagePack<String> loginFail = new MessagePack<>();
            loginFail.setCommand(SystemCommand.LOGINACK.getCommand());
            loginFail.setData(errorMsg);

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
            InetAddress localHost = InetAddress.getLocalHost();
            userSession.setBrokerHost(localHost.getHostAddress());
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
        UserStatusChangeNotifyPack userStatusChangeNotifyPack = new UserStatusChangeNotifyPack();
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
     * 发送登录成功响应
     */
    private void sendLoginAckResponse(CommandContext context, LoginPack loginPack, Message msg) {
        LoginAckPack loginAckPack = new LoginAckPack();
        loginAckPack.setUserId(loginPack.getUserId());

        MessagePack<LoginAckPack> loginSuccess = new MessagePack<>();
        loginSuccess.setCommand(SystemCommand.LOGINACK.getCommand());
        loginSuccess.setData(loginAckPack);
        loginSuccess.setImei(msg.getMessageHeader().getImei());
        loginSuccess.setAppId(msg.getMessageHeader().getAppId());

        context.getCtx().channel().writeAndFlush(loginSuccess);
    }
}
