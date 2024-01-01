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

package com.tencent.bk.job.execute.model.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 滚动执行-执行对象分批 DO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@PersistenceObject
public class RollingExecuteObjectsBatchDO {
    /**
     * 滚动执行批次
     */
    private Integer batch;
    /**
     * 该批次的执行目标
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容老数据，数据失效后可删除。使用 executeObjects 替换")
    private List<HostDTO> hosts;

    private List<ExecuteObject> executeObjects;

    public RollingExecuteObjectsBatchDO(Integer batch, List<ExecuteObject> executeObjects) {
        this.batch = batch;
        boolean isSupportExecuteObject = executeObjects.get(0).isSupportExecuteObjectFeature();
        if (isSupportExecuteObject) {
            this.executeObjects = executeObjects;
        } else {
            // 执行对象发布兼容
            this.hosts = executeObjects.stream().map(ExecuteObject::getHost).collect(Collectors.toList());
        }
    }

    /**
     * 获取所有执行对象列表(兼容当前版本+历史版本数据）
     */
    @JsonIgnore
    public List<ExecuteObject> getExecuteObjectsCompatibly() {
        if (executeObjects != null) {
            return executeObjects;
        } else if (hosts != null) {
            return hosts.stream().map(ExecuteObject::buildCompatibleExecuteObject).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

}
