package com.sztus.lib.back.end.basic.filter;

import com.alibaba.fastjson.JSON;
import com.sztus.lib.back.end.basic.config.RedisCache;
import com.sztus.lib.back.end.basic.object.domain.LoginUser;
import com.sztus.lib.back.end.basic.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * @author AustinWang
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    private RedisCache redisCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 获取token
        String token = request.getHeader("token");
        // token是空则放行
        if (!StringUtils.hasText(token)) {
            //放行
            filterChain.doFilter(request, response);
            return;
        }
        // 解析token
        String uuid;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            uuid = claims.getSubject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("token非法！！！");
        }
        // 从redis中获取用户信息
        String redisKey = "login_" + uuid;
        LoginUser loginUser = JSON.parseObject(redisCache.getCacheObject(redisKey), LoginUser.class);
        if (Objects.isNull(loginUser)) {
            throw new RuntimeException("用户未登录！！！");
        }
        // 获取权限信息封装到Authentication中
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        // 存入SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        // 放行
        filterChain.doFilter(request, response);
    }
}