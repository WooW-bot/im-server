package com.pd.im.codec.pack.friendship;

import lombok.Builder;
import lombok.Data;

/**
 * 已读所有好友申请通知包
 */
@Data
@Builder
public class ReadAllFriendRequestNotifyPack {

  private String fromId;
  private Long sequence;
}
