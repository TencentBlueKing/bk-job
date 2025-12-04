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

package com.tencent.bk.job.manage.service.host.strategy.impl;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.service.host.strategy.NoTenantListHostStrategy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据策略列表依次查询主机的组合策略，如果所有主机都查询到则停止查询
 *
 * @param <K> 查询主机使用的key类型
 */
public class ComposedNoTenantListHostStrategy<K> implements NoTenantListHostStrategy<K> {

    // 子策略列表
    protected List<NoTenantListHostStrategy<K>> strategyList = new ArrayList<>();

    public void addChildStrategy(NoTenantListHostStrategy<K> strategy) {
        strategyList.add(strategy);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Pair<List<K>, List<ApplicationHostDTO>> listHosts(List<K> keys) {
        if (CollectionUtils.isEmpty(strategyList) || CollectionUtils.isEmpty(keys)) {
            return Pair.of(keys, new ArrayList<>());
        }

        List<ApplicationHostDTO> appHosts = new ArrayList<>();
        List<K> notExistKeys = new ArrayList<>(keys);

        for (NoTenantListHostStrategy<K> strategy : strategyList) {
            Pair<List<K>, List<ApplicationHostDTO>> result = strategy.listHosts(notExistKeys);
            if (CollectionUtils.isNotEmpty(result.getRight())) {
                appHosts.addAll(result.getRight());
            }
            notExistKeys = result.getLeft();
            if (CollectionUtils.isEmpty(notExistKeys)) {
                break;
            }
        }
        return Pair.of(notExistKeys, appHosts);
    }
}
