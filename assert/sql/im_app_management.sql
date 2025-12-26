-- ========================================
-- IM应用管理数据库 - 生产级SQL脚本
-- 版本: 1.1
-- 创建日期: 2025-12-26
-- 说明: 应用配置管理，支持多租户应用隔离
-- ========================================

USE `im_core`;

-- ========================================
-- 应用基本信息表
-- ========================================
DROP TABLE IF EXISTS `im_app`;
CREATE TABLE `im_app`
(
    `app_id`          INT          NOT NULL AUTO_INCREMENT COMMENT '应用ID(自增主键)',
    `app_name`        VARCHAR(100) NOT NULL COMMENT '应用名称',
    `encryption_key`  VARCHAR(500) NOT NULL COMMENT '数据加密密钥(已使用MasterSecert加密)',
    `private_key`     VARCHAR(500) NOT NULL COMMENT 'UserSig签名私钥(已加密)',
    `app_status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '应用状态: 0-已停用 1-正常 2-已锁定',
    `callback_url`    VARCHAR(500)          DEFAULT NULL COMMENT '回调地址',
    `max_user_count`  INT                   DEFAULT 10000 COMMENT '最大用户数限制',
    `max_group_count` INT                   DEFAULT 1000 COMMENT '最大群组数限制',
    `expire_time`     BIGINT                DEFAULT NULL COMMENT '过期时间(毫秒时间戳, NULL表示永不过期)',
    `remark`          VARCHAR(500)          DEFAULT NULL COMMENT '备注说明',
    `extra`           VARCHAR(2048)         DEFAULT NULL COMMENT '扩展字段(JSON)',
    `create_time`     BIGINT       NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time`     BIGINT                DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`app_id`),
    KEY               `idx_app_status` (`app_status`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用基本信息表';

-- ========================================
-- 应用配置表(可选，用于存储更灵活的配置项)
-- ========================================
DROP TABLE IF EXISTS `im_app_config`;
CREATE TABLE `im_app_config`
(
    `config_id`    BIGINT AUTO_INCREMENT COMMENT '配置ID(自增主键)',
    `app_id`       INT          NOT NULL COMMENT '应用ID',
    `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT                  DEFAULT NULL COMMENT '配置值',
    `config_type`  VARCHAR(50)           DEFAULT 'string' COMMENT '配置类型: string, int, boolean, json',
    `description`  VARCHAR(500)          DEFAULT NULL COMMENT '配置描述',
    `is_encrypted` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否加密: 0-否 1-是',
    `create_time`  BIGINT       NOT NULL COMMENT '创建时间(毫秒时间戳)',
    `update_time`  BIGINT                DEFAULT NULL COMMENT '更新时间(毫秒时间戳)',
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_app_config` (`app_id`, `config_key`),
    KEY            `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用配置表';

-- ========================================
-- 应用操作日志表
-- ========================================
DROP TABLE IF EXISTS `im_app_audit_log`;
CREATE TABLE `im_app_audit_log`
(
    `log_id`       BIGINT AUTO_INCREMENT COMMENT '日志ID(自增主键)',
    `app_id`       INT          NOT NULL COMMENT '应用ID',
    `operator_id`  VARCHAR(50)           DEFAULT NULL COMMENT '操作人ID',
    `operator_ip`  VARCHAR(50)           DEFAULT NULL COMMENT '操作人IP',
    `action_type`  VARCHAR(50)  NOT NULL COMMENT '操作类型: create, update, delete, config_change',
    `action_desc`  VARCHAR(500)          DEFAULT NULL COMMENT '操作描述',
    `old_value`    TEXT                  DEFAULT NULL COMMENT '修改前的值(JSON)',
    `new_value`    TEXT                  DEFAULT NULL COMMENT '修改后的值(JSON)',
    `extra`        VARCHAR(1024)         DEFAULT NULL COMMENT '扩展字段(JSON)',
    `create_time`  BIGINT       NOT NULL COMMENT '创建时间(毫秒时间戳)',
    PRIMARY KEY (`log_id`),
    KEY            `idx_app_id` (`app_id`),
    KEY            `idx_operator_id` (`operator_id`),
    KEY            `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用操作日志表';

-- ========================================
-- 初始化测试数据
-- ========================================

-- ========================================
-- 重要说明：privateKey 使用基于应用属性的加密存储（无需额外管理密钥）
-- ========================================
-- 加密方式：
--   使用 appId 自动派生加密密钥（PBKDF2-HMAC-SHA256）
--   无需配置额外的加密密钥，系统自动处理
--
-- 生成方式：
--   1. 运行 SQL 生成工具：
--      mvn exec:java -Dexec.mainClass="com.pd.im.common.tools.AppSqlGenerator"
--
--   2. 按提示输入：
--      - 应用ID (例如: 10001)
--      - 应用名称 (例如: IM测试应用)
--
--   3. 工具会输出：
--      - 自动生成的明文 privateKey (64位 Hex，请妥善保管)
--      - 自动生成的加密 privateKey
--      - 完整的 SQL 插入语句
--
-- 重要提示：
--   ✓ 无需配置额外的加密密钥
--   ✓ 系统会自动使用 appId 解密
-- ========================================

-- 插入默认应用（privateKey 已加密）
-- 注意：下面的 private_key 需要使用 AppSqlGenerator 工具生成
INSERT INTO `im_app` (`app_id`, `app_name`, `private_key`,
                      `app_status`, `callback_url`, `max_user_count`, `max_group_count`,
                      `expire_time`, `remark`, `extra`, `create_time`, `update_time`)
VALUES (10001, 'IM测试应用',
        -- 加密后的 privateKey（原始值: 123456... 需根据实际生成的Hex密钥匹配）
        -- 使用 AppSqlGenerator 工具生成
        'ENCRYPTED_PRIVATE_KEY_PLACEHOLDER',
        1, NULL, 100000, 10000, NULL, '默认测试应用', NULL, 1680000000000, NULL);

-- 插入一些应用配置示例
INSERT INTO `im_app_config` (`app_id`, `config_key`, `config_value`, `config_type`, `description`, `is_encrypted`,
                              `create_time`, `update_time`)
VALUES (10001, 'msg_recall_time_limit', '120', 'int', '消息撤回时间限制(秒)', 0, 1680000000000, NULL),
       (10001, 'enable_message_read_receipt', 'true', 'boolean', '是否启用消息已读回执', 0, 1680000000000, NULL),
       (10001, 'max_group_member_count', '500', 'int', '单个群组最大成员数', 0, 1680000000000, NULL),
       (10001, 'sensitive_words', '["敏感词1", "敏感词2"]', 'json', '敏感词列表', 0, 1680000000000, NULL);

-- 插入操作日志示例
INSERT INTO `im_app_audit_log` (`app_id`, `operator_id`, `operator_ip`, `action_type`, `action_desc`, `old_value`,
                                 `new_value`, `extra`, `create_time`)
VALUES (10001, 'admin', '127.0.0.1', 'create', '创建应用', NULL,
        '{"app_name": "IM测试应用", "app_status": 1}', NULL, 1680000000000);
