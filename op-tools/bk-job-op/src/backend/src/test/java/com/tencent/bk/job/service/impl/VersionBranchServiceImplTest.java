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
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionBranchServiceImplTest {

    @Mock
    private VersionBranchMapper versionBranchMapper;

    private VersionBranchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VersionBranchServiceImpl(versionBranchMapper);
    }

    @Test
    void createShouldInsertAndReturnPersistedRow() {
        VersionBranchReq req = sampleReq("3.10.x");
        when(versionBranchMapper.countByVersionBranch("3.10.x")).thenReturn(0);
        when(versionBranchMapper.insert(any(VersionBranchDTO.class))).thenReturn(1);

        VersionBranchDTO persisted = sampleDto("3.10.x");
        persisted.setCreateTime(100L);
        persisted.setLastModifyTime(100L);
        when(versionBranchMapper.selectByVersionBranch("3.10.x")).thenReturn(persisted);

        VersionBranchDTO result = service.create(req);

        ArgumentCaptor<VersionBranchDTO> captor = ArgumentCaptor.forClass(VersionBranchDTO.class);
        verify(versionBranchMapper).insert(captor.capture());
        VersionBranchDTO inserted = captor.getValue();
        assertEquals("3.10.x", inserted.getVersionBranch());
        assertEquals("desc", inserted.getDescription());
        assertNotNull(inserted.getCreateTime());
        assertEquals(inserted.getCreateTime(), inserted.getLastModifyTime());
        assertEquals(persisted, result);
    }

    @Test
    void createShouldTrimVersionBranchWithoutChangingCase() {
        VersionBranchReq req = sampleReq("  Master  ");
        when(versionBranchMapper.countByVersionBranch("Master")).thenReturn(0);
        when(versionBranchMapper.insert(any(VersionBranchDTO.class))).thenReturn(1);
        when(versionBranchMapper.selectByVersionBranch("Master")).thenReturn(sampleDto("Master"));

        service.create(req);

        ArgumentCaptor<VersionBranchDTO> captor = ArgumentCaptor.forClass(VersionBranchDTO.class);
        verify(versionBranchMapper).insert(captor.capture());
        assertEquals("Master", captor.getValue().getVersionBranch());
    }

    @Test
    void createShouldRejectDuplicateBeforeInsert() {
        VersionBranchReq req = sampleReq("3.10.x");
        when(versionBranchMapper.countByVersionBranch("3.10.x")).thenReturn(1);

        OpApiException ex = assertThrows(OpApiException.class, () -> service.create(req));
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals("versionBranch already exists: 3.10.x", ex.getMessage());
        verify(versionBranchMapper, never()).insert(any());
    }

    @Test
    void createShouldConvertWrappedDuplicateKeyToConflict() {
        VersionBranchReq req = sampleReq("3.10.x");
        when(versionBranchMapper.countByVersionBranch("3.10.x")).thenReturn(0);
        PersistenceException wrapped = new PersistenceException(
            new DuplicateKeyException("dup", new SQLIntegrityConstraintViolationException("Duplicate entry")));
        when(versionBranchMapper.insert(any(VersionBranchDTO.class))).thenThrow(wrapped);

        OpApiException ex = assertThrows(OpApiException.class, () -> service.create(req));
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals("versionBranch already exists: 3.10.x", ex.getMessage());
    }

    @Test
    void createShouldRejectBlankVersionBranch() {
        VersionBranchReq req = sampleReq("   ");
        OpApiException ex = assertThrows(OpApiException.class, () -> service.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("versionBranch is required", ex.getMessage());
    }

    @Test
    void createShouldRejectOverlongVersionBranch() {
        VersionBranchReq req = sampleReq("x".repeat(65));
        OpApiException ex = assertThrows(OpApiException.class, () -> service.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("versionBranch length"));
    }

    @Test
    void createShouldRejectOverlongDescription() {
        VersionBranchReq req = sampleReq("3.10.x");
        req.setDescription("d".repeat(513));
        OpApiException ex = assertThrows(OpApiException.class, () -> service.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("description length"));
    }

    @Test
    void createShouldRejectOverlongPipelineCmd() {
        VersionBranchReq req = sampleReq("3.10.x");
        req.setDevBranchDeployPipelineCmd("c".repeat(65536));
        OpApiException ex = assertThrows(OpApiException.class, () -> service.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("devBranchDeployPipelineCmd length"));
    }

    @Test
    void createShouldRejectOverlongPipelineCmdDesc() {
        VersionBranchReq req = sampleReq("3.10.x");
        req.setDevBranchDeployPipelineCmdDesc("d".repeat(65536));
        OpApiException ex = assertThrows(OpApiException.class, () -> service.create(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("devBranchDeployPipelineCmdDesc length"));
    }

    @Test
    void getShouldReturnRow() {
        VersionBranchDTO dto = sampleDto("3.10.x");
        when(versionBranchMapper.selectByVersionBranch("3.10.x")).thenReturn(dto);
        assertEquals(dto, service.get(" 3.10.x "));
    }

    @Test
    void getShouldReturnNotFound() {
        when(versionBranchMapper.selectByVersionBranch("missing")).thenReturn(null);
        OpApiException ex = assertThrows(OpApiException.class, () -> service.get("missing"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        assertEquals("versionBranch not found: missing", ex.getMessage());
    }

    @Test
    void listShouldReturnEmptyList() {
        when(versionBranchMapper.selectAll()).thenReturn(Collections.emptyList());
        List<VersionBranchDTO> result = service.list();
        assertTrue(result.isEmpty());
    }

    @Test
    void updateShouldOverwriteFieldsAndRefreshLastModifyTime() {
        VersionBranchReq req = sampleReq("3.10.x");
        req.setDescription("new-desc");
        req.setDevBranch(null);
        req.setDevBranchDeployPipelineCmd(null);
        req.setDevBranchDeployPipelineCmdDesc(null);
        when(versionBranchMapper.updateByVersionBranch(any(VersionBranchDTO.class))).thenReturn(1);

        VersionBranchDTO persisted = sampleDto("3.10.x");
        persisted.setDescription("new-desc");
        persisted.setDevBranch(null);
        persisted.setCreateTime(1L);
        persisted.setLastModifyTime(2L);
        when(versionBranchMapper.selectByVersionBranch("3.10.x")).thenReturn(persisted);

        VersionBranchDTO result = service.update(req);

        ArgumentCaptor<VersionBranchDTO> captor = ArgumentCaptor.forClass(VersionBranchDTO.class);
        verify(versionBranchMapper).updateByVersionBranch(captor.capture());
        VersionBranchDTO updated = captor.getValue();
        assertEquals("new-desc", updated.getDescription());
        assertEquals(null, updated.getDevBranch());
        assertEquals(null, updated.getDevBranchDeployPipelineCmd());
        assertNotNull(updated.getLastModifyTime());
        assertEquals(null, updated.getCreateTime());
        assertEquals(1L, result.getCreateTime());
        assertEquals(2L, result.getLastModifyTime());
    }

    @Test
    void updateShouldReturnNotFound() {
        VersionBranchReq req = sampleReq("missing");
        when(versionBranchMapper.updateByVersionBranch(any(VersionBranchDTO.class))).thenReturn(0);
        OpApiException ex = assertThrows(OpApiException.class, () -> service.update(req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        verify(versionBranchMapper, never()).selectByVersionBranch(any());
    }

    @Test
    void deleteShouldReturnDeleted() {
        when(versionBranchMapper.deleteByVersionBranch("3.10.x")).thenReturn(1);
        DeleteVersionBranchResp resp = service.delete("3.10.x");
        assertTrue(resp.isDeleted());
        assertEquals("3.10.x", resp.getVersionBranch());
    }

    @Test
    void deleteShouldReturnNotFound() {
        when(versionBranchMapper.deleteByVersionBranch("missing")).thenReturn(0);
        OpApiException ex = assertThrows(OpApiException.class, () -> service.delete("missing"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
        assertEquals("versionBranch not found: missing", ex.getMessage());
    }

    @Test
    void containsDuplicateKeyShouldDetectNestedCause() {
        Throwable wrapped = new PersistenceException(
            new DuplicateKeyException("dup", new SQLIntegrityConstraintViolationException("dup")));
        assertTrue(VersionBranchServiceImpl.containsDuplicateKey(wrapped));
    }

    private VersionBranchReq sampleReq(String versionBranch) {
        VersionBranchReq req = new VersionBranchReq();
        req.setVersionBranch(versionBranch);
        req.setDescription("desc");
        req.setDevBranch("bk-dev_3.10.x");
        req.setDevBranchDeployPipelineCmd("curl ...");
        req.setDevBranchDeployPipelineCmdDesc("usage");
        return req;
    }

    private VersionBranchDTO sampleDto(String versionBranch) {
        VersionBranchDTO dto = new VersionBranchDTO();
        dto.setVersionBranch(versionBranch);
        dto.setDescription("desc");
        dto.setDevBranch("bk-dev_3.10.x");
        dto.setDevBranchDeployPipelineCmd("curl ...");
        dto.setDevBranchDeployPipelineCmdDesc("usage");
        return dto;
    }
}
