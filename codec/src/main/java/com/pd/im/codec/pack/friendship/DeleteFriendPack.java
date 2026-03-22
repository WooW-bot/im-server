package com.pd.im.codec.pack.friendship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除好友通知包
 *
 * @author Parker
 * @date 12/8/25
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeleteFriendPack {

  private String fromId;
  private String toId;
  private Long sequence;
}
