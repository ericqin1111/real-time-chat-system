package server.handler.utils;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import com.auth0.jwt.interfaces.DecodedJWT;


import io.jsonwebtoken.Claims;

import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class JwtUtil {

//
//    @Value("${secretKey}")
//    private  String key;

    private static String secretKey="aABfwdeUympsYdY4h6tHzpZNzvXa6zmpVdQzi2hEtwqXDVAZAgvcb5FaR4gAYfpHepYKyYMy";
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private static final Long expiration = 604800L;

    //    @PostConstruct
//    public void init(){
//        secretKey = key;
//    }
    public static String createToken(String username) {
        // 检查用户名是否为空，避免创建无效的token
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("账号不能为空");
        }

        // 设置JWT的过期时间为当前时间加上1周（1周的毫秒数）
        long twoWeeksInMilliseconds = 7 * 24 * 60 * 60 * 1000L;

        Date now = new Date();
        //创建了一个Date对象，表示从当前时间起加上一周后的日期和时间，这个日期和时间将被用作JWT的过期时间
        Date expireDate = new Date(now.getTime() + twoWeeksInMilliseconds);

        // 创建一个Map来存储JWT头部信息
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("alg", "HS256"); // 指定签名算法为HMAC256
        headerMap.put("typ", "JWT");   // 指定令牌类型为JWT

        // 使用Auth0的JWT库创建JWT
        // 首先创建一个JWT.builder()来配置JWT的各个部分
        JWTCreator.Builder builder = JWT.create()
                .withHeader(headerMap) // 添加头部信息
                .withSubject(username)
                .withExpiresAt(expireDate) // 设置过期时间
                .withIssuedAt(now) // 设置签发时间
                .withIssuer("L"); // 可选：设置发行者


        // 检查secretKey是否已加载，避免签名时出现空指针异常
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("密钥未加载或为空");
        }

        // 使用密钥进行签名，并构建最终的JWT字符串
        String token = builder.sign(Algorithm.HMAC256(secretKey));

        // 记录生成的JWT，便于调试和监控
        logger.info("Token created for user {}: {}", username, token);
        return token;
    }


    public static boolean verifyToken(String token) {
        // 检查传入的token是否为空，如果为空直接返回false
        if (token == null || token.isEmpty()) {
            logger.error("令牌空值");
            return false;
        }

        try {
            // 使用Auth0的JWT库构建一个JWTVerifier实例，用于验证JWT
            // 这里假设secretKey是一个已经定义好的密钥，用于HMAC256算法
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secretKey)).build();

            // 使用验证器验证JWT，如果token无效或被篡改，会抛出JWTVerificationException异常
            DecodedJWT jwt = verifier.verify(token);

            // 检查JWT是否过期，如果过期则返回false
            if (jwt.getExpiresAt().before(new Date())) {
                // 如果JWT过期，记录警告日志并返回false
                logger.warn("令牌已过期");
                return false;
            }

            // 如果JWT有效且未过期，返回true
            return true;
        } catch (JWTVerificationException e) {
            // 如果JWT验证失败，记录错误日志并返回false
            // 这可能发生在token无效、被篡改或签名不匹配时
            logger.error("令牌验证错误: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            // 如果发生其他异常，记录错误日志并返回false
            // 这可能是由于内部错误或配置问题导致的
            logger.error("令牌验证过程中出现意外错误", e);
            return false;
        }

    }

    public static Claims parseJwt(String jwt) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));


        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    public static String getUsername(String token) {

        Claims claims = parseJwt(token);
        System.out.println(claims.getSubject());
        String username = claims.getSubject();

        return username;


    }


}
