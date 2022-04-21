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

import java.util.List;

/**
 * 服务器滚动分批Resolver
 */
public class RollingBatchServersResolver {

    /**
     * 滚动表达式解析上下文
     */
    private final RollingExprResolveContext context;

    /**
     * Constructor
     *
     * @param servers     滚动执行的主机
     * @param rollingExpr 滚动表达式
     */
    public RollingBatchServersResolver(List<IpDTO> servers, String rollingExpr) {
        this.context = new RollingExprResolveContext(servers, rollingExpr);
    }

    /**
     * 解析滚动表达式，滚动分批
     *
     * @return 服务器分批情况
     */
    public List<RollingServerBatch> resolve() {
        RollingExpr rollingExpr = new RollingExpr(context.getRollingExpr());
        while (context.hasRemainedServer()) {
            context.increaseBatchCount();
            RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(context.getBatchCount());
            List<IpDTO> serversOnBatch = rollingExprPart.compute(context.getTotal(), context.getRemainedServers());
            context.removeResolvedServers(serversOnBatch);
            RollingServerBatch rollingServerBatch = new RollingServerBatch();
            rollingServerBatch.setBatch(context.getBatchCount());
            rollingServerBatch.setServers(serversOnBatch);
            rollingServerBatch.setRollingExprPart(rollingExprPart);
            context.addServerBatch(rollingServerBatch);
        }
        return context.getServerBatches();
    }

}
