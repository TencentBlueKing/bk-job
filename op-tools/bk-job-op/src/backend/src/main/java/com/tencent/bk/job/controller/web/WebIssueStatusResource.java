/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.controller.web;

import com.tencent.bk.job.model.DeleteIssueStatusReq;
import com.tencent.bk.job.model.DeleteIssueStatusResp;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.service.model.PageData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OP Web页面的Issue状态管理接口。
 */
@RequestMapping("/web/issueStatus")
public interface WebIssueStatusResource {

    @GetMapping("/list")
    PageData<IssueStatusResp> listIssueStatus(
        @RequestParam(value = "idKeyword", required = false) String idKeyword,
        @RequestParam(value = "start", required = false, defaultValue = "0") Integer start,
        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize
    );

    @PostMapping("/delete")
    DeleteIssueStatusResp deleteIssueStatus(@RequestBody DeleteIssueStatusReq req);
}
