package com.pd.im.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户基本信息 DTO (用于嵌套在其它 DTO 中)
 *
 * @author Parker
 * @date 2026-03-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

  private String userId;
  private String nickName;
  private String faceUrl;
  private Integer gender;
}
