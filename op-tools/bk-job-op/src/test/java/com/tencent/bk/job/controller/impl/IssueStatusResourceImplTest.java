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

import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import com.tencent.bk.job.service.model.PageData;
import com.tencent.bk.job.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Issue状态列表查询。
 *
 * <p>重点覆盖：只用SCAN不用KEYS、游标被关闭、排序与分页稳定、超过扫描上限时total退化为截断近似值。</p>
 */
@SuppressWarnings("unchecked")
class IssueStatusResourceImplTest {

    private static final String KEY_PREFIX = "job:issue:status:";

    private RedisTemplate<String, String> redisTemplate;

    private ValueOperations<String, String> valueOperations;

    private IssueStatusResourceImpl resource;

    private final List<Cursor<String>> createdCursors = new ArrayList<>();

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        resource = new IssueStatusResourceImpl(redisTemplate);
    }

    /**
     * 让mock的RedisTemplate按给定的key集合返回一个可遍历的游标
     */
    private void givenIssueKeys(List<String> ids) {
        when(redisTemplate.scan(any(ScanOptions.class))).thenAnswer(invocation -> {
            Iterator<String> iterator = ids.stream().map(id -> KEY_PREFIX + id).iterator();
            Cursor<String> cursor = mock(Cursor.class);
            when(cursor.hasNext()).thenAnswer(i -> iterator.hasNext());
            when(cursor.next()).thenAnswer(i -> iterator.next());
            createdCursors.add(cursor);
            return cursor;
        });
    }

    private void givenValues(List<String> ids) {
        when(valueOperations.multiGet(anyCollection())).thenAnswer(invocation -> {
            Collection<String> keys = invocation.getArgument(0);
            List<String> values = new ArrayList<>(keys.size());
            for (String key : keys) {
                String id = key.substring(KEY_PREFIX.length());
                values.add(ids.contains(id)
                    ? JsonUtils.toJson(new IssueStatusResp(id, "issue_todo", "待处理", 1758542400000L))
                    : null);
            }
            return values;
        });
    }

    @Test
    @DisplayName("列表按Issue编号降序排列并正确分页")
    void shouldSortByIssueNoDescAndPaginate() {
        List<String> ids = List.of("issue_10", "issue_2", "issue_4523", "issue_100");
        givenIssueKeys(ids);
        givenValues(ids);

        PageData<IssueStatusResp> firstPage = resource.listIssueStatus(null, 0, 2);

        assertThat(firstPage.getTotal()).isEqualTo(4);
        assertThat(firstPage.getStart()).isEqualTo(0);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getData()).extracting(IssueStatusResp::getId)
            .containsExactly("issue_4523", "issue_100");

        PageData<IssueStatusResp> secondPage = resource.listIssueStatus(null, 2, 2);
        assertThat(secondPage.getData()).extracting(IssueStatusResp::getId)
            .containsExactly("issue_10", "issue_2");
    }

    @Test
    @DisplayName("关键字做大小写不敏感的模糊匹配，total为过滤后的条数")
    void shouldFilterByKeywordIgnoringCase() {
        List<String> ids = List.of("issue_4523", "issue_4517", "ISSUE_99", "other_1");
        givenIssueKeys(ids);
        givenValues(ids);

        PageData<IssueStatusResp> pageData = resource.listIssueStatus("  ISSUE_45  ", 0, 20);

        assertThat(pageData.getTotal()).isEqualTo(2);
        assertThat(pageData.getData()).extracting(IssueStatusResp::getId)
            .containsExactly("issue_4523", "issue_4517");
    }

    @Test
    @DisplayName("非法分页参数被静默纠正，不抛异常")
    void shouldCorrectIllegalPageParams() {
        List<String> ids = List.of("issue_1", "issue_2");
        givenIssueKeys(ids);
        givenValues(ids);

        assertThat(resource.listIssueStatus(null, -5, 0).getStart()).isZero();
        assertThat(resource.listIssueStatus(null, null, null).getSize()).isEqualTo(20);
        assertThat(resource.listIssueStatus(null, 0, 10000).getSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("起始位置越界时返回空列表但保留真实total")
    void shouldReturnEmptyDataWhenStartExceedsTotal() {
        List<String> ids = List.of("issue_1", "issue_2");
        givenIssueKeys(ids);
        givenValues(ids);

        PageData<IssueStatusResp> pageData = resource.listIssueStatus(null, 100, 20);

        assertThat(pageData.getTotal()).isEqualTo(2);
        assertThat(pageData.getData()).isEmpty();
    }

    @Test
    @DisplayName("扫描与取值之间key过期时降级为空状态记录，整个列表不失败")
    void shouldDegradeToEmptyRecordWhenKeyExpiredBetweenScanAndGet() {
        List<String> ids = List.of("issue_1", "issue_2");
        givenIssueKeys(ids);
        // 只有 issue_2 还在，issue_1 在SCAN之后过期了
        givenValues(List.of("issue_2"));

        PageData<IssueStatusResp> pageData = resource.listIssueStatus(null, 0, 20);

        assertThat(pageData.getTotal()).isEqualTo(2);
        IssueStatusResp expired = pageData.getData().get(1);
        assertThat(expired.getId()).isEqualTo("issue_1");
        assertThat(expired.getStatus()).isNull();
        assertThat(expired.getUpdateTime()).isNull();
    }

    @Test
    @DisplayName("只用SCAN扫描key，且游标一定被关闭")
    void shouldUseScanInsteadOfKeysAndCloseCursor() throws Exception {
        List<String> ids = List.of("issue_1");
        givenIssueKeys(ids);
        givenValues(ids);

        resource.listIssueStatus(null, 0, 20);

        verify(redisTemplate, never()).keys(anyString());
        assertThat(createdCursors).hasSize(1);
        verify(createdCursors.get(0)).close();
    }

    @Test
    @DisplayName("超过扫描上限时截断，total退化为截断后的近似值")
    void shouldTruncateWhenExceedingMaxScanKeys() {
        List<String> ids = new ArrayList<>(10050);
        for (int i = 1; i <= 10050; i++) {
            ids.add("issue_" + i);
        }
        givenIssueKeys(ids);
        givenValues(ids);

        PageData<IssueStatusResp> pageData = resource.listIssueStatus(null, 0, 1);

        // 截断上限 MAX_SCAN_KEYS = 10000，此时 total 是真实总数的下界而非精确值
        assertThat(pageData.getTotal()).isEqualTo(10000);
        assertThat(pageData.getData()).hasSize(1);
    }

    @Test
    @DisplayName("写入状态时由服务端生成updateTime")
    void shouldGenerateUpdateTimeOnWrite() {
        SetIssueStatusReq req = new SetIssueStatusReq();
        req.setId("issue_4523");
        req.setStatus("issue_todo");
        req.setStatusDescription("待处理");
        long before = System.currentTimeMillis();

        IssueStatusResp resp = resource.setIssueStatus(req);

        assertThat(resp.getUpdateTime()).isNotNull().isBetween(before, System.currentTimeMillis());
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("查询不存在的Issue状态时返回空状态对象，保持存量行为")
    void shouldReturnEmptyRespWhenKeyAbsent() {
        when(valueOperations.get(KEY_PREFIX + "issue_404")).thenReturn(null);

        IssueStatusResp resp = resource.getIssueStatus("issue_404");

        assertThat(resp.getId()).isEqualTo("issue_404");
        assertThat(resp.getStatus()).isNull();
        assertThat(resp.getStatusDescription()).isNull();
        assertThat(resp.getUpdateTime()).isNull();
    }
}
