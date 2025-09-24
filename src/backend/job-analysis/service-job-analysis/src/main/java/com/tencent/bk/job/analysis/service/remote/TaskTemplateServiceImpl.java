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

package com.tencent.bk.job.analysis.service.remote;

import com.tencent.bk.job.analysis.service.TaskTemplateService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service("jobAnalysisTaskTemplateServiceImpl")
public class TaskTemplateServiceImpl implements TaskTemplateService {
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final ServiceTaskTemplateResource taskTemplateResource;
    private final ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    public TaskTemplateServiceImpl(ServiceTaskTemplateResource taskTemplateResource,
                                   @Qualifier("jobTemplateFetchTaskExecutor") ThreadPoolExecutor threadPoolExecutor) {
        this.taskTemplateResource = taskTemplateResource;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public PageData<ServiceTaskTemplateDTO> listPageTaskTemplates(Long appId,
                                                                  BaseSearchCondition baseSearchCondition) {
        val result =
            taskTemplateResource.listPageTaskTemplates(appId,
                baseSearchCondition.getStart(), baseSearchCondition.getLength());
        if (result != null) {
            return result.getData();
        } else {
            throw new RuntimeException("Fail to call job-manage:serviceTaskTemplateResourceClient" +
                ".listPageTaskTemplates, please check");
        }
    }

    @Override
    public PageData<ServiceTaskTemplateDTO> batchGetTaskTemplateList(Long appId) {
        InternalResponse<Integer> resp = taskTemplateResource.countTemplates(appId);
        if (resp.getData() == 0) {
            log.info("Job templates is empty, appId:{}", appId);
            return PageData.emptyPageData(0, 0);
        }

        Integer templateCount = resp.getData();
        int totalPage = (templateCount + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE;
        List<Integer> pageList = IntStream.range(0, totalPage).boxed().collect(Collectors.toList());

        log.info("Starting batch fetch job templates, appId={}, totalTemplates={}, totalPage={}, pageSize={}",appId,
            templateCount, totalPage, DEFAULT_PAGE_SIZE);

        List<ServiceTaskTemplateDTO> result = ConcurrencyUtil.getResultWithThreads(
            pageList,
            threadPoolExecutor,
            page -> {
                try {
                    int start = page * DEFAULT_PAGE_SIZE;
                    log.debug("Fetching templates page {}, start={}, pageSize={}", page, start,
                        DEFAULT_PAGE_SIZE);
                    InternalResponse<PageData<ServiceTaskTemplateDTO>> pageResp =
                        taskTemplateResource.listPageTaskTemplates(appId, start, DEFAULT_PAGE_SIZE);
                    if (CollectionUtils.isEmpty(pageResp.getData().getData())) {
                        log.warn("No templates returned for page {}, appId={}", page, appId);
                        return Collections.emptyList();
                    }
                    return pageResp.getData().getData();
                } catch (Exception e) {
                    log.error("Error fetching page {}, appId={}", page, appId, e);
                    return Collections.emptyList();
                }
            }
        );

        log.info("Completed batch fetch, appId={}, totalFetchedTemplates={}", appId, result.size());

        PageData<ServiceTaskTemplateDTO> resultData = new PageData<>();
        resultData.setData(result);
        resultData.setTotal(Long.valueOf(templateCount));
        resultData.setStart(0);
        resultData.setPageSize(DEFAULT_PAGE_SIZE);
        return resultData;
    }
}
