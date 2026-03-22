package com.pd.im.codec.pack.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资料快照包，用于在通知中携带精简的用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshotPack {
    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户头像
     */
    private String faceUrl;

    /**
     * 用户性别 (1: 男, 2: 女, 0: 未知)
     */
    private Integer gender;
}
