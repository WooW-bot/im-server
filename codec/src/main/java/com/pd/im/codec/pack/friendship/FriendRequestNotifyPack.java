package com.pd.im.codec.pack.friendship;

import com.pd.im.codec.pack.user.UserSnapshotPack;
import lombok.Builder;
import lombok.Data;

/**
 * 好友申请单统一通知包（涵盖发起、已读、审批所有状态）
 */
@Data
@Builder
public class FriendRequestNotifyPack {

  private Long id;
  private String fromId;
  private String toId;
  // 来源（如：手机号搜索、群聊等）
  private String addSource;
  // 申请附加语（如：我是阿强，通过一下）
  private String addWording;
  private Integer approveStatus;
  private Long createTime;
  private Long updateTime;
  // 服务端生成的序列号（用于拉取增量同步）
  private Long sequence;

  // 扩展资料快照 (对端用户)
  private UserSnapshotPack userProfile;
}
