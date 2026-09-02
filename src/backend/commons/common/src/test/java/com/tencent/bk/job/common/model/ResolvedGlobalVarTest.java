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

package com.tencent.bk.job.common.model;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局变量概要的两条不变式：主机清单按上限封顶、变量取值按长度封顶。
 * <p>
 * 二者都收在模型里而不是各下游各写一遍，这里守住的就是"下游少写一处判断也不会写出不一致的单据、
 * 更不会让单个变量取值把整份概要撑到落库失败"
 */
@DisplayName("ResolvedGlobalVar 主机清单与取值封顶测试")
class ResolvedGlobalVarTest {

    @Test
    @DisplayName("主机台数超过上限时清掉清单、只留总数")
    void hostListIsDroppedWhenExceedingLimit() {
        ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
        for (int i = 0; i <= ResolvedSummary.MAX_DISPLAY_ITEM_COUNT; i++) {
            globalVar.addHost((long) i, "0:127.0.0." + i);
        }

        assertThat(globalVar.getHosts()).isNull();
        assertThat(globalVar.getHostCount()).isEqualTo(ResolvedSummary.MAX_DISPLAY_ITEM_COUNT + 1);
    }

    @Test
    @DisplayName("台数正好到上限时仍逐台列出")
    void hostListIsKeptAtLimit() {
        ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
        for (int i = 0; i < ResolvedSummary.MAX_DISPLAY_ITEM_COUNT; i++) {
            globalVar.addHost((long) i, "0:127.0.0." + i);
        }

        assertThat(globalVar.getHosts()).hasSize(ResolvedSummary.MAX_DISPLAY_ITEM_COUNT);
        assertThat(globalVar.getHostCount()).isEqualTo(ResolvedSummary.MAX_DISPLAY_ITEM_COUNT);
    }

    @Test
    @DisplayName("无IP时用主机ID兜底展示，两者都没有则不计入台数")
    void hostIdIsUsedWhenCloudIpAbsent() {
        ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
        globalVar.addHost(100L, null);
        globalVar.addHost(null, " ");

        assertThat(globalVar.getHosts()).containsExactly("host_id:100");
        assertThat(globalVar.getHostCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("超长变量取值被截断")
    void overlongVarValueIsTruncated() {
        ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
        globalVar.setValue(StringUtils.repeat('a', ResolvedSummary.MAX_DISPLAY_VALUE_LENGTH + 100));

        assertThat(globalVar.getValue()).hasSize(ResolvedSummary.MAX_DISPLAY_VALUE_LENGTH + 1);
        assertThat(globalVar.getValue()).endsWith("…");
    }

    @Test
    @DisplayName("主机类变量台数合计只算静态主机，动态目标算不出台数不计入")
    void totalGlobalVarHostCountSumsStaticHostsOnly() {
        ResolvedSummary summary = new ResolvedSummary();
        assertThat(summary.totalGlobalVarHostCount()).isZero();

        ResolvedSummary.ResolvedGlobalVar staticVar = new ResolvedSummary.ResolvedGlobalVar();
        staticVar.addHost(1L, "0:127.0.0.1");
        staticVar.addHost(2L, "0:127.0.0.2");
        summary.addGlobalVar(staticVar);
        ResolvedSummary.ResolvedGlobalVar dynamicVar = new ResolvedSummary.ResolvedGlobalVar();
        dynamicVar.setDynamicGroupCount(3);
        dynamicVar.setTopoNodeCount(4);
        summary.addGlobalVar(dynamicVar);

        assertThat(summary.totalGlobalVarHostCount()).isEqualTo(2);
    }
}
