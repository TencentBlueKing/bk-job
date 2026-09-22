package com.tencent.bk.job.controller;

import com.tencent.bk.job.model.ReleaseStatusResp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 环境发布状态管理接口
 */
public interface ReleaseStatusResource {

    /**
     * 查询指定环境的发布状态
     *
     * @param env 环境标识
     * @return 发布状态响应
     */
    @GetMapping("/releaseStatus")
    ReleaseStatusResp getReleaseStatus(
        @RequestParam(value = "env") String env
    );

    /**
     * 设置指定环境的发布状态
     *
     * @param env       环境标识
     * @param releasing 是否正在发布
     * @return 更新后的发布状态响应
     */
    @PostMapping("/releaseStatus")
    ReleaseStatusResp setReleaseStatus(
        @RequestParam(value = "env") String env,
        @RequestParam(value = "releasing") boolean releasing
    );
}
