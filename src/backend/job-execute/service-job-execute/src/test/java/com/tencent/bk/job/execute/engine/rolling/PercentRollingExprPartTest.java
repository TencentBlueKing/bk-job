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
import com.tencent.bk.job.execute.common.exception.RollingExprParseException;
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

    @Test
    void compute() {
        List<IpDTO> candidateServers = new ArrayList<>();
        candidateServers.add(new IpDTO(0L, "10.0.0.1"));
        candidateServers.add(new IpDTO(0L, "10.0.0.2"));
        candidateServers.add(new IpDTO(0L, "10.0.0.3"));
        candidateServers.add(new IpDTO(0L, "10.0.0.4"));
        candidateServers.add(new IpDTO(0L, "10.0.0.5"));
        candidateServers.add(new IpDTO(0L, "10.0.0.6"));
        candidateServers.add(new IpDTO(0L, "10.0.0.7"));
        candidateServers.add(new IpDTO(0L, "10.0.0.8"));
        candidateServers.add(new IpDTO(0L, "10.0.0.9"));
        candidateServers.add(new IpDTO(0L, "10.0.0.10"));
        candidateServers.add(new IpDTO(0L, "10.0.0.11"));
        candidateServers.add(new IpDTO(0L, "10.0.0.12"));
        candidateServers.add(new IpDTO(0L, "10.0.0.13"));
        candidateServers.add(new IpDTO(0L, "10.0.0.14"));
        candidateServers.add(new IpDTO(0L, "10.0.0.15"));
        candidateServers.add(new IpDTO(0L, "10.0.0.16"));

        PercentRollingExprPart percentRollingExprPart =
            (PercentRollingExprPart) PERCENT_ROLLING_EXPR_PART.parseExpr("10%");
        List<IpDTO> serversOnBatch = percentRollingExprPart.compute(10, candidateServers);
        assertThat(serversOnBatch).hasSize(1);
        assertThat(serversOnBatch).containsSequence(
            new IpDTO(0L, "10.0.0.1")
        );

        percentRollingExprPart =
            (PercentRollingExprPart) PERCENT_ROLLING_EXPR_PART.parseExpr("20%");
        serversOnBatch = percentRollingExprPart.compute(20, candidateServers);
        assertThat(serversOnBatch).hasSize(4);
        assertThat(serversOnBatch).containsSequence(
            new IpDTO(0L, "10.0.0.1"),
            new IpDTO(0L, "10.0.0.2"),
            new IpDTO(0L, "10.0.0.3"),
            new IpDTO(0L, "10.0.0.4")
        );


        percentRollingExprPart =
            (PercentRollingExprPart) PERCENT_ROLLING_EXPR_PART.parseExpr("17%");
        serversOnBatch = percentRollingExprPart.compute(30, candidateServers);
        assertThat(serversOnBatch).hasSize(5);
        assertThat(serversOnBatch).containsSequence(
            new IpDTO(0L, "10.0.0.1"),
            new IpDTO(0L, "10.0.0.2"),
            new IpDTO(0L, "10.0.0.3"),
            new IpDTO(0L, "10.0.0.4"),
            new IpDTO(0L, "10.0.0.5")
        );

        percentRollingExprPart =
            (PercentRollingExprPart) PERCENT_ROLLING_EXPR_PART.parseExpr("100%");
        serversOnBatch = percentRollingExprPart.compute(30, candidateServers);
        assertThat(serversOnBatch).hasSize(16);
        assertThat(serversOnBatch).containsSequence(
            new IpDTO(0L, "10.0.0.1"),
            new IpDTO(0L, "10.0.0.2"),
            new IpDTO(0L, "10.0.0.3"),
            new IpDTO(0L, "10.0.0.4"),
            new IpDTO(0L, "10.0.0.5"),
            new IpDTO(0L, "10.0.0.6"),
            new IpDTO(0L, "10.0.0.7"),
            new IpDTO(0L, "10.0.0.8"),
            new IpDTO(0L, "10.0.0.9"),
            new IpDTO(0L, "10.0.0.10"),
            new IpDTO(0L, "10.0.0.11"),
            new IpDTO(0L, "10.0.0.12"),
            new IpDTO(0L, "10.0.0.13"),
            new IpDTO(0L, "10.0.0.14"),
            new IpDTO(0L, "10.0.0.15"),
            new IpDTO(0L, "10.0.0.16")
        );
    }
}
