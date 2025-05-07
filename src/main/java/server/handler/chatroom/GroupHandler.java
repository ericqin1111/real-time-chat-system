package server.handler.chatroom;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import mapper.GroupMemberReadMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mapper.ChatGroupMapper;
import mapper.GroupMemberMapper;
import mapper.GroupMessageStatMapper;
import entity.ChatGroup;
import entity.GroupMember;
import entity.GroupMessageStat;
import entity.GroupMemberRead;
import java.util.List;

@Service
public class GroupHandler {

    private final ChatGroupMapper chatGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupMessageStatMapper groupMessageStatMapper;
    private final GroupMemberReadMapper groupMemberReadMapper;

    // 构造函数注入修正后的Mapper
    public GroupHandler(ChatGroupMapper chatGroupMapper,
                        GroupMemberMapper groupMemberMapper,
                        GroupMemberReadMapper groupMemberReadMapper,
                        GroupMessageStatMapper groupMessageStatMapper) {
        this.chatGroupMapper = chatGroupMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMemberReadMapper = groupMemberReadMapper;
        this.groupMessageStatMapper = groupMessageStatMapper;
    }

    @Transactional
    public void createGroup(String groupName, List<Integer> userIds, List<String> usernames) {
        validateParameters(groupName, userIds, usernames);


        ChatGroup newGroup = new ChatGroup();
        newGroup.setGroupName(groupName);
        newGroup.setMemberCount(userIds.size());
        chatGroupMapper.insert(newGroup);
        int generatedGroupId = newGroup.getGroupId();

        // 初始化群消息统计（使用GroupMessageStatMapper）
        initGroupMessageStats(generatedGroupId);

        // 批量添加群成员及相关记录
        for (int i = 0; i < userIds.size(); i++) {
            int userId = userIds.get(i);
            String username = usernames.get(i);

            addGroupMember(generatedGroupId, userId, username);
            initMemberReadStatus(generatedGroupId, userId);
        }
    }

    private void validateParameters(String groupName, List<Integer> userIds, List<String> usernames) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("群名称不能为空");
        }
        if (userIds == null || usernames == null || userIds.size() != usernames.size()) {
            throw new IllegalArgumentException("用户ID列表和用户名列表不匹配");
        }
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("群组成员不能为空");
        }
    }

    private void initGroupMessageStats(int groupId) {
        GroupMessageStat stats = new GroupMessageStat();
        stats.setGroupId(groupId);
        stats.setTotalMessages(0);
        groupMessageStatMapper.insert(stats);
    }

    private void addGroupMember(int groupId, int userId, String username) {
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setMemberAlias(username);
        groupMemberMapper.insert(member);
    }

    private void initMemberReadStatus(int groupId, int userId) {
        GroupMemberRead readRecord = new GroupMemberRead();
        readRecord.setGroupId(groupId);
        readRecord.setUserId(userId);
        readRecord.setReadCount(0);
        groupMemberReadMapper.insert(readRecord);
    }
}