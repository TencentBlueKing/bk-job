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

import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;

public interface FileTaskDAO {
    Long insertFileTask(DSLContext dslContext, FileTaskDTO fileTaskDTO);

    int updateFileTask(DSLContext dslContext, FileTaskDTO fileTaskDTO);

    int resetFileTasks(DSLContext dslContext, String fileSourceTaskId);

    int deleteFileTaskById(DSLContext dslContext, Long id);

    int deleteFileTaskByFileSourceTaskId(DSLContext dslContext, String fileSourceTaskId);

    FileTaskDTO getFileTaskById(DSLContext dslContext, Long id);

    FileTaskDTO getOneFileTask(DSLContext dslContext, String fileSourceTaskId, String filePath);

    Long countFileTasks(DSLContext dslContext, String fileSourceTaskId);

    List<FileTaskDTO> listFileTasks(String fileSourceTaskId, Integer start, Integer pageSize);

    List<String> listTimeoutFileSourceTaskIds(DSLContext dslContext, Long expireTimeMills, Collection<Byte> statusSet
        , Integer start, Integer pageSize);

    List<FileTaskDTO> listFileTasks(String fileSourceTaskId);

    Long countFileTask(String fileSourceTaskId, Byte status);
}
