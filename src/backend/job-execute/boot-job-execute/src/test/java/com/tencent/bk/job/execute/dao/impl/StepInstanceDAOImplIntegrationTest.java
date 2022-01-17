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

import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.model.ConfirmStepInstanceDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.FileStepInstanceDTO;
import com.tencent.bk.job.execute.model.ScriptStepInstanceDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import org.assertj.core.util.Lists;
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
@Sql({"/init_step_instance_data.sql"})
public class StepInstanceDAOImplIntegrationTest {
    @Autowired
    private StepInstanceDAO stepInstanceDAO;

    @Test
    public void testGetStepInstanceBase() {
        long stepInstanceId = 1L;
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(stepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(stepInstance.getAppId()).isEqualTo(2L);
        assertThat(stepInstance.getTaskInstanceId()).isEqualTo(1L);
        assertThat(stepInstance.getStepId()).isEqualTo(1L);
        assertThat(stepInstance.getStepNum()).isEqualTo(2);
        assertThat(stepInstance.getStepOrder()).isEqualTo(1);
        assertThat(stepInstance.getName()).isEqualTo("task1-step1");
        assertThat(stepInstance.getExecuteType()).isEqualTo(1);
        assertThat(stepInstance.getIpList()).isEqualTo("0:10.0.0.1");
        assertThat(stepInstance.getOperator()).isEqualTo("admin");
        assertThat(stepInstance.getStatus()).isEqualTo(3);
        assertThat(stepInstance.getExecuteCount()).isEqualTo(0);
        assertThat(stepInstance.getStartTime()).isEqualTo(1572868800000L);
        assertThat(stepInstance.getEndTime()).isEqualTo(1572868801000L);
        assertThat(stepInstance.getTotalTime()).isEqualTo(1111L);
        assertThat(stepInstance.getCreateTime()).isEqualTo(1572868800000L);
        assertThat(stepInstance.getTargetServers()).isNotNull();
        List<IpDTO> expectedServer = new ArrayList<>();
        expectedServer.add(new IpDTO(0L, "10.0.0.1"));
        assertThat(stepInstance.getTargetServers().getIpList()).containsAll(expectedServer);
        assertThat(stepInstance.getBatch()).isEqualTo(0);
    }

    @Test
    public void testAddStepInstanceBase() {
        StepInstanceBaseDTO stepInstanceDTO = new StepInstanceBaseDTO();
        stepInstanceDTO.setAppId(2L);
        stepInstanceDTO.setName("task1-step1");
        stepInstanceDTO.setTaskInstanceId(1L);
        stepInstanceDTO.setStepId(1L);
        stepInstanceDTO.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue());
        ServersDTO servers = new ServersDTO();
        List<IpDTO> ipList = new ArrayList<>();
        ipList.add(new IpDTO(0L, "10.0.0.1"));
        servers.setIpList(ipList);
        stepInstanceDTO.setTargetServers(servers);
        stepInstanceDTO.setOperator("admin");
        stepInstanceDTO.setStatus(RunStatusEnum.SUCCESS.getValue());
        stepInstanceDTO.setExecuteCount(0);
        stepInstanceDTO.setStartTime(1572868800000L);
        stepInstanceDTO.setEndTime(1572868801000L);
        stepInstanceDTO.setTotalTime(1000L);
        stepInstanceDTO.setCreateTime(1572868800000L);
        stepInstanceDTO.setStepNum(3);
        stepInstanceDTO.setStepOrder(1);

        long stepInstanceId = stepInstanceDAO.addStepInstanceBase(stepInstanceDTO);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getAppId()).isEqualTo(2L);
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(1L);
        assertThat(returnStepInstance.getStepId()).isEqualTo(1L);
        assertThat(returnStepInstance.getName()).isEqualTo("task1-step1");
        assertThat(returnStepInstance.getExecuteType()).isEqualTo(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue());
        assertThat(returnStepInstance.getIpList()).isEqualTo("0:10.0.0.1");
        assertThat(returnStepInstance.getTargetServers().getIpList()).hasSize(1);
        List<IpDTO> expectedServer = new ArrayList<>();
        expectedServer.add(new IpDTO(0L, "10.0.0.1"));
        assertThat(returnStepInstance.getTargetServers().getIpList()).containsAll(expectedServer);
        assertThat(returnStepInstance.getOperator()).isEqualTo("admin");
        assertThat(returnStepInstance.getStatus()).isEqualTo(RunStatusEnum.SUCCESS.getValue());
        assertThat(returnStepInstance.getExecuteCount()).isEqualTo(0);
        assertThat(returnStepInstance.getStartTime()).isEqualTo(1572868800000L);
        assertThat(returnStepInstance.getEndTime()).isEqualTo(1572868801000L);
        assertThat(returnStepInstance.getTotalTime()).isEqualTo(1000L);
        assertThat(returnStepInstance.getCreateTime()).isEqualTo(1572868800000L);
        assertThat(returnStepInstance.getStepNum()).isEqualTo(3);
        assertThat(returnStepInstance.getStepOrder()).isEqualTo(1);
        assertThat(returnStepInstance.getBatch()).isEqualTo(0);
    }

    @Test
    public void testListStepInstanceBaseByTaskInstanceId() {
        long taskInstanceId = 1L;

        List<StepInstanceBaseDTO> stepInstanceList =
            stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);

        assertThat(stepInstanceList).hasSize(2);
        assertThat(stepInstanceList).extracting("id").containsSequence(1L, 2L);
    }

    @Test
    public void testResetStepStatus() {
        long stepInstanceId = 1L;
        stepInstanceDAO.resetStepStatus(stepInstanceId);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStartTime()).isNull();
        assertThat(returnStepInstance.getEndTime()).isNull();
        assertThat(returnStepInstance.getTotalTime()).isNull();
        assertThat(returnStepInstance.getSuccessIPNum()).isEqualTo(0);
        assertThat(returnStepInstance.getFailIPNum()).isEqualTo(0);
        assertThat(returnStepInstance.getRunIPNum()).isEqualTo(0);
    }

    @Test
    public void testAddStepRetryCount() {
        long stepInstanceId = 1L;

        stepInstanceDAO.addStepExecuteCount(stepInstanceId);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getExecuteCount()).isEqualTo(1);
    }

    @Test
    public void testUpdateStepStatus() {
        long stepInstanceId = 1L;
        int status = RunStatusEnum.RUNNING.getValue();

        stepInstanceDAO.updateStepStatus(stepInstanceId, status);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStatus()).isEqualTo(status);
    }

    @Test
    public void testUpdateStepStartTime() {
        long stepInstanceId = 4L;
        long startTime = 1573041600000L;
        stepInstanceDAO.updateStepStartTimeIfNull(stepInstanceId, startTime);
        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStartTime()).isEqualTo(startTime);

        stepInstanceId = 1L;
        startTime = 1573041700000L;
        stepInstanceDAO.updateStepStartTimeIfNull(stepInstanceId, startTime);
        returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStartTime()).isEqualTo(1572868800000L);
    }

    @Test
    void testUpdateStepStartTimeIfNull() {
        long stepInstanceId = 1L;
        long startTime = 1573041600000L;

        stepInstanceDAO.updateStepStartTime(stepInstanceId, startTime);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStartTime()).isEqualTo(startTime);
    }

    @Test
    public void testUpdateStepEndTime() {
        long stepInstanceId = 1L;
        long endTime = 1573041600000L;

        stepInstanceDAO.updateStepEndTime(stepInstanceId, endTime);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getEndTime()).isEqualTo(endTime);
    }

    @Test
    public void testUpdateStepTotalTime() {
        long stepInstanceId = 1L;
        long totalTime = 1234L;

        stepInstanceDAO.updateStepTotalTime(stepInstanceId, totalTime);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getTotalTime()).isEqualTo(totalTime);
    }

    @Test
    public void testUpdateStepStatInfo() {
        long stepInstanceId = 1L;
        int successIPNum = 1;
        int failIPNum = 2;
        int runIPNum = 3;

        stepInstanceDAO.updateStepStatInfo(stepInstanceId, runIPNum, successIPNum, failIPNum);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getSuccessIPNum()).isEqualTo(successIPNum);
        assertThat(returnStepInstance.getFailIPNum()).isEqualTo(failIPNum);
        assertThat(returnStepInstance.getRunIPNum()).isEqualTo(runIPNum);

    }

    @Test
    public void testGetFirstStepStartTime() {
        long taskInstanceId = 1L;

        Long startTime = stepInstanceDAO.getFirstStepStartTime(taskInstanceId);

        assertThat(startTime).isEqualTo(1572868800000L);
    }

    @Test
    public void testGetLastStepEndTime() {
        long taskInstanceId = 1L;

        Long endTime = stepInstanceDAO.getLastStepEndTime(taskInstanceId);

        assertThat(endTime).isEqualTo(1572868802000L);
    }

    @Test
    public void testGetAllStepTotalTime() {
        long taskInstanceId = 1L;

        float totalTime = stepInstanceDAO.getAllStepTotalTime(taskInstanceId);

        assertThat(totalTime).isEqualTo(2223L);
    }

    @Test
    public void testResetStepExecuteInfoForRetry() {
        long stepInstanceId = 1L;
        stepInstanceDAO.resetStepExecuteInfoForRetry(stepInstanceId);
        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance).isNotNull();
        assertThat(returnStepInstance.getStartTime()).isNotNull();
        assertThat(returnStepInstance.getEndTime()).isNull();
        assertThat(returnStepInstance.getStatus()).isEqualTo(RunStatusEnum.RUNNING.getValue());
        assertThat(returnStepInstance.getTotalTime()).isNull();
        assertThat(returnStepInstance.getSuccessIPNum()).isEqualTo(0);
        assertThat(returnStepInstance.getFailIPNum()).isEqualTo(0);
        assertThat(returnStepInstance.getRunIPNum()).isEqualTo(0);
    }

    @Test
    public void testGetScriptStepInstance() {
        ScriptStepInstanceDTO returnStepInstance = stepInstanceDAO.getScriptStepInstance(1L);
        assertThat(returnStepInstance.getStepInstanceId()).isEqualTo(1L);
        assertThat(returnStepInstance.getAccount()).isEqualTo("root");
        assertThat(returnStepInstance.getAccountId()).isEqualTo(1L);
        assertThat(returnStepInstance.getDbPass()).isEqualTo("db_password");
        assertThat(returnStepInstance.getDbPort()).isEqualTo(3306);
        assertThat(returnStepInstance.getDbAccount()).isEqualTo("root");
        assertThat(returnStepInstance.getDbAccountId()).isEqualTo(11L);
        assertThat(returnStepInstance.getDbType()).isEqualTo(1);
        assertThat(returnStepInstance.getScriptContent()).isEqualTo("script_content");
        assertThat(returnStepInstance.getScriptParam()).isEqualTo("${var1}");
        assertThat(returnStepInstance.getResolvedScriptParam()).isEqualTo("var1");
        assertThat(returnStepInstance.getScriptType()).isEqualTo(1);
        assertThat(returnStepInstance.getTimeout()).isEqualTo(1000);
        assertThat(returnStepInstance.isSecureParam()).isEqualTo(false);
    }

    @Test
    public void testAddScriptStepInstance() {
        StepInstanceDTO scriptStepInstance = new StepInstanceDTO();
        scriptStepInstance.setId(100L);
        scriptStepInstance.setAccount("root");
        scriptStepInstance.setAccountId(100L);
        scriptStepInstance.setAccountAlias("root");
        scriptStepInstance.setDbPass("password");
        scriptStepInstance.setDbPort(3306);
        scriptStepInstance.setDbAccount("root");
        scriptStepInstance.setDbAccountId(1001L);
        scriptStepInstance.setDbType(1);
        scriptStepInstance.setScriptContent("script_content");
        scriptStepInstance.setScriptParam("${var1} ${var2}");
        scriptStepInstance.setResolvedScriptParam("var1 var2");
        scriptStepInstance.setScriptType(ScriptTypeEnum.SHELL.getValue());
        scriptStepInstance.setTimeout(1000);

        stepInstanceDAO.addScriptStepInstance(scriptStepInstance);

        ScriptStepInstanceDTO savedStepInstance = stepInstanceDAO.getScriptStepInstance(100L);
        assertThat(savedStepInstance.getStepInstanceId()).isEqualTo(100L);
        assertThat(savedStepInstance.getAccount()).isEqualTo("root");
        assertThat(savedStepInstance.getAccountId()).isEqualTo(100L);
        assertThat(savedStepInstance.getDbPass()).isEqualTo("password");
        assertThat(savedStepInstance.getDbPort()).isEqualTo(3306);
        assertThat(savedStepInstance.getDbAccount()).isEqualTo("root");
        assertThat(savedStepInstance.getDbAccountId()).isEqualTo(1001L);
        assertThat(savedStepInstance.getDbType()).isEqualTo(1);
        assertThat(savedStepInstance.getScriptContent()).isEqualTo("script_content");
        assertThat(savedStepInstance.getScriptParam()).isEqualTo("${var1} ${var2}");
        assertThat(savedStepInstance.getResolvedScriptParam()).isEqualTo("var1 var2");
        assertThat(savedStepInstance.getScriptType()).isEqualTo(ScriptTypeEnum.SHELL.getValue());
        assertThat(savedStepInstance.getTimeout()).isEqualTo(1000);
    }

    @Test
    public void testAddFileStepInstance() {
        StepInstanceDTO fileStepInstance = new StepInstanceDTO();
        fileStepInstance.setId(101L);
        fileStepInstance.setAccount("root");
        fileStepInstance.setAccountId(1L);
        fileStepInstance.setAccountAlias("root");
        fileStepInstance.setFileDownloadSpeedLimit(100);
        fileStepInstance.setFileUploadSpeedLimit(200);
        fileStepInstance.setFileDuplicateHandle(1);
        fileStepInstance.setNotExistPathHandler(1);
        fileStepInstance.setFileTargetPath("/tmp/");
        fileStepInstance.setFileTargetName("test.log");
        fileStepInstance.setTimeout(1000);

        List<FileSourceDTO> fileSources = new ArrayList<>();
        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setAccount("root");
        fileSource.setAccountId(1L);
        fileSource.setLocalUpload(false);
        fileSource.setFileType(TaskFileTypeEnum.SERVER.getType());
        ServersDTO fileSourceServers = new ServersDTO();
        fileSourceServers.setIpList(Lists.newArrayList(new IpDTO(1L, "10.10.10.10")));
        fileSource.setServers(fileSourceServers);
        FileDetailDTO fileDetail = new FileDetailDTO();
        fileDetail.setFilePath("/tmp/1.log");
        fileDetail.setFileHash("hash");
        fileSource.setFiles(Lists.newArrayList(fileDetail));
        fileSources.add(fileSource);

        fileStepInstance.setFileSourceList(fileSources);

        stepInstanceDAO.addFileStepInstance(fileStepInstance);

        FileStepInstanceDTO savedStepInstance = stepInstanceDAO.getFileStepInstance(101L);
        assertThat(savedStepInstance.getStepInstanceId()).isEqualTo(101L);
        assertThat(savedStepInstance.getAccount()).isEqualTo("root");
        assertThat(savedStepInstance.getAccountId()).isEqualTo(1L);
        assertThat(savedStepInstance.getFileUploadSpeedLimit()).isEqualTo(200);
        assertThat(savedStepInstance.getFileDownloadSpeedLimit()).isEqualTo(100);
        assertThat(savedStepInstance.getFileDuplicateHandle()).isEqualTo(1);
        assertThat(savedStepInstance.getNotExistPathHandler()).isEqualTo(1);
        assertThat(savedStepInstance.getFileTargetPath()).isEqualTo("/tmp/");
        assertThat(savedStepInstance.getFileTargetName()).isEqualTo("test.log");
        assertThat(savedStepInstance.getTimeout()).isEqualTo(1000);
        assertThat(savedStepInstance.getFileSourceList()).isNotEmpty();
        assertThat(savedStepInstance.getFileSourceList().get(0)).isNotNull();
        assertThat(savedStepInstance.getFileSourceList().get(0).getAccountId()).isEqualTo(1L);
        assertThat(savedStepInstance.getFileSourceList().get(0).getAccount()).isEqualTo("root");
        assertThat(savedStepInstance.getFileSourceList().get(0).getServers()).isNotNull();
        assertThat(savedStepInstance.getFileSourceList().get(0).getServers().getIpList()).isNotEmpty();
        assertThat(savedStepInstance.getFileSourceList().get(0).getServers().getIpList())
            .containsOnly(new IpDTO(1L,
            "10.10.10.10"));
        assertThat(savedStepInstance.getFileSourceList().get(0).getFiles()).isNotEmpty();
        assertThat(savedStepInstance.getFileSourceList().get(0).getFiles().get(0).getFilePath())
            .isEqualTo("/tmp/1.log");
    }

    @Test
    public void testGetFileStepInstance() {
        FileStepInstanceDTO returnStepInstance = stepInstanceDAO.getFileStepInstance(2L);
        assertThat(returnStepInstance).isNotNull();
        assertThat(returnStepInstance.getStepInstanceId()).isEqualTo(2L);
        assertThat(returnStepInstance.getFileSourceList()).isNotEmpty();
        assertThat(returnStepInstance.getFileSourceList().get(0).getFiles().get(0).getFilePath()).isEqualTo("/$" +
            "{log_dir}/1.log");
        assertThat(returnStepInstance.getResolvedFileSourceList().get(0).getFiles().get(0).getResolvedFilePath())
            .isEqualTo("/tmp/1.log");
        assertThat(returnStepInstance.getFileTargetPath()).isEqualTo("/${log_dir}/");
        assertThat(returnStepInstance.getFileTargetName()).isEqualTo("2.log");
        assertThat(returnStepInstance.getResolvedFileTargetPath()).isEqualTo("/tmp/");
        assertThat(returnStepInstance.getFileUploadSpeedLimit()).isEqualTo(100);
        assertThat(returnStepInstance.getFileDownloadSpeedLimit()).isEqualTo(100);
        assertThat(returnStepInstance.getFileDuplicateHandle()).isEqualTo(1);
        assertThat(returnStepInstance.getNotExistPathHandler()).isEqualTo(1);
        assertThat(returnStepInstance.getTimeout()).isEqualTo(1000);
        assertThat(returnStepInstance.getAccountId()).isEqualTo(1L);
        assertThat(returnStepInstance.getAccount()).isEqualTo("root");
    }


    @Test
    public void testGetConfirmStepInstance() {
        ConfirmStepInstanceDTO returnStepInstance = stepInstanceDAO.getConfirmStepInstance(3L);
        assertThat(returnStepInstance).isNotNull();
        assertThat(returnStepInstance.getStepInstanceId()).isEqualTo(3L);
        assertThat(returnStepInstance.getConfirmMessage()).isEqualTo("confirm_message");
        assertThat(returnStepInstance.getConfirmReason()).isEqualTo("confirm_reason");
        assertThat(returnStepInstance.getConfirmRoles()).containsOnly("JOB_RESOURCE_TRIGGER_USER");
        assertThat(returnStepInstance.getConfirmUsers()).containsOnly("admin", "test");
        assertThat(returnStepInstance.getNotifyChannels()).containsOnly("weixin");
    }

    @Test
    public void testAddConfirmStepInstance() {
        StepInstanceDTO confirmStepInstance = new StepInstanceDTO();
        confirmStepInstance.setId(102L);
        confirmStepInstance.setConfirmMessage("confirm_message");
        confirmStepInstance.setConfirmUsers(Lists.newArrayList("admin"));
        confirmStepInstance.setConfirmRoles(Lists.newArrayList("JOB_RESOURCE_TRIGGER_USER"));
        confirmStepInstance.setNotifyChannels(Lists.newArrayList("weixin"));

        stepInstanceDAO.addConfirmStepInstance(confirmStepInstance);

        ConfirmStepInstanceDTO returnStepInstance = stepInstanceDAO.getConfirmStepInstance(102L);
        assertThat(returnStepInstance).isNotNull();
        assertThat(returnStepInstance.getStepInstanceId()).isEqualTo(102L);
        assertThat(returnStepInstance.getConfirmMessage()).isEqualTo("confirm_message");
        assertThat(returnStepInstance.getConfirmRoles()).containsOnly("JOB_RESOURCE_TRIGGER_USER");
        assertThat(returnStepInstance.getConfirmUsers()).containsOnly("admin");
        assertThat(returnStepInstance.getNotifyChannels()).containsOnly("weixin");

    }

    @Test
    void testUpdateResolvedScriptParam() {
        stepInstanceDAO.updateResolvedScriptParam(1L, "resolved_var");
        ScriptStepInstanceDTO updatedStepInstance = stepInstanceDAO.getScriptStepInstance(1L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getResolvedScriptParam()).isEqualTo("resolved_var");

    }

    @Test
    void testUpdateResolvedFileSource() {
        List<FileSourceDTO> fileSources = new ArrayList<>();
        List<FileDetailDTO> fileList = new ArrayList<>();
        FileDetailDTO fileDetail = new FileDetailDTO();
        fileDetail.setFilePath("/data/${log_dir}/1.log");
        fileDetail.setResolvedFilePath("/data/logs/1.log");
        fileDetail.setFileName("1.log");
        fileList.add(fileDetail);
        FileSourceDTO fileSourceDTO = new FileSourceDTO();
        fileSourceDTO.setFiles(fileList);
        fileSourceDTO.setLocalUpload(false);
        fileSourceDTO.setFileType(TaskFileTypeEnum.SERVER.getType());
        fileSourceDTO.setAccountId(1L);
        fileSourceDTO.setAccount("root");
        ServersDTO servers = new ServersDTO();
        List<IpDTO> ips = new ArrayList<>();
        ips.add(new IpDTO(1L, "10.10.10.10"));
        servers.setIpList(ips);
        fileSourceDTO.setServers(servers);
        fileSources.add(fileSourceDTO);


        stepInstanceDAO.updateResolvedSourceFile(2L, fileSources);
        FileStepInstanceDTO updatedStepInstance = stepInstanceDAO.getFileStepInstance(2L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getFileSourceList()).hasSize(1);
        assertThat(updatedStepInstance.getFileSourceList().get(0).getFiles().get(0).getFilePath()).isEqualTo("/data/$" +
            "{log_dir}/1.log");
        assertThat(updatedStepInstance.getFileSourceList().get(0).getFiles().get(0).getResolvedFilePath()).isEqualTo(
            "/data/logs/1.log");
        assertThat(updatedStepInstance.getFileSourceList().get(0).getFiles().get(0).getResolvedFilePath()).isEqualTo(
            "/data/logs/1.log");
        assertThat(updatedStepInstance.getFileSourceList().get(0).getServers().getIpList()).contains(new IpDTO(1L, 
            "10.10.10.10"));
    }

    @Test
    void testUpdateResolvedFileTargetPath() {
        stepInstanceDAO.updateResolvedTargetPath(2L, "/data/bkee/");
        FileStepInstanceDTO updatedStepInstance = stepInstanceDAO.getFileStepInstance(2L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getResolvedFileTargetPath()).isEqualTo("/data/bkee/");
    }

    @Test
    void testUpdateConfirmReason() {
        stepInstanceDAO.updateConfirmReason(3L, "ok");
        ConfirmStepInstanceDTO updatedStepInstance = stepInstanceDAO.getConfirmStepInstance(3L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getConfirmReason()).isEqualTo("ok");
    }

    @Test
    void testUpdateStepOperator() {
        stepInstanceDAO.updateStepOperator(3L, "test");
        StepInstanceBaseDTO updatedStepInstance = stepInstanceDAO.getStepInstanceBase(3L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getOperator()).isEqualTo("test");
    }

    @Test
    void testGetPreExecutableStepInstance() {
        StepInstanceBaseDTO preStepInstance = stepInstanceDAO.getPreExecutableStepInstance(1L, 2L);
        assertThat(preStepInstance).isNotNull();
        assertThat(preStepInstance.getId()).isEqualTo(1L);
    }

    @Test
    void updateStepCurrentBatch() {
        stepInstanceDAO.updateStepCurrentBatch(1L, 1);

        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(1L);
        assertThat(stepInstance.getBatch()).isEqualTo(1);
    }

    @Test
    void updateStepRollingConfigId() {
        stepInstanceDAO.updateStepRollingConfigId(1L, 1000L);

        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(1L);
        assertThat(stepInstance.getRollingConfigId()).isEqualTo(1000L);
    }


}
