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

package com.tencent.bk.job.manage.background.ha.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.stereotype.Service;

/**
 * 后台任务监听控制器，用于控制对任务监听的启动与停止
 */
@Slf4j
@Service
public class BackGroundTaskListenerController {
    private final String BINDING_NAME_HANDLE_BACKGROUND_TASK = "handleBackGroundTask-in-0";
    private final BindingsLifecycleController bindingsLifecycleController;

    @Autowired
    public BackGroundTaskListenerController(BindingsLifecycleController bindingsLifecycleController) {
        this.bindingsLifecycleController = bindingsLifecycleController;
    }

    /**
     * 启动监听
     */
    public void start() {
        bindingsLifecycleController.changeState(
            BINDING_NAME_HANDLE_BACKGROUND_TASK,
            BindingsLifecycleController.State.STARTED
        );
        log.info(
            "SetStateOfBinding: bindingName={}, state={}",
            BINDING_NAME_HANDLE_BACKGROUND_TASK,
            BindingsLifecycleController.State.STARTED
        );
    }

    /**
     * 停止监听
     */
    public void stop() {
        bindingsLifecycleController.changeState(
            BINDING_NAME_HANDLE_BACKGROUND_TASK,
            BindingsLifecycleController.State.STOPPED
        );
        log.info(
            "SetStateOfBinding: bindingName={}, state={}",
            BINDING_NAME_HANDLE_BACKGROUND_TASK,
            BindingsLifecycleController.State.STOPPED
        );
    }
}
