package com.pd.im.service.friendship.model.req;

import com.pd.im.common.enums.friend.FriendshipStatus;
import com.pd.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class ImportFriendShipReq extends RequestBase {
    @NotBlank(message = "fromId不能为空")
    private String fromId;
    private List<ImportFriendDto> friendItem;

    @Data
    public static class ImportFriendDto {

        private String toId;

        private String remark;

        private String addSource;

        private Integer status = FriendshipStatus.FRIEND_STATUS_NO_FRIEND.getCode();

        private Integer black = FriendshipStatus.BLACK_STATUS_NORMAL.getCode();
    }
}
