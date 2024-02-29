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

import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RollingExecuteObjectBatchResolverTest {

    @Test
    @DisplayName("计算滚动批次 - 一个子表达式，按百分比分批")
    void resolveForSinglePercentExpr() {
        List<ExecuteObject> executeObjects = new ArrayList<>();
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L)));

        RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "25%");
        List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
        assertThat(executeObjectBatchList).hasSize(4);

        assertThat(executeObjectBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(executeObjectBatchList.get(0).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L))
        );
        assertThat(executeObjectBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("25%");

        assertThat(executeObjectBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(executeObjectBatchList.get(1).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L))
        );
        assertThat(executeObjectBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("25%");

        assertThat(executeObjectBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(executeObjectBatchList.get(2).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L))
        );
        assertThat(executeObjectBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("25%");

        assertThat(executeObjectBatchList.get(3).getBatch()).isEqualTo(4);
        assertThat(executeObjectBatchList.get(3).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L))
        );
        assertThat(executeObjectBatchList.get(3).getRollingExprPart().getExpr()).isEqualTo("25%");
    }

    @Test
    @DisplayName("计算滚动批次 - 多个子表达式，按百分比分批")
    void resolveForMultiPercentExpr() {
        List<ExecuteObject> executeObjects = new ArrayList<>();
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L)));
        RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "10% 30%");
        List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
        assertThat(executeObjectBatchList).hasSize(4);

        assertThat(executeObjectBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(executeObjectBatchList.get(0).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L))
        );
        assertThat(executeObjectBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("10%");

        assertThat(executeObjectBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(executeObjectBatchList.get(1).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L))
        );
        assertThat(executeObjectBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(executeObjectBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(executeObjectBatchList.get(2).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L))
        );
        assertThat(executeObjectBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(executeObjectBatchList.get(3).getBatch()).isEqualTo(4);
        assertThat(executeObjectBatchList.get(3).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L))
        );
        assertThat(executeObjectBatchList.get(3).getRollingExprPart().getExpr()).isEqualTo("30%");
    }

    @Test
    @DisplayName("计算滚动批次 - 一个子表达式，按数量分批")
    void resolveForSingleQuantityExpr() {
        List<ExecuteObject> executeObjects = new ArrayList<>();
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L)));
        RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "10");
        List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
        assertThat(executeObjectBatchList).hasSize(2);

        assertThat(executeObjectBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(executeObjectBatchList.get(0).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L))
        );
        assertThat(executeObjectBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("10");

        assertThat(executeObjectBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(executeObjectBatchList.get(1).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L))
        );
        assertThat(executeObjectBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("10");
    }

    @Test
    @DisplayName("计算滚动批次 - 多个子表达式，按数量分批")
    void resolveForMultiQuantityExpr() {
        List<ExecuteObject> executeObjects = new ArrayList<>();
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L)));
        RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "1 5");
        List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
        assertThat(executeObjectBatchList).hasSize(4);

        assertThat(executeObjectBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(executeObjectBatchList.get(0).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L))
        );
        assertThat(executeObjectBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("1");

        assertThat(executeObjectBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(executeObjectBatchList.get(1).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L))
        );
        assertThat(executeObjectBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("5");

        assertThat(executeObjectBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(executeObjectBatchList.get(2).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L))
        );
        assertThat(executeObjectBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("5");

        assertThat(executeObjectBatchList.get(3).getBatch()).isEqualTo(4);
        assertThat(executeObjectBatchList.get(3).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L))
        );
        assertThat(executeObjectBatchList.get(3).getRollingExprPart().getExpr()).isEqualTo("5");
    }

    @Test
    @DisplayName("计算滚动批次 - 混合表达式，按数量和百分比分批")
    void resolveForMixedExpr() {
        List<ExecuteObject> executeObjects = new ArrayList<>();
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)));
        RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "1 30%");
        List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
        assertThat(executeObjectBatchList).hasSize(4);

        assertThat(executeObjectBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(executeObjectBatchList.get(0).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L))
        );
        assertThat(executeObjectBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("1");

        assertThat(executeObjectBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(executeObjectBatchList.get(1).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L))
        );
        assertThat(executeObjectBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(executeObjectBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(executeObjectBatchList.get(2).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L))
        );
        assertThat(executeObjectBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(executeObjectBatchList.get(3).getBatch()).isEqualTo(4);
        assertThat(executeObjectBatchList.get(3).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L))

        );
        assertThat(executeObjectBatchList.get(3).getRollingExprPart().getExpr()).isEqualTo("30%");
    }


    @Test
    @DisplayName("计算滚动批次 - 最后一批包含所有")
    void resolveForAllRemainedExpr() {
        List<ExecuteObject> executeObjects = new ArrayList<>();
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)));
        executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L)));
        RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "1 30% " +
            "100%");
        List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
        assertThat(executeObjectBatchList).hasSize(3);

        assertThat(executeObjectBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(executeObjectBatchList.get(0).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(1L))
        );
        assertThat(executeObjectBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("1");

        assertThat(executeObjectBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(executeObjectBatchList.get(1).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(2L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(3L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(4L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(5L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(6L))
        );
        assertThat(executeObjectBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(executeObjectBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(executeObjectBatchList.get(2).getExecuteObjects()).containsSequence(
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(7L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(8L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(9L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(10L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(11L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(12L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(13L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(14L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(15L)),
            ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId(16L))
        );
        assertThat(executeObjectBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("100%");
    }

    @Test
    @DisplayName("计算滚动批次 - 超过最大允许批次 500")
    void resolveExceedMaxBatchSize() {
        List<ExecuteObject> executeObjects = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId((long) i)));
        }
        RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "1");
        assertThatExceptionOfType(FailedPreconditionException.class).isThrownBy(
            context::resolve
        );
    }

    @Nested
    @DisplayName("计算滚动批次性能测试")
    class RollingResolvePerformanceTest {

        @Test
        @DisplayName("计算滚动批次 - 按数量分批，性能测试")
        @Timeout(1)
        void resolveQuantityExpr() {
            List<ExecuteObject> executeObjects = new ArrayList<>();
            for (int i = 1; i <= 50000; i++) {
                executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId((long) i)));
            }
            RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "500");
            List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
            assertThat(executeObjectBatchList).hasSize(100);
        }

        @Test
        @DisplayName("计算滚动批次 - 按比例分批，性能测试")
        @Timeout(1)
        void resolvePercentExpr() {
            List<ExecuteObject> executeObjects = new ArrayList<>();
            for (int i = 1; i <= 50000; i++) {
                executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId((long) i)));
            }
            RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "1%");
            List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
            assertThat(executeObjectBatchList).hasSize(100);
        }

        @Test
        @DisplayName("计算滚动批次 - 按加法递增分批，性能测试")
        @Timeout(1)
        void resolvePlusIncrementRollingExpr() {
            List<ExecuteObject> executeObjects = new ArrayList<>();
            for (int i = 1; i <= 50000; i++) {
                executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId((long) i)));
            }
            RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "+10");
            List<RollingExecuteObjectBatch> executeObjectBatchList = context.resolve();
            assertThat(executeObjectBatchList).hasSize(100);
        }

        @Test
        @DisplayName("计算滚动批次 - 按指数递增分批，性能测试")
        @Timeout(1)
        void resolveExponentIncrRollingExpr() {
            List<ExecuteObject> executeObjects = new ArrayList<>();
            for (int i = 1; i <= 50000; i++) {
                executeObjects.add(ExecuteObject.buildCompatibleExecuteObject(HostDTO.fromHostId((long) i)));
            }
            RollingBatchExecuteObjectsResolver context = new RollingBatchExecuteObjectsResolver(executeObjects, "*2");
            context.resolve();
        }
    }
}
