package com.tencent.bk.job.service.impl;

import com.tencent.bk.job.dao.PullRequestMapper;
import com.tencent.bk.job.exception.OpApiException;
import com.tencent.bk.job.model.CreatePullRequestReq;
import com.tencent.bk.job.model.PullRequestDTO;
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.model.PullRequestStatusEnum;
import com.tencent.bk.job.model.UpdatePullRequestStatusReq;
import com.tencent.bk.job.service.PullRequestService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PullRequestServiceImpl implements PullRequestService {

    private static final int CODE_DIR_MAX_LENGTH = 1024;
    private static final int NAME_MAX_LENGTH = 128;
    private static final int CONVERSATION_ID_MAX_LENGTH = 128;
    private static final int CONVERSATION_TITLE_MAX_LENGTH = 512;

    private final PullRequestMapper pullRequestMapper;

    public PullRequestServiceImpl(PullRequestMapper pullRequestMapper) {
        this.pullRequestMapper = pullRequestMapper;
    }

    @Override
    public PullRequestResp create(CreatePullRequestReq req) {
        PullRequestDTO dto = buildFromReq(req);
        long now = System.currentTimeMillis();
        dto.setCreateTime(now);
        dto.setUpdateTime(now);
        try {
            pullRequestMapper.insert(dto);
        } catch (RuntimeException ex) {
            if (containsDuplicateKey(ex)) {
                throw new OpApiException(HttpStatus.CONFLICT, "prId already exists: " + dto.getPrId());
            }
            throw ex;
        }
        return toResp(dto);
    }

    @Override
    public PullRequestResp getByPrId(Long prId) {
        validatePositiveId(prId, "prId");
        PullRequestDTO dto = pullRequestMapper.selectByPrId(prId);
        if (dto == null) {
            throw notFound(prId);
        }
        return toResp(dto);
    }

    @Override
    public PullRequestResp updateStatus(UpdatePullRequestStatusReq req) {
        if (req == null) {
            throw badRequest("request body is required");
        }
        validatePositiveId(req.getPrId(), "prId");
        String status = validateStatus(req.getStatus());
        int affected = pullRequestMapper.updateStatusByPrId(req.getPrId(), status, System.currentTimeMillis());
        if (affected == 0) {
            throw notFound(req.getPrId());
        }
        PullRequestDTO dto = pullRequestMapper.selectByPrId(req.getPrId());
        if (dto == null) {
            throw notFound(req.getPrId());
        }
        return toResp(dto);
    }

    @Override
    public Map<Long, Long> findLatestPrIdsByIssueIds(Collection<Long> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> normalizedIds = issueIds.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .collect(Collectors.toList());
        if (normalizedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new LinkedHashMap<>();
        pullRequestMapper.selectLatestByIssueIds(normalizedIds)
            .forEach(dto -> result.put(dto.getIssueId(), dto.getPrId()));
        return result;
    }

    private PullRequestDTO buildFromReq(CreatePullRequestReq req) {
        if (req == null) {
            throw badRequest("request body is required");
        }
        validatePositiveId(req.getPrId(), "prId");
        validatePositiveId(req.getIssueId(), "issueId");
        PullRequestDTO dto = new PullRequestDTO();
        dto.setPrId(req.getPrId());
        dto.setIssueId(req.getIssueId());
        dto.setCodeDir(normalizeRequired(req.getCodeDir(), CODE_DIR_MAX_LENGTH, "codeDir"));
        dto.setStatus(validateStatus(req.getStatus()));
        dto.setAgentName(normalizeRequired(req.getAgentName(), NAME_MAX_LENGTH, "agentName"));
        dto.setAuthorName(normalizeRequired(req.getAuthorName(), NAME_MAX_LENGTH, "authorName"));
        dto.setImateConversationId(
            normalizeOptional(req.getImateConversationId(), CONVERSATION_ID_MAX_LENGTH, "imateConversationId"));
        dto.setImateConversationTitle(
            normalizeOptional(req.getImateConversationTitle(), CONVERSATION_TITLE_MAX_LENGTH,
                "imateConversationTitle"));
        return dto;
    }

    private void validatePositiveId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw badRequest(fieldName + " must be a positive integer");
        }
    }

    private String validateStatus(String status) {
        if (status == null) {
            throw badRequest("status is required");
        }
        try {
            return PullRequestStatusEnum.fromValue(status).name();
        } catch (IllegalArgumentException ex) {
            throw badRequest("invalid status: " + status);
        }
    }

    private String normalizeRequired(String value, int maxLength, String fieldName) {
        String normalized = StringUtils.trimToNull(value);
        if (normalized == null) {
            throw badRequest(fieldName + " is required");
        }
        validateMaxLength(normalized, maxLength, fieldName);
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength, String fieldName) {
        String normalized = StringUtils.trimToNull(value);
        validateMaxLength(normalized, maxLength, fieldName);
        return normalized;
    }

    private void validateMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw badRequest(fieldName + " length must be <= " + maxLength);
        }
    }

    private PullRequestResp toResp(PullRequestDTO dto) {
        return new PullRequestResp(
            dto.getPrId(),
            dto.getIssueId(),
            dto.getCodeDir(),
            dto.getStatus(),
            dto.getAgentName(),
            dto.getAuthorName(),
            dto.getImateConversationId(),
            dto.getImateConversationTitle(),
            dto.getCreateTime(),
            dto.getUpdateTime()
        );
    }

    private OpApiException badRequest(String message) {
        return new OpApiException(HttpStatus.BAD_REQUEST, message);
    }

    private OpApiException notFound(Long prId) {
        return new OpApiException(HttpStatus.NOT_FOUND, "prId not found: " + prId);
    }

    static boolean containsDuplicateKey(Throwable throwable) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        while (current != null && visited.add(current)) {
            if (current instanceof DuplicateKeyException
                || current instanceof SQLIntegrityConstraintViolationException
                || current instanceof SQLException && ((SQLException) current).getErrorCode() == 1062) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
