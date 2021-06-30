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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.execute.dao.GseTaskIpLogDAO;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_gse_task_ip_log_data.sql"})
public class GseTaskIpLogDAOImplIntegrationTest {
    @Autowired
    private GseTaskIpLogDAO gseTaskIpLogDAO;

    @Test
    public void testGetIpLogByIp() {
        String ip = "0:10.0.0.1";
        long stepInstanceId = 1L;
        int executeCount = 0;
        GseTaskIpLogDTO gseTaskIpLog = gseTaskIpLogDAO.getIpLogByIp(stepInstanceId, executeCount, ip);

        assertThat(gseTaskIpLog.getStepInstanceId()).isEqualTo(stepInstanceId);
        assertThat(gseTaskIpLog.getExecuteCount()).isEqualTo(executeCount);
        assertThat(gseTaskIpLog.getCloudAreaAndIp()).isEqualTo(ip);
        assertThat(gseTaskIpLog.getStatus()).isEqualTo(9);
        Long expectStartTime = 1565767148000L;
        Long expectEndTime = 1565767149000L;
        assertThat(gseTaskIpLog.getStartTime()).isEqualTo(expectStartTime);
        assertThat(gseTaskIpLog.getEndTime()).isEqualTo(expectEndTime);
        assertThat(gseTaskIpLog.getTotalTime()).isEqualTo(1316L);
        assertThat(gseTaskIpLog.getErrCode()).isEqualTo(0);
        assertThat(gseTaskIpLog.getExitCode()).isEqualTo(0);
        assertThat(gseTaskIpLog.getTag()).isEqualTo("succ");
        assertThat(gseTaskIpLog.getOffset()).isEqualTo(0);
        assertThat(gseTaskIpLog.getCloudAreaId()).isEqualTo(0);
        assertThat(gseTaskIpLog.getDisplayIp()).isEqualTo("10.0.0.1");
        assertThat(gseTaskIpLog.isTargetServer()).isEqualTo(true);
        assertThat(gseTaskIpLog.isSourceServer()).isEqualTo(false);
    }

    @Test
    public void testBatchSaveIpLog() {
        List<GseTaskIpLogDTO> ipLogList = new ArrayList<>();
        GseTaskIpLogDTO ipLog1 = new GseTaskIpLogDTO();
        ipLog1.setStepInstanceId(1L);
        ipLog1.setExecuteCount(0);
        ipLog1.setCloudAreaAndIp("0:10.0.0.1");
        ipLog1.setDisplayIp("10.0.0.1");
        ipLog1.setErrCode(99);
        ipLog1.setStatus(1);
        ipLog1.setExitCode(1);
        ipLogList.add(ipLog1);

        GseTaskIpLogDTO ipLog2 = new GseTaskIpLogDTO();
        ipLog2.setStepInstanceId(3L);
        ipLog2.setExecuteCount(0);
        ipLog2.setCloudAreaAndIp("0:10.0.0.1");
        ipLog2.setErrCode(88);
        ipLog2.setExitCode(1);
        ipLog2.setDisplayIp("10.0.0.1");
        long startTime = 1572858330000L;
        ipLog2.setStartTime(startTime);
        long endTime = 1572858331000L;
        ipLog2.setEndTime(endTime);
        ipLog2.setStatus(2);
        ipLogList.add(ipLog2);

        gseTaskIpLogDAO.batchSaveIpLog(ipLogList);

        GseTaskIpLogDTO ipLog1Return = gseTaskIpLogDAO.getIpLogByIp(1L, 0, "0:10.0.0.1");
        assertThat(ipLog1Return.getStepInstanceId()).isEqualTo(1L);
        assertThat(ipLog1Return.getExecuteCount()).isEqualTo(0L);
        assertThat(ipLog1Return.getCloudAreaAndIp()).isEqualTo("0:10.0.0.1");
        assertThat(ipLog1Return.getErrCode()).isEqualTo(99);
        assertThat(ipLog1Return.getStatus()).isEqualTo(1);
        assertThat(ipLog1Return.getExitCode()).isEqualTo(1);


        GseTaskIpLogDTO ipLog2Return = gseTaskIpLogDAO.getIpLogByIp(3L, 0, "0:10.0.0.1");
        assertThat(ipLog2Return.getStepInstanceId()).isEqualTo(3L);
        assertThat(ipLog2Return.getExecuteCount()).isEqualTo(0L);
        assertThat(ipLog2Return.getCloudAreaAndIp()).isEqualTo("0:10.0.0.1");
        assertThat(ipLog2Return.getStartTime()).isEqualTo(startTime);
        assertThat(ipLog2Return.getEndTime()).isEqualTo(endTime);
        assertThat(ipLog2Return.getErrCode()).isEqualTo(88);
        assertThat(ipLog2Return.getStatus()).isEqualTo(2);
        assertThat(ipLog2Return.getExitCode()).isEqualTo(1);
    }

