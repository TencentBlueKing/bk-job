package com.tencent.bk.job.controller;

import com.tencent.bk.job.consts.WebPaths;
import com.tencent.bk.job.model.WebUserInfoResp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * OP Web页面用户信息接口
 */
public interface WebUserResource {

    /**
     * 查询当前登录用户信息，供OP Web页面展示
     *
     * @param request 当前请求，用户名由Web登录拦截器写入其attribute
     * @return 当前登录用户信息
     */
    @GetMapping(WebPaths.WEB_USER_INFO)
    WebUserInfoResp getCurrentUserInfo(HttpServletRequest request);
}
