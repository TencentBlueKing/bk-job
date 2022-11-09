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

import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;

import java.util.Collection;
import java.util.List;

public interface FileWorkerDAO {
    // 新增
    Long insertFileWorker(FileWorkerDTO fileSourceDTO);

    // 更新
    int updateFileWorker(FileWorkerDTO fileSourceDTO);

    int updateAllFileWorkerOnlineStatus(Integer onlineStatus, Long aliveTime);

    // 查询
    FileWorkerDTO getFileWorkerById(Long id);

    List<FileWorkerDTO> listPublicFileWorkers(Collection<Long> includeIds, Collection<Long> excludeIds);

    List<FileWorkerDTO> listFileWorkers(Long appId, Collection<Long> includeIds, Collection<Long> excludeIds);

    List<FileWorkerDTO> listFileWorkersByAbilityTag(Long appId,
                                                    String tag,
                                                    Collection<Long> includeIds,
                                                    Collection<Long> excludeIds);

    List<FileWorkerDTO> listPublicFileWorkersByAbilityTag(String tag,
                                                          Collection<Long> includeIds,
                                                          Collection<Long> excludeIds);

    Long countFileWorkers();

    Long countOnlineFileWorkers();

    boolean existsFileWorker(String accessHost, Integer accessPort);

    FileWorkerDTO getFileWorker(String accessHost, Integer accessPort);
}
