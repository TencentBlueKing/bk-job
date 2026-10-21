/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.controller.web.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tencent.bk.job.config.web.WebAuthInterceptor;
import com.tencent.bk.job.dao.IssueStatusMapper;
import com.tencent.bk.job.model.DeleteIssueStatusReq;
import com.tencent.bk.job.model.DeleteIssueStatusResp;
import com.tencent.bk.job.model.IssueStatusDTO;
import com.tencent.bk.job.model.WebIssueStatusResp;
import com.tencent.bk.job.service.PullRequestService;
import com.tencent.bk.job.service.model.PageData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebIssueStatusResourceImplTest {

    private IssueStatusMapper issueStatusMapper;

    private PullRequestService pullRequestService;

    private WebIssueStatusResourceImpl resource;

    private Logger logger;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        issueStatusMapper = mock(IssueStatusMapper.class);
        pullRequestService = mock(PullRequestService.class);
        resource = new WebIssueStatusResourceImpl(issueStatusMapper, pullRequestService);
        logger = (Logger) LoggerFactory.getLogger(WebIssueStatusResourceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    private IssueStatusDTO dto(String id) {
        return new IssueStatusDTO(id, "issue_todo", "待处理", 1758542400000L);
    }

    @Test
    @DisplayName("列表透传SQL顺序并返回精确总数")
    void shouldReturnExactTotalAndKeepSqlOrder() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(4);
        when(issueStatusMapper.listByKeyword(isNull(), eq(0), eq(2)))
            .thenReturn(List.of(dto("issue_4523"), dto("issue_100")));

        PageData<WebIssueStatusResp> pageData = resource.listIssueStatus(null, 0, 2);

        assertThat(pageData.getTotal()).isEqualTo(4);
        assertThat(pageData.getData()).extracting(WebIssueStatusResp::getId)
            .containsExactly("issue_4523", "issue_100");
    }

    @Test
    @DisplayName("关键字去除两端空白后作为LIKE参数下推到SQL")
    void shouldPushDownTrimmedKeyword() {
        when(issueStatusMapper.countByKeyword(any())).thenReturn(0);

        resource.listIssueStatus("  ISSUE_45  ", 0, 20);

        // 下划线是LIKE的单字符通配符，转义后才是字面量；大小写不敏感由表的 utf8mb4_unicode_ci 排序规则保证
        verify(issueStatusMapper).countByKeyword("ISSUE\\_45");
    }

    @Test
    @DisplayName("关键字中的LIKE元字符被转义，退化为纯contains匹配")
    void shouldEscapeLikeMetaCharactersInKeyword() {
        when(issueStatusMapper.countByKeyword(any())).thenReturn(0);

        resource.listIssueStatus("a%b_c\\d", 0, 20);

        verify(issueStatusMapper).countByKeyword("a\\%b\\_c\\\\d");
    }

    @Test
    @DisplayName("空白关键字视为不过滤")
    void shouldTreatBlankKeywordAsNoFilter() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(0);

        resource.listIssueStatus("   ", 0, 20);

        verify(issueStatusMapper).countByKeyword(isNull());
    }

    @Test
    @DisplayName("非法分页参数被静默纠正")
    void shouldCorrectIllegalPageParams() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(0);

        assertThat(resource.listIssueStatus(null, -5, 0).getStart()).isZero();
        assertThat(resource.listIssueStatus(null, null, null).getSize()).isEqualTo(20);
        assertThat(resource.listIssueStatus(null, 0, 10000).getSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("起始位置越界时返回空列表且不查询数据")
    void shouldReturnEmptyDataWhenStartExceedsTotal() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(2);

        PageData<WebIssueStatusResp> pageData = resource.listIssueStatus(null, 100, 20);

        assertThat(pageData.getTotal()).isEqualTo(2);
        assertThat(pageData.getData()).isEmpty();
        verify(issueStatusMapper, never()).listByKeyword(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("列表一次批量查询去重后的合法Issue ID并聚合PR")
    void shouldBatchLatestPullRequestsForValidIssueIds() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(5);
        when(issueStatusMapper.listByKeyword(isNull(), eq(0), eq(20))).thenReturn(List.of(
            dto("issue_4555"),
            dto("issue_4555"),
            dto("invalid"),
            dto("issue_9223372036854775808"),
            dto(null)
        ));
        when(pullRequestService.findLatestPrIdsByIssueIds(Set.of(4555L)))
            .thenReturn(Map.of(4555L, 4564L));

        PageData<WebIssueStatusResp> result = resource.listIssueStatus(null, 0, 20);

        assertThat(result.getData()).extracting(WebIssueStatusResp::getPrId)
            .containsExactly(4564L, 4564L, null, null, null);
        verify(pullRequestService).findLatestPrIdsByIssueIds(Set.of(4555L));
    }

    @Test
    @DisplayName("无PR时列表prId留空")
    void shouldLeavePrIdNullWhenNoPullRequestExists() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(1);
        when(issueStatusMapper.listByKeyword(isNull(), eq(0), eq(20)))
            .thenReturn(List.of(dto("issue_4555")));
        when(pullRequestService.findLatestPrIdsByIssueIds(Set.of(4555L))).thenReturn(Map.of());

        assertThat(resource.listIssueStatus(null, 0, 20).getData().get(0).getPrId()).isNull();
    }

    @Test
    @DisplayName("删除Issue状态时去除id两端空白并返回影响行数")
    void shouldDeleteIssueStatusAndReturnAffectedRows() {
        when(issueStatusMapper.deleteById("issue_4523")).thenReturn(1);

        DeleteIssueStatusResp resp = resource.deleteIssueStatus(new DeleteIssueStatusReq(" issue_4523 "));

        assertThat(resp.getDeleted()).isOne();
        verify(issueStatusMapper).deleteById("issue_4523");
    }

    @Test
    @DisplayName("删除不存在的Issue状态保持幂等")
    void shouldReturnZeroWhenDeletingAbsentIssueStatus() {
        when(issueStatusMapper.deleteById("issue_404")).thenReturn(0);

        assertThat(resource.deleteIssueStatus(new DeleteIssueStatusReq("issue_404")).getDeleted()).isZero();
    }

    @Test
    @DisplayName("删除审计日志记录操作人、id和影响行数")
    void shouldAuditIssueStatusDeletion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(WebAuthInterceptor.ATTR_USERNAME, "admin");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(issueStatusMapper.deleteById("issue_4523")).thenReturn(1);

        resource.deleteIssueStatus(new DeleteIssueStatusReq("issue_4523"));

        assertThat(listAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .contains("Delete issue status, username=admin, id=issue_4523, deleted=1");
    }

    @Test
    @DisplayName("取不到用户名时审计日志降级为unknown")
    void shouldAuditDeletionWithUnknownUsername() {
        when(issueStatusMapper.deleteById("issue_404")).thenReturn(0);

        resource.deleteIssueStatus(new DeleteIssueStatusReq("issue_404"));

        assertThat(listAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .contains("Delete issue status, username=<unknown>, id=issue_404, deleted=0");
    }

    @Test
    @DisplayName("删除时id为空白返回明确参数错误")
    void shouldRejectBlankIdWhenDeletingIssueStatus() {
        assertThatThrownBy(() -> resource.deleteIssueStatus(new DeleteIssueStatusReq("  ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("id不能为空");
        verify(issueStatusMapper, never()).deleteById(any());
    }
}
