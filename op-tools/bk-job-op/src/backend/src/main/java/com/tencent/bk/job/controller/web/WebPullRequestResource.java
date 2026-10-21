/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.controller.web;

import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.service.model.PageData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/web/pullRequest")
public interface WebPullRequestResource {

    @GetMapping("/list")
    PageData<PullRequestResp> listPullRequests(
        @RequestParam(value = "start", required = false, defaultValue = "0") Integer start,
        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize
    );
}
