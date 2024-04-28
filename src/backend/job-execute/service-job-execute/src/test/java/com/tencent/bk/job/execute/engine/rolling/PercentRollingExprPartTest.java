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

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.exception.RollingExprParseException;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PercentRollingExprPartTest {
    private static final PercentRollingExprPart PERCENT_ROLLING_EXPR_PART = new PercentRollingExprPart();

    @Test
    void parseInvalidExpr() {
        RollingExprPart rollingExprPart = PERCENT_ROLLING_EXPR_PART.parseExpr("1");
        assertThat(rollingExprPart).isNull();

        rollingExprPart = PERCENT_ROLLING_EXPR_PART.parseExpr("test");
        assertThat(rollingExprPart).isNull();

        rollingExprPart = PERCENT_ROLLING_EXPR_PART.parseExpr("2.0%");
        assertThat(rollingExprPart).isNull();

        assertThrows(RollingExprParseException.class, () -> PERCENT_ROLLING_EXPR_PART.parseExpr("0%"));
        assertThrows(RollingExprParseException.class, () -> PERCENT_ROLLING_EXPR_PART.parseExpr("101%"));
    }

    @Test
    void parseValidExpr() {
        RollingExprPart rollingExprPart = PERCENT_ROLLING_EXPR_PART.parseExpr("1%");
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        PercentRollingExprPart PercentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(PercentRollingExprPart.getExpr()).isEqualTo("1%");
        assertThat(PercentRollingExprPart.getPercent()).isEqualTo(1);

        rollingExprPart = PERCENT_ROLLING_EXPR_PART.parseExpr("100%");
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PercentRollingExprPart.class);
        PercentRollingExprPart = (PercentRollingExprPart) rollingExprPart;
        assertThat(PercentRollingExprPart.getExpr()).isEqualTo("100%");
        assertThat(PercentRollingExprPart.getPercent()).isEqualTo(100);
    }

    @Nested
    @DisplayName("验证根据百分比计算执行对象分批大小")
    class ComputeTest {

        @Test
        @DisplayName("验证100%表达式")
        void compute() {
            List<ExecuteObject> rollingExecuteObjects = new ArrayList<>();
            rollingExecuteObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)));
            rollingExecuteObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)));
            rollingExecuteObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)));

            PercentRollingExprPart percentRollingExprPart =
                (PercentRollingExprPart) PERCENT_ROLLING_EXPR_PART.parseExpr("100%");
            RollingExecuteObjectBatchContext context = new RollingExecuteObjectBatchContext(rollingExecuteObjects);
            List<ExecuteObject> executeObjectsOnBatch = percentRollingExprPart.compute(context);
            assertThat(executeObjectsOnBatch).hasSize(3);
            assertThat(executeObjectsOnBatch).containsSequence(
                ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)),
                ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)),
                ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L))
            );
        }

        @Test
        @DisplayName("验证批次计算向上取整")
        void testCeil() {
            List<ExecuteObject> rollingExecuteObjects = new ArrayList<>();
            rollingExecuteObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)));
            rollingExecuteObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)));
            rollingExecuteObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)));
            rollingExecuteObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)));
            rollingExecuteObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)));
            RollingExecuteObjectBatchContext context = new RollingExecuteObjectBatchContext(rollingExecuteObjects);

            PercentRollingExprPart percentRollingExprPart =
                (PercentRollingExprPart) PERCENT_ROLLING_EXPR_PART.parseExpr("10%");
            List<ExecuteObject> executeObjectsOnBatch = percentRollingExprPart.compute(context);
            assertThat(executeObjectsOnBatch).hasSize(1);
            assertThat(executeObjectsOnBatch).containsSequence(
                ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L))
            );

            percentRollingExprPart =
                (PercentRollingExprPart) PERCENT_ROLLING_EXPR_PART.parseExpr("20%");
            executeObjectsOnBatch = percentRollingExprPart.compute(context);
            assertThat(executeObjectsOnBatch).hasSize(1);
            assertThat(executeObjectsOnBatch).containsSequence(
                ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L))
            );

            percentRollingExprPart =
                (PercentRollingExprPart) PERCENT_ROLLING_EXPR_PART.parseExpr("30%");
            executeObjectsOnBatch = percentRollingExprPart.compute(context);
            assertThat(executeObjectsOnBatch).hasSize(2);
            assertThat(executeObjectsOnBatch).containsSequence(
                ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)),
                ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L))
            );
        }
    }

}
