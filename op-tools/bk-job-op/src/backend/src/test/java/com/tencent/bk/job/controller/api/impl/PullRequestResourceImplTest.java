package com.tencent.bk.job.controller.api.impl;

import com.tencent.bk.job.model.CreatePullRequestReq;
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.model.UpdatePullRequestStatusReq;
import com.tencent.bk.job.service.PullRequestService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PullRequestResourceImplTest {

    private final PullRequestService service = mock(PullRequestService.class);
    private final PullRequestResourceImpl resource = new PullRequestResourceImpl(service);

    @Test
    void shouldDelegateAllOperations() {
        CreatePullRequestReq createReq = new CreatePullRequestReq();
        UpdatePullRequestStatusReq updateReq = new UpdatePullRequestStatusReq();
        PullRequestResp expected = new PullRequestResp();
        expected.setPrId(4564L);
        when(service.create(createReq)).thenReturn(expected);
        when(service.getByPrId(4564L)).thenReturn(expected);
        when(service.updateStatus(updateReq)).thenReturn(expected);

        assertThat(resource.create(createReq)).isSameAs(expected);
        assertThat(resource.getByPrId(4564L)).isSameAs(expected);
        assertThat(resource.updateStatus(updateReq)).isSameAs(expected);
        verify(service).create(createReq);
        verify(service).getByPrId(4564L);
        verify(service).updateStatus(updateReq);
    }
}
