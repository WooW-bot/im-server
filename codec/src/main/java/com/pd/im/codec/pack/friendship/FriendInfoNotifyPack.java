package com.pd.im.codec.pack.friendship;

import com.pd.im.codec.pack.user.UserSnapshotPack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友信息通知包 (对应 3000)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendInfoNotifyPack {

  private Integer appId;
  private String fromId;
  private String toId;
  private String remark;
  private Integer status;
  private Integer black;
  private Long createTime;
  private Long friendSequence;
  private Long blackSequence;
  private String addSource;
  private String extra;

  // 扩展资料快照
  private UserSnapshotPack userProfile;
}
