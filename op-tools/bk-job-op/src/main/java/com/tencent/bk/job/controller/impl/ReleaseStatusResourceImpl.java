package com.tencent.bk.job.controller.impl;

import com.tencent.bk.job.controller.ReleaseStatusResource;
import com.tencent.bk.job.model.ReleaseStatusResp;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class ReleaseStatusResourceImpl implements ReleaseStatusResource {

    private static final String KEY_PREFIX = "job:release:status:";

    private final RedisTemplate<String, String> redisTemplate;

    public ReleaseStatusResourceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ReleaseStatusResp getReleaseStatus(String env) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + env);
        boolean releasing = Boolean.parseBoolean(value);
        return new ReleaseStatusResp(releasing);
    }

    @Override
    public ReleaseStatusResp setReleaseStatus(String env, boolean releasing) {
        redisTemplate.opsForValue().set(KEY_PREFIX + env, String.valueOf(releasing), 1, TimeUnit.DAYS);
        return new ReleaseStatusResp(releasing);
    }
}
