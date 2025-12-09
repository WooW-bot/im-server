package com.pd.im.common.constant;

/**
 * @author Parker
 * @date 12/3/25
 */
public class Constants {
    public static class ChannelConstants {
        /**
         * channel 绑定的 userId Key
         */
        public static final String USER_ID = "userId";
        /**
         * channel 绑定的 appId Key
         */
        public static final String APP_ID = "appId";
        /**
         * channel 绑定的端类型
         */
        public static final String CLIENT_TYPE = "clientType";
        /**
         * channel 绑定的读写时间
         */
        public static final String READ_TIME = "readTime";

        /**
         * channel 绑定的 imei 号，标识用户登录设备号
         */
        public static final String IMEI = "imei";

        /**
         * channel 绑定的 clientType 和 imei Key
         */
        public static final String CLIENT_IMEI = "clientImei";

        /**
         * 标志位：Channel正在被重新绑定而关闭（同设备重复登录）
         */
        public static final String CLOSING_BY_REBIND = "closingByRebind";

        /**
         * 标志位：Channel正在被用户主动登出而关闭
         */
        public static final String CLOSING_BY_LOGOUT = "closingByLogout";

        /**
         * 标志位：Channel正在被cleanupAndClose方法处理
         */
        public static final String CLOSING_BY_CLEANUP = "closingByCleanup";
    }

    public static class RedisConstants {
        /**
         * UserSign，格式：appId:UserSign:
         */
        public static final String USER_SIGN = ":userSign:";
        /**
         * 用户登录端消息通道信息
         */
        public static final String USER_LOGIN_CHANNEL = "signal/channel/LOGIN_USER_INNER_QUEUE";
        /**
         * 用户session：格式为 appId + userSessionConstants + 用户 ID
         * 例如：10001:userSession:userId
         */
        public static final String USER_SESSION = ":userSession:";
        /**
         * 缓存客户端消息防重，格式： appId + :cacheMessage: + messageId
         */
        public static final String CACHE_MESSAGE = ":cacheMessage:";
        /**
         * 缓存离线消息 获取用户消息队列 格式：appId + :offlineMessage: + fromId / toId
         */
        public static final String OFFLINE_MESSAGE = ":offlineMessage:";
        /**
         * 用户所有模块的偏序前缀
         */
        public static final String SEQ_PREFIX = ":seq:";
        /**
         * 用户订阅列表，格式 ：appId + :subscribe: + userId。Hash结构，filed为订阅自己的人
         */
        public static final String SUBSCRIBE = ":subscribe:";
        /**
         * 用户自定义在线状态，格式 ：appId + :userCustomerStatus: + userId。set，value为用户id
         */
        public static final String USER_CUSTOMER_STATUS = ":userCustomerStatus:";
    }

    public static class RabbitmqConstants {

        public static final String IM_TO_USER_SERVICE = "pipeline2UserService";

        public static final String IM_TO_MESSAGE_SERVICE = "pipeline2MessageService";

        public static final String IM_TO_GROUP_SERVICE = "pipeline2GroupService";

        public static final String IM_TO_FRIENDSHIP_SERVICE = "pipeline2FriendshipService";

        public static final String MESSAGE_SERVICE_TO_IM = "messageService2Pipeline";

        public static final String GROUP_SERVICE_TO_IM = "GroupService2Pipeline";

        public static final String FRIENDSHIP_TO_IM = "friendShip2Pipeline";

        public static final String STORE_P2P_MESSAGE = "storeP2PMessage";

        public static final String STORE_GROUP_MESSAGE = "storeGroupMessage";

    }

    public static class CallbackCommand {
        public static final String MODIFY_USER_AFTER = "user.modify.after";
        public static final String CREATE_GROUP_AFTER = "group.create.after";
        public static final String UPDATE_GROUP_AFTER = "group.update.after";
        public static final String DESTROY_GROUP_AFTER = "group.destroy.after";
        public static final String TRANSFER_GROUP_AFTER = "group.transfer.after";
        public static final String GROUP_MEMBER_ADD_BEFORE = "group.member.add.before";
        public static final String GROUP_MEMBER_ADD_AFTER = "group.member.add.after";
        public static final String GROUP_MEMBER_DELETE_AFTER = "group.member.delete.after";
        public static final String ADD_FRIEND_BEFORE = "friend.add.before";
        public static final String ADD_FRIEND_AFTER = "friend.add.after";
        public static final String UPDATE_FRIEND_BEFORE = "friend.update.before";
        public static final String UPDATE_FRIEND_AFTER = "friend.update.after";
        public static final String DELETE_FRIEND_AFTER = "friend.delete.after";
        public static final String ADD_BLACK_AFTER = "black.add.after";
        public static final String DELETE_BLACK = "black.delete";
        public static final String SEND_GROUP_MESSAGE_AFTER = "group.message.send.after";
        public static final String SEND_GROUP_MESSAGE_BEFORE = "group.message.send.before";
        public static final String SEND_MESSAGE_AFTER = "message.send.after";
        public static final String SEND_MESSAGE_BEFORE = "message.send.before";
    }

    public static class ZKConstants {
        public static final String IM_CORE_ZK_ROOT = "/im-coreRoot";

        public static final String IM_CORE_ZK_ROOT_TCP = "/tcp";

        public static final String IM_CORE_ZK_ROOT_WEB = "/web";
    }

    public static class SeqConstants {
        // 保证消息有序性 Key
        /**
         * 单聊消息有序
         */
        public static final String MESSAGE_SEQ = "messageSeq";
        /**
         * 群聊消息有序
         */
        public static final String GROUP_MESSAGE_SEQ = "groupMessageSeq";

        /**
         * 好友数量记录
         */
        public static final String FRIENDSHIP = "friendshipSeq";

        public static final String FRIENDSHIP_REQUEST = "friendshipRequestSeq";

        public static final String FRIENDSHIP_GROUP = "friendshipGroupSeq";
        /**
         * 会话消息有序
         */
        public static final String CONVERSATION_SEQ = "conversationSeq";
        public static final String GROUP_SEQ = "groupSeq";
    }

    public static class MsgPackConstants {
        public static final String FROM_ID = "fromId";

        public static final String TO_ID = "toId";

        public static final String GROUP_ID = "groupId";

        public static final String MSG_ID = "messageId";
    }
}
