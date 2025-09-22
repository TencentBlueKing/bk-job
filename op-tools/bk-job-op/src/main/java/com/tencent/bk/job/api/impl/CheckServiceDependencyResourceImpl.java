package com.tencent.bk.job.api.impl;

import com.tencent.bk.job.api.CheckServiceDependencyResource;
import com.tencent.bk.job.model.CheckStatusResp;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckServiceDependencyResourceImpl implements CheckServiceDependencyResource {

    private final RedisTemplate<String, String> redisTemplate;

    public CheckServiceDependencyResourceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CheckStatusResp checkServiceDependency(String namespace, String serviceName) {
        System.out.println("namespace=" + namespace + ", serviceName=" + serviceName);
        String redisKey = namespace + ":" + serviceName;
        String value = redisTemplate.opsForValue().get(redisKey);
        boolean isReady = StringUtils.isNotBlank(value) && Boolean.parseBoolean(value);
        return new CheckStatusResp(isReady);
    }
}
