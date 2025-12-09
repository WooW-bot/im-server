package com.pd.im.codec.pack.friendship;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class DeleteBlackPack {

    private String fromId;

    private String toId;

    private Long sequence;
}
