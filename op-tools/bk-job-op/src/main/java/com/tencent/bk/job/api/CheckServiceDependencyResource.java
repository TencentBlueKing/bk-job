package com.tencent.bk.job.api;

import com.tencent.bk.job.model.CheckStatusResp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 检查服务外部依赖相关接口
 */
public interface CheckServiceDependencyResource {

    /**
     * 检查服务外部依赖是否就绪
     *
     * @param namespace   服务所在命名空间
     * @param serviceName 服务名称
     * @return 外部依赖状态响应
     */
    @GetMapping({"/checkServiceDependency"})
    CheckStatusResp checkServiceDependency(
        @RequestParam(value = "namespace", required = false)
        String namespace,
        @RequestParam(value = "serviceName", required = false)
        String serviceName
    );

}
