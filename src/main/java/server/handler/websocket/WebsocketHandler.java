package server.handler.websocket;

import config.MyBatisConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mapper.GroupMemberMapper;
import mapper.GroupMessageStatMapper;
import server.GlobalVar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebsocketHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {

        Map<String, String> content = ctx.channel().attr(GlobalVar.DATA_CONTEXT).get();
        if (content.get("type").equals("2")) {
            handlerFriend(content, ctx);
        }else if (content.get("type").equals("3")) {
            handlerGroup(content, ctx);
        }
        else if (content.get("type").equals("4")) {
            handlerFriendFile(content, ctx);
        }else if (content.get("type").equals("5")) {
            handlerGroupFile(content, ctx);
        }
    }

    private void handlerFriend(Map<String, String> content, ChannelHandlerContext ctx) {
        String target = content.get("target");
        String userid =  ctx.channel().attr(GlobalVar.USERID).get();
        String type = content.get("type");
        String message = content.get("content");


        GlobalVar.businessExecutor.execute(() ->{

        });

        content.put("from", userid);
        GlobalVar.sendMessageToUser(target, content);

    }

    private void handlerGroup(Map<String, String> content, ChannelHandlerContext ctx) {
        String target = content.get("target");
        String userid =  ctx.channel().attr(GlobalVar.USERID).get();
        String type = content.get("type");
        String message = content.get("content");
        content.put("from", userid);

        GlobalVar.businessExecutor.execute(() ->{

            MyBatisConfig.execute(GroupMessageStatMapper.class, mapper ->{
                mapper.updateTotalMessages(Integer.parseInt(target));
            });

            MyBatisConfig.execute(GroupMemberMapper.class, mapper -> {

                List<Integer> userIdList = mapper.findUsersByGroupId(Integer.parseInt(target));
                for (Integer userId : userIdList) {
                    GlobalVar.sendMessageToUser(Integer.toString(userId), content);
                }
            });


        });



    }
    private void handlerFriendFile(Map<String, String> content, ChannelHandlerContext ctx) {
        String target = content.get("target");
        String userid =  ctx.channel().attr(GlobalVar.USERID).get();
        String type = content.get("type");
        String message = content.get("content");


        GlobalVar.businessExecutor.execute(() ->{
            content.put("from", userid);
            GlobalVar.sendMessageToUser(target, content);
        });
    }
    private void handlerGroupFile(Map<String, String> content, ChannelHandlerContext ctx) {

    }
}
