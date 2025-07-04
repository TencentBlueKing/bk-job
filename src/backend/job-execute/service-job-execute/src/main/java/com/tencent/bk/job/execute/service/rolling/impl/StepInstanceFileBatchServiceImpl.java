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

package com.tencent.bk.job.execute.service.rolling.impl;

import com.tencent.bk.job.execute.dao.StepInstanceFileBatchDAO;
import com.tencent.bk.job.execute.dao.common.IdGen;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceFileBatchDTO;
import com.tencent.bk.job.execute.service.rolling.StepInstanceFileBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class StepInstanceFileBatchServiceImpl implements StepInstanceFileBatchService {

    private final StepInstanceFileBatchDAO stepInstanceFileBatchDAO;
    private final IdGen idGen;

    @Autowired
    public StepInstanceFileBatchServiceImpl(StepInstanceFileBatchDAO stepInstanceFileBatchDAO, IdGen idGen) {
        this.stepInstanceFileBatchDAO = stepInstanceFileBatchDAO;
        this.idGen = idGen;
    }

    @Override
    public int batchInsert(List<StepInstanceFileBatchDTO> stepInstanceFileBatchDTOList) {
        if (CollectionUtils.isEmpty(stepInstanceFileBatchDTOList)) {
            return 0;
        }
        stepInstanceFileBatchDTOList.forEach(stepInstanceFileBatchDTO -> {
            stepInstanceFileBatchDTO.setId(idGen.genStepInstanceFileBatchId());
        });
        return stepInstanceFileBatchDAO.batchInsert(stepInstanceFileBatchDTOList);
    }

    @Override
    public Integer getMaxBatch(long taskInstanceId, long stepInstanceId) {
        return stepInstanceFileBatchDAO.getMaxBatch(taskInstanceId, stepInstanceId);
    }

    @Override
    public StepInstanceFileBatchDTO get(long taskInstanceId, long stepInstanceId, int batch) {
        return stepInstanceFileBatchDAO.get(taskInstanceId, stepInstanceId, batch);
    }

    @Override
    public int updateResolvedSourceFile(long taskInstanceId,
                                        long stepInstanceId,
                                        int batch,
                                        List<FileSourceDTO> resolvedFileSources) {
        return stepInstanceFileBatchDAO.updateResolvedSourceFile(
            taskInstanceId,
            stepInstanceId,
            batch, resolvedFileSources
        );
    }
}
