package com.pd.im.service.friendship.model.resp;

import com.pd.im.common.model.UserProfileDto;
import lombok.Data;

/**
 * 好友关系 DTO，带上好友基本信息
 *
 * @author Parker
 * @date 2026-03-20
 */
@Data
public class ImFriendDto {

  private Integer appId;
  private String fromId;
  private String toId;
  private String remark;
  private Integer status;
  private Integer black;
  private Long createTime;
  private Long friendSequence;
  private String addSource;
  private String extra;

  /**
   * 好友的基本信息
   */
  private UserProfileDto userProfile;
}
