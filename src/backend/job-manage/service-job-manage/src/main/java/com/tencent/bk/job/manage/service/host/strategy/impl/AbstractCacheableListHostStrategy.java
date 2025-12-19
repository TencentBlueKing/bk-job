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
import com.tencent.bk.job.manage.manager.host.HostCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 支持刷新缓存的查询主机策略
 *
 * @param <T> 主机Key类型
 */
@Slf4j
public abstract class AbstractCacheableListHostStrategy<T> {
    private final HostCache hostCache;

    public AbstractCacheableListHostStrategy(HostCache hostCache) {
        this.hostCache = hostCache;
    }

    /**
     * 查询主机并刷新缓存
     *
     * @param loadHostsFuncName 加载主机数据函数名称
     * @param loadHostsFunc     加载主机数据函数
     * @param extractKeyFunc    提取主机key的函数
     * @return Pair<主机Key列表, 主机列表>
     */
    protected Pair<List<T>, List<ApplicationHostDTO>> listHostsAndRefreshCache(
        String loadHostsFuncName,
        Function<Void, Pair<List<T>, List<ApplicationHostDTO>>> loadHostsFunc,
        Function<ApplicationHostDTO, T> extractKeyFunc
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostDTO> appHosts = new ArrayList<>();

        watch.start(loadHostsFuncName);
        Pair<List<T>, List<ApplicationHostDTO>> resultPair = loadHostsFunc.apply(null);
        List<T> keyList = resultPair.getLeft();
        List<T> notExistKeys = new ArrayList<>(keyList);
        List<ApplicationHostDTO> hostsInDb = resultPair.getRight();
        watch.stop();

        if (CollectionUtils.isNotEmpty(hostsInDb)) {
            watch.start("addHostsToCache");
            for (ApplicationHostDTO appHost : hostsInDb) {
                if (appHost.getBizId() == null || appHost.getBizId() <= 0) {
                    log.info("Host: {}|{} missing bizId, skip!", appHost.getHostId(), appHost.getCloudIp());
                    // 加载的主机可能没有业务信息(依赖的主机事件还没有处理),那么暂时跳过该主机
                    continue;
                }
                notExistKeys.remove(extractKeyFunc.apply(appHost));
                appHosts.add(appHost);
            }
            hostCache.batchAddOrUpdateHosts(appHosts);
            watch.stop();
        }

        if (watch.getTotalTimeMillis() > 3000) {
            log.warn(
                "{} and updateCache slow, hostSize: {}, cost: {}",
                loadHostsFuncName,
                keyList.size(),
                watch.prettyPrint()
            );
        }
        return Pair.of(notExistKeys, appHosts);
    }
}
