package config;


import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import server.GlobalVar;
// 正确！
import com.baomidou.mybatisplus.core.MybatisConfiguration;


import java.util.function.Consumer;

// 配置类
public class MyBatisConfig {

    private static SqlSessionFactory sqlSessionFactory;

    // 初始化数据库配置（单例模式）
    public static void init() {
        try {
            // 1. 配置 HikariCP 数据源
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(GlobalVar.JDBC);
            hikariConfig.setUsername(GlobalVar.USER);
            hikariConfig.setPassword(GlobalVar.PASS);
            hikariConfig.setMaximumPoolSize(20);
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);

            // 2. 创建事务工厂和环境
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("development", transactionFactory, dataSource);

            // 3. 创建 MyBatis 配置并绑定环境
            MybatisConfiguration mybatisConfig = new MybatisConfiguration(environment);
            mybatisConfig.setMapUnderscoreToCamelCase(true); // 启用驼峰命名映射
            mybatisConfig.addMappers("mapper"); // 确保包路径正确（如 mapper 是包名）

            // 4. 构建 SqlSessionFactory
            sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(mybatisConfig);
        } catch (Exception e) {
            throw new RuntimeException("MyBatis 初始化失败", e);
        }
    }

    public static <T> void execute(Class<T> mapperClass, Consumer<T> action) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            T mapper = session.getMapper(mapperClass);
            action.accept(mapper);
            session.commit(); // 可选：提交事务
        }
    }

    public static void execute(Consumer<SqlSession> action) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            action.accept(session);
            session.commit();
        }
    }
}