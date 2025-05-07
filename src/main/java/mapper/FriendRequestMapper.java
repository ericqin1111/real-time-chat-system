package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.FriendRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
//    @Select("SELECT id, requester_id, target_id, request_type, created_at, requester_name " +
//            "FROM friend_requests " +
//            "WHERE target_id = #{targetUserId} AND request_type = 0 " +
//            "ORDER BY created_at DESC")
//    List<FriendRequest> findPendingRequestsForTarget(@Param("targetUserId") Integer targetUserId);
//
//    // --- 用于接受/拒绝请求的 Mapper 方法 ---
//    @Update("UPDATE friend_requests SET request_type = #{newRequestType} " +
//            "WHERE id = #{requestId} AND target_id = #{targetUserId} AND request_type = 0")
//    int updateRequestStatus(@Param("requestId") Integer requestId,
//                            @Param("newRequestType") Integer newRequestType,
//                            @Param("targetId") Integer targetId); // targetId 用于权限校验
//
//    @Insert("INSERT INTO user_friends (user1_id, user2_id, alias_name,) " + // 假设你的好友关系表名为 friendships
//            "VALUES (#{user1Id}, #{user2Id}, #{aliaName})")
//    int addFriendship(@Param("user1Id") Integer user1Id, @Param("user2Id") Integer user2Id,@Param("alias_name") String aliasName);
//
//    @Insert("")
//    @Select("SELECT id, requester_id, target_id FROM friend_requests WHERE id = #{requestId}")
//    FriendRequest findRequestDetailsById(@Param("requestId") Integer requestId);

    @Select("SELECT id, requester_id, target_id, request_type, created_at, requester_name " +
            "FROM friend_requests " +
            "WHERE target_id = #{currentUserId} AND request_type = 0 " + // request_type = 0 是待处理
            "ORDER BY created_at DESC")
    List<FriendRequest> findPendingRequestsForUser(@Param("currentUserId") Integer currentUserId);

    @Update("UPDATE friend_requests SET request_type = #{newRequestType} " +
            "WHERE id = #{requestId} AND target_id = #{currentUserId} AND request_type = 0")
    int updateRequestStatus(@Param("requestId") Integer requestId,
                            @Param("newRequestType") Integer newRequestType,
                            @Param("currentUserId") Integer currentUserId); // currentUserId 是请求的 target_id

    @Select("SELECT id, requester_id, target_id, request_type, created_at, requester_name " +
            "FROM friend_requests WHERE id = #{requestId}")
    FriendRequest findRequestById(@Param("requestId") Integer requestId);

    /**
     * 添加好友关系到 user_friend 表
     * @param userId 用户自己的ID
     * @param friendId 好友的ID
     * @param aliasName 给好友设置的备注名
     * @return 插入的行数
     */
    @Insert("INSERT INTO user_friends (user_id, friend_id, alias_name) " +
            "VALUES (#{userId}, #{friendId}, #{aliasName})")
    int addUserFriend(@Param("userId") Integer userId,
                      @Param("friendId") Integer friendId,
                      @Param("aliasName") String aliasName);

    /**
     * 插入一条新的好友请求
     */
    @Insert("INSERT INTO friend_requests (requester_id, target_id, requester_name, request_type) " + // 假设有 message 字段
            "VALUES (#{requesterId}, #{targetId}, #{requesterName}, 0)") // request_type = 0 (pending)
    int insertFriendRequest(FriendRequest request);
}
