package com.pd.im.codec.pack.friendship;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class ApproveFriendRequestPack {
    private Long id;
    //1同意 2拒绝
    private Integer status;
    private Long sequence;
}
