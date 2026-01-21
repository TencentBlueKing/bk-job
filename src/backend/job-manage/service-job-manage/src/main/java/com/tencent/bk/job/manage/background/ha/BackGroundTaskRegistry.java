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

package com.tencent.bk.job.manage.background.ha;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 后台任务注册仓库接口
 */
@Service
public interface BackGroundTaskRegistry {

    /**
     * 判断任务是否存在
     *
     * @param uniqueCode 任务编码
     * @return 布尔值
     */
    boolean existsTask(String uniqueCode);

    /**
     * 注册任务
     *
     * @param uniqueCode 任务编码
     * @param task       后台任务
     * @return 是否注册成功
     */
    boolean registerTask(String uniqueCode, BackGroundTask task);

    /**
     * 移除任务
     *
     * @param uniqueCode 任务编码
     * @return 被移除的任务
     */
    BackGroundTask removeTask(String uniqueCode);

    /**
     * 获取所有任务Map
     *
     * @return Map<任务编码, 任务对象>
     */
    Map<String, BackGroundTask> getTaskMap();
}
