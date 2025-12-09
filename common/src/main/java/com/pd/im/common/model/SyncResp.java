package com.pd.im.common.model;

import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/7/25
 */
@Data
public class SyncResp<T> {
    /**
     * 服务端本次拉取的最大 Seq
     */
    private Long maxSequence;
    /**
     * 是否拉取完成
     */
    private boolean isCompleted;
    /**
     * 服务端拉取的所有数据列表
     */
    private List<T> dataList;
}
