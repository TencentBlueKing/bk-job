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

package com.tencent.bk.job.analysis.task.analysis;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.analysis.dao.AnalysisTaskDAO;
import com.tencent.bk.job.analysis.dao.AnalysisTaskInstanceDAO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskDTO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceDTO;
import com.tencent.bk.job.analysis.task.analysis.anotation.AnalysisTask;
import com.tencent.bk.job.analysis.task.analysis.task.IAnalysisTask;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import com.tencent.bk.job.manage.remote.RemoteAppService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseAnalysisTask implements IAnalysisTask {

    protected final AnalysisTaskInstanceDAO analysisTaskInstanceDAO;
    private final String taskCode;
    private final RemoteAppService remoteAppService;
    private final String KEY_ANALYSIS_TASK_DTO = "KEY_ANALYSIS_TASK_DTO";
    private final AnalysisTaskDAO analysisTaskDAO;
    private LoadingCache<String, AnalysisTaskDTO> analysisTaskDTOCache = CacheBuilder.newBuilder()
        .maximumSize(10).expireAfterWrite(10, TimeUnit.SECONDS).
            build(new CacheLoader<String, AnalysisTaskDTO>() {
                      @Override
                      public AnalysisTaskDTO load(String searchKey) throws Exception {
                          if (searchKey.equals(KEY_ANALYSIS_TASK_DTO)) {
                              return analysisTaskDAO.getAnalysisTaskByCode(taskCode);
                          } else {
                              return null;
                          }
                      }
                  }
            );

    public BaseAnalysisTask(AnalysisTaskDAO analysisTaskDAO,
                            AnalysisTaskInstanceDAO analysisTaskInstanceDAO,
                            RemoteAppService remoteAppService) {
        this.analysisTaskDAO = analysisTaskDAO;
        this.analysisTaskInstanceDAO = analysisTaskInstanceDAO;
        this.remoteAppService = remoteAppService;
        this.taskCode = this.getClass().getAnnotation(AnalysisTask.class).value();
    }

    /**
     * 数据库内容更新时同步更新缓存
     */
    public void refreshAnalysisTask() {
        try {
            this.analysisTaskDTOCache.refresh(KEY_ANALYSIS_TASK_DTO);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            log.error("refreshAnalysisTask fail", e);
        }
    }

    public Long insertAnalysisTaskInstance(AnalysisTaskInstanceDTO analysisTaskInstanceDTO) {
        return analysisTaskInstanceDAO.insertAnalysisTaskInstance(analysisTaskInstanceDTO);
    }

    public int updateAnalysisTaskInstanceById(AnalysisTaskInstanceDTO analysisTaskInstanceDTO) {
        // 先将历史任务结果全部清除
        analysisTaskInstanceDAO.deleteHistoryAnalysisTaskInstance(analysisTaskInstanceDTO.getAppId(),
            analysisTaskInstanceDTO.getTaskId());
        return analysisTaskInstanceDAO.updateAnalysisTaskInstanceById(analysisTaskInstanceDTO);
    }

    public List<ServiceApplicationDTO> getAppInfoList() {
        return remoteAppService.listLocalDBApps();
    }

    @Override
    public String getTaskCode() {
        return taskCode;
    }

    @Override
    public AnalysisTaskDTO getAnalysisTask() {
        try {
            return analysisTaskDTOCache.get(KEY_ANALYSIS_TASK_DTO);
        } catch (ExecutionException | CacheLoader.InvalidCacheLoadException e) {
            log.warn("fail to getAnalysisTask", e);
        }
        return null;
    }

    @Override
    public long getPeriodSeconds() {
        return getAnalysisTask().getPeriodSeconds();
    }
}
