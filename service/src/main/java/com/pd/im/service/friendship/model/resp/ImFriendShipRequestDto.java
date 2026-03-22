package com.pd.im.service.friendship.model.resp;

import com.pd.im.codec.pack.user.UserSnapshotPack;
import lombok.Data;

/**
 * 好友申请 DTO，带上申请人基本信息 (精简版) 剔除了用户不关心的 ID 和重复 ID
 *
 * @author Parker
 * @date 12/9/25
 */
@Data
public class ImFriendShipRequestDto {

  /**
   * 申请唯一 ID
   */
  private Long id;

  /**
   * 申请人 ID
   */
  private String fromId;

  /**
   * 是否已读 (0: 未读, 1: 已读)
   */
  private Integer readStatus;

  /**
   * 好友来源 (例如: "搜索", "扫码")
   */
  private String addSource;

  /**
   * 申请话术/打招呼内容
   */
  private String addWording;

  /**
   * 审批状态 (0: 待审批, 1: 同意, 2: 拒绝)
   */
  private Integer approveStatus;

  /**
   * 申请创建时间 (毫秒时间戳)
   */
  private Long createTime;

  /**
   * 最近一次更新时间 (毫秒时间戳)
   */
  private Long updateTime;

  /**
   * 全局增量序列号 (Timeline Seq)
   */
  private Long sequence;

  /**
   * 对方 (Counterparty) 资料快照
   */
  private UserSnapshotPack userProfile;
}
