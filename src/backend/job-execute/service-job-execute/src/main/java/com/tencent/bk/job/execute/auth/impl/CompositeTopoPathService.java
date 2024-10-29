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

package com.tencent.bk.job.execute.auth.impl;

import com.tencent.bk.sdk.iam.service.TopoPathService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 支持从多种来源查询主机拓扑路径的服务，先添加的TopoPathService实例优先级高，若查询失败则遍历列表中其他服务进行查询，直至成功为止
 */
@NoArgsConstructor
@Slf4j
public class CompositeTopoPathService implements TopoPathService {

    private final List<TopoPathService> topoPathServiceList = new ArrayList<>();

    public void addTopoPathService(TopoPathService topoPathService) {
        topoPathServiceList.add(topoPathService);
    }

    @Override
    public Map<String, List<String>> getTopoPathByHostIds(Set<String> hostIds) {
        for (TopoPathService topoPathService : topoPathServiceList) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Use {} to get topoPath, hostIds={}",
                        topoPathService.getClass().getName(),
                        hostIds
                    );
                }
                return topoPathService.getTopoPathByHostIds(hostIds);
            } catch (Exception e) {
                String message = MessageFormatter.format(
                    "Fail to get topoPath by hostIds using {}, hostIds={}",
                    topoPathService.getClass().getName(),
                    hostIds
                ).getMessage();
                log.warn(message, e);
            }
        }
        return Collections.emptyMap();
    }
}
