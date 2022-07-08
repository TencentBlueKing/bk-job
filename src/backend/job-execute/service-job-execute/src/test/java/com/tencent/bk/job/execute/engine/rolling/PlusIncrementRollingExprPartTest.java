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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PlusIncrementRollingExprPartTest")
class PlusIncrementRollingExprPartTest {
    private static final PlusIncrementRollingExprPart ROLLING_EXPR_PART = new PlusIncrementRollingExprPart();

    @Test
    @DisplayName("解析非法滚动策略")
    void parseInvalidExpr() {
        RollingExprPart rollingExprPart = ROLLING_EXPR_PART.parseExpr("1%");
        assertThat(rollingExprPart).isNull();

        rollingExprPart = ROLLING_EXPR_PART.parseExpr("test");
        assertThat(rollingExprPart).isNull();

        rollingExprPart = ROLLING_EXPR_PART.parseExpr("-1");
        assertThat(rollingExprPart).isNull();

        assertThrows(RollingExprParseException.class, () -> ROLLING_EXPR_PART.parseExpr("+0"));

        rollingExprPart = ROLLING_EXPR_PART.parseExpr("*2");
        assertThat(rollingExprPart).isNull();
    }

    @Test
    @DisplayName("解析滚动策略")
    void parseValidExpr() {
        RollingExprPart rollingExprPart = ROLLING_EXPR_PART.parseExpr("+10");
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PlusIncrementRollingExprPart.class);
        PlusIncrementRollingExprPart plusIncrementRollingExprPart = (PlusIncrementRollingExprPart) rollingExprPart;
        assertThat(plusIncrementRollingExprPart.getExpr()).isEqualTo("+10");
        assertThat(plusIncrementRollingExprPart.getAddend()).isEqualTo(10);

        rollingExprPart = ROLLING_EXPR_PART.parseExpr("+100");
        assertThat(rollingExprPart).isNotNull();
        assertThat(rollingExprPart).isInstanceOf(PlusIncrementRollingExprPart.class);
        plusIncrementRollingExprPart = (PlusIncrementRollingExprPart) rollingExprPart;
        assertThat(plusIncrementRollingExprPart.getExpr()).isEqualTo("+100");
        assertThat(plusIncrementRollingExprPart.getAddend()).isEqualTo(100);
    }

    @Test
    void compute() {
        List<HostDTO> rollingServers = new ArrayList<>();
        rollingServers.add(new HostDTO(0L, "127.0.0.1"));
        rollingServers.add(new HostDTO(0L, "127.0.0.2"));
        rollingServers.add(new HostDTO(0L, "127.0.0.3"));
        rollingServers.add(new HostDTO(0L, "127.0.0.4"));
        rollingServers.add(new HostDTO(0L, "127.0.0.5"));
        RollingServerBatchContext context = new RollingServerBatchContext(rollingServers);

        List<HostDTO> remainingServers = new ArrayList<>();
        remainingServers.add(new HostDTO(0L, "127.0.0.2"));
        remainingServers.add(new HostDTO(0L, "127.0.0.3"));
        remainingServers.add(new HostDTO(0L, "127.0.0.4"));
        remainingServers.add(new HostDTO(0L, "127.0.0.5"));
        context.setRemainedServers(remainingServers);

        RollingServerBatch preRollingServerBatch = new RollingServerBatch();
        preRollingServerBatch.setBatch(1);
        preRollingServerBatch.setServers(Collections.singletonList(new HostDTO(0L, "127.0.0.1")));
        context.addServerBatch(preRollingServerBatch);
        context.setBatchCount(1);

        PlusIncrementRollingExprPart plusIncrementRollingExprPart =
            (PlusIncrementRollingExprPart) ROLLING_EXPR_PART.parseExpr("+3");
        List<HostDTO> serversOnBatch = plusIncrementRollingExprPart.compute(context);
        assertThat(serversOnBatch).containsSequence(
            new HostDTO(0L, "127.0.0.2"),
            new HostDTO(0L, "127.0.0.3"),
            new HostDTO(0L, "127.0.0.4"),
            new HostDTO(0L, "127.0.0.5")
        );
    }
}