    @Test
    public void testGetSuccessIpCount() {
        Integer count = gseTaskIpLogDAO.getSuccessIpCount(1L, 0);
        assertThat(count).isEqualTo(2);
    }

    @Test
    public void testGetSuccessIpList() {
        List<GseTaskIpLogDTO> gseTaskIpLogList = gseTaskIpLogDAO.getSuccessGseTaskIp(1L, 0);
        assertThat(gseTaskIpLogList).extracting("cloudAreaAndIp").containsOnly("0:10.0.0.1", "0:10.0.0.2");
    }

    @Test
    public void testGetIpErrorStatList() {
        List<ResultGroupBaseDTO> resultGroups = gseTaskIpLogDAO.getResultGroups(1L, 0);

        assertThat(resultGroups.size()).isEqualTo(1);
        assertThat(resultGroups.get(0).getTag()).isEqualTo("succ");
        assertThat(resultGroups.get(0).getResultType()).isEqualTo(9);
        assertThat(resultGroups.get(0).getAgentTaskCount()).isEqualTo(2);
    }

    @Test
    public void testGetIpLogByResultType() {
        List<GseTaskIpLogDTO> ipLogs = gseTaskIpLogDAO.getIpLogByResultType(1L, 0, 9, "succ");
        assertThat(ipLogs.size()).isEqualTo(2);
        assertThat(ipLogs).extracting("tag").containsOnly("succ", "succ");
        assertThat(ipLogs).extracting("stepInstanceId").containsOnly(1L, 1L);
    }

    @Test
    public void testGetIpLogByIps() {
        String[] ipArray = {"0:10.0.0.1", "0:10.0.0.2"};
        List<GseTaskIpLogDTO> ipLogs = gseTaskIpLogDAO.getIpLogByIps(1L, 0, ipArray);

        assertThat(ipLogs.size()).isEqualTo(2);
        assertThat(ipLogs).extracting("cloudAreaAndIp").containsOnly("0:10.0.0.1", "0:10.0.0.2");
        assertThat(ipLogs).extracting("stepInstanceId").containsOnly(1L, 1L);
        assertThat(ipLogs).extracting("executeCount").containsOnly(0, 0);
    }

    @Test
    public void testDeleteAllIpLog() {
        gseTaskIpLogDAO.deleteAllIpLog(1L, 0);

        List<GseTaskIpLogDTO> ipLogs = gseTaskIpLogDAO.getIpLog(1L, 0, false);
        assertThat(ipLogs.size()).isEqualTo(0);
    }

    @Test
    public void testGetTaskFileSourceIps() {
        List<String> fileSourceIps = gseTaskIpLogDAO.getTaskFileSourceIps(1L, 0);
        assertThat(fileSourceIps.size()).isEqualTo(1);
        assertThat(fileSourceIps.get(0)).isEqualTo("0:10.0.0.3");
    }

    @Test
    public void testFuzzySearchTargetIpsByIp() {
        List<String> matchIps = gseTaskIpLogDAO.fuzzySearchTargetIpsByIp(1L, 0, "0.0.2");
        assertThat(matchIps.size()).isEqualTo(1);
        assertThat(matchIps.get(0)).isEqualTo("0:10.0.0.2");
    }


}
