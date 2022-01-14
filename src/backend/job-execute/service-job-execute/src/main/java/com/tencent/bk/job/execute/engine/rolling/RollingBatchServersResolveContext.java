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

package com.tencent.bk.job.execute.engine.rolling;

import com.tencent.bk.job.common.model.dto.IpDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务器滚动分批上下文
 */
public class RollingBatchServersResolveContext {
    /**
     * 需要分批的服务器
     */
    private final List<IpDTO> servers;
    /**
     * 未分批的服务器
     */
    private final List<IpDTO> remainedServers;
    /**
     * 滚动策略表达式
     */
    private final String rollingExpr;
    /**
     * 需要分批的服务器数量
     */
    private final int total;
    /**
     * 分批数量
     */
    private int batchCount;
    /**
     * 分批结果
     */
    private final List<RollingBatchServers> serverBatches;

    public RollingBatchServersResolveContext(List<IpDTO> servers, String rollingExpr) {
        this.servers = servers;
        this.remainedServers = new ArrayList<>(this.servers);
        this.rollingExpr = rollingExpr;
        this.total = servers.size();
        this.serverBatches = new ArrayList<>();
    }

    public List<RollingBatchServers> resolve() {
        RollingExpr rollingExpr = new RollingExpr(this.rollingExpr);
        while (hasRemainedServer()) {
            this.batchCount++;
            RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(this.batchCount);
            List<IpDTO> serversOnBatch = rollingExprPart.compute(this.total, this.remainedServers);
            this.remainedServers.removeAll(serversOnBatch);
            RollingBatchServers rollingBatchServers = new RollingBatchServers();
            rollingBatchServers.setBatch(this.batchCount);
            rollingBatchServers.setServers(serversOnBatch);
            rollingBatchServers.setRollingExprPart(rollingExprPart);
            this.serverBatches.add(rollingBatchServers);
        }
        return this.serverBatches;
    }

    private boolean hasRemainedServer() {
        return this.remainedServers.size() > 0;
    }

}
