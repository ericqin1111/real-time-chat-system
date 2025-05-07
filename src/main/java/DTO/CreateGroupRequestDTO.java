package DTO;

import java.util.List;

public  class CreateGroupRequestDTO {
    private String groupName;
    private List<Integer> userIds;
    private List<String> usernames; // 假设这个列表与 userIds 对应

    // Jackson 反序列化需要默认构造函数
    public CreateGroupRequestDTO() {}

    // Getters 和 Setters
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public List<Integer> getUserIds() { return userIds; }
    public void setUserIds(List<Integer> userIds) { this.userIds = userIds; }

    public List<String> getUsernames() { return usernames; }
    public void setUsernames(List<String> usernames) { this.usernames = usernames; }
}
