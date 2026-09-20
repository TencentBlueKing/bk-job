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

package com.tencent.bk.job.config;

import com.tencent.bk.job.exception.OpApiException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void shouldMapConflict() {
        OpApiException ex = new OpApiException(HttpStatus.CONFLICT, "versionBranch already exists: 3.10.x");
        ResponseEntity<Map<String, Object>> resp = handler.handleOpApiException(ex);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("Conflict", resp.getBody().get("error"));
        assertEquals("versionBranch already exists: 3.10.x", resp.getBody().get("message"));
        assertEquals(409, resp.getBody().get("status"));
    }

    @Test
    void shouldMapNotFound() {
        OpApiException ex = new OpApiException(HttpStatus.NOT_FOUND, "versionBranch not found: 3.10.x");
        ResponseEntity<Map<String, Object>> resp = handler.handleOpApiException(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("Not Found", resp.getBody().get("error"));
        assertEquals(404, resp.getBody().get("status"));
    }

    @Test
    void shouldMapIllegalArgumentToBadRequest() {
        ResponseEntity<Map<String, Object>> resp =
            handler.handleIllegalArgumentException(new IllegalArgumentException("versionBranch is required"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Bad Request", resp.getBody().get("error"));
        assertEquals("versionBranch is required", resp.getBody().get("message"));
        assertEquals(400, resp.getBody().get("status"));
    }

    @Test
    void shouldMapDuplicateKeyToConflict() {
        ResponseEntity<Map<String, Object>> resp =
            handler.handleDuplicateKeyException(new DuplicateKeyException("dup"));
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("Conflict", resp.getBody().get("error"));
        assertEquals(409, resp.getBody().get("status"));
    }

    @Test
    void shouldMapMissingParamToBadRequest() {
        ResponseEntity<Map<String, Object>> resp =
            handler.handleMissingParam(new MissingServletRequestParameterException("versionBranch", "String"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Bad Request", resp.getBody().get("error"));
        assertEquals("versionBranch is required", resp.getBody().get("message"));
    }
}
