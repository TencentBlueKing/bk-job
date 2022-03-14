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

package com.tencent.bk.job.manage.auth;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;

import java.util.List;

/**
 * 作业模板相关操作鉴权接口
 */
public interface TemplateAuthService {

    /**
     * 资源范围下创建作业模板鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @return 鉴权结果
     */
    AuthResult authCreateJobTemplate(String username,
                                   AppResourceScope appResourceScope);

    /**
     * 资源范围下查看作业模板鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @return 鉴权结果
     */
    AuthResult authViewJobTemplate(String username,
                                   AppResourceScope appResourceScope,
                                   Long jobTemplateId);

    /**
     * 资源范围下编辑作业模板鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @return 鉴权结果
     */
    AuthResult authEditJobTemplate(String username,
                                   AppResourceScope appResourceScope,
                                   Long jobTemplateId);

    /**
     * 资源范围下删除作业模板鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @return 鉴权结果
     */
    AuthResult authDeleteJobTemplate(String username,
                                     AppResourceScope appResourceScope,
                                     Long jobTemplateId);

    /**
     * 资源范围下调试作业模板鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @return 鉴权结果
     */
    AuthResult authDebugJobTemplate(String username,
                                    AppResourceScope appResourceScope,
                                    Long jobTemplateId);

    /**
     * 资源范围下查看作业模板批量鉴权
     *
     * @param username          用户名
     * @param appResourceScope     资源范围
     * @param jobTemplateIdList 作业模板ID列表
     * @return 有权限的作业模板ID
     */
    List<Long> batchAuthViewJobTemplate(String username,
                                        AppResourceScope appResourceScope,
                                        List<Long> jobTemplateIdList);

    /**
     * 资源范围下编辑作业模板批量鉴权
     *
     * @param username          用户名
     * @param appResourceScope     资源范围
     * @param jobTemplateIdList 作业模板ID列表
     * @return 有权限的作业模板ID
     */
    List<Long> batchAuthEditJobTemplate(String username,
                                        AppResourceScope appResourceScope,
                                        List<Long> jobTemplateIdList);

    /**
     * 资源范围下编辑作业模板批量鉴权并返回鉴权结果
     *
     * @param username          用户名
     * @param appResourceScope     资源范围
     * @param jobTemplateIdList 作业模板ID列表
     * @return 鉴权结果
     */
    AuthResult batchAuthResultEditJobTemplate(String username,
                                        AppResourceScope appResourceScope,
                                        List<Long> jobTemplateIdList);

    /**
     * 资源范围下删除作业模板批量鉴权
     *
     * @param username          用户名
     * @param appResourceScope     资源范围
     * @param jobTemplateIdList 作业模板ID列表
     * @return 有权限的作业模板ID
     */
    List<Long> batchAuthDeleteJobTemplate(String username,
                                          AppResourceScope appResourceScope,
                                          List<Long> jobTemplateIdList);

    /**
     * 注册作业模板实例
     *
     * @param id      资源实例 ID
     * @param name    资源实例名称
     * @param creator 资源实例创建者
     * @return 是否注册成功
     */
    boolean registerTemplate(Long id, String name, String creator);
}
