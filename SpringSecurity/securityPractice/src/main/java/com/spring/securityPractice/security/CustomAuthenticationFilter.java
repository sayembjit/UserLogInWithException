package com.spring.securityPractice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.securityPractice.SpringApplicationContext;
import com.spring.securityPractice.constants.AppConstants;
import com.spring.securityPractice.model.UserDto;
import com.spring.securityPractice.model.UserLoginRequestModel;
import com.spring.securityPractice.service.UserService;
import com.spring.securityPractice.utils.JWTUtils;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            UserLoginRequestModel creds = new ObjectMapper().readValue(request.getInputStream(), UserLoginRequestModel.class);
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(creds.getEmail(),creds.getPassword())
            );
        } catch (IOException e) {
            log.info("Exception occured at attemptAuthentication method: {}",e.getLocalizedMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        String user = ((User)authResult.getPrincipal()).getUsername();
        String accessToken = JWTUtils.generateToken(user);
        UserService userService = (UserService) SpringApplicationContext.getBean("userServiceImpl");
        UserDto userDto = userService.getUser(user);
        response.addHeader("userId",userDto.getUserId());
        response.addHeader(AppConstants.HEADER_STRING,AppConstants.TOKEN_PREFIX+accessToken);
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("userId", userDto.getUserId());
        responseBody.put("accessToken", AppConstants.TOKEN_PREFIX + accessToken);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(responseBody));
        response.getWriter().flush();

    }
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String errorMessage = "Authentication failed: " + failed.getMessage();
        response.getWriter().write(errorMessage);
        response.getWriter().flush();
    }
}
