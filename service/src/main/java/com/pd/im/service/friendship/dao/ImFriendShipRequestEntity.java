package com.pd.im.service.friendship.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * @author Parker
 * @date 12/9/25
 */
@Data
@TableName("im_friendship_request")
public class ImFriendShipRequestEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  /**
   * 应用 ID
   */
  private Integer appId;
  /**
   * 申请人 ID
   */
  private String fromId;
  /**
   * 接收人 ID
   */
  private String toId;
  /**
   * 备注 (申请人对接收人的备注)
   */
  private String remark;
  /**
   * 申请来源 (1-搜索, 2-扫码, 3-群组)
   */
  private String addSource;
  /**
   * 申请验证语
   */
  private String addWording;
  /**
   * 审批状态 0: 待处理, 1: 同意, 2: 拒绝
   */
  private Integer approveStatus;
  /**
   * 增量同步序列号
   */
  private Long sequence;
  /**
   * 创建时间
   */
  private Long createTime;
  /**
   * 更新时间
   */
  private Long updateTime;
}
