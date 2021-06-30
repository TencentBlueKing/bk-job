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

package com.tencent.bk.job.execute.engine.listener;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.message.NotifyMsgProcessor;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;
import com.tencent.bk.job.execute.service.NotifyService;
import com.tencent.bk.job.manage.common.consts.notify.ExecuteStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

/**
 * 任务执行结果通知处理
 */
@Component
@EnableBinding({NotifyMsgProcessor.class})
@Slf4j
public class NotifyMsgListener {
    private final NotifyService notifyService;

    @Autowired
    public NotifyMsgListener(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @StreamListener(NotifyMsgProcessor.INPUT)
    public void handleMessage(TaskNotifyDTO taskNotifyDTO) {
        log.info("Begin to send msg:{}", JsonUtils.toJson(taskNotifyDTO));
        ExecuteStatusEnum executeStatus = ExecuteStatusEnum.get(taskNotifyDTO.getResourceExecuteStatus());
        if (executeStatus == null) {
            log.warn("Empty execute status");
            return;
        }
        switch (executeStatus) {
            case SUCCESS:
                notifyService.notifyTaskSuccess(taskNotifyDTO);
                break;
            case FAIL:
                notifyService.notifyTaskFail(taskNotifyDTO);
                break;
            case READY:
                notifyService.notifyTaskConfirm(taskNotifyDTO);
                break;
            default:
                log.warn("Invalid notification!");
        }
    }
}
