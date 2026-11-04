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

package com.tencent.bk.job.manage.service;

/**
 * 租户初始化服务
 */
public interface TenantInitService {

    /**
     * 初始化指定租户，无条件执行，仅通过分布式锁保证多实例互斥。
     * 供OP接口调用，同时也是自动初始化失败后的人工兜底入口。
     * <p>
     * 本方法不写入初始化完成标记，因此通过本方法初始化过的租户，
     * 在下一次实例启动时仍会被{@link #initTenantOnce}再执行一次（初始化步骤幂等，重复执行安全）。
     *
     * @param tenantId 租户ID
     * @return true：初始化成功；null：已有其他实例正在执行
     * @throws Exception 初始化过程中的异常，由调用方决定如何转换为响应
     */
    Boolean initTenant(String tenantId) throws Exception;

    /**
     * 初始化指定租户，保证整个软件生命周期内至多成功执行一次，成功后写入永不过期的完成标记。
     * 供系统租户自动初始化任务调用，内部捕获所有异常，不向外抛出。
     *
     * @param tenantId 租户ID
     * @return 本次初始化的结果
     */
    TenantInitResult initTenantOnce(String tenantId);
}
