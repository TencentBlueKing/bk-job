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

package com.tencent.bk.job.execute.engine.gse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.execute.config.GseConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class GseServer {
    private static final AtomicLong currentIpIndex = new AtomicLong(0);
    private final transient AtomicBoolean isInit = new AtomicBoolean(false);

    private List<Map.Entry<String, Integer>> servers;
    @Autowired
    private GseConfig gseConfig;

    public GseServer() {

    }

    @PostConstruct
    public void init() {
        if (!isInit.get()) {
            Map<String, Integer> maps = Maps.newHashMap();
            log.info("Init gseConfig, config={}", gseConfig);
            String[] gseServerIps = gseConfig.getTaskServerHost().split(",");
            int gseServerPort = gseConfig.getTaskServerPort();
            for (String gseServerIp : gseServerIps) {
                maps.put(gseServerIp, gseServerPort);
            }
            servers = Lists.newArrayList(maps.entrySet());
            isInit.compareAndSet(false, true);
        }
    }

    /**
     * 构建gse访问客户端, 并连接服务端
     */
    public GseClient getClient() {
        return getGseClient(Lists.newArrayList(servers));
    }

    List<Map.Entry<String, Integer>> getGseServer() {
        return servers;
    }

    @SuppressWarnings("all")
    GseClient getGseClient(List<Map.Entry<String, Integer>> servers) {
        if (servers == null) {
            return null;
        }
        int tryTimes = servers.size();
        while (tryTimes > 0) {
            int ipIndex = (int) (currentIpIndex.incrementAndGet() % servers.size());
            if (ipIndex < 0) {
                ipIndex = 0;
                if (currentIpIndex.get() < 0) {
                    currentIpIndex.set(0L);
                }
            }
            Map.Entry<String, Integer> server = servers.get(ipIndex);
            try {
                return GseClient.getClient(server.getKey(), server.getValue());
            } catch (Throwable e) {
                String msg = MessageFormatter.arrayFormat(
                    "Get GseClient fail|{}:{}|msg={}",
                    ArrayUtil.toArray(server.getKey(), server.getValue(), e.getMessage()))
                    .getMessage();
                log.error(msg, e);
                if ((--tryTimes) == 0) {
                    return null;
                }
            }
        }
        return null;
    }

}
