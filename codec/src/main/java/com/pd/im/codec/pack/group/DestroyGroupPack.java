package com.pd.im.codec.pack.group;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class DestroyGroupPack {
    private String groupId;
    private Long sequence;
}
