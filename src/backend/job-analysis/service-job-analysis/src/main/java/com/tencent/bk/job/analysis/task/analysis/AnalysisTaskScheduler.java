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

package com.tencent.bk.job.analysis.task.analysis;

import com.tencent.bk.job.analysis.task.analysis.anotation.AnalysisTask;
import com.tencent.bk.job.analysis.task.analysis.task.IAnalysisTask;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Description
 * @Date 2020/3/6
 * @Version 1.0
 */
@Component
@Slf4j
public class AnalysisTaskScheduler {

    public static final String VALUE_MODULE_ANALYSIS_TASK = "analysisTask";
    public static final String NAME_ANALYSIS_TASK_SCHEDULE_POOL_SIZE = "analysisTask.schedule.pool.size";
    public static final String NAME_ANALYSIS_TASK_SCHEDULE_QUEUE_SIZE = "analysisTask.schedule.queue.size";

    private final ThreadPoolExecutor analysisScheduleExecutor;
    public static List<IAnalysisTask> analysisTaskList = new ArrayList<>();
    public static Map<String, IAnalysisTask> analysisTaskMap = new ConcurrentHashMap<>();
    private static long count = -1L;

    @Autowired
    public AnalysisTaskScheduler(MeterRegistry meterRegistry,
                                 @Qualifier("analysisScheduleExecutor") ThreadPoolExecutor analysisScheduleExecutor) {
        this.analysisScheduleExecutor = analysisScheduleExecutor;
        meterRegistry.gauge(
            NAME_ANALYSIS_TASK_SCHEDULE_POOL_SIZE,
            Collections.singletonList(Tag.of(StatisticsConstants.TAG_KEY_MODULE, VALUE_MODULE_ANALYSIS_TASK)),
            this.analysisScheduleExecutor,
            ThreadPoolExecutor::getPoolSize
        );
        meterRegistry.gauge(
            NAME_ANALYSIS_TASK_SCHEDULE_QUEUE_SIZE,
            Collections.singletonList(Tag.of(StatisticsConstants.TAG_KEY_MODULE, VALUE_MODULE_ANALYSIS_TASK)),
            this.analysisScheduleExecutor,
            threadPoolExecutor -> threadPoolExecutor.getQueue().size()
        );
    }

    public static void findAnalysisTask() {
        ApplicationContext context = ApplicationContextRegister.getContext();
        List<String> beanNames = Arrays.asList(context.getBeanDefinitionNames());
        beanNames.forEach(beanName -> {
            Object bean = context.getBean(beanName);
            log.info("check " + beanName);
            if (bean instanceof IAnalysisTask) {
                log.info("add " + beanName);
                IAnalysisTask taskBean = (IAnalysisTask) bean;
                analysisTaskList.add(taskBean);
                analysisTaskMap.put(bean.getClass().getAnnotation(AnalysisTask.class).value(), taskBean);
            }
        });
        log.info(String.format("There are %d AnalysisTasks", analysisTaskList.size()));
    }

    /**
     * 1s调度一次
     */
    public void schedule() {
        count += 1;
        //找到所有的分析任务
        if (analysisTaskList.isEmpty()) {
            findAnalysisTask();
        }
        //根据任务各自周期进行调度
        analysisTaskList.forEach(analysisTask -> {
            if (analysisTask.getAnalysisTask() == null) {
                //跳过管理员未配置的分析任务
                log.info("Task " + analysisTask.getTaskCode() + " is not configured, skipped");
            } else if (!analysisTask.getAnalysisTask().isActive()) {
                //跳过不活跃的分析任务
                log.info("Task " + analysisTask.getTaskCode() + " is not active, skipped");
            } else {
                if (count % analysisTask.getPeriodSeconds() == 0) {
                    //开一个新线程跑分析任务
                    analysisScheduleExecutor.submit(analysisTask);
                }
            }
        });
    }
}
