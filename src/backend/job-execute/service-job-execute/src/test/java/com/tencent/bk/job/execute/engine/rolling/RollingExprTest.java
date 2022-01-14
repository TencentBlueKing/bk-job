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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RollingExprTest {

    @Test
    @DisplayName("解析滚动表达式 - 一个子表达式，按数量分批")
    void nextRollingExprPartForSingleQuantityExpr() {
        RollingExpr rollingExpr = new RollingExpr("1");
        RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(1);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(QuantityRollingExprPart.class);
        QuantityRollingExprPart quantityRollingExprPart = (QuantityRollingExprPart) rollingExprPart;
        assertThat(quantityRollingExprPart.getQuantity()).isEqualTo(1);
        assertThat(quantityRollingExprPart.getExpr()).isEqualTo("1");

        rollingExprPart = rollingExpr.nextRollingExprPart(2);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(QuantityRollingExprPart.class);
        quantityRollingExprPart = (QuantityRollingExprPart) rollingExprPart;
        assertThat(quantityRollingExprPart.getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("解析滚动表达式 - 多个子表达式，按数量分批")
    void nextRollingExprPartForMultiQuantityExpr() {
        RollingExpr rollingExpr = new RollingExpr("1 2 3");

        // 第一批次
        RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(1);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(QuantityRollingExprPart.class);
        QuantityRollingExprPart quantityRollingExprPart = (QuantityRollingExprPart) rollingExprPart;
        assertThat(quantityRollingExprPart.getQuantity()).isEqualTo(1);
        assertThat(quantityRollingExprPart.getExpr()).isEqualTo("1");

        // 第二批次
        rollingExprPart = rollingExpr.nextRollingExprPart(2);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(QuantityRollingExprPart.class);
        quantityRollingExprPart = (QuantityRollingExprPart) rollingExprPart;
        assertThat(quantityRollingExprPart.getQuantity()).isEqualTo(2);
        assertThat(quantityRollingExprPart.getExpr()).isEqualTo("2");

        // 第三批次
        rollingExprPart = rollingExpr.nextRollingExprPart(3);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(QuantityRollingExprPart.class);
        quantityRollingExprPart = (QuantityRollingExprPart) rollingExprPart;
        assertThat(quantityRollingExprPart.getQuantity()).isEqualTo(3);
        assertThat(quantityRollingExprPart.getExpr()).isEqualTo("3");

        // 第四批次
        rollingExprPart = rollingExpr.nextRollingExprPart(4);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(QuantityRollingExprPart.class);
        quantityRollingExprPart = (QuantityRollingExprPart) rollingExprPart;
        assertThat(quantityRollingExprPart.getQuantity()).isEqualTo(3);
        assertThat(quantityRollingExprPart.getExpr()).isEqualTo("3");
    }

    @Test
    @DisplayName("解析滚动表达式 - 一个子表达式，按百分比分批")
    void nextRollingExprPartForSinglePercentExpr() {
        RollingExpr rollingExpr = new RollingExpr("10%");

        RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(1);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        PercentRollingExprPart percentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(percentRollingExprPart.getPercent()).isEqualTo(10);
        assertThat(percentRollingExprPart.getExpr()).isEqualTo("10%");

        rollingExprPart = rollingExpr.nextRollingExprPart(2);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        percentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(percentRollingExprPart.getPercent()).isEqualTo(10);
        assertThat(percentRollingExprPart.getExpr()).isEqualTo("10%");
    }

    @Test
    @DisplayName("解析滚动表达式 - 多个子表达式，按百分比分批")
    void nextRollingExprPartForMultiPercentExpr() {
        RollingExpr rollingExpr = new RollingExpr("10% 20%");

        // 第一批次
        RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(1);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        PercentRollingExprPart percentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(percentRollingExprPart.getPercent()).isEqualTo(10);
        assertThat(percentRollingExprPart.getExpr()).isEqualTo("10%");

        // 第二批次
        rollingExprPart = rollingExpr.nextRollingExprPart(2);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        percentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(percentRollingExprPart.getPercent()).isEqualTo(20);
        assertThat(percentRollingExprPart.getExpr()).isEqualTo("20%");

        // 第三批次
        rollingExprPart = rollingExpr.nextRollingExprPart(3);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        percentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(percentRollingExprPart.getPercent()).isEqualTo(20);
        assertThat(percentRollingExprPart.getExpr()).isEqualTo("20%");
    }

    @Test
    @DisplayName("解析滚动表达式 - 混合数量与百分比")
    void nextRollingExprPartForMixedExpr() {
        RollingExpr rollingExpr = new RollingExpr("1 10%");
        RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(1);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(QuantityRollingExprPart.class);
        QuantityRollingExprPart quantityRollingExprPart = (QuantityRollingExprPart) rollingExprPart;
        assertThat(quantityRollingExprPart.getQuantity()).isEqualTo(1);
        assertThat(quantityRollingExprPart.getExpr()).isEqualTo("1");

        rollingExprPart = rollingExpr.nextRollingExprPart(2);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        PercentRollingExprPart percentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(percentRollingExprPart.getPercent()).isEqualTo(10);

        rollingExprPart = rollingExpr.nextRollingExprPart(3);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        percentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(percentRollingExprPart.getPercent()).isEqualTo(10);
    }

    @Test
    @DisplayName("解析滚动表达式 - 剩下所有表达式解析")
    void nextRollingExprPartForAllRemainedExpr() {
        RollingExpr rollingExpr = new RollingExpr("1 100%");
        RollingExprPart rollingExprPart = rollingExpr.nextRollingExprPart(1);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(QuantityRollingExprPart.class);
        QuantityRollingExprPart quantityRollingExprPart = (QuantityRollingExprPart) rollingExprPart;
        assertThat(quantityRollingExprPart.getQuantity()).isEqualTo(1);
        assertThat(quantityRollingExprPart.getExpr()).isEqualTo("1");

        rollingExprPart = rollingExpr.nextRollingExprPart(2);
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        PercentRollingExprPart percentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(percentRollingExprPart.getPercent()).isEqualTo(100);
    }
}
