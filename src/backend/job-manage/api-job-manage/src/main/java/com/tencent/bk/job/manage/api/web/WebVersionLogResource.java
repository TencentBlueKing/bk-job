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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 作业平台版本日志相关Web API
 */
@Api(tags = {"job-manage:web:version_log"})
@RequestMapping("/web/versionLog")
@WebAPI
public interface WebVersionLogResource {

    /**
     * 按语言获取版本日志。
     * <p>返回数据为JSON数组，数组元素按version倒序排列（新版本在前）。</p>
     * <p>示例返回：</p>
     *
     * <pre>
     * [
     *   {
     *     "content": "新增xxx\n优化xxx\n修复xxx",
     *     "version": "V3.11.3",
     *     "time": "2025-02-04"
     *   },
     *   {
     *     "content": "新增xxx\n优化xxx\n修复xxx",
     *     "version": "V3.10.2",
     *     "time": "2024-11-04"
     *   }
     * ]
     * </pre>
     *
     * <p>字段说明：</p>
     * <ul>
     *   <li><b>content</b>：版本更新日志内容</li>
     *   <li><b>version</b>：版本号</li>
     *   <li><b>time</b>：版本发布时间</li>
     * </ul>
     */
    @ApiOperation(value = "按语言获取版本日志", produces = "application/json")
    @GetMapping
    Response<Object> getVersionLog();
}
