package com.pd.im.common.enums.friend;

import lombok.Getter;

/**
 * 好友验证方式
 *
 * @author Parker
 * @date 12/8/25
 */
@Getter
public enum AllowType {
  /**
   * 允许任何人添加（不需要验证）
   */
  ALLOW_ANY(0),

  /**
   * 需要经过验证（手动同意）
   */
  NEED_CONFIRM(1),

  /**
   * 拒绝任何人添加
   */
  DENY_ANY(2);

  private final int code;

  AllowType(int code) {
    this.code = code;
  }

  /**
   * 判断传入的 code 是否属于当前枚举类型 使用场景：if (AllowType.NEED_CONFIRM.isMe(info.getAllowType())) { ... }
   */
  public boolean isMe(int code) {
    return this.code == code;
  }

  /**
   * 根据 SDK 返回的 code 获取枚举对象
   */
  public static AllowType fromCode(int code) {
    for (AllowType type : AllowType.values()) {
      if (type.code == code) {
        return type;
      }
    }
    return ALLOW_ANY; // 默认值
  }
}
