package com.tencent.bk.job.controller.web.impl;

import com.tencent.bk.job.config.web.WebAuthInterceptor;
import com.tencent.bk.job.controller.web.WebUserResource;
import com.tencent.bk.job.exception.OpApiException;
import com.tencent.bk.job.model.WebUserInfoResp;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class WebUserResourceImpl implements WebUserResource {

    @Override
    public WebUserInfoResp getCurrentUserInfo(HttpServletRequest request) {
        // 能进入本方法说明已通过登录态+白名单校验，用户名必然已由拦截器写入
        Object username = request.getAttribute(WebAuthInterceptor.ATTR_USERNAME);
        if (username == null || StringUtils.isBlank(username.toString())) {
            log.error("Username attribute is absent, {} is probably missing from the web auth interceptor patterns",
                request.getRequestURI());
            throw new OpApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Current username is not available");
        }
        return new WebUserInfoResp(username.toString());
    }
}
