package com.pd.im.common.model;

import lombok.Data;

/**
 * @author Parker
 * @date 12/7/25
 */
@Data
public class SyncReq extends RequestBase {
    /**
     * 客户端最大 Seq
     */
    private Long lastSequence;

    /**
     * 一次性最大拉取多少
     */
    private Integer maxLimit;
}
