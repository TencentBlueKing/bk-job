package com.tencent.bk.job.service.impl;

import com.tencent.bk.job.dao.PullRequestMapper;
import com.tencent.bk.job.exception.OpApiException;
import com.tencent.bk.job.model.CreatePullRequestReq;
import com.tencent.bk.job.model.PullRequestDTO;
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.model.UpdatePullRequestStatusReq;
import com.tencent.bk.job.service.model.PageData;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PullRequestServiceImplTest {

    private PullRequestMapper mapper;
    private PullRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(PullRequestMapper.class);
        service = new PullRequestServiceImpl(mapper);
    }

    @Test
    void shouldCreateWithServerTimestampsAndNormalizedFields() {
        CreatePullRequestReq req = sampleCreateReq();
        req.setCodeDir(" C:\\repo ");
        req.setImateConversationId(" ");

        PullRequestResp result = service.create(req);

        ArgumentCaptor<PullRequestDTO> captor = ArgumentCaptor.forClass(PullRequestDTO.class);
        verify(mapper).insert(captor.capture());
        PullRequestDTO inserted = captor.getValue();
        assertThat(inserted.getCodeDir()).isEqualTo("C:\\repo");
        assertThat(inserted.getImateConversationId()).isNull();
        assertThat(inserted.getCreateTime()).isPositive().isEqualTo(inserted.getUpdateTime());
        assertThat(result.getPrId()).isEqualTo(4564L);
    }

    @Test
    void shouldRejectNullRequest() {
        assertBadRequest(() -> service.create(null), "request body is required");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void shouldRejectNonPositiveIds(long invalidId) {
        CreatePullRequestReq req = sampleCreateReq();
        req.setPrId(invalidId);

        assertBadRequest(() -> service.create(req), "prId must be a positive integer");
        verify(mapper, never()).insert(any());
    }

    @Test
    void shouldRejectMissingAndOverlongFields() {
        CreatePullRequestReq blankNameReq = sampleCreateReq();
        blankNameReq.setAgentName(" ");
        assertBadRequest(() -> service.create(blankNameReq), "agentName is required");

        CreatePullRequestReq overlongTitleReq = sampleCreateReq();
        overlongTitleReq.setImateConversationTitle("x".repeat(513));
        assertBadRequest(
            () -> service.create(overlongTitleReq), "imateConversationTitle length must be <= 512");
    }

    @Test
    void shouldRejectInvalidStatusCaseSensitively() {
        CreatePullRequestReq req = sampleCreateReq();
        req.setStatus("merged");

        assertBadRequest(() -> service.create(req), "invalid status: merged");
    }

    @Test
    void shouldAcceptAllDeclaredStatuses() {
        for (String status : List.of("WAIT_FOR_REVIEW", "HANDLING", "MERGED", "CLOSED")) {
            CreatePullRequestReq req = sampleCreateReq();
            req.setStatus(status);
            service.create(req);
        }
        verify(mapper, org.mockito.Mockito.times(4)).insert(any());
    }

    @Test
    void shouldConvertDirectAndWrappedDuplicateKeyToConflict() {
        when(mapper.insert(any())).thenThrow(new DuplicateKeyException("dup"));
        assertConflict(() -> service.create(sampleCreateReq()));

        org.mockito.Mockito.reset(mapper);
        when(mapper.insert(any())).thenThrow(new PersistenceException(
            new DuplicateKeyException("dup", new SQLIntegrityConstraintViolationException("dup"))));
        assertConflict(() -> service.create(sampleCreateReq()));
    }

    @Test
    void shouldConvertMysql1062ToConflict() {
        when(mapper.insert(any())).thenThrow(new PersistenceException(
            new SQLException("Duplicate entry", "23000", 1062)));

        assertConflict(() -> service.create(sampleCreateReq()));
    }

    @Test
    void shouldRethrowNonDuplicateException() {
        RuntimeException failure = new RuntimeException("database unavailable");
        when(mapper.insert(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.create(sampleCreateReq())).isSameAs(failure);
    }

    @Test
    void duplicateDetectionShouldTerminateForCyclicCause() {
        RuntimeException cyclic = new RuntimeException("cycle") {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        };

        assertThat(PullRequestServiceImpl.containsDuplicateKey(cyclic)).isFalse();
    }

    @Test
    void shouldGetAndUpdateByPrId() {
        PullRequestDTO dto = sampleDto();
        when(mapper.selectByPrId(4564L)).thenReturn(dto);
        when(mapper.updateStatusByPrId(any(), any(), any())).thenReturn(1);

        assertThat(service.getByPrId(4564L).getIssueId()).isEqualTo(4563L);
        UpdatePullRequestStatusReq req = new UpdatePullRequestStatusReq();
        req.setPrId(4564L);
        req.setStatus("MERGED");
        assertThat(service.updateStatus(req).getPrId()).isEqualTo(4564L);
        verify(mapper).updateStatusByPrId(
            org.mockito.ArgumentMatchers.eq(4564L),
            org.mockito.ArgumentMatchers.eq("MERGED"),
            any()
        );
    }

    @Test
    void shouldReturnNotFoundForMissingGetAndUpdate() {
        assertNotFound(() -> service.getByPrId(999L));

        UpdatePullRequestStatusReq req = new UpdatePullRequestStatusReq();
        req.setPrId(999L);
        req.setStatus("CLOSED");
        assertNotFound(() -> service.updateStatus(req));
        verify(mapper).selectByPrId(999L);
    }

    @Test
    void shouldDeleteByPrIdIdempotently() {
        when(mapper.deleteByPrId(4564L)).thenReturn(1, 0);

        assertThat(service.delete(4564L)).isEqualTo(1);
        assertThat(service.delete(4564L)).isZero();
        verify(mapper, org.mockito.Mockito.times(2)).deleteByPrId(4564L);
    }

    @Test
    void shouldRejectNullDeletePrId() {
        assertBadRequest(() -> service.delete(null), "prId must be a positive integer");
        verify(mapper, never()).deleteByPrId(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void shouldRejectNonPositiveDeletePrId(long invalidPrId) {
        assertBadRequest(() -> service.delete(invalidPrId), "prId must be a positive integer");
        verify(mapper, never()).deleteByPrId(any());
    }

    @Test
    void shouldListPullRequestsWithTotalAndSkipOutOfRangeQuery() {
        PullRequestDTO first = sampleDto();
        PullRequestDTO second = sampleDto();
        second.setPrId(4563L);
        when(mapper.countAll()).thenReturn(2);
        when(mapper.listAll(0, 20)).thenReturn(List.of(first, second));

        PageData<PullRequestResp> result = service.list(0, 20);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getData()).extracting(PullRequestResp::getPrId).containsExactly(4564L, 4563L);

        assertThat(service.list(2, 20).getData()).isEmpty();
        verify(mapper, never()).listAll(2, 20);
    }

    @Test
    void shouldBatchDistinctValidIssueIdsAndMapResults() {
        PullRequestDTO first = sampleDto();
        PullRequestDTO second = sampleDto();
        second.setIssueId(4565L);
        second.setPrId(4566L);
        when(mapper.selectLatestByIssueIds(List.of(4563L, 4565L))).thenReturn(List.of(first, second));

        Map<Long, PullRequestResp> result = service.findLatestByIssueIds(
            java.util.Arrays.asList(4563L, null, 4563L, -1L, 4565L));

        assertThat(result.get(4563L).getPrId()).isEqualTo(4564L);
        assertThat(result.get(4563L).getStatus()).isEqualTo("WAIT_FOR_REVIEW");
        assertThat(result.get(4565L).getPrId()).isEqualTo(4566L);
        verify(mapper).selectLatestByIssueIds(List.of(4563L, 4565L));
    }

    @Test
    void shouldSkipMapperForEmptyIssueIds() {
        assertThat(service.findLatestByIssueIds(List.of())).isEmpty();
        verify(mapper, never()).selectLatestByIssueIds(any());
    }

    private CreatePullRequestReq sampleCreateReq() {
        CreatePullRequestReq req = new CreatePullRequestReq();
        req.setPrId(4564L);
        req.setIssueId(4563L);
        req.setCodeDir("C:\\repo");
        req.setStatus("WAIT_FOR_REVIEW");
        req.setAgentName("agent");
        req.setAuthorName("author");
        req.setImateConversationId("conv-1");
        req.setImateConversationTitle("Issue 4563");
        return req;
    }

    private PullRequestDTO sampleDto() {
        PullRequestDTO dto = new PullRequestDTO();
        dto.setPrId(4564L);
        dto.setIssueId(4563L);
        dto.setCodeDir("C:\\repo");
        dto.setStatus("WAIT_FOR_REVIEW");
        dto.setAgentName("agent");
        dto.setAuthorName("author");
        dto.setCreateTime(1L);
        dto.setUpdateTime(1L);
        return dto;
    }

    private void assertBadRequest(Runnable action, String message) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(OpApiException.class, ex -> {
                assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(ex.getMessage()).isEqualTo(message);
            });
    }

    private void assertConflict(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(OpApiException.class, ex -> {
                assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(ex.getMessage()).isEqualTo("prId already exists: 4564");
            });
    }

    private void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(OpApiException.class, ex -> {
                assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(ex.getMessage()).isEqualTo("prId not found: 999");
            });
    }
}
