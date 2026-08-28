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

package com.tencent.bk.job.manage.service.template;

import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;

/**
 * 校验作业模板中引用的第三方文件源
 */
public interface FileSourceReferenceService {

    /**
     * 校验作业模板文件步骤引用的第三方文件源（fileType = 3）对该业务可用、且操作者对本业务的文件源有查看权限，
     * 不满足则抛异常。模板未引用第三方文件源时直接返回，不发起任何查询。
     *
     * @param username         操作者用户名
     * @param taskTemplateInfo 待保存的作业模板，须已带上步骤信息
     * @param create           是否为新建。更新时会放行原模板已引用的已禁用文件源
     */
    void validateReferencedFileSources(String username, TaskTemplateInfoDTO taskTemplateInfo, boolean create);
}
