package com.pd.im.service.user.dao;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 数据库用户数据实体类
 *
 * @author Parker
 * @date 12/7/25
 */
@Data
@TableName("im_user_data")
public class ImUserDataEntity {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String nickName;

    /**
     * 地址
     */
    private String location;

    /**
     * 生日 (YYYY-MM-DD)
     */
    private String birthday;

    /**
     * 密码
     */
    private String password;

    /**
     * 头像URL
     */
    private String faceUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 个性签名
     */
    private String selfSignature;

    /**
     * 加好友验证类型
     * 1: 需要验证
     */
    private Integer friendAllowType;

    /**
     * 禁止添加好友
     * 0: 允许, 1: 禁止
     */
    private Integer disableAddFriend;

    /**
     * 禁用标识
     * 0: 正常, 1: 已禁用
     */
    private Integer forbiddenFlag;

    /**
     * 禁言标识
     * 0: 正常, 1: 已禁言
     */
    private Integer silentFlag;

    /**
     * 用户类型
     * 1: 普通用户, 2: 客服, 3: 机器人
     */
    private Integer userType;
    private Integer appId;
    private Integer delFlag;
    private String extra;

    // 创建时间(毫秒时间戳) - 插入时自动填充
    @TableField(fill = FieldFill.INSERT)
    private Long createTime;

    // 更新时间(毫秒时间戳) - 插入和更新时自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateTime;
}
