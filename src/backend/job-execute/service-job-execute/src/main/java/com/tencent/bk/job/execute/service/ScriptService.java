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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;

/**
 * 脚本Service
 */
public interface ScriptService {
    /**
     * 根据id获取脚本（某个版本）
     *
     * @param username        用户
     * @param appId           业务ID
     * @param scriptVersionId 脚本版本 ID
     * @return 脚本信息
     */
    ServiceScriptDTO getScriptByScriptVersionId(String username, long appId,
                                                long scriptVersionId) throws ServiceException;

    /**
     * 根据id获取脚本（某个版本）
     *
     * @param scriptVersionId 脚本版本 ID
     * @return 脚本信息
     */
    ServiceScriptDTO getScriptByScriptVersionId(long scriptVersionId) throws ServiceException;

    /**
     * 获取脚本基本信息
     *
     * @param scriptId 脚本ID
     * @return 脚本基本信息
     */
    ServiceScriptDTO getBasicScriptInfo(String scriptId);

    /**
     * 获取已上线版本
     *
     * @param scriptId 脚本ID
     * @return 已上线的版本；如果没有，返回null
     */
    ServiceScriptDTO getOnlineScriptVersion(String scriptId);
}
