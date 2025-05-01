在jwtauthconfig里的是不需要经过jwt验证的api路径

在routerconfig里配置的是不同的api所需要的handler链

如果需要使用mapper，参照下面的方法：
        // 使用封装方法执行操作
        MyBatisConfig.execute(UserMapper.class, mapper -> {
            List<User> users = mapper.getAll();
            System.out.println("查询结果: " + users);
       });

在返回响应时，自定义handler这样写：
        ctx.writeAndFlush(Object);

在获取http参数时这样写： Map<String, String> params = ctx.channel().attr(GlobalVar.PARAM_KEY).get();

在获取websocket内容时这样写： Map<String, String> content = ctx.channel().attr(GlobalVar.DATA_CONTEXT).get();

如果是get请求，千万不能尝试获取post参数；

由于数据库操作全部基于userid,所以改用userid而非username
在获取userid时这样写: String userid =  ctx.channel().attr(GlobalVar.USERID).get();
但是userid在数据库存储时为int类型，所以你需要Integer.parseInt(userid)变化一次后再存储到数据库或查询

vue的前端端口必须是8090

连接数据库的配置在globalvar

如果post参数中有文件会自动储存到upload目录

如果访问要文件就用http://localhost:8080/file/文件名的路径，文件名太复杂？没事,因为通过api会返回给你文件名。

前端的传输数据必须带一个meta，具体格式是{'target': 传输的对象的id，为字符串， 'content': 文件名或者文字消息，为字符串}

线程池使用方法：
对于费时的业务逻辑，比如写入、更新数据库、复杂的查询和文件的传输，最好都使用线程池.只需要把原来的方法封装到Runnable对象中，
然后交给线程池处理
如果不清楚，可以参考GenericBusinessHandler这个模板

使用方法：
protected void channelRead0(ChannelHandlerContext ctx, T request) throws Exception {
    Runnable businessLogicTask = () -> { 业务逻辑 }  业务逻辑封装到任务内

    GlobalVar.businessExecutor.execute(timeConsumingTask);  把任务交给线程池

    ctx.channel().eventLoop().execute(() -> {   把响应调度交还给eventloop处理
        final String responseThreadName = Thread.currentThread().getName();
        System.out.println("[" + responseThreadName + "] 准备发送响应...");
        if (ctx.channel().isActive()) {
        // 发送响应，可以根据需要添加 Listener
            ctx.writeAndFlush(response)/*.addListener(ChannelFutureListener.CLOSE_ON_FAILURE)*/;
            System.out.println("[" + responseThreadName + "] 响应已发送。");
        } 
        else {
            System.out.println("[" + responseThreadName + "] Channel 不再活跃，响应未发送。");
        }
    });

}
