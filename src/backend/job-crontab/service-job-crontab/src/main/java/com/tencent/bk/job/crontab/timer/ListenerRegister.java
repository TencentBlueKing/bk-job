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

package com.tencent.bk.job.crontab.timer;

import com.tencent.bk.job.crontab.timer.listener.AbstractJobListener;
import com.tencent.bk.job.crontab.timer.listener.AbstractSchedulerListener;
import com.tencent.bk.job.crontab.timer.listener.AbstractTriggerListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@Lazy(false)
public class ListenerRegister implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;

    private Scheduler scheduler;

    @Autowired
    public ListenerRegister(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void afterPropertiesSet() {
        awareSchedulerListeners();
        awareJobListeners();
        awareTriggerListeners();
    }

    private void awareSchedulerListeners() {
        log.info("Start Loading SchedulerListener");
        Map<String, AbstractSchedulerListener> schedulerListenerMap =
            applicationContext.getBeansOfType(AbstractSchedulerListener.class);

        if (MapUtils.isEmpty(schedulerListenerMap)) {
            log.info("No SchedulerListener to load!");
            return;
        }

        schedulerListenerMap.values().forEach(listener -> {
            try {
                scheduler.getListenerManager().addSchedulerListener(listener);
            } catch (SchedulerException e) {
                log.error("Error while register scheduler listener!", e);
            }
        });

        log.info("Loading SchedulerListener Finished");
    }

    private void awareJobListeners() {
        log.info("Start Loading JobListener");
        Map<String, AbstractJobListener> jobListenerMap = applicationContext.getBeansOfType(AbstractJobListener.class);

        if (MapUtils.isEmpty(jobListenerMap)) {
            log.info("No JobListener!");
            return;
        }

        jobListenerMap.values().forEach(listener -> {
            try {
                scheduler.getListenerManager().addJobListener(listener);
            } catch (SchedulerException e) {
                log.error("Error while register job listener!", e);
            }
        });

        log.info("Loading JobListener Finished");
    }

    private void awareTriggerListeners() {
        log.info("Start Loading TriggerListener");
        Map<String, AbstractTriggerListener> triggerListenerMap =
            applicationContext.getBeansOfType(AbstractTriggerListener.class);

        if (MapUtils.isEmpty(triggerListenerMap)) {
            log.info("No TriggerListener!");
            return;
        }

        triggerListenerMap.values().forEach(listener -> {
            try {
                scheduler.getListenerManager().addTriggerListener(listener);
            } catch (SchedulerException e) {
                log.error("Error while register trigger listener!", e);
            }
        });

        log.info("Loading TriggerListener Finished");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
