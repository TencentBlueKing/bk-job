/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.controller.web.impl;

import com.tencent.bk.job.config.web.WebAuthInterceptor;
import com.tencent.bk.job.controller.web.WebIssueStatusResource;
import com.tencent.bk.job.dao.IssueStatusMapper;
import com.tencent.bk.job.model.DeleteIssueStatusReq;
import com.tencent.bk.job.model.DeleteIssueStatusResp;
import com.tencent.bk.job.model.IssueStatusDTO;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.service.model.PageData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class WebIssueStatusResourceImpl implements WebIssueStatusResource {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    private static final int MAX_KEYWORD_LENGTH = 128;

    private final IssueStatusMapper issueStatusMapper;

    public WebIssueStatusResourceImpl(IssueStatusMapper issueStatusMapper) {
        this.issueStatusMapper = issueStatusMapper;
    }

    @Override
    public PageData<IssueStatusResp> listIssueStatus(String idKeyword, Integer start, Integer pageSize) {
        int startIndex = (start == null || start < 0) ? 0 : start;
        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        String keyword = normalizeKeyword(idKeyword);

        int total = issueStatusMapper.countByKeyword(keyword);
        if (startIndex >= total) {
            return new PageData<>(total, startIndex, size, Collections.emptyList());
        }
        List<IssueStatusResp> records = issueStatusMapper.listByKeyword(keyword, startIndex, size).stream()
            .map(this::toResp)
            .collect(Collectors.toList());
        return new PageData<>(total, startIndex, size, records);
    }

    @Override
    public DeleteIssueStatusResp deleteIssueStatus(DeleteIssueStatusReq req) {
        String id = req == null ? null : StringUtils.trimToNull(req.getId());
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        int deleted = issueStatusMapper.deleteById(id);
        log.info("Delete issue status, username={}, id={}, deleted={}", getCurrentUsername(), id, deleted);
        return new DeleteIssueStatusResp(deleted);
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

    private IssueStatusResp toResp(IssueStatusDTO dto) {
        return new IssueStatusResp(dto.getId(), dto.getStatus(), dto.getStatusDescription(), dto.getUpdateTime());
    }

    private String normalizeKeyword(String idKeyword) {
        String keyword = StringUtils.trimToNull(idKeyword);
        if (keyword == null) {
            return null;
        }
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        return escapeLike(keyword);
    }

    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
