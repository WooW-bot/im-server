package com.pd.im.service.user.model.resp;

import lombok.Data;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
@Data
public class ImportUserResp {
    private List<String> successId;
    private List<String> errorId;
}
