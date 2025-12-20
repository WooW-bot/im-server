-- ========================================
-- IM核心数据库 - 生产级SQL脚本
-- 版本: 2.0
-- 创建日期: 2025-12-20
-- 说明: 统一字段规范、完善注释、优化索引
-- ========================================

CREATE
DATABASE IF NOT EXISTS `im_core`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE
`im_core`;

-- ========================================
-- 应用用户表
-- ========================================
DROP TABLE IF EXISTS `app_user`;
CREATE TABLE `app_user`
(
    `user_id`     VARCHAR(50) NOT NULL COMMENT '用户ID',
    `user_name`   VARCHAR(100) DEFAULT NULL COMMENT '用户名',
    `password`    VARCHAR(255) DEFAULT NULL COMMENT '密码(加密)',
    `mobile`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `create_time` BIGINT      NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT       DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`user_id`),
    KEY           `idx_mobile` (`mobile`),
    KEY           `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用用户表';

-- ========================================
-- 会话列表表
-- ========================================
DROP TABLE IF EXISTS `im_conversation_set`;
CREATE TABLE `im_conversation_set`
(
    `app_id`            INT          NOT NULL COMMENT '应用ID',
    `conversation_id`   VARCHAR(255) NOT NULL COMMENT '会话ID',
    `conversation_type` TINYINT      NOT NULL DEFAULT 0 COMMENT '会话类型: 0-单聊 1-群聊 2-机器人 3-公众号',
    `from_id`           VARCHAR(50)  NOT NULL COMMENT '发起方用户ID',
    `to_id`             VARCHAR(50)  NOT NULL COMMENT '接收方ID(用户ID或群ID)',
    `is_mute`           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否免打扰: 0-正常 1-免打扰',
    `is_top`            TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶: 0-不置顶 1-置顶',
    `sequence`          BIGINT                DEFAULT NULL COMMENT '会话序列号',
    `read_sequence`     BIGINT                DEFAULT NULL COMMENT '已读序列号',
    `create_time`       BIGINT       NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time`       BIGINT                DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`app_id`, `conversation_id`),
    KEY                 `idx_app_from` (`app_id`, `from_id`),
    KEY                 `idx_app_update_time` (`app_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话列表表';

-- ========================================
-- 好友关系表
-- ========================================
DROP TABLE IF EXISTS `im_friendship`;
CREATE TABLE `im_friendship`
(
    `app_id`          INT         NOT NULL COMMENT '应用ID',
    `from_id`         VARCHAR(50) NOT NULL COMMENT '用户ID',
    `to_id`           VARCHAR(50) NOT NULL COMMENT '好友用户ID',
    `remark`          VARCHAR(100)         DEFAULT NULL COMMENT '好友备注',
    `status`          TINYINT     NOT NULL DEFAULT 0 COMMENT '好友状态: 0-未添加 1-正常 2-已删除',
    `black`           TINYINT     NOT NULL DEFAULT 0 COMMENT '黑名单状态: 0-正常(未拉黑) 1-已拉黑',
    `add_source`      VARCHAR(50)          DEFAULT NULL COMMENT '添加来源',
    `extra`           VARCHAR(1024)        DEFAULT NULL COMMENT '扩展字段(JSON)',
    `friend_sequence` BIGINT               DEFAULT NULL COMMENT '好友关系序列号(用于增量同步)',
    `black_sequence`  BIGINT               DEFAULT NULL COMMENT '黑名单序列号(用于增量同步)',
    `create_time`     BIGINT      NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time`     BIGINT               DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`app_id`, `from_id`, `to_id`),
    KEY               `idx_app_from` (`app_id`, `from_id`),
    KEY               `idx_app_to` (`app_id`, `to_id`),
    KEY               `idx_app_from_sequence` (`app_id`, `from_id`, `friend_sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- ========================================
-- 好友分组表
-- ========================================
DROP TABLE IF EXISTS `im_friendship_group`;
CREATE TABLE `im_friendship_group`
(
    `group_id`    INT AUTO_INCREMENT COMMENT '分组ID(自增主键)',
    `app_id`      INT         NOT NULL COMMENT '应用ID',
    `from_id`     VARCHAR(50) NOT NULL COMMENT '用户ID',
    `group_name`  VARCHAR(50) NOT NULL COMMENT '分组名称',
    `sequence`    BIGINT               DEFAULT NULL COMMENT '序列号(用于增量同步)',
    `del_flag`    TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标识: 0-正常 1-已删除',
    `create_time` BIGINT      NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time` BIGINT               DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`group_id`),
    UNIQUE KEY `uk_app_from_name` (`app_id`, `from_id`, `group_name`),
    KEY           `idx_app_from` (`app_id`, `from_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组表';

-- ========================================
-- 好友分组成员表
-- ========================================
DROP TABLE IF EXISTS `im_friendship_group_member`;
CREATE TABLE `im_friendship_group_member`
(
    `id`       BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `app_id`   INT         NOT NULL COMMENT '应用ID',
    `group_id` INT         NOT NULL COMMENT '分组ID',
    `to_id`    VARCHAR(50) NOT NULL COMMENT '好友用户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_group_to` (`app_id`, `group_id`, `to_id`),
    KEY        `idx_app_group` (`app_id`, `group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组成员表';

-- ========================================
-- 好友申请表
-- ========================================
DROP TABLE IF EXISTS `im_friendship_request`;
CREATE TABLE `im_friendship_request`
(
    `id`             BIGINT AUTO_INCREMENT COMMENT '申请ID(自增主键)',
    `app_id`         INT         NOT NULL COMMENT '应用ID',
    `from_id`        VARCHAR(50) NOT NULL COMMENT '申请方用户ID',
    `to_id`          VARCHAR(50) NOT NULL COMMENT '被申请方用户ID',
    `remark`         VARCHAR(100)         DEFAULT NULL COMMENT '备注',
    `add_source`     VARCHAR(50)          DEFAULT NULL COMMENT '添加来源',
    `add_wording`    VARCHAR(200)         DEFAULT NULL COMMENT '好友验证信息',
    `read_status`    TINYINT     NOT NULL DEFAULT 0 COMMENT '已读状态: 0-未读 1-已读',
    `approve_status` TINYINT     NOT NULL DEFAULT 0 COMMENT '审批状态: 0-待审批 1-已同意 2-已拒绝',
    `sequence`       BIGINT               DEFAULT NULL COMMENT '序列号(用于增量同步)',
    `create_time`    BIGINT      NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time`    BIGINT               DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`id`),
    KEY              `idx_app_from` (`app_id`, `from_id`),
    KEY              `idx_app_to` (`app_id`, `to_id`),
    KEY              `idx_app_create_time` (`app_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- ========================================
-- 群组表
-- ========================================
DROP TABLE IF EXISTS `im_group`;
CREATE TABLE `im_group`
(
    `app_id`           INT          NOT NULL COMMENT '应用ID',
    `group_id`         VARCHAR(50)  NOT NULL COMMENT '群组ID',
    `owner_id`         VARCHAR(50)  NOT NULL COMMENT '群主用户ID',
    `group_type`       TINYINT      NOT NULL DEFAULT 1 COMMENT '群类型: 1-私有群(类似微信) 2-公开群(类似QQ)',
    `group_name`       VARCHAR(100) NOT NULL COMMENT '群名称',
    `photo`            VARCHAR(500)          DEFAULT NULL COMMENT '群头像URL',
    `introduction`     VARCHAR(500)          DEFAULT NULL COMMENT '群简介',
    `notification`     VARCHAR(1024)         DEFAULT NULL COMMENT '群公告',
    `mute`             TINYINT      NOT NULL DEFAULT 0 COMMENT '全员禁言: 0-不禁言 1-全员禁言',
    `apply_join_type`  TINYINT      NOT NULL DEFAULT 2 COMMENT '申请加群类型: 0-禁止任何人申请 1-需要审批 2-允许自由加入',
    `max_member_count` INT                   DEFAULT 200 COMMENT '最大成员数',
    `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '群状态: 0-正常 1-已解散',
    `sequence`         BIGINT                DEFAULT NULL COMMENT '序列号(用于增量同步)',
    `extra`            VARCHAR(1024)         DEFAULT NULL COMMENT '扩展字段(JSON)',
    `create_time`      BIGINT       NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time`      BIGINT                DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`app_id`, `group_id`),
    KEY                `idx_app_owner` (`app_id`, `owner_id`),
    KEY                `idx_app_status` (`app_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- ========================================
-- 群成员表
-- ========================================
DROP TABLE IF EXISTS `im_group_member`;
CREATE TABLE `im_group_member`
(
    `group_member_id` BIGINT AUTO_INCREMENT COMMENT '群成员ID(自增主键)',
    `app_id`          INT         NOT NULL COMMENT '应用ID',
    `group_id`        VARCHAR(50) NOT NULL COMMENT '群组ID',
    `member_id`       VARCHAR(50) NOT NULL COMMENT '成员用户ID',
    `role`            TINYINT     NOT NULL DEFAULT 0 COMMENT '群成员角色: 0-普通成员 1-管理员 2-群主 3-已禁言 4-已移除',
    `alias`           VARCHAR(100)         DEFAULT NULL COMMENT '群昵称',
    `mute`            TINYINT     NOT NULL DEFAULT 0 COMMENT '单人禁言: 0-不禁言 1-禁言',
    `speak_date`      BIGINT               DEFAULT NULL COMMENT '禁言到期时间(毫秒时间戳, NULL表示永久)',
    `join_type`       VARCHAR(50)          DEFAULT NULL COMMENT '加入类型',
    `join_time`       BIGINT      NOT NULL COMMENT '加入时间(毫秒时间戳)',
    `leave_time`      BIGINT               DEFAULT NULL COMMENT '离开时间(毫秒时间戳)',
    `extra`           VARCHAR(1024)        DEFAULT NULL COMMENT '扩展字段(JSON)',
    PRIMARY KEY (`group_member_id`),
    UNIQUE KEY `uk_group_member` (`app_id`, `group_id`, `member_id`),
    KEY               `idx_group_id` (`app_id`, `group_id`),
    KEY               `idx_member_id` (`app_id`, `member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ========================================
-- 群消息历史表
-- ========================================
DROP TABLE IF EXISTS `im_group_message_history`;
CREATE TABLE `im_group_message_history`
(
    `app_id`         INT         NOT NULL COMMENT '应用ID',
    `group_id`       VARCHAR(50) NOT NULL COMMENT '群组ID',
    `message_key`    BIGINT      NOT NULL COMMENT '消息ID(关联im_message_body)',
    `from_id`        VARCHAR(50) NOT NULL COMMENT '发送方用户ID',
    `sequence`       BIGINT DEFAULT NULL COMMENT '消息序列号',
    `message_random` INT    DEFAULT NULL COMMENT '消息随机数(去重)',
    `message_time`   BIGINT      NOT NULL COMMENT '消息时间(毫秒时间戳)',
    `create_time`    BIGINT      NOT NULL COMMENT '创建时间(毫秒时间戳)',
    PRIMARY KEY (`app_id`, `group_id`, `message_key`),
    KEY              `idx_app_group_time` (`app_id`, `group_id`, `message_time`),
    KEY              `idx_app_message_key` (`app_id`, `message_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息历史表';

-- ========================================
-- 消息内容表
-- ========================================
DROP TABLE IF EXISTS `im_message_body`;
CREATE TABLE `im_message_body`
(
    `message_key`  BIGINT  NOT NULL COMMENT '消息ID(全局唯一)',
    `app_id`       INT     NOT NULL COMMENT '应用ID',
    `message_body` TEXT    NOT NULL COMMENT '消息内容(JSON格式)',
    `security_key` VARCHAR(100)     DEFAULT NULL COMMENT '加密密钥',
    `message_time` BIGINT  NOT NULL COMMENT '消息时间(毫秒时间戳)',
    `del_flag`     TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识: 0-正常 1-已删除',
    `extra`        VARCHAR(1024)    DEFAULT NULL COMMENT '扩展字段(JSON)',
    `create_time`  BIGINT  NOT NULL COMMENT '创建时间(毫秒时间戳)',
    PRIMARY KEY (`message_key`),
    KEY            `idx_app_time` (`app_id`, `message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息内容表';

-- ========================================
-- 单聊消息历史表
-- ========================================
DROP TABLE IF EXISTS `im_message_history`;
CREATE TABLE `im_message_history`
(
    `app_id`         INT         NOT NULL COMMENT '应用ID',
    `owner_id`       VARCHAR(50) NOT NULL COMMENT '消息拥有者ID(用于分表)',
    `message_key`    BIGINT      NOT NULL COMMENT '消息ID(关联im_message_body)',
    `from_id`        VARCHAR(50) NOT NULL COMMENT '发送方用户ID',
    `to_id`          VARCHAR(50) NOT NULL COMMENT '接收方用户ID',
    `sequence`       BIGINT DEFAULT NULL COMMENT '消息序列号',
    `message_random` INT    DEFAULT NULL COMMENT '消息随机数(去重)',
    `message_time`   BIGINT      NOT NULL COMMENT '消息时间(毫秒时间戳)',
    `create_time`    BIGINT      NOT NULL COMMENT '创建时间(毫秒时间戳)',
    PRIMARY KEY (`app_id`, `owner_id`, `message_key`),
    KEY              `idx_app_owner_time` (`app_id`, `owner_id`, `message_time`),
    KEY              `idx_app_message_key` (`app_id`, `message_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单聊消息历史表';

-- ========================================
-- 用户资料表
-- ========================================
DROP TABLE IF EXISTS `im_user_data`;
CREATE TABLE `im_user_data`
(
    `app_id`             INT         NOT NULL COMMENT '应用ID',
    `user_id`            VARCHAR(50) NOT NULL COMMENT '用户ID',
    `nick_name`          VARCHAR(100)         DEFAULT NULL COMMENT '昵称',
    `password`           VARCHAR(255)         DEFAULT NULL COMMENT '密码(加密)',
    `photo`              VARCHAR(500)         DEFAULT NULL COMMENT '头像URL',
    `user_sex`           TINYINT              DEFAULT NULL COMMENT '性别: 0-未知 1-男 2-女',
    `birth_day`          VARCHAR(20)          DEFAULT NULL COMMENT '生日(格式: YYYY-MM-DD)',
    `location`           VARCHAR(100)         DEFAULT NULL COMMENT '地址',
    `self_signature`     VARCHAR(500)         DEFAULT NULL COMMENT '个性签名',
    `friend_allow_type`  TINYINT     NOT NULL DEFAULT 1 COMMENT '加好友验证类型: 1-无需验证 2-需要验证',
    `forbidden_flag`     TINYINT     NOT NULL DEFAULT 0 COMMENT '禁用标识: 0-正常 1-已禁用',
    `disable_add_friend` TINYINT     NOT NULL DEFAULT 0 COMMENT '禁止添加好友: 0-允许 1-禁止',
    `silent_flag`        TINYINT     NOT NULL DEFAULT 0 COMMENT '禁言标识: 0-正常 1-已禁言',
    `user_type`          TINYINT     NOT NULL DEFAULT 1 COMMENT '用户类型: 1-普通用户 2-客服 3-机器人 100-管理员',
    `del_flag`           TINYINT     NOT NULL DEFAULT 0 COMMENT '删除标识: 0-正常 1-已删除',
    `extra`              VARCHAR(1024)        DEFAULT NULL COMMENT '扩展字段(JSON)',
    `create_time`        BIGINT      NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time`        BIGINT               DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`app_id`, `user_id`),
    KEY                  `idx_app_nick_name` (`app_id`, `nick_name`),
    KEY                  `idx_app_user_type` (`app_id`, `user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资料表';

-- ========================================
-- 初始化测试数据
-- ========================================

-- 用户数据
INSERT INTO `im_user_data` (`app_id`, `user_id`, `nick_name`, `password`, `photo`, `user_sex`, `birth_day`, `location`,
                            `self_signature`, `friend_allow_type`, `forbidden_flag`, `disable_add_friend`,
                            `silent_flag`,
                            `user_type`, `del_flag`, `extra`, `create_time`, `update_time`)
VALUES (10001, '10001', '段勇', 'encrypted_password_hash', 'http://example.com/avatar/10001.jpg', 1, '2004-01-28',
        '北京市朝阳区', '热爱生活，享受当下', 1, 0, 0, 0, 1, 0, NULL, 1680000000000, NULL),
       (10001, '10002', '汤敏', 'encrypted_password_hash', 'http://example.com/avatar/10002.jpg', 2, '1991-07-23',
        '上海市浦东新区', '做最好的自己', 1, 0, 0, 0, 1, 0, NULL, 1680000000000, NULL),
       (10001, '10003', '白艳', 'encrypted_password_hash', 'http://example.com/avatar/10003.jpg', 1, '2008-11-12',
        '广州市天河区', '人生如戏，全靠演技', 1, 0, 0, 0, 1, 0, NULL, 1680000000000, NULL),
       (10001, 'bantanger', '半糖', NULL, NULL, NULL, NULL, NULL, NULL, 2, 0, 0, 0, 1, 0, NULL, 1680000000000, NULL),
       (10001, 'admin', 'admin', NULL, NULL, NULL, NULL, NULL, NULL, 1, 0, 0, 0, 100, 0, NULL, 1680000000000, NULL);

-- 好友关系数据
INSERT INTO `im_friendship` (`app_id`, `from_id`, `to_id`, `remark`, `status`, `black`, `add_source`, `extra`,
                             `friend_sequence`, `black_sequence`, `create_time`, `update_time`)
VALUES (10001, '10001', '10002', '好友备注1', 1, 0, 'search', NULL, 1, NULL, 1680608016816, NULL),
       (10001, '10002', '10001', '好友备注2', 1, 0, 'search', NULL, 1, NULL, 1680608016850, NULL),
       (10001, '10001', 'bantanger', '半糖', 1, 0, 'qrcode', NULL, 2, NULL, 1680608016850, NULL),
       (10001, 'bantanger', '10001', '', 1, 0, 'qrcode', NULL, 2, NULL, 1680608016850, NULL);

-- 群组数据
INSERT INTO `im_group` (`app_id`, `group_id`, `owner_id`, `group_type`, `group_name`, `photo`, `introduction`,
                        `notification`, `mute`, `apply_join_type`, `max_member_count`, `status`, `sequence`,
                        `extra`, `create_time`, `update_time`)
VALUES (10001, '27a35ff2f9be4cc9a8d3db1ad3322804', 'bantanger', 1, '半糖的IM小屋', 'http://example.com/group/001.jpg',
        '半糖的IM聊天室小屋', '大家好，我是 BanTanger 半糖', 0, 2, 200, 0, 1, NULL, 1680055132161, NULL);

-- 群成员数据
INSERT INTO `im_group_member` (`group_member_id`, `app_id`, `group_id`, `member_id`, `role`, `alias`, `mute`,
                               `speak_date`, `join_type`, `join_time`, `leave_time`, `extra`)
VALUES (1, 10001, '27a35ff2f9be4cc9a8d3db1ad3322804', 'bantanger', 2, '群主', 0, NULL, 'create', 1680055132161, NULL,
        NULL),
       (2, 10001, '27a35ff2f9be4cc9a8d3db1ad3322804', '10001', 0, NULL, 0, NULL, 'invite', 1679400643080, NULL, NULL),
       (3, 10001, '27a35ff2f9be4cc9a8d3db1ad3322804', '10002', 0, NULL, 0, NULL, 'invite', 1679400643080, NULL, NULL);

-- 好友申请数据
INSERT INTO `im_friendship_request` (`id`, `app_id`, `from_id`, `to_id`, `remark`, `add_source`, `add_wording`,
                                     `read_status`, `approve_status`, `sequence`, `create_time`, `update_time`)
VALUES (1, 10001, '10003', 'bantanger', '想加你好友', 'search', '你好，我是10003', 0, 0, 1, 1681025483768, NULL);
