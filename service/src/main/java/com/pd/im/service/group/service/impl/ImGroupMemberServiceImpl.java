package com.pd.im.service.group.service.impl;

import com.pd.im.service.group.dao.mapper.ImGroupMemberMapper;
import com.pd.im.service.group.service.ImGroupMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Parker
 * @date 12/6/25
 */
@Service
@Slf4j
public class ImGroupMemberServiceImpl implements ImGroupMemberService {
    @Autowired
    ImGroupMemberMapper imGroupMemberMapper;

    @Override
    public List<String> getGroupMemberIds(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupMemberIds(appId, groupId);
    }
}
