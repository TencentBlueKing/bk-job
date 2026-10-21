package com.tencent.bk.job.controller.web;

import com.tencent.bk.job.model.WebUserInfoResp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * OP Web页面用户信息接口
 */
@RequestMapping("/web")
public interface WebUserResource {

    /**
     * 查询当前登录用户信息，供OP Web页面展示
     *
     * @param request 当前请求，用户名由Web登录拦截器写入其attribute
     * @return 当前登录用户信息
     */
    @GetMapping("/userInfo")
    WebUserInfoResp getCurrentUserInfo(HttpServletRequest request);
}
