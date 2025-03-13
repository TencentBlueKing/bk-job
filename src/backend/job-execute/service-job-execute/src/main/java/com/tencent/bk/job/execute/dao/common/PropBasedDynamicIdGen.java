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

package com.tencent.bk.job.execute.dao.common;

import com.tencent.bk.job.common.mysql.dynamic.id.IdGenType;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于属性动态控制的ID生成器
 */
@Slf4j
public class PropBasedDynamicIdGen extends AbstractPropBasedDynamicComponent<IdGen> implements IdGen {

    public PropBasedDynamicIdGen(
        AutoIncrementIdGen autoIncrementIdGenerator,
        SegmentIdGen segmentIdGenerator,
        RedisTemplate<String, Object> redisTemplate,
        PropToggleStore propToggleStore) {
        super(redisTemplate, propToggleStore, "execute_id_gen");
        Map<String, IdGen> candidateIdGens = new HashMap<>();
        if (autoIncrementIdGenerator != null) {
            candidateIdGens.put(IdGenType.Constants.AUTO_INCREMENT, autoIncrementIdGenerator);
        }
        if (segmentIdGenerator != null) {
            candidateIdGens.put(IdGenType.Constants.LEAF_SEGMENT, segmentIdGenerator);
        }
        super.initCandidateComponents(candidateIdGens);
    }

    private IdGen currentIdGen() {
        return getCurrent(true);
    }

    @Override
    protected IdGen getComponentByProp(Map<String, IdGen> candidateComponents, String propValue) {
        if (!IdGenType.checkValid(propValue)) {
            log.error("Invalid target IdGenType : {}, skip migration", propValue);
            return null;
        }
        return candidateComponents.get(propValue);
    }

    @Override
    public Long genTaskInstanceId() {
        return currentIdGen().genTaskInstanceId();
    }

    @Override
    public Long genStepInstanceId() {
        return currentIdGen().genStepInstanceId();
    }

    @Override
    public Long genGseTaskId() {
        return currentIdGen().genGseTaskId();
    }

    @Override
    public Long genOperationLogId() {
        return currentIdGen().genOperationLogId();
    }

    @Override
    public Long genFileSourceTaskLogId() {
        return currentIdGen().genFileSourceTaskLogId();
    }

    @Override
    public Long genGseFileExecuteObjTaskId() {
        return currentIdGen().genGseFileExecuteObjTaskId();
    }

    @Override
    public Long genGseScriptExecuteObjTaskId() {
        return currentIdGen().genGseScriptExecuteObjTaskId();
    }

    @Override
    public Long genRollingConfigId() {
        return currentIdGen().genRollingConfigId();
    }

    @Override
    public Long genStepInstanceRollingTaskId() {
        return currentIdGen().genStepInstanceRollingTaskId();
    }

    @Override
    public Long genStepInstanceVariableId() {
        return currentIdGen().genStepInstanceVariableId();
    }

    @Override
    public Long genTaskInstanceVariableId() {
        return currentIdGen().genTaskInstanceVariableId();
    }
}
