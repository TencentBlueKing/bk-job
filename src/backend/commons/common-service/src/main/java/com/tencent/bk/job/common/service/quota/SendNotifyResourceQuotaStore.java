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

package com.tencent.bk.job.common.service.quota;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.resource.quota.QuotaResourceId;

/**
 * 资源配额存储-通知发送数量配额限制
 */
public class SendNotifyResourceQuotaStore {

    private final ResourceQuotaStore resourceQuotaStore;

    public SendNotifyResourceQuotaStore(ResourceQuotaStore resourceQuotaStore) {
        this.resourceQuotaStore = resourceQuotaStore;
    }

    public boolean isQuotaLimitEnabled() {
        return resourceQuotaStore.isQuotaLimitEnabled(QuotaResourceId.SEND_NOTIFY);
    }

    /**
     * 根据资源管理空间获取消息通知配额限制
     *
     * @param resourceScope 资源管理空间
     * @return 每天最大消息通知限制
     */
    public long getQuotaLimitByResourceScope(ResourceScope resourceScope) {
        return resourceQuotaStore.getQuotaLimitByResourceScope(QuotaResourceId.SEND_NOTIFY, resourceScope);
    }

    /**
     * 根据用户id获取消息通知配额限制
     *
     * @param userId userId
     * @return 每天最大消息通知限制
     */
    public long getQuotaLimitByUserId(String userId) {
        return resourceQuotaStore.getQuotaLimitByUserId(QuotaResourceId.SEND_NOTIFY, userId);
    }

    /**
     * 获取整个Job系统消息通知配额限制
     *
     * @return 每天最大消息通知限制
     */
    public long getSystemQuotaLimit() {
        return resourceQuotaStore.getSystemQuotaLimit(QuotaResourceId.SEND_NOTIFY);
    }
}
