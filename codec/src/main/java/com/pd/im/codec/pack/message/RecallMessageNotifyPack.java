package com.pd.im.codec.pack.message;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Parker
 * @date 12/7/25
 */
@Data
@NoArgsConstructor
public class RecallMessageNotifyPack {
    private String fromId;
    private String toId;
    private Long messageKey;
    private Long messageSequence;
}
