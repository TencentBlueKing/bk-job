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

package com.tencent.bk.job.common.context;

import com.tencent.bk.job.common.model.User;
import io.micrometer.core.instrument.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JobContext#copyForChildThread()} 单元测试。
 */
class JobContextTest {

    @Test
    void copyForChildThreadShouldCopyImmutableFieldsByReference() {
        JobContext parent = newPopulatedContext();

        JobContext child = parent.copyForChildThread();

        // 不可变 / 只读字段直接复用父线程引用
        assertThat(child).isNotSameAs(parent);
        assertThat(child.getStartTime()).isEqualTo(parent.getStartTime());
        assertThat(child.getRequestId()).isEqualTo(parent.getRequestId());
        assertThat(child.getUserLang()).isEqualTo(parent.getUserLang());
        assertThat(child.getRequestFrom()).isEqualTo(parent.getRequestFrom());
        assertThat(child.getTimeZone()).isEqualTo(parent.getTimeZone());
        assertThat(child.getAllowMigration()).isEqualTo(parent.getAllowMigration());
        assertThat(child.getControllerClassName()).isEqualTo(parent.getControllerClassName());
        assertThat(child.getUser()).isSameAs(parent.getUser());
    }

    @Test
    void copyForChildThreadShouldResetMutableCollections() {
        JobContext parent = newPopulatedContext();
        parent.setMetricTagsMap(new HashMap<>());
        parent.getMetricTagsMap().put("scope1", Pair.of("name", new ArrayList<>()));
        parent.setDebugMessage(new ArrayList<>());
        parent.getDebugMessage().add("debug from parent");
        parent.setHttpMetricTags(new ArrayList<>());
        parent.getHttpMetricTags().add(Tag.of("k", "v"));

        JobContext child = parent.copyForChildThread();

        // 可变集合字段在子线程中应被重置，避免与父线程共享同一份集合
        assertThat(child.getMetricTagsMap()).isNull();
        assertThat(child.getDebugMessage()).isNull();
        assertThat(child.getHttpMetricTags()).isNull();
    }

    @Test
    void mutatingChildShouldNotAffectParent() {
        JobContext parent = newPopulatedContext();
        parent.setMetricTagsMap(new HashMap<>());

        JobContext child = parent.copyForChildThread();
        child.setMetricTagsMap(new HashMap<>());
        child.getMetricTagsMap().put("scope-child", Pair.of("name", new ArrayList<>()));

        // 子线程写入自己的 metricTagsMap，父线程 map 必须保持原样
        assertThat(parent.getMetricTagsMap()).isEmpty();
    }

    private JobContext newPopulatedContext() {
        JobContext ctx = new JobContext();
        ctx.setStartTime(1000L);
        ctx.setRequestId("req-1");
        ctx.setUserLang("zh-CN");
        ctx.setRequestFrom("web");
        ctx.setTimeZone(ZoneId.of("Asia/Shanghai"));
        ctx.setAllowMigration(true);
        ctx.setControllerClassName("FooController");
        User user = new User();
        user.setUsername("alice");
        ctx.setUser(user);
        return ctx;
    }
}
