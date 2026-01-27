package com.pd.im.service.friendship.model.resp;

import lombok.Data;

/**
 * 黑名单操作结果
 *
 * @author Parker
 * @date 2026-01-27
 */
@Data
public class BlackListOperationResp {
    /**
     * 目标用户ID
     */
    private String toId;

    /**
     * 结果码: 0-成功, 其他-失败
     */
    private Integer resultCode;

    /**
     * 结果信息
     */
    private String resultInfo;
}
