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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RollingServerBatchResolverTest {

    @Test
    @DisplayName("计算滚动批次 - 一个子表达式，按百分比分批")
    void resolveForSinglePercentExpr() {
        List<IpDTO> servers = new ArrayList<>();
        servers.add(new IpDTO(0L, "127.0.0.1"));
        servers.add(new IpDTO(0L, "127.0.0.2"));
        servers.add(new IpDTO(0L, "127.0.0.3"));
        servers.add(new IpDTO(0L, "127.0.0.4"));
        servers.add(new IpDTO(0L, "127.0.0.5"));
        servers.add(new IpDTO(0L, "127.0.0.6"));
        servers.add(new IpDTO(0L, "127.0.0.7"));
        servers.add(new IpDTO(0L, "127.0.0.8"));
        servers.add(new IpDTO(0L, "127.0.0.9"));
        servers.add(new IpDTO(0L, "127.0.0.10"));
        servers.add(new IpDTO(0L, "127.0.0.11"));
        servers.add(new IpDTO(0L, "127.0.0.12"));
        servers.add(new IpDTO(0L, "127.0.0.13"));
        servers.add(new IpDTO(0L, "127.0.0.14"));
        servers.add(new IpDTO(0L, "127.0.0.15"));
        servers.add(new IpDTO(0L, "127.0.0.16"));
        RollingBatchServersResolver context = new RollingBatchServersResolver(servers, "25%");
        List<RollingServerBatch> serverBatchList = context.resolve();
        assertThat(serverBatchList).hasSize(4);

        assertThat(serverBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(serverBatchList.get(0).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.1"),
            new IpDTO(0L, "127.0.0.2"),
            new IpDTO(0L, "127.0.0.3"),
            new IpDTO(0L, "127.0.0.4")
        );
        assertThat(serverBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("25%");

        assertThat(serverBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(serverBatchList.get(1).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.5"),
            new IpDTO(0L, "127.0.0.6"),
            new IpDTO(0L, "127.0.0.7"),
            new IpDTO(0L, "127.0.0.8")
        );
        assertThat(serverBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("25%");

        assertThat(serverBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(serverBatchList.get(2).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.9"),
            new IpDTO(0L, "127.0.0.10"),
            new IpDTO(0L, "127.0.0.11"),
            new IpDTO(0L, "127.0.0.12")
        );
        assertThat(serverBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("25%");

        assertThat(serverBatchList.get(3).getBatch()).isEqualTo(4);
        assertThat(serverBatchList.get(3).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.13"),
            new IpDTO(0L, "127.0.0.14"),
            new IpDTO(0L, "127.0.0.15"),
            new IpDTO(0L, "127.0.0.16")
        );
        assertThat(serverBatchList.get(3).getRollingExprPart().getExpr()).isEqualTo("25%");
    }

    @Test
    @DisplayName("计算滚动批次 - 多个子表达式，按百分比分批")
    void resolveForMultiPercentExpr() {
        List<IpDTO> servers = new ArrayList<>();
        servers.add(new IpDTO(0L, "127.0.0.1"));
        servers.add(new IpDTO(0L, "127.0.0.2"));
        servers.add(new IpDTO(0L, "127.0.0.3"));
        servers.add(new IpDTO(0L, "127.0.0.4"));
        servers.add(new IpDTO(0L, "127.0.0.5"));
        servers.add(new IpDTO(0L, "127.0.0.6"));
        servers.add(new IpDTO(0L, "127.0.0.7"));
        servers.add(new IpDTO(0L, "127.0.0.8"));
        servers.add(new IpDTO(0L, "127.0.0.9"));
        servers.add(new IpDTO(0L, "127.0.0.10"));
        servers.add(new IpDTO(0L, "127.0.0.11"));
        servers.add(new IpDTO(0L, "127.0.0.12"));
        servers.add(new IpDTO(0L, "127.0.0.13"));
        servers.add(new IpDTO(0L, "127.0.0.14"));
        servers.add(new IpDTO(0L, "127.0.0.15"));
        servers.add(new IpDTO(0L, "127.0.0.16"));
        RollingBatchServersResolver context = new RollingBatchServersResolver(servers, "10% 30%");
        List<RollingServerBatch> serverBatchList = context.resolve();
        assertThat(serverBatchList).hasSize(4);

        assertThat(serverBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(serverBatchList.get(0).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.1"),
            new IpDTO(0L, "127.0.0.2")
        );
        assertThat(serverBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("10%");

        assertThat(serverBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(serverBatchList.get(1).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.3"),
            new IpDTO(0L, "127.0.0.4"),
            new IpDTO(0L, "127.0.0.5"),
            new IpDTO(0L, "127.0.0.6"),
            new IpDTO(0L, "127.0.0.7")
        );
        assertThat(serverBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(serverBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(serverBatchList.get(2).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.8"),
            new IpDTO(0L, "127.0.0.9"),
            new IpDTO(0L, "127.0.0.10"),
            new IpDTO(0L, "127.0.0.11"),
            new IpDTO(0L, "127.0.0.12")
        );
        assertThat(serverBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(serverBatchList.get(3).getBatch()).isEqualTo(4);
        assertThat(serverBatchList.get(3).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.13"),
            new IpDTO(0L, "127.0.0.14"),
            new IpDTO(0L, "127.0.0.15"),
            new IpDTO(0L, "127.0.0.16")
        );
        assertThat(serverBatchList.get(3).getRollingExprPart().getExpr()).isEqualTo("30%");
    }

    @Test
    @DisplayName("计算滚动批次 - 一个子表达式，按数量分批")
    void resolveForSingleQuantityExpr() {
        List<IpDTO> servers = new ArrayList<>();
        servers.add(new IpDTO(0L, "127.0.0.1"));
        servers.add(new IpDTO(0L, "127.0.0.2"));
        servers.add(new IpDTO(0L, "127.0.0.3"));
        servers.add(new IpDTO(0L, "127.0.0.4"));
        servers.add(new IpDTO(0L, "127.0.0.5"));
        servers.add(new IpDTO(0L, "127.0.0.6"));
        servers.add(new IpDTO(0L, "127.0.0.7"));
        servers.add(new IpDTO(0L, "127.0.0.8"));
        servers.add(new IpDTO(0L, "127.0.0.9"));
        servers.add(new IpDTO(0L, "127.0.0.10"));
        servers.add(new IpDTO(0L, "127.0.0.11"));
        servers.add(new IpDTO(0L, "127.0.0.12"));
        servers.add(new IpDTO(0L, "127.0.0.13"));
        servers.add(new IpDTO(0L, "127.0.0.14"));
        servers.add(new IpDTO(0L, "127.0.0.15"));
        servers.add(new IpDTO(0L, "127.0.0.16"));
        RollingBatchServersResolver context = new RollingBatchServersResolver(servers, "10");
        List<RollingServerBatch> serverBatchList = context.resolve();
        assertThat(serverBatchList).hasSize(2);

        assertThat(serverBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(serverBatchList.get(0).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.1"),
            new IpDTO(0L, "127.0.0.2"),
            new IpDTO(0L, "127.0.0.3"),
            new IpDTO(0L, "127.0.0.4"),
            new IpDTO(0L, "127.0.0.5"),
            new IpDTO(0L, "127.0.0.6"),
            new IpDTO(0L, "127.0.0.7"),
            new IpDTO(0L, "127.0.0.8"),
            new IpDTO(0L, "127.0.0.9"),
            new IpDTO(0L, "127.0.0.10")
        );
        assertThat(serverBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("10");

        assertThat(serverBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(serverBatchList.get(1).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.11"),
            new IpDTO(0L, "127.0.0.12"),
            new IpDTO(0L, "127.0.0.13"),
            new IpDTO(0L, "127.0.0.14"),
            new IpDTO(0L, "127.0.0.15"),
            new IpDTO(0L, "127.0.0.16")
        );
        assertThat(serverBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("10");
    }

    @Test
    @DisplayName("计算滚动批次 - 多个子表达式，按数量分批")
    void resolveForMultiQuantityExpr() {
        List<IpDTO> servers = new ArrayList<>();
        servers.add(new IpDTO(0L, "127.0.0.1"));
        servers.add(new IpDTO(0L, "127.0.0.2"));
        servers.add(new IpDTO(0L, "127.0.0.3"));
        servers.add(new IpDTO(0L, "127.0.0.4"));
        servers.add(new IpDTO(0L, "127.0.0.5"));
        servers.add(new IpDTO(0L, "127.0.0.6"));
        servers.add(new IpDTO(0L, "127.0.0.7"));
        servers.add(new IpDTO(0L, "127.0.0.8"));
        servers.add(new IpDTO(0L, "127.0.0.9"));
        servers.add(new IpDTO(0L, "127.0.0.10"));
        servers.add(new IpDTO(0L, "127.0.0.11"));
        servers.add(new IpDTO(0L, "127.0.0.12"));
        servers.add(new IpDTO(0L, "127.0.0.13"));
        servers.add(new IpDTO(0L, "127.0.0.14"));
        servers.add(new IpDTO(0L, "127.0.0.15"));
        servers.add(new IpDTO(0L, "127.0.0.16"));
        RollingBatchServersResolver context = new RollingBatchServersResolver(servers, "1 5");
        List<RollingServerBatch> serverBatchList = context.resolve();
        assertThat(serverBatchList).hasSize(4);

        assertThat(serverBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(serverBatchList.get(0).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.1")
        );
        assertThat(serverBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("1");

        assertThat(serverBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(serverBatchList.get(1).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.2"),
            new IpDTO(0L, "127.0.0.3"),
            new IpDTO(0L, "127.0.0.4"),
            new IpDTO(0L, "127.0.0.5"),
            new IpDTO(0L, "127.0.0.6")
        );
        assertThat(serverBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("5");

        assertThat(serverBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(serverBatchList.get(2).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.7"),
            new IpDTO(0L, "127.0.0.8"),
            new IpDTO(0L, "127.0.0.9"),
            new IpDTO(0L, "127.0.0.10"),
            new IpDTO(0L, "127.0.0.11")
        );
        assertThat(serverBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("5");

        assertThat(serverBatchList.get(3).getBatch()).isEqualTo(4);
        assertThat(serverBatchList.get(3).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.12"),
            new IpDTO(0L, "127.0.0.13"),
            new IpDTO(0L, "127.0.0.14"),
            new IpDTO(0L, "127.0.0.15"),
            new IpDTO(0L, "127.0.0.16")
        );
        assertThat(serverBatchList.get(3).getRollingExprPart().getExpr()).isEqualTo("5");
    }

    @Test
    @DisplayName("计算滚动批次 - 混合表达式，按数量和百分比分批")
    void resolveForMixedExpr() {
        List<IpDTO> servers = new ArrayList<>();
        servers.add(new IpDTO(0L, "127.0.0.1"));
        servers.add(new IpDTO(0L, "127.0.0.2"));
        servers.add(new IpDTO(0L, "127.0.0.3"));
        servers.add(new IpDTO(0L, "127.0.0.4"));
        servers.add(new IpDTO(0L, "127.0.0.5"));
        servers.add(new IpDTO(0L, "127.0.0.6"));
        servers.add(new IpDTO(0L, "127.0.0.7"));
        servers.add(new IpDTO(0L, "127.0.0.8"));
        servers.add(new IpDTO(0L, "127.0.0.9"));
        servers.add(new IpDTO(0L, "127.0.0.10"));
        servers.add(new IpDTO(0L, "127.0.0.11"));
        servers.add(new IpDTO(0L, "127.0.0.12"));
        servers.add(new IpDTO(0L, "127.0.0.13"));
        servers.add(new IpDTO(0L, "127.0.0.14"));
        servers.add(new IpDTO(0L, "127.0.0.15"));
        RollingBatchServersResolver context = new RollingBatchServersResolver(servers, "1 30%");
        List<RollingServerBatch> serverBatchList = context.resolve();
        assertThat(serverBatchList).hasSize(4);

        assertThat(serverBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(serverBatchList.get(0).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.1")
        );
        assertThat(serverBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("1");

        assertThat(serverBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(serverBatchList.get(1).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.2"),
            new IpDTO(0L, "127.0.0.3"),
            new IpDTO(0L, "127.0.0.4"),
            new IpDTO(0L, "127.0.0.5"),
            new IpDTO(0L, "127.0.0.6")
        );
        assertThat(serverBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(serverBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(serverBatchList.get(2).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.7"),
            new IpDTO(0L, "127.0.0.8"),
            new IpDTO(0L, "127.0.0.9"),
            new IpDTO(0L, "127.0.0.10"),
            new IpDTO(0L, "127.0.0.11")
        );
        assertThat(serverBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(serverBatchList.get(3).getBatch()).isEqualTo(4);
        assertThat(serverBatchList.get(3).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.12"),
            new IpDTO(0L, "127.0.0.13"),
            new IpDTO(0L, "127.0.0.14"),
            new IpDTO(0L, "127.0.0.15")

        );
        assertThat(serverBatchList.get(3).getRollingExprPart().getExpr()).isEqualTo("30%");
    }


    @Test
    @DisplayName("计算滚动批次 - 最后一批包含所有")
    void resolveForAllRemainedExpr() {
        List<IpDTO> servers = new ArrayList<>();
        servers.add(new IpDTO(0L, "127.0.0.1"));
        servers.add(new IpDTO(0L, "127.0.0.2"));
        servers.add(new IpDTO(0L, "127.0.0.3"));
        servers.add(new IpDTO(0L, "127.0.0.4"));
        servers.add(new IpDTO(0L, "127.0.0.5"));
        servers.add(new IpDTO(0L, "127.0.0.6"));
        servers.add(new IpDTO(0L, "127.0.0.7"));
        servers.add(new IpDTO(0L, "127.0.0.8"));
        servers.add(new IpDTO(0L, "127.0.0.9"));
        servers.add(new IpDTO(0L, "127.0.0.10"));
        servers.add(new IpDTO(0L, "127.0.0.11"));
        servers.add(new IpDTO(0L, "127.0.0.12"));
        servers.add(new IpDTO(0L, "127.0.0.13"));
        servers.add(new IpDTO(0L, "127.0.0.14"));
        servers.add(new IpDTO(0L, "127.0.0.15"));
        servers.add(new IpDTO(0L, "127.0.0.16"));
        RollingBatchServersResolver context = new RollingBatchServersResolver(servers, "1 30% 100%");
        List<RollingServerBatch> serverBatchList = context.resolve();
        assertThat(serverBatchList).hasSize(3);

        assertThat(serverBatchList.get(0).getBatch()).isEqualTo(1);
        assertThat(serverBatchList.get(0).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.1")
        );
        assertThat(serverBatchList.get(0).getRollingExprPart().getExpr()).isEqualTo("1");

        assertThat(serverBatchList.get(1).getBatch()).isEqualTo(2);
        assertThat(serverBatchList.get(1).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.2"),
            new IpDTO(0L, "127.0.0.3"),
            new IpDTO(0L, "127.0.0.4"),
            new IpDTO(0L, "127.0.0.5"),
            new IpDTO(0L, "127.0.0.6")
        );
        assertThat(serverBatchList.get(1).getRollingExprPart().getExpr()).isEqualTo("30%");

        assertThat(serverBatchList.get(2).getBatch()).isEqualTo(3);
        assertThat(serverBatchList.get(2).getServers()).containsSequence(
            new IpDTO(0L, "127.0.0.7"),
            new IpDTO(0L, "127.0.0.8"),
            new IpDTO(0L, "127.0.0.9"),
            new IpDTO(0L, "127.0.0.10"),
            new IpDTO(0L, "127.0.0.11"),
            new IpDTO(0L, "127.0.0.12"),
            new IpDTO(0L, "127.0.0.13"),
            new IpDTO(0L, "127.0.0.14"),
            new IpDTO(0L, "127.0.0.15"),
            new IpDTO(0L, "127.0.0.16")
        );
        assertThat(serverBatchList.get(2).getRollingExprPart().getExpr()).isEqualTo("100%");
    }
}
