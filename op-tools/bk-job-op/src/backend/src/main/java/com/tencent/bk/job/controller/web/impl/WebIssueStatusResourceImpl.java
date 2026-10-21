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
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.model.WebIssueStatusResp;
import com.tencent.bk.job.service.PullRequestService;
import com.tencent.bk.job.service.model.PageData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class WebIssueStatusResourceImpl implements WebIssueStatusResource {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    private static final int MAX_KEYWORD_LENGTH = 128;

    private static final Pattern ISSUE_ID_PATTERN = Pattern.compile("^issue_(\\d+)$");

    private final IssueStatusMapper issueStatusMapper;

    private final PullRequestService pullRequestService;

    public WebIssueStatusResourceImpl(IssueStatusMapper issueStatusMapper, PullRequestService pullRequestService) {
        this.issueStatusMapper = issueStatusMapper;
        this.pullRequestService = pullRequestService;
    }

    @Override
    public PageData<WebIssueStatusResp> listIssueStatus(String idKeyword, Integer start, Integer pageSize) {
        int startIndex = (start == null || start < 0) ? 0 : start;
        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        String keyword = normalizeKeyword(idKeyword);

        int total = issueStatusMapper.countByKeyword(keyword);
        if (startIndex >= total) {
            return new PageData<>(total, startIndex, size, Collections.emptyList());
        }
        List<IssueStatusDTO> rows = issueStatusMapper.listByKeyword(keyword, startIndex, size);
        Set<Long> issueIds = rows.stream()
            .map(IssueStatusDTO::getId)
            .map(this::parseIssueId)
            .filter(id -> id != null)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, PullRequestResp> latestPullRequests = pullRequestService.findLatestByIssueIds(issueIds);
        List<WebIssueStatusResp> records = rows.stream()
            .map(dto -> toResp(dto, latestPullRequests))
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

    private WebIssueStatusResp toResp(IssueStatusDTO dto, Map<Long, PullRequestResp> latestPullRequests) {
        Long issueId = parseIssueId(dto.getId());
        PullRequestResp pullRequest = issueId == null ? null : latestPullRequests.get(issueId);
        return new WebIssueStatusResp(
            dto.getId(),
            dto.getStatus(),
            dto.getStatusDescription(),
            dto.getUpdateTime(),
            pullRequest == null ? null : pullRequest.getStatus(),
            pullRequest == null ? null : pullRequest.getPrId()
        );
    }

    private Long parseIssueId(String id) {
        if (id == null) {
            return null;
        }
        Matcher matcher = ISSUE_ID_PATTERN.matcher(id);
        if (!matcher.matches()) {
            return null;
        }
        try {
            long issueId = Long.parseLong(matcher.group(1));
            return issueId > 0 ? issueId : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
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
