package com.tencent.bk.job.controller.web.impl;

import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.service.PullRequestService;
import com.tencent.bk.job.service.model.PageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebPullRequestResourceImplTest {

    private PullRequestService service;
    private WebPullRequestResourceImpl resource;

    @BeforeEach
    void setUp() {
        service = mock(PullRequestService.class);
        resource = new WebPullRequestResourceImpl(service);
    }

    @Test
    void shouldUseDefaultAndCorrectIllegalPageParams() {
        PageData<PullRequestResp> page = new PageData<>(0, 0, 20, Collections.emptyList());
        when(service.list(0, 20)).thenReturn(page);

        assertThat(resource.listPullRequests(null, null)).isSameAs(page);
        assertThat(resource.listPullRequests(-1, 0)).isSameAs(page);
        verify(service, org.mockito.Mockito.times(2)).list(0, 20);
    }

    @Test
    void shouldLimitPageSizeAndKeepValidParams() {
        PageData<PullRequestResp> page = new PageData<>(0, 10, 200, Collections.emptyList());
        when(service.list(10, 200)).thenReturn(page);

        assertThat(resource.listPullRequests(10, 1000)).isSameAs(page);
        verify(service).list(10, 200);
    }
}
