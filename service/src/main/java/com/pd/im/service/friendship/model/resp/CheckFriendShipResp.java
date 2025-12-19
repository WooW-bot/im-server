package com.pd.im.service.friendship.model.resp;

import lombok.Data;

/**
 * @author Parker
 * @date 12/9/25
 */
@Data
public class CheckFriendShipResp {

    private String fromId;

    private String toId;

    /**
     * 校验状态，根据双向校验和单向校验有不同的status值
     *
     * <p><b>【好友关系】单向校验：</b>
     * <ul>
     *   <li>1 - from添加了to（不确定to是否添加了from）</li>
     *   <li>0 - from没有添加to（不确定to是否添加了from）</li>
     * </ul>
     *
     * <p><b>【好友关系】双向校验：</b>
     * <ul>
     *   <li>1 - 双向好友（from添加了to，to也添加了from）</li>
     *   <li>2 - 单向好友A→B（from添加了to，to没有添加from）</li>
     *   <li>3 - 单向好友B→A（from没有添加to，to添加了from）</li>
     *   <li>4 - 无好友关系（双方都没有添加）</li>
     * </ul>
     *
     * <p><b>【黑名单】单向校验：</b>
     * <ul>
     *   <li>1 - from拉黑了to（不确定to是否拉黑了from）</li>
     *   <li>0 - from没有拉黑to（不确定to是否拉黑了from）</li>
     * </ul>
     *
     * <p><b>【黑名单】双向校验：</b>
     * <ul>
     *   <li>1 - 互相拉黑（from拉黑了to，to也拉黑了from）</li>
     *   <li>2 - 单向拉黑A→B（from拉黑了to，to没有拉黑from）</li>
     *   <li>3 - 单向拉黑B→A（from没有拉黑to，to拉黑了from）</li>
     *   <li>4 - 无拉黑关系（双方都没有拉黑）</li>
     * </ul>
     */
    private Integer status;
}
