package com.example.collabedit.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    // 从配置文件读取密钥
    @Value("${jwt.secret:VGhpcyIsYXRoaW5ncyBmb3IgdGVzdGluZyBqd3Qgc2VjcmV0LCBub3QgZm9yIHVzZSBpbiByZWFsIHByaW1lbnRzIQ==}")
    private String secret;

    // 令牌过期时间（24小时）
    private static final long EXPIRATION_TIME = 86400000;
    // 生成令牌
    public String generateToken(String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)// 设置主题(唯一标识）为用户ID
                .setIssuedAt(new Date())//令牌生成时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))// 设置过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  
                .compact();
    }

    //设置声明信息
      public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())     
                .build()
                .parseClaimsJws(token)         
                .getBody();                    
    }
    
    // 从令牌中提取用户名
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 从令牌中提取角色
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return (String) claims.get("role");
    }

    // 提取过期时间
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 提取声明
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 检查令牌是否过期
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 验证令牌
    public Boolean validateToken(String token, String userId) {
        final String extractedUserId = extractUserId(token);
        return (extractedUserId.equals(userId) && !isTokenExpired(token));
    }

    // 获取签名密钥
    private Key getSigningKey() {
        byte[] decodedKey = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(decodedKey);
    }
}