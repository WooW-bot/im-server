package com.pd.im.codec.pack.friendship;

import com.pd.im.common.model.UserProfileDto;
import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class AddFriendPack {

  private String fromId;
  /**
   * 备注
   */
  private String remark;
  private String toId;
  /**
   * 好友来源
   */
  private String addSource;
  /**
   * 添加好友时的描述信息（用于打招呼）
   */
  private String addWording;
  private Long sequence;

  /**
   * 用户基本信息
   */
  private UserProfileDto userProfile;
}
