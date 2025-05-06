package server.handler.websocket;

import config.MyBatisConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import mapper.UserFriendMapper;
import server.GlobalVar;

import java.util.ArrayList;
import java.util.List;

public class OnlineHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        String userid =  ctx.channel().attr(GlobalVar.USERID).get();
        GlobalVar.businessExecutor.execute(()->{
            System.out.println("online handler:" + userid);

            List<String> onlineFriends = new ArrayList<>();
            MyBatisConfig.execute(UserFriendMapper.class, mapper->{
                System.out.println("online handler mapper ready");
                List<Integer> friends = mapper.findFriendIdsByUserIds(Integer.parseInt(userid));
                System.out.println("online handler mapper friends:" + friends);
                for(Integer friendId:friends){
                    if(GlobalVar.userChannelMap.containsKey(Integer.toString(friendId))){
                        onlineFriends.add(Integer.toString(friendId));
                    }
                }

            });
            ctx.channel().eventLoop().execute(() -> {
                ctx.writeAndFlush(onlineFriends);
            });
        });
    }
}
