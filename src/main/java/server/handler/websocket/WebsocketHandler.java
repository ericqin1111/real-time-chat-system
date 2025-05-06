package server.handler.websocket;

import config.MyBatisConfig;
import entity.FriendMessage;
import entity.GroupMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mapper.*;
import server.GlobalVar;

import java.time.LocalDateTime;
import java.util.*;

public class WebsocketHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        Map<String, String> content = ctx.channel().attr(GlobalVar.DATA_CONTEXT).get();

        String type = content.get("type");
        System.out.println("contentType:" + type);
        //给好友发送消息
        if (type.equals("2")) {
            System.out.println("handleFriendmessage");
            handlerFriend(content, ctx);
        }
        //给群组发送消息
        else if (type.equals("3")) {
            System.out.println("handleGroupmessage");
            handlerGroup(content, ctx);
        }
        //给好友发送文件
        else if (type.equals("4")) {
            System.out.println("handleFriendFile");
            handlerFriendFile(content, ctx);
        }
        //给群组发送文件
        else if (type.equals("5")) {
            System.out.println("handleGroupFile");
            handlerGroupFile(content, ctx);
        }
    }

    private void handlerFriend(Map<String, String> content, ChannelHandlerContext ctx) {
        String target = content.get("target");
        String userid =  ctx.channel().attr(GlobalVar.USERID).get();
        System.out.println("userID: " + userid);
        String message = content.get("content");
        String contentType=content.get("contentType");
        System.out.println("contenttype:");
        content.put("from", userid);
        content.put("mainbody", userid);

        GlobalVar.businessExecutor.execute(() ->{



            MyBatisConfig.execute(sqlSession -> {
                FriendMessageMapper friendMessageMapper = sqlSession.getMapper(FriendMessageMapper.class);
                FriendMessageStatMapper friendMessageStatMapper = sqlSession.getMapper(FriendMessageStatMapper.class);

                System.out.println("Mappers start");
                //保证第一个id小于第二个id
                int first = Integer.parseInt(userid);
                int second = Integer.parseInt(target);
                if (first > second) {
                    int mid = first;
                    first = second;
                    second = mid;
                }
                int mess_id = friendMessageStatMapper.getTotal(first, second);
                friendMessageStatMapper.updateTotalCount(first,second);
                System.out.println("friendMapper Ok");

                content.put("messId", Integer.toString(mess_id));

                FriendMessage friendMessage = new FriendMessage();

//                friendMessage.setMessageId(mess_id);
                friendMessage.setSenderId(Integer.parseInt(userid));
                friendMessage.setReceiverId(Integer.parseInt(target));
                friendMessage.setContent(message);
                LocalDateTime now = GlobalVar.toLocalDateTime(new Date());
                content.put("time", now.toString());
                friendMessage.setSentTime(now);
                //1表示文字消息
                friendMessage.setContentType(1);


                System.out.println("ready to insert friendMessage");
                friendMessageMapper.insert(friendMessage);

                System.out.println("insert friendMessage ok");
            });
            System.out.println("readyToSend");
            GlobalVar.sendMessageToUser(target, content);
            GlobalVar.sendMessageToUser(userid, content);

        });



    }

    private void handlerGroup(Map<String, String> content, ChannelHandlerContext ctx) {
        String target = content.get("target");
        String userid =  ctx.channel().attr(GlobalVar.USERID).get();
        System.out.println("userID: " + userid);
        String message = content.get("content");
        content.put("from", userid);
        content.put("mainbody", userid);


        List<Integer> userIdList = new ArrayList<>();
        GlobalVar.businessExecutor.execute(() ->{



            MyBatisConfig.execute(sqlSession -> {
                GroupMessageStatMapper groupMessageStatMapper = sqlSession.getMapper(GroupMessageStatMapper.class);
                GroupMemberMapper groupMemberMapper = sqlSession.getMapper(GroupMemberMapper.class);
                int mess_id = groupMessageStatMapper.getTotalMessagesByGroupId(Integer.parseInt(target));


                groupMessageStatMapper.updateTotalMessages(Integer.parseInt(target));
                content.put("messId", Integer.toString(mess_id));

                GroupMessage groupMessage = new GroupMessage();

//                groupMessage.setMessageId(mess_id);
                groupMessage.setGroupId(Integer.parseInt(target));
                groupMessage.setSenderId(Integer.parseInt(userid));
                groupMessage.setContent(message);
                //1表示文字消息
                groupMessage.setContentType(1);
                LocalDateTime now = GlobalVar.toLocalDateTime(new Date());
                content.put("time", now.toString());
                groupMessage.setSentTime(now);

                GroupMessageMapper groupMessageMapper = sqlSession.getMapper(GroupMessageMapper.class);
                groupMessageMapper.insert(groupMessage);

                userIdList.addAll(groupMemberMapper.findUsersByGroupId(Integer.parseInt(target))) ;

            });



            for (Integer userId : userIdList) {
                System.out.println("current messid: " + content.get("messId"));
                System.out.println("go throughing group: userId: " + userId);
                GlobalVar.sendMessageToUser(Integer.toString(userId), content);
            }


        });



    }
    private void handlerFriendFile(Map<String, String> content, ChannelHandlerContext ctx) {
        String target = content.get("target");
        String userid =  ctx.channel().attr(GlobalVar.USERID).get();
        System.out.println("userID: " + userid);
        String fileName = content.get("fileName");
        content.put("from", userid);
        content.put("mainbody", userid);

        GlobalVar.businessExecutor.execute(() ->{

            System.out.println("received friend file");

            MyBatisConfig.execute(sqlSession -> {
                FriendMessageMapper friendMessageMapper = sqlSession.getMapper(FriendMessageMapper.class);
                FriendMessageStatMapper friendMessageStatMapper = sqlSession.getMapper(FriendMessageStatMapper.class);


                //保证第一个id小于第二个id
                int first = Integer.parseInt(userid);
                int second = Integer.parseInt(target);
                if (first > second) {
                    int mid = first;
                    first = second;
                    second = mid;
                }
                int mess_id = friendMessageStatMapper.getTotal(first, second);
                friendMessageStatMapper.updateTotalCount(first,second);
                System.out.println("friendMapper Ok");

                content.put("messId", Integer.toString(mess_id));

                FriendMessage friendMessage = new FriendMessage();

//                friendMessage.setMessageId(mess_id);
                friendMessage.setSenderId(Integer.parseInt(userid));
                friendMessage.setReceiverId(Integer.parseInt(target));
                friendMessage.setContent(fileName);
                LocalDateTime now = GlobalVar.toLocalDateTime(new Date());
                content.put("time", now.toString());
                friendMessage.setSentTime(now);
                //2表示文件
                friendMessage.setContentType(2);

                friendMessageMapper.insert(friendMessage);


            });
            System.out.println("sql Over in frinend file websocket ");
            GlobalVar.sendMessageToUser(target, content);
            GlobalVar.sendMessageToUser(userid, content);

        });

    }
    private void handlerGroupFile(Map<String, String> content, ChannelHandlerContext ctx) {
        String target = content.get("target");
        String userid =  ctx.channel().attr(GlobalVar.USERID).get();
        String fileName = content.get("fileName");
        content.put("from", userid);
        content.put("mainbody", userid);

        System.out.println("userID: " + userid);
        List<Integer> userIdList = new ArrayList<>();
        GlobalVar.businessExecutor.execute(() ->{



            MyBatisConfig.execute(sqlSession -> {
                GroupMessageStatMapper groupMessageStatMapper = sqlSession.getMapper(GroupMessageStatMapper.class);
                GroupMemberMapper groupMemberMapper = sqlSession.getMapper(GroupMemberMapper.class);

                int mess_id = groupMessageStatMapper.getTotalMessagesByGroupId(Integer.parseInt(target));
                groupMessageStatMapper.updateTotalMessages(Integer.parseInt(target));

                content.put("messId", Integer.toString(mess_id));

                GroupMessage groupMessage = new GroupMessage();

//                groupMessage.setMessageId(mess_id);
                groupMessage.setGroupId(Integer.parseInt(target));
                groupMessage.setSenderId(Integer.parseInt(userid));
                groupMessage.setContent(fileName);

                System.out.println("File:"+fileName);
                //2表示文件消息
                groupMessage.setContentType(2);
                LocalDateTime now = GlobalVar.toLocalDateTime(new Date());
                content.put("time", now.toString());
                groupMessage.setSentTime(now);

                GroupMessageMapper groupMessageMapper = sqlSession.getMapper(GroupMessageMapper.class);
                groupMessageMapper.insert(groupMessage);

                userIdList.addAll(groupMemberMapper.findUsersByGroupId(Integer.parseInt(target))) ;

            });



            for (Integer userId : userIdList) {
                GlobalVar.sendMessageToUser(Integer.toString(userId), content);
            }


        });
    }
}
