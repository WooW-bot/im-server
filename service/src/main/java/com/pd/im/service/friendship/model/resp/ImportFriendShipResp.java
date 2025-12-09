package com.pd.im.service.friendship.model.resp;

import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class ImportFriendShipResp {

    private List<String> successId;

    private List<String> errorId;
}
