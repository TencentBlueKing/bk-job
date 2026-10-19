/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.controller.impl;

import com.tencent.bk.job.dao.IssueStatusMapper;
import com.tencent.bk.job.model.DeleteIssueStatusReq;
import com.tencent.bk.job.model.DeleteIssueStatusResp;
import com.tencent.bk.job.model.IssueStatusDTO;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import com.tencent.bk.job.service.model.PageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

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

/**
 * Issue状态读写接口。分页、模糊过滤与排序都下沉到SQL，这里覆盖参数归一化、契约字段与DAO调用方式。
 */
class IssueStatusResourceImplTest {

    private IssueStatusMapper issueStatusMapper;

    private IssueStatusResourceImpl resource;

    @BeforeEach
    void setUp() {
        issueStatusMapper = mock(IssueStatusMapper.class);
        resource = new IssueStatusResourceImpl(issueStatusMapper);
    }

    private IssueStatusDTO dto(String id, Long issueNo) {
        return new IssueStatusDTO(id, issueNo, "issue_todo", "待处理", 1758542400000L);
    }

    @Test
    @DisplayName("列表直接透传SQL返回的顺序，total取COUNT(*)的精确值")
    void shouldReturnExactTotalAndKeepSqlOrder() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(4);
        when(issueStatusMapper.listByKeyword(isNull(), eq(0), eq(2)))
            .thenReturn(List.of(dto("issue_4523", 4523L), dto("issue_100", 100L)));

        PageData<IssueStatusResp> pageData = resource.listIssueStatus(null, 0, 2);

        assertThat(pageData.getTotal()).isEqualTo(4);
        assertThat(pageData.getStart()).isZero();
        assertThat(pageData.getSize()).isEqualTo(2);
        assertThat(pageData.getData()).extracting(IssueStatusResp::getId)
            .containsExactly("issue_4523", "issue_100");
        assertThat(pageData.getData().get(0).getUpdateTime()).isEqualTo(1758542400000L);
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
    @DisplayName("空白关键字视为不过滤，传null给SQL")
    void shouldTreatBlankKeywordAsNoFilter() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(0);

        resource.listIssueStatus("   ", 0, 20);

        verify(issueStatusMapper).countByKeyword(isNull());
    }

    @Test
    @DisplayName("非法分页参数被静默纠正，不抛异常")
    void shouldCorrectIllegalPageParams() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(0);

        assertThat(resource.listIssueStatus(null, -5, 0).getStart()).isZero();
        assertThat(resource.listIssueStatus(null, null, null).getSize()).isEqualTo(20);
        assertThat(resource.listIssueStatus(null, 0, 10000).getSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("起始位置越界时返回空列表且不再查数据，total仍是精确总数")
    void shouldReturnEmptyDataWhenStartExceedsTotal() {
        when(issueStatusMapper.countByKeyword(isNull())).thenReturn(2);

        PageData<IssueStatusResp> pageData = resource.listIssueStatus(null, 100, 20);

        assertThat(pageData.getTotal()).isEqualTo(2);
        assertThat(pageData.getData()).isEmpty();
        verify(issueStatusMapper, never()).listByKeyword(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("写入时由服务端生成updateTime，并解析出用于排序的Issue编号")
    void shouldGenerateUpdateTimeAndParseIssueNoOnWrite() {
        SetIssueStatusReq req = new SetIssueStatusReq("issue_4523", "issue_todo", "待处理");
        long before = System.currentTimeMillis();

        IssueStatusResp resp = resource.setIssueStatus(req);

        ArgumentCaptor<IssueStatusDTO> captor = ArgumentCaptor.forClass(IssueStatusDTO.class);
        verify(issueStatusMapper).upsert(captor.capture());
        assertThat(captor.getValue().getIssueNo()).isEqualTo(4523L);
        assertThat(captor.getValue().getUpdateTime()).isBetween(before, System.currentTimeMillis());
        assertThat(resp.getId()).isEqualTo("issue_4523");
        assertThat(resp.getStatus()).isEqualTo("issue_todo");
        assertThat(resp.getStatusDescription()).isEqualTo("待处理");
        assertThat(resp.getUpdateTime()).isEqualTo(captor.getValue().getUpdateTime());
    }

    @Test
    @DisplayName("id不符合issue_数字格式时issueNo为null，由SQL把它排到列表末尾")
    void shouldLeaveIssueNoNullForUnparsableId() {
        resource.setIssueStatus(new SetIssueStatusReq("other_1", "issue_todo", "待处理"));

        ArgumentCaptor<IssueStatusDTO> captor = ArgumentCaptor.forClass(IssueStatusDTO.class);
        verify(issueStatusMapper).upsert(captor.capture());
        assertThat(captor.getValue().getIssueNo()).isNull();
    }

    @Test
    @DisplayName("查询不存在的Issue状态时返回空状态对象，保持存量契约")
    void shouldReturnEmptyRespWhenRecordAbsent() {
        when(issueStatusMapper.selectById("issue_404")).thenReturn(null);

        IssueStatusResp resp = resource.getIssueStatus("issue_404");

        assertThat(resp.getId()).isEqualTo("issue_404");
        assertThat(resp.getStatus()).isNull();
        assertThat(resp.getStatusDescription()).isNull();
        assertThat(resp.getUpdateTime()).isNull();
    }

    @Test
    @DisplayName("查询已有Issue状态时原样返回存储的字段")
    void shouldReturnStoredFields() {
        when(issueStatusMapper.selectById("issue_4523")).thenReturn(dto("issue_4523", 4523L));

        IssueStatusResp resp = resource.getIssueStatus("issue_4523");

        assertThat(resp).isEqualTo(new IssueStatusResp("issue_4523", "issue_todo", "待处理", 1758542400000L));
    }

    @Test
    @DisplayName("删除Issue状态时去除id两端空白并返回实际删除行数")
    void shouldDeleteIssueStatusAndReturnAffectedRows() {
        when(issueStatusMapper.deleteById("issue_4523")).thenReturn(1);

        DeleteIssueStatusResp resp = resource.deleteIssueStatus(new DeleteIssueStatusReq(" issue_4523 "));

        assertThat(resp.getDeleted()).isOne();
        verify(issueStatusMapper).deleteById("issue_4523");
    }

    @Test
    @DisplayName("删除不存在的Issue状态保持幂等并返回0")
    void shouldReturnZeroWhenDeletingAbsentIssueStatus() {
        when(issueStatusMapper.deleteById("issue_404")).thenReturn(0);

        DeleteIssueStatusResp resp = resource.deleteIssueStatus(new DeleteIssueStatusReq("issue_404"));

        assertThat(resp.getDeleted()).isZero();
    }

    @Test
    @DisplayName("删除Issue状态时id为空白返回明确参数错误且不访问数据库")
    void shouldRejectBlankIdWhenDeletingIssueStatus() {
        assertThatThrownBy(() -> resource.deleteIssueStatus(new DeleteIssueStatusReq("  ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("id不能为空");

        verify(issueStatusMapper, never()).deleteById(any());
    }
}
