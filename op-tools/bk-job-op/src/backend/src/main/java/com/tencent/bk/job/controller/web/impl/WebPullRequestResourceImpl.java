/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.controller.web.impl;

import com.tencent.bk.job.config.web.WebAuthInterceptor;
import com.tencent.bk.job.controller.web.WebPullRequestResource;
import com.tencent.bk.job.model.DeletePullRequestReq;
import com.tencent.bk.job.model.DeletePullRequestResp;
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.service.PullRequestService;
import com.tencent.bk.job.service.model.PageData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RestController
public class WebPullRequestResourceImpl implements WebPullRequestResource {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    private final PullRequestService pullRequestService;

    public WebPullRequestResourceImpl(PullRequestService pullRequestService) {
        this.pullRequestService = pullRequestService;
    }

    @Override
    public PageData<PullRequestResp> listPullRequests(Integer start, Integer pageSize) {
        int startIndex = (start == null || start < 0) ? 0 : start;
        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return pullRequestService.list(startIndex, size);
    }

    @Override
    public DeletePullRequestResp deletePullRequest(DeletePullRequestReq req) {
        Long prId = req == null ? null : req.getPrId();
        int deleted = pullRequestService.delete(prId);
        log.info("Delete pull request, username={}, prId={}, deleted={}", getCurrentUsername(), prId, deleted);
        return new DeletePullRequestResp(deleted);
    }

    private String getCurrentUsername() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return "<unknown>";
        }
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        Object username = request.getAttribute(WebAuthInterceptor.ATTR_USERNAME);
        return StringUtils.defaultIfBlank(username == null ? null : username.toString(), "<unknown>");
    }
}
