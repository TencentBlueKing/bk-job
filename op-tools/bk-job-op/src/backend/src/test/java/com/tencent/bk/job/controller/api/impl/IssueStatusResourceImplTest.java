/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.controller.api.impl;

import com.tencent.bk.job.dao.IssueStatusMapper;
import com.tencent.bk.job.model.IssueStatusDTO;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IssueStatusResourceImplTest {

    private IssueStatusMapper issueStatusMapper;

    private IssueStatusResourceImpl resource;

    @BeforeEach
    void setUp() {
        issueStatusMapper = mock(IssueStatusMapper.class);
        resource = new IssueStatusResourceImpl(issueStatusMapper);
    }

    @Test
    @DisplayName("写入时由服务端生成updateTime")
    void shouldGenerateUpdateTimeOnWrite() {
        SetIssueStatusReq req = new SetIssueStatusReq("issue_4523", "issue_todo", "待处理");
        long before = System.currentTimeMillis();

        IssueStatusResp resp = resource.setIssueStatus(req);

        ArgumentCaptor<IssueStatusDTO> captor = ArgumentCaptor.forClass(IssueStatusDTO.class);
        verify(issueStatusMapper).upsert(captor.capture());
        assertThat(captor.getValue().getUpdateTime()).isBetween(before, System.currentTimeMillis());
        assertThat(resp.getUpdateTime()).isEqualTo(captor.getValue().getUpdateTime());
    }

    @Test
    @DisplayName("查询不存在的Issue状态时返回空状态对象")
    void shouldReturnEmptyRespWhenRecordAbsent() {
        when(issueStatusMapper.selectById("issue_404")).thenReturn(null);

        IssueStatusResp resp = resource.getIssueStatus("issue_404");

        assertThat(resp).isEqualTo(new IssueStatusResp("issue_404", null, null, null));
    }

    @Test
    @DisplayName("查询已有Issue状态时原样返回存储字段")
    void shouldReturnStoredFields() {
        when(issueStatusMapper.selectById("issue_4523"))
            .thenReturn(new IssueStatusDTO("issue_4523", "issue_todo", "待处理", 1758542400000L));

        IssueStatusResp resp = resource.getIssueStatus("issue_4523");

        assertThat(resp).isEqualTo(new IssueStatusResp("issue_4523", "issue_todo", "待处理", 1758542400000L));
    }
}
