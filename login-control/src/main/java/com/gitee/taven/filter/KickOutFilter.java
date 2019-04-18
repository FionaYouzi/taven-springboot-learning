package com.gitee.taven.filter;

import com.gitee.taven.pojo.UserDTO;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class KickOutFilter implements Filter {

    public final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public RedissonClient redissonClient;

    public static final String PREFIX = "uni_token_";

    public static final String PREFIX_LOCK = "uni_token_lock_";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        if (checkToken(request, response) && isAccessAllowed(request, response)) {
            filterChain.doFilter(req, resp);

        } else {
            return;

        }

    }

    /**
     * 当前用户是否允许访问
     *
     * @param request
     * @param response
     * @return
     */
    public abstract boolean isAccessAllowed(HttpServletRequest request, HttpServletResponse response);

    /**
     * 检查是否携带token 以及token是否失效
     *
     * @param request
     * @param response
     * @return
     */
    public boolean checkToken(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            sendResponse(response, 401, "你无权访问");
            return false;
        }

        // 校验token是否存在
        RBucket<UserDTO> rBucket = redissonClient.getBucket(token);

        if (rBucket.get() == null) {
            sendResponse(response, 403, "令牌过期");
            return false;
        }

        return true;
    }


    /**
     * 发送json响应
     *
     * @param resp
     * @param code
     * @param message
     */
    public static void sendResponse(HttpServletResponse resp, int code, String message) {
        responseOutWithJson(resp, String.format(jsonTemplate(), code, message));
    }

    public static String jsonTemplate() {
        return "{\"code\":%s,\"msg\":\"%s\",\"data\":null,\"errors\":null}";
    }

    public static void responseOutWithJson(HttpServletResponse response,
                                              String json) {
        //将实体对象转换为JSON Object转换
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            out.append(json);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

}
