/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.file_gateway.dao.filesource;

import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;

public interface FileSourceTaskDAO {
    String insertFileSourceTask(DSLContext dslContext, FileSourceTaskDTO fileSourceTaskDTO);

    int updateFileSourceTask(DSLContext dslContext, FileSourceTaskDTO fileSourceTaskDTO);

    int updateFileClearStatus(DSLContext dslContext, List<String> taskIdList, boolean fileCleared);

    int deleteById(DSLContext dslContext, String id);

    FileSourceTaskDTO getFileSourceTaskById(DSLContext dslContext, String id);

    Long countFileSourceTasks(DSLContext dslContext, Long appId);

    Long countFileSourceTasksByBatchTaskId(DSLContext dslContext, String batchTaskId, Byte status);

    List<FileSourceTaskDTO> listFileSourceTasks(DSLContext dslContext, Long appId, Integer start, Integer pageSize);

    List<FileSourceTaskDTO> listTimeoutTasks(DSLContext dslContext, Long expireTimeMills, Collection<Byte> statusSet,
                                             Integer start, Integer pageSize);

    List<FileSourceTaskDTO> listByBatchTaskId(String batchTaskId);

    int deleteByBatchTaskId(DSLContext dslContext, String batchTaskId);
}
