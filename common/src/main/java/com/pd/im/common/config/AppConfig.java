package com.pd.im.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Parker
 * @date 12/5/25
 */
@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class AppConfig {
    private String privateKey;

    /**
     * zk连接地址
     */
    private String zkAddr;
    /**
     * zk连接超时时间
     */
    private Integer zkConnectTimeOut;

    /**
     * im 管道路由策略
     */
    private Integer imRouteModel;

    /**
     * 一致性哈希所使用的底层数据结构
     */
    private Integer consistentHashModel;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * 删除会话同步模式
     */
    private Integer deleteConversationSyncMode;

    /**
     * 离线消息最大条数
     */
    private Integer offlineMessageCount = 100;

    /**
     * 好友关系每次增量拉取最大条目数
     */
    private Integer friendShipMaxCount = 50;

    /**
     * 会话消息每次递增拉取最大条目数
     */
    private Integer conversationMaxCount = 100;

    /**
     * 用户被拉入群通知每次递增拉取最大条目数
     */
    private Integer joinGroupMaxCount = 100;

    /**
     * 发送消息是否校验关系链 TODO 需要持久化到数据库表
     */
    private boolean sendMessageCheckFriend;

    /**
     * 发送消息是否校验黑名单 TODO 需要持久化到数据库表
     */
    private boolean sendMessageCheckBlack;

    /**
     * 添加好友之前回调开关
     */
    private boolean addFriendBeforeCallback;

    /**
     * 添加好友之后回调开关
     */
    private boolean addFriendAfterCallback;

    /**
     * 修改好友之后回调开关
     */
    private boolean modifyFriendAfterCallback;

    /**
     * 删除好友之后回调开关
     */
    private boolean deleteFriendAfterCallback;

    /**
     * 添加黑名单之后回调开关
     */
    private boolean addFriendShipBlackAfterCallback;

    /**
     * 删除黑名单之后回调开关
     */
    private boolean deleteFriendShipBlackAfterCallback;


    /**
     * 创建群聊之后回调开关
     */
    private boolean createGroupAfterCallback;

    /**
     * 修改群聊之后回调开关
     */
    private boolean modifyGroupAfterCallback;

    /**
     * 解散群聊之后回调开关
     */
    private boolean destroyGroupAfterCallback;

    /**
     * 删除群成员之后回调
     */
    private boolean deleteGroupMemberAfterCallback;

    /**
     * 拉人入群之前回调
     */
    private boolean addGroupMemberBeforeCallback;

    /**
     * 拉人入群之后回调
     */
    private boolean addGroupMemberAfterCallback;

    /**
     * 用户资料变更之后回调开关
     */
    private boolean modifyUserAfterCallback;

    /**
     * 发送单/群聊消息之前
     */
    private boolean sendMessageBeforeCallback;

    /**
     * 发送单/群聊消息之后
     */
    private boolean sendMessageAfterCallback;
}
