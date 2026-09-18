package com.tencent.bk.job.controller.impl;

import com.tencent.bk.job.controller.IssueStatusResource;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import com.tencent.bk.job.utils.json.JsonUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class IssueStatusResourceImpl implements IssueStatusResource {

    private static final String KEY_PREFIX = "job:issue:status:";

    /**
     * Issue状态数据在Redis中的过期时间，避免数据无限堆积
     */
    private static final Duration EXPIRE_DURATION = Duration.ofDays(30);

    private final RedisTemplate<String, String> redisTemplate;

    public IssueStatusResourceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public IssueStatusResp getIssueStatus(String id) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + id);
        if (value == null) {
            return new IssueStatusResp(id, null, null);
        }
        IssueStatusResp resp = JsonUtils.fromJson(value, IssueStatusResp.class);
        resp.setId(id);
        return resp;
    }

    @Override
    public IssueStatusResp setIssueStatus(SetIssueStatusReq req) {
        IssueStatusResp resp = new IssueStatusResp(req.getId(), req.getStatus(), req.getStatusDescription());
        redisTemplate.opsForValue().set(KEY_PREFIX + req.getId(), JsonUtils.toJson(resp), EXPIRE_DURATION);
        return resp;
    }
}
