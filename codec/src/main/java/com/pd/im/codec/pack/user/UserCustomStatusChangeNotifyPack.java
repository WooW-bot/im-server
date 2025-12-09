package com.pd.im.codec.pack.user;

import lombok.Data;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class UserCustomStatusChangeNotifyPack {

    private String customText;

    private Integer customStatus;

    private String userId;
}