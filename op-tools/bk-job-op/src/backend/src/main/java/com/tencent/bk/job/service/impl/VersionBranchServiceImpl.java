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

package com.tencent.bk.job.service.impl;

import com.tencent.bk.job.dao.VersionBranchMapper;
import com.tencent.bk.job.exception.OpApiException;
import com.tencent.bk.job.model.DeleteVersionBranchResp;
import com.tencent.bk.job.model.VersionBranchDTO;
import com.tencent.bk.job.model.VersionBranchReq;
import com.tencent.bk.job.service.VersionBranchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@Service
public class VersionBranchServiceImpl implements VersionBranchService {

    private static final int VERSION_BRANCH_MAX_LEN = 64;
    private static final int DESCRIPTION_MAX_LEN = 512;
    private static final int DEV_BRANCH_MAX_LEN = 128;
    private static final int PIPELINE_CMD_MAX_LEN = 65535;

    private final VersionBranchMapper versionBranchMapper;

    public VersionBranchServiceImpl(VersionBranchMapper versionBranchMapper) {
        this.versionBranchMapper = versionBranchMapper;
    }

    @Override
    public VersionBranchDTO create(VersionBranchReq req) {
        VersionBranchDTO dto = buildFromReq(req);
        if (versionBranchMapper.countByVersionBranch(dto.getVersionBranch()) > 0) {
            throw conflict(dto.getVersionBranch());
        }
        long now = System.currentTimeMillis();
        dto.setCreateTime(now);
        dto.setLastModifyTime(now);
        try {
            versionBranchMapper.insert(dto);
        } catch (RuntimeException ex) {
            if (containsDuplicateKey(ex)) {
                throw conflict(dto.getVersionBranch());
            }
            throw ex;
        }
        return versionBranchMapper.selectByVersionBranch(dto.getVersionBranch());
    }

    @Override
    public VersionBranchDTO get(String versionBranch) {
        String normalized = normalizeAndValidateVersionBranch(versionBranch);
        VersionBranchDTO row = versionBranchMapper.selectByVersionBranch(normalized);
        if (row == null) {
            throw notFound(normalized);
        }
        return row;
    }

    @Override
    public List<VersionBranchDTO> list() {
        return versionBranchMapper.selectAll();
    }

    @Override
    public VersionBranchDTO update(VersionBranchReq req) {
        VersionBranchDTO dto = buildFromReq(req);
        dto.setLastModifyTime(System.currentTimeMillis());
        int affected = versionBranchMapper.updateByVersionBranch(dto);
        if (affected == 0) {
            throw notFound(dto.getVersionBranch());
        }
        return versionBranchMapper.selectByVersionBranch(dto.getVersionBranch());
    }

    @Override
    public DeleteVersionBranchResp delete(String versionBranch) {
        String normalized = normalizeAndValidateVersionBranch(versionBranch);
        int affected = versionBranchMapper.deleteByVersionBranch(normalized);
        if (affected == 0) {
            throw notFound(normalized);
        }
        return new DeleteVersionBranchResp(true, normalized);
    }

    private VersionBranchDTO buildFromReq(VersionBranchReq req) {
        if (req == null) {
            throw new OpApiException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        VersionBranchDTO dto = new VersionBranchDTO();
        dto.setVersionBranch(normalizeAndValidateVersionBranch(req.getVersionBranch()));
        validateContentFields(req);
        dto.setDescription(req.getDescription());
        dto.setDevBranch(req.getDevBranch());
        dto.setDevBranchDeployPipelineCmd(req.getDevBranchDeployPipelineCmd());
        dto.setDevBranchDeployPipelineCmdDesc(req.getDevBranchDeployPipelineCmdDesc());
        return dto;
    }

    private String normalizeAndValidateVersionBranch(String versionBranch) {
        if (StringUtils.isBlank(versionBranch)) {
            throw new OpApiException(HttpStatus.BAD_REQUEST, "versionBranch is required");
        }
        String normalized = versionBranch.trim();
        if (normalized.length() > VERSION_BRANCH_MAX_LEN) {
            throw new OpApiException(HttpStatus.BAD_REQUEST, "versionBranch length must be <= " + VERSION_BRANCH_MAX_LEN);
        }
        return normalized;
    }

    private void validateContentFields(VersionBranchReq req) {
        validateMaxLength(req.getDescription(), DESCRIPTION_MAX_LEN, "description");
        validateMaxLength(req.getDevBranch(), DEV_BRANCH_MAX_LEN, "devBranch");
        validateMaxLength(req.getDevBranchDeployPipelineCmd(), PIPELINE_CMD_MAX_LEN, "devBranchDeployPipelineCmd");
        validateMaxLength(
            req.getDevBranchDeployPipelineCmdDesc(), PIPELINE_CMD_MAX_LEN, "devBranchDeployPipelineCmdDesc");
    }

    private void validateMaxLength(String value, int maxLen, String fieldName) {
        if (value != null && value.length() > maxLen) {
            throw new OpApiException(HttpStatus.BAD_REQUEST, fieldName + " length must be <= " + maxLen);
        }
    }

    private OpApiException conflict(String versionBranch) {
        return new OpApiException(HttpStatus.CONFLICT, "versionBranch already exists: " + versionBranch);
    }

    private OpApiException notFound(String versionBranch) {
        return new OpApiException(HttpStatus.NOT_FOUND, "versionBranch not found: " + versionBranch);
    }

    static boolean containsDuplicateKey(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DuplicateKeyException
                || current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
