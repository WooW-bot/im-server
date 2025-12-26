package com.pd.im.service.app.dao;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * IM应用基本信息实体类
 *
 * @author Parker
 * @date 12/24/25
 */
@Data
@TableName("im_app")
public class ImAppEntity {

    /**
     * 应用ID(自增主键)
     */
    @TableId(type = IdType.AUTO)
    private Integer appId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 数据加密密钥(已使用MasterSecert加密)
     */
    private String encryptionKey;


    /**
     * UserSig签名私钥
     */
    private String privateKey;


    /**
     * 应用状态: 0-已停用 1-正常 2-已锁定
     */
    private Integer appStatus;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * 最大用户数限制
     */
    private Integer maxUserCount;

    /**
     * 最大群组数限制
     */
    private Integer maxGroupCount;

    /**
     * 过期时间(毫秒时间戳, NULL表示永不过期)
     */
    private Long expireTime;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 扩展字段(JSON)
     */
    private String extra;

    /**
     * 创建时间(毫秒时间戳) - 插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 更新时间(毫秒时间戳) - 插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;
}
