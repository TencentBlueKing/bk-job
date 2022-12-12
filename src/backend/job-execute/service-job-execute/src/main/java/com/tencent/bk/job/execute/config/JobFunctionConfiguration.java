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

package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.execute.engine.listener.CallbackListener;
import com.tencent.bk.job.execute.engine.listener.GseTaskListener;
import com.tencent.bk.job.execute.engine.listener.JobListener;
import com.tencent.bk.job.execute.engine.listener.NotifyMsgListener;
import com.tencent.bk.job.execute.engine.listener.ResultHandleResumeListener;
import com.tencent.bk.job.execute.engine.listener.StepListener;
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.ResultHandleTaskResumeEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * spring cloud function 定义
 * <p>
 * 注意：方法名与配置文件中的spring.cloud.function.definition对应，修改需要注意！！！
 */
@Configuration
@Slf4j
public class JobFunctionConfiguration {
    @Bean
    public Consumer<JobEvent> handleJobEvent(@Autowired JobListener jobListener) {
        log.info("Init handleJobEvent consumer");
        return jobListener::handleEvent;
    }

    @Bean
    public Consumer<StepEvent> handleStepEvent(@Autowired StepListener stepListener) {
        log.info("Init handleStepEvent consumer");
        return stepListener::handleEvent;
    }

    @Bean
    public Consumer<GseTaskEvent> handleGseTaskEvent(@Autowired GseTaskListener gseTaskListener) {
        log.info("Init handleGseTaskEvent consumer");
        return gseTaskListener::handleEvent;
    }

    @Bean
    public Consumer<ResultHandleTaskResumeEvent> handleResultHandleResumeEvent(
        @Autowired ResultHandleResumeListener resultHandleResumeListener) {
        log.info("Init handleResultHandleResumeEvent consumer");
        return resultHandleResumeListener::handleEvent;
    }

    @Bean
    public Consumer<TaskNotifyDTO> handleNotifyMsg(@Autowired NotifyMsgListener notifyMsgListener) {
        log.info("Init handleNotifyMsg consumer");
        return notifyMsgListener::handleMessage;
    }

    @Bean
    public Consumer<JobCallbackDTO> handleCallbackMsg(@Autowired CallbackListener callbackListener) {
        log.info("Init handleCallbackMsg consumer");
        return callbackListener::handleMessage;
    }

}
