package com.tencent.bk.job.controller.web.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tencent.bk.job.config.web.WebAuthInterceptor;
import com.tencent.bk.job.model.DeletePullRequestReq;
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.service.PullRequestService;
import com.tencent.bk.job.service.model.PageData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebPullRequestResourceImplTest {

    private PullRequestService service;
    private WebPullRequestResourceImpl resource;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        service = mock(PullRequestService.class);
        resource = new WebPullRequestResourceImpl(service);
        logger = (Logger) LoggerFactory.getLogger(WebPullRequestResourceImpl.class);
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

    @Test
    void shouldDeletePullRequestAndReturnAffectedRows() {
        when(service.delete(4564L)).thenReturn(1);

        assertThat(resource.deletePullRequest(new DeletePullRequestReq(4564L)).getDeleted()).isOne();
        verify(service).delete(4564L);
    }

    @Test
    void shouldReturnZeroWhenDeletingAbsentPullRequest() {
        when(service.delete(4564L)).thenReturn(0);

        assertThat(resource.deletePullRequest(new DeletePullRequestReq(4564L)).getDeleted()).isZero();
    }

    @Test
    void shouldAuditPullRequestDeletion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(WebAuthInterceptor.ATTR_USERNAME, "admin");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(service.delete(4564L)).thenReturn(1);

        resource.deletePullRequest(new DeletePullRequestReq(4564L));

        assertThat(listAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .contains("Delete pull request, username=admin, prId=4564, deleted=1");
    }

    @Test
    void shouldAuditDeletionWithUnknownUsername() {
        when(service.delete(4564L)).thenReturn(0);

        resource.deletePullRequest(new DeletePullRequestReq(4564L));

        assertThat(listAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .contains("Delete pull request, username=<unknown>, prId=4564, deleted=0");
    }
}
