package cn.akira.interceptor;

import cn.akira.pojo.User;
import cn.akira.service.UserService;
import cn.akira.util.RSAUtil;
import cn.akira.util.ServletUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 会话拦截器
 */
public class SessionInterceptor implements HandlerInterceptor {
    private static final Logger LOGGER = Logger.getLogger(SessionInterceptor.class);

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            if (userService == null) {
                WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
                userService = wac.getBean(UserService.class);
            }
            Object userSession = request.getSession().getAttribute("SESSION_USER");
            if (userSession == null) {
                LOGGER.warn("[" + request.getRequestURI() + "] 需要登录有相应权限的用户才能进行");
                ServletUtil.redirectOutOfIframe(request.getContextPath() + "/user/login", response);
                return false;
            }
            if (userSession instanceof User) {
                User user = (User) userSession;
                String rsaEncryptedPassword = user.getPassword();
                String sha1HexedPassword = RSAUtil.decrypt(rsaEncryptedPassword); //将用户session中的密码解密为sha1校验码
                user.setPassword(sha1HexedPassword);
                //这里去数据库查询并核实用户信息
                if (userService.getUser(user) == null) {
                    request.getSession().removeAttribute("SESSION_USER");
                    response.sendRedirect(request.getContextPath() + "/user/login");
                    ServletUtil.redirectOutOfIframe(request.getContextPath() + "/user/login", response);
                    LOGGER.warn("[" + request.getRequestURI() + "] 请求需要登录有相应权限的用户才能继续");
                    return false;
                }
                user.setPassword(rsaEncryptedPassword);
                request.getSession().setAttribute("SESSION_USER", user);
                LOGGER.info("会话用户名:\"" + user.getUname() + "\"");
                return true;
            } else {
                LOGGER.error("请先登录");
                ServletUtil.redirectOutOfIframe("/login.jsp", response);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}