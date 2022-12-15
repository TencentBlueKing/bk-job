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

package com.tencent.bk.job.execute.engine.evict;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.inner.AppCodeTaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.AppIdTaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.ComposedTaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.TaskEvictPolicyDTO;
import com.tencent.bk.job.execute.model.inner.TaskInstanceIdEvictPolicyDTO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskEvictPolicyTest {

    @Test
    public void testTaskEvictPolicy() {
        TaskEvictPolicyDTO taskInstanceIdPolicy = new TaskInstanceIdEvictPolicyDTO(Arrays.asList(1001L, 1002L));
        TaskEvictPolicyDTO appIdPolicy = new AppIdTaskEvictPolicyDTO(Arrays.asList(2L, 3L));
        TaskEvictPolicyDTO appCodePolicy = new AppCodeTaskEvictPolicyDTO(Arrays.asList("appCode1", "appCode2"));
        // 测试策略OR组合
        ComposedTaskEvictPolicyDTO composedPolicyDTO = new ComposedTaskEvictPolicyDTO(
            ComposedTaskEvictPolicy.ComposeOperator.OR,
            taskInstanceIdPolicy,
            appIdPolicy,
            appCodePolicy
        );
        // 测试序列化与反序列化
        String jsonStr = JsonUtils.toJson(composedPolicyDTO);
        System.out.println(jsonStr);
        composedPolicyDTO = JsonUtils.fromJson(jsonStr, new TypeReference<ComposedTaskEvictPolicyDTO>() {
        });

        ITaskEvictPolicy composedPolicy = new ComposedTaskEvictPolicy(composedPolicyDTO);
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        // 空值检验
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(false);
        // 负例
        taskInstance.setAppCode("appCode3");
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(false);
        taskInstance.setId(1003L);
        taskInstance.setAppId(20L);
        taskInstance.setAppCode("appCode3");
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(false);
        // 正例
        taskInstance.setId(1003L);
        taskInstance.setAppId(2L);
        taskInstance.setAppCode("appCode3");
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(true);
        taskInstance.setId(1003L);
        taskInstance.setAppId(20L);
        taskInstance.setAppCode("appCode2");
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(true);
        taskInstance.setId(1002L);
        taskInstance.setAppId(20L);
        taskInstance.setAppCode("appCode4");
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(true);

        // 测试策略AND组合
        composedPolicyDTO = new ComposedTaskEvictPolicy(
            ComposedTaskEvictPolicy.ComposeOperator.AND,
            taskInstanceIdPolicy,
            appIdPolicy,
            appCodePolicy
        );
        // 测试序列化与反序列化
        jsonStr = JsonUtils.toJson(composedPolicyDTO);
        System.out.println(jsonStr);
        composedPolicyDTO = JsonUtils.fromJson(jsonStr, new TypeReference<ComposedTaskEvictPolicy>() {
        });
        composedPolicy = new ComposedTaskEvictPolicy(composedPolicyDTO);
        // 负例
        taskInstance.setId(1001L);
        taskInstance.setAppId(2L);
        taskInstance.setAppCode("appCode3");
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(false);
        taskInstance.setId(1001L);
        taskInstance.setAppId(20L);
        taskInstance.setAppCode("appCode2");
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(false);
        // 正例
        taskInstance.setId(1001L);
        taskInstance.setAppId(2L);
        taskInstance.setAppCode("appCode1");
        assertThat(composedPolicy.needToEvict(taskInstance)).isEqualTo(true);
    }
}
