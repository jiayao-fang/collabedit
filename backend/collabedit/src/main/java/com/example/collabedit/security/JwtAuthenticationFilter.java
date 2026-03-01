package com.example.collabedit.security;

import com.example.collabedit.modules.system.entity.Permission;
import com.example.collabedit.modules.system.service.PermissionService;
import com.example.collabedit.modules.user.service.UserStatusService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import org.springframework.lang.NonNull;
 
import java.io.IOException;
import java.util.ArrayList; 
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private UserStatusService userStatusService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        // 从请求头中获取 token
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // 移除 "Bearer " 前缀
            
            try {
                // 提取用户ID和角色
                String userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);
                
                // 验证 token 是否有效
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 检查 token 是否过期
                    if (!jwtUtil.extractExpiration(token).before(new java.util.Date())) {
                        // 检查用户状态是否有效
                        if (!userStatusService.isUserActive(Long.valueOf(userId))) {
                            logger.warn("当前用户已被禁用，拒绝访问");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"账号已被禁用，请联系管理员\"}");
                            return; // 终止请求
                        }
                        
                        // 获取用户权限
                        List<Permission> permissions = permissionService.getPermissionsByUserId(Long.valueOf(userId));
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                        // 添加角色
                        authorities.add(new SimpleGrantedAuthority("ROLE_"+role));

                         // 添加权限
                        if (permissions != null && !permissions.isEmpty()) {
                            for (Permission permission : permissions) {
                                authorities.add(new SimpleGrantedAuthority(permission.getPermKey()));
                            }
                        }

                        // 创建认证对象
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userId, 
                                null, 
                                authorities
                            );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // 设置到 Spring Security 上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // Token 无效或过期，继续过滤器链（让 Spring Security 处理未认证请求）
                logger.debug("JWT token validation failed: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}



