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

在获取参数时这样写： Map<String, String> params = ctx.channel().attr(GlobalVar.PARAM_KEY).get();
如果是get请求，千万不能尝试获取post参数；
在获取username时这样写：String username =  ctx.channel().attr(GlobalVar.USERNAME).get();
vue的前端端口必须是8090
连接数据库的配置在globalvar