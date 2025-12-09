package com.pd.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.proto.group.AddGroupMemberPack;
import com.pd.im.codec.proto.group.RemoveGroupMemberPack;
import com.pd.im.codec.proto.group.UpdateGroupMemberPack;
import com.pd.im.common.enums.command.Command;
import com.pd.im.common.enums.command.GroupEventCommand;
import com.pd.im.common.enums.device.ClientType;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.service.group.model.req.GroupMemberDto;
import com.pd.im.service.group.service.ImGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Parker
 * @date 12/8/25
 */
@Component
public class GroupMessageProducer {
    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    public void producer(String userId, Command command, Object data, ClientInfo clientInfo) {
        JSONObject o = (JSONObject) JSONObject.toJSON(data);
        String groupId = o.getString("groupId");

        // 获取所有群成员
        List<String> groupMemberIds = imGroupMemberService.getGroupMemberIds(groupId, clientInfo.getAppId());

        if (command.equals(GroupEventCommand.ADDED_MEMBER)) {
            //发送给管理员和被加入人本身
            List<GroupMemberDto> groupManagers = imGroupMemberService.getGroupManagers(groupId, clientInfo.getAppId());
            AddGroupMemberPack addGroupMemberPack = o.toJavaObject(AddGroupMemberPack.class);
            List<String> members = addGroupMemberPack.getMembers();
            for (GroupMemberDto groupMemberDto : groupManagers) {
                if (!clientInfo.getClientType().equals(ClientType.WEBAPI.getCode())
                        && groupMemberDto.getMemberId().equals(userId)) {
                    messageProducer.sendToOtherClients(groupMemberDto.getMemberId(), command, data, clientInfo);
                } else {
                    messageProducer.sendToAllClients(groupMemberDto.getMemberId(), command, data, clientInfo.getAppId());
                }
            }
            for (String member : members) {
                if (!clientInfo.getClientType().equals(ClientType.WEBAPI.getCode()) && member.equals(userId)) {
                    messageProducer.sendToOtherClients(member, command, data, clientInfo);
                } else {
                    messageProducer.sendToAllClients(member, command, data, clientInfo.getAppId());
                }
            }
        } else if (command.equals(GroupEventCommand.DELETED_MEMBER)) {
            RemoveGroupMemberPack pack = o.toJavaObject(RemoveGroupMemberPack.class);
            String member = pack.getMember();
            List<String> members = imGroupMemberService.getGroupMemberIds(groupId, clientInfo.getAppId());
            members.add(member);
            for (String memberId : members) {
                if (!clientInfo.getClientType().equals(ClientType.WEBAPI.getCode()) && member.equals(userId)) {
                    messageProducer.sendToOtherClients(memberId, command, data, clientInfo);
                } else {
                    messageProducer.sendToAllClients(memberId, command, data, clientInfo.getAppId());
                }
            }
        } else if (command.equals(GroupEventCommand.UPDATED_MEMBER)) {
            UpdateGroupMemberPack pack = o.toJavaObject(UpdateGroupMemberPack.class);
            String memberId = pack.getMemberId();

            List<GroupMemberDto> groupManagers = imGroupMemberService.getGroupManagers(groupId, clientInfo.getAppId());
            GroupMemberDto groupMemberDto = new GroupMemberDto();
            groupMemberDto.setMemberId(memberId);
            groupManagers.add(groupMemberDto);
            // 只通知管理员跟被更新的人？
            for (GroupMemberDto member : groupManagers) {
                if (!clientInfo.getClientType().equals(ClientType.WEBAPI.getCode())
                        && member.getMemberId().equals(userId)) {
                    messageProducer.sendToOtherClients(member.getMemberId(), command, data, clientInfo);
                } else {
                    messageProducer.sendToAllClients(member.getMemberId(), command, data, clientInfo.getAppId());
                }
            }
        } else {
            for (String memberId : groupMemberIds) {
                if (clientInfo.getClientType() != null
                        && !clientInfo.getClientType().equals(ClientType.WEBAPI.getCode())
                        && memberId.equals(userId)) {
                    messageProducer.sendToOtherClients(memberId, command, data, clientInfo);
                } else {
                    messageProducer.sendToAllClients(memberId, command, data, clientInfo.getAppId());
                }
            }
        }
    }
}
