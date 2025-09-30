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

package com.tencent.bk.job.analysis.task.analysis.task.impl;

import com.tencent.bk.job.analysis.dao.AnalysisTaskDAO;
import com.tencent.bk.job.analysis.dao.AnalysisTaskInstanceDAO;
import com.tencent.bk.job.analysis.service.ApplicationService;
import com.tencent.bk.job.analysis.service.TaskTemplateService;
import com.tencent.bk.job.analysis.task.analysis.BaseAnalysisTask;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 作业模版分析抽象实现
 */
@Slf4j
public abstract class AbstractTemplateAnalysisTask extends BaseAnalysisTask {
    // 分页获取作业模板的数量
    private static final int PAGE_SIZE = 50;
    // 生产者和消费者之间的缓冲队列容量，防止过度堆积造成内存泄露
    private static final int QUEUE_CAPACITY = 5;
    // 并发分析消费者数量
    private static final int ANALYSIS_THREAD_COUNT = 5;

    protected final TaskTemplateService templateService;
    protected final ThreadPoolExecutor threadPoolExecutor;

    public AbstractTemplateAnalysisTask(
        AnalysisTaskDAO analysisTaskDAO,
        AnalysisTaskInstanceDAO analysisTaskInstanceDAO,
        ApplicationService applicationService,
        TaskTemplateService templateService,
        ThreadPoolExecutor threadPoolExecutor
    ) {
        super(analysisTaskDAO,
            analysisTaskInstanceDAO,
            applicationService
        );
        this.templateService = templateService;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    /**
     * 并发分析指定业务下的作业模版
     * appId 业务ID
     * consumer 分析逻辑
     */
    protected void runAnalysisForApp(Long appId,
                                     Consumer<List<ServiceTaskTemplateDTO>> consumer) throws InterruptedException {
        BlockingQueue<List<ServiceTaskTemplateDTO>> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        // 消费者，多线程分析
        CountDownLatch countDownLatch = new CountDownLatch(ANALYSIS_THREAD_COUNT);
        for (int i = 0; i < ANALYSIS_THREAD_COUNT; i++) {
            threadPoolExecutor.submit(() -> {
                try {
                    while (true) {
                        List<ServiceTaskTemplateDTO> templateList = queue.poll(10, TimeUnit.SECONDS);
                        if (templateList == null) continue;
                        if (templateList.isEmpty()) break;
                        log.debug("Consumer start analysis templates, className={}, thread={}, size={}",
                            this.getClass().getSimpleName(), Thread.currentThread().getName(), templateList.size());
                        consumer.accept(templateList);
                    }
                } catch (InterruptedException e) {
                    log.warn("Consumer analysis templates interrupted, className={}, thread={}",
                        this.getClass().getSimpleName(), Thread.currentThread().getName());
                    Thread.currentThread().interrupt();
                } finally {
                    countDownLatch.countDown();
                    log.debug("Consumer analysis templates finished", Thread.currentThread().getName(),
                        this.getClass().getSimpleName(), Thread.currentThread().getName());
                }
            });
        }

        // 生产者，分页获取作业模板列表
        try {
            int total = getTemplateTotal(appId);
            int totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
            log.info("App {} template total: {}, pages: {}", appId, total, totalPages);
            for (int i = 0; i < totalPages; i++) {
                List<ServiceTaskTemplateDTO> templateList = listTaskTemplates(appId, i);
                if (!CollectionUtils.isEmpty(templateList)) {
                    log.debug("Fetched {} templates for app {} page {}/{}", templateList.size(), appId, i, totalPages);
                    queue.put(templateList);
                }
            }
        } catch (Exception e) {
            log.error("App {} Get templateList error: ", appId, e);
        } finally {
            // 空列表结束标识，通知所有消费者结束
            for (int i = 0; i < ANALYSIS_THREAD_COUNT; i++) {
                queue.put(Collections.emptyList());
            }
            log.debug("Producer finished, sent end signals.");
        }
        countDownLatch.await();
        log.debug("Template analysis finished for app {}", appId);
    }

    protected int getTemplateTotal(Long appId) {
        return templateService.countTemplates(appId).getData();
    }

    protected List<ServiceTaskTemplateDTO> listTaskTemplates(Long appId, int pageIndex) {
        BaseSearchCondition condition = new BaseSearchCondition();
        condition.setStart(pageIndex * PAGE_SIZE);
        condition.setLength(PAGE_SIZE);
        PageData<ServiceTaskTemplateDTO> pageData = templateService.listPageTaskTemplates(appId, condition);
        return pageData.getData();
    }
}
