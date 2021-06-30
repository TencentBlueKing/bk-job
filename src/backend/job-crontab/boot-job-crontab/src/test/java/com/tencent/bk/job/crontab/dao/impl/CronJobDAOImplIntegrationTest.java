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

package com.tencent.bk.job.crontab.dao.impl;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.util.CronExpressionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @since 26/12/2019 22:09
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_cron_job_data.sql"})
public class CronJobDAOImplIntegrationTest {

    private static final Random random = new Random();
    private static final CronJobInfoDTO CRON_JOB_1 = new CronJobInfoDTO();
    private static final CronJobInfoDTO CRON_JOB_2 = new CronJobInfoDTO();
    private static final CronJobInfoDTO CRON_JOB_3 = new CronJobInfoDTO();
    private static final CronJobInfoDTO CRON_JOB_4 = new CronJobInfoDTO();
    private static final CronJobInfoDTO CRON_JOB_5 = new CronJobInfoDTO();
    private static final CronJobInfoDTO CRON_JOB_6 = new CronJobInfoDTO();
    private static final CronJobInfoDTO CRON_JOB_7 = new CronJobInfoDTO();
    private static final CronJobInfoDTO CRON_JOB_8 = new CronJobInfoDTO();
    private static final CronJobInfoDTO CRON_JOB_9 = new CronJobInfoDTO();
    private static final CronJobVariableDTO VARIABLE_1 = new CronJobVariableDTO();
    private static final CronJobVariableDTO VARIABLE_2 = new CronJobVariableDTO();
    private static final UserRoleInfoDTO NOTIFY_USER_1 = new UserRoleInfoDTO();
    private static final UserRoleInfoDTO NOTIFY_USER_2 = new UserRoleInfoDTO();
    private static final List<CronJobVariableDTO> VARIABLE_LIST = Arrays.asList(VARIABLE_1, VARIABLE_2);

    @Autowired
    private CronJobDAO cronJobDAO;

    @BeforeEach
    void initTest() {
        VARIABLE_1.setName("a");
        VARIABLE_1.setValue("b");
        VARIABLE_1.setType(TaskVariableTypeEnum.HOST_LIST);

        VARIABLE_2.setName("b");
        VARIABLE_2.setValue("c");
        VARIABLE_2.setType(TaskVariableTypeEnum.CIPHER);

        NOTIFY_USER_1.setUserList(Arrays.asList("userC", "userJ"));
        NOTIFY_USER_1.setRoleList(Arrays.asList("JOB_ROLE_1", "JOB_ROLE_2"));

        NOTIFY_USER_2.setUserList(Arrays.asList("userT", "userJ"));
        NOTIFY_USER_2.setRoleList(Arrays.asList("JOB_ROLE_3", "JOB_ROLE_4"));

        CRON_JOB_1.setId(1L);
        CRON_JOB_1.setAppId(2L);
        CRON_JOB_1.setName("cron_job_1");
        CRON_JOB_1.setCreator("userC");
        CRON_JOB_1.setTaskTemplateId(1L);
        CRON_JOB_1.setTaskPlanId(100L);
        CRON_JOB_1.setScriptId(null);
        CRON_JOB_1.setScriptVersionId(null);
        CRON_JOB_1.setCronExpression("* * * * *");
        CRON_JOB_1.setExecuteTime(null);
        CRON_JOB_1.setVariableValue(VARIABLE_LIST);
        CRON_JOB_1.setLastExecuteStatus(0);
        CRON_JOB_1.setEnable(true);
        CRON_JOB_1.setDelete(false);
        CRON_JOB_1.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_1.setLastModifyUser("userT");
        CRON_JOB_1.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_1.setEndTime(0L);
        CRON_JOB_1.setNotifyOffset(10L * 60L);
        CRON_JOB_1.setNotifyUser(NOTIFY_USER_1);
        CRON_JOB_1.setNotifyChannel(Arrays.asList("wechat", "email"));

        CRON_JOB_2.setId(2L);
        CRON_JOB_2.setAppId(2L);
        CRON_JOB_2.setName("cron_job_2");
        CRON_JOB_2.setCreator("userT");
        CRON_JOB_2.setTaskTemplateId(2L);
        CRON_JOB_2.setTaskPlanId(200L);
        CRON_JOB_2.setScriptId(null);
        CRON_JOB_2.setScriptVersionId(null);
        CRON_JOB_2.setCronExpression(null);
        CRON_JOB_2.setExecuteTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_2.setVariableValue(VARIABLE_LIST);
        CRON_JOB_2.setLastExecuteStatus(1);
        CRON_JOB_2.setEnable(false);
        CRON_JOB_2.setDelete(true);
        CRON_JOB_2.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_2.setLastModifyUser("userT");
        CRON_JOB_2.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_2.setEndTime(0L);
        CRON_JOB_2.setNotifyOffset(10L * 60L);
        CRON_JOB_2.setNotifyUser(NOTIFY_USER_2);
        CRON_JOB_2.setNotifyChannel(Collections.singletonList("email"));

        CRON_JOB_3.setId(3L);
        CRON_JOB_3.setAppId(2L);
        CRON_JOB_3.setName("cron_job_3");
        CRON_JOB_3.setCreator("userC");
        CRON_JOB_3.setTaskTemplateId(3L);
        CRON_JOB_3.setTaskPlanId(300L);
        CRON_JOB_3.setScriptId(null);
        CRON_JOB_3.setScriptVersionId(null);
        CRON_JOB_3.setCronExpression("* * * * *");
        CRON_JOB_3.setExecuteTime(null);
        CRON_JOB_3.setVariableValue(VARIABLE_LIST);
        CRON_JOB_3.setLastExecuteStatus(0);
        CRON_JOB_3.setEnable(true);
        CRON_JOB_3.setDelete(false);
        CRON_JOB_3.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_3.setLastModifyUser("userC");
        CRON_JOB_3.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_3.setEndTime(LocalDateTime.of(2020, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_3.setNotifyOffset(0L);
        CRON_JOB_3.setNotifyUser(new UserRoleInfoDTO());
        CRON_JOB_3.setNotifyChannel(Collections.emptyList());

        CRON_JOB_4.setId(4L);
        CRON_JOB_4.setAppId(2L);
        CRON_JOB_4.setName("cron_job_4");
        CRON_JOB_4.setCreator("userT");
        CRON_JOB_4.setTaskTemplateId(4L);
        CRON_JOB_4.setTaskPlanId(400L);
        CRON_JOB_4.setScriptId(null);
        CRON_JOB_4.setScriptVersionId(null);
        CRON_JOB_4.setCronExpression(null);
        CRON_JOB_4.setExecuteTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_4.setVariableValue(VARIABLE_LIST);
        CRON_JOB_4.setLastExecuteStatus(0);
        CRON_JOB_4.setEnable(true);
        CRON_JOB_4.setDelete(false);
        CRON_JOB_4.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_4.setLastModifyUser("userC");
        CRON_JOB_4.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_4.setEndTime(0L);
        CRON_JOB_4.setNotifyOffset(0L);
        CRON_JOB_4.setNotifyUser(new UserRoleInfoDTO());
        CRON_JOB_4.setNotifyChannel(Collections.emptyList());

        CRON_JOB_5.setId(5L);
        CRON_JOB_5.setAppId(2L);
        CRON_JOB_5.setName("cron_job_5");
        CRON_JOB_5.setCreator("userC");
        CRON_JOB_5.setTaskTemplateId(5L);
        CRON_JOB_5.setTaskPlanId(500L);
        CRON_JOB_5.setScriptId(null);
        CRON_JOB_5.setScriptVersionId(null);
        CRON_JOB_5.setCronExpression("* * * * *");
        CRON_JOB_5.setExecuteTime(null);
        CRON_JOB_5.setVariableValue(VARIABLE_LIST);
        CRON_JOB_5.setLastExecuteStatus(1);
        CRON_JOB_5.setEnable(true);
        CRON_JOB_5.setDelete(false);
        CRON_JOB_5.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_5.setLastModifyUser("userT");
        CRON_JOB_5.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_5.setEndTime(0L);
        CRON_JOB_5.setNotifyOffset(0L);
        CRON_JOB_5.setNotifyUser(new UserRoleInfoDTO());
        CRON_JOB_5.setNotifyChannel(Collections.emptyList());

        CRON_JOB_6.setId(6L);
        CRON_JOB_6.setAppId(2L);
        CRON_JOB_6.setName("cron_job_6");
        CRON_JOB_6.setCreator("userT");
        CRON_JOB_6.setTaskTemplateId(null);
        CRON_JOB_6.setTaskPlanId(null);
        CRON_JOB_6.setScriptId("aaaa");
        CRON_JOB_6.setScriptVersionId(1L);
        CRON_JOB_6.setCronExpression(null);
        CRON_JOB_6.setExecuteTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_6.setVariableValue(VARIABLE_LIST);
        CRON_JOB_6.setLastExecuteStatus(0);
        CRON_JOB_6.setEnable(true);
        CRON_JOB_6.setDelete(false);
        CRON_JOB_6.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_6.setLastModifyUser("userT");
        CRON_JOB_6.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_6.setEndTime(0L);
        CRON_JOB_6.setNotifyOffset(0L);
        CRON_JOB_6.setNotifyUser(new UserRoleInfoDTO());
        CRON_JOB_6.setNotifyChannel(Collections.emptyList());

        CRON_JOB_7.setId(7L);
        CRON_JOB_7.setAppId(2L);
        CRON_JOB_7.setName("cron_job_7");
        CRON_JOB_7.setCreator("userC");
        CRON_JOB_7.setTaskTemplateId(null);
        CRON_JOB_7.setTaskPlanId(null);
        CRON_JOB_7.setScriptId("bbbb");
        CRON_JOB_7.setScriptVersionId(2L);
        CRON_JOB_7.setCronExpression("* * * * *");
        CRON_JOB_7.setExecuteTime(null);
        CRON_JOB_7.setVariableValue(VARIABLE_LIST);
        CRON_JOB_7.setLastExecuteStatus(0);
        CRON_JOB_7.setEnable(false);
        CRON_JOB_7.setDelete(true);
        CRON_JOB_7.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_7.setLastModifyUser("userC");
        CRON_JOB_7.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_7.setEndTime(0L);
        CRON_JOB_7.setNotifyOffset(0L);
        CRON_JOB_7.setNotifyUser(new UserRoleInfoDTO());
        CRON_JOB_7.setNotifyChannel(Collections.emptyList());

        CRON_JOB_8.setId(8L);
        CRON_JOB_8.setAppId(2L);
        CRON_JOB_8.setName("cron_job_8");
        CRON_JOB_8.setCreator("userT");
        CRON_JOB_8.setTaskTemplateId(null);
        CRON_JOB_8.setTaskPlanId(null);
        CRON_JOB_8.setScriptId("cccc");
        CRON_JOB_8.setScriptVersionId(3L);
        CRON_JOB_8.setCronExpression(null);
        CRON_JOB_8.setExecuteTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_8.setVariableValue(VARIABLE_LIST);
        CRON_JOB_8.setLastExecuteStatus(1);
        CRON_JOB_8.setEnable(true);
        CRON_JOB_8.setDelete(false);
        CRON_JOB_8.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_8.setLastModifyUser("userC");
        CRON_JOB_8.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_8.setEndTime(0L);
        CRON_JOB_8.setNotifyOffset(0L);
        CRON_JOB_8.setNotifyUser(new UserRoleInfoDTO());
        CRON_JOB_8.setNotifyChannel(Collections.emptyList());

        CRON_JOB_9.setId(9L);
        CRON_JOB_9.setAppId(2L);
        CRON_JOB_9.setName("cron_job_9");
        CRON_JOB_9.setCreator("userC");
        CRON_JOB_9.setTaskTemplateId(null);
        CRON_JOB_9.setTaskPlanId(null);
        CRON_JOB_9.setScriptId("vvvv");
        CRON_JOB_9.setScriptVersionId(4L);
        CRON_JOB_9.setCronExpression("* * * * *");
        CRON_JOB_9.setExecuteTime(null);
        CRON_JOB_9.setVariableValue(VARIABLE_LIST);
        CRON_JOB_9.setLastExecuteStatus(0);
        CRON_JOB_9.setEnable(true);
        CRON_JOB_9.setDelete(false);
        CRON_JOB_9.setCreateTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_9.setLastModifyUser("userC");
        CRON_JOB_9.setLastModifyTime(LocalDateTime.of(2019, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.ofHours(8)));
        CRON_JOB_9.setEndTime(0L);
        CRON_JOB_9.setNotifyOffset(0L);
        CRON_JOB_9.setNotifyUser(new UserRoleInfoDTO());
        CRON_JOB_9.setNotifyChannel(Collections.emptyList());
    }

    @Test
    void giveCronJobIdReturnInfo() {
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isEqualTo(CRON_JOB_1);
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_2.getAppId(), CRON_JOB_2.getId())).isNull();
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_3.getAppId(), CRON_JOB_3.getId())).isEqualTo(CRON_JOB_3);
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_4.getAppId(), CRON_JOB_4.getId())).isEqualTo(CRON_JOB_4);
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_5.getAppId(), CRON_JOB_5.getId())).isEqualTo(CRON_JOB_5);
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_6.getAppId(), CRON_JOB_6.getId())).isEqualTo(CRON_JOB_6);
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_7.getAppId(), CRON_JOB_7.getId())).isNull();
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_8.getAppId(), CRON_JOB_8.getId())).isEqualTo(CRON_JOB_8);
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_9.getAppId(), CRON_JOB_9.getId())).isEqualTo(CRON_JOB_9);
    }

    @Test
    void giveWrongIdReturnNull() {
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_1.getAppId(), getRandomPositiveLong())).isNull();
        assertThat(cronJobDAO.getCronJobById(getRandomPositiveLong(), CRON_JOB_1.getId())).isNull();
    }

    @Test
    void updateCronJobById() {
        CRON_JOB_1.setName(UUID.randomUUID().toString());
        CRON_JOB_1.setTaskTemplateId(getRandomPositiveLong());
        CRON_JOB_1.setTaskPlanId(getRandomPositiveLong());
        CRON_JOB_1.setScriptId(UUID.randomUUID().toString().replace("-", ""));
        CRON_JOB_1.setScriptVersionId(getRandomPositiveLong());
        CRON_JOB_1.setCronExpression(UUID.randomUUID().toString());
        CRON_JOB_1.setExecuteTime(DateUtils.currentTimeSeconds());
        CRON_JOB_1.setVariableValue(Collections.singletonList(VARIABLE_1));
        CRON_JOB_1.setLastExecuteStatus(1);
        CRON_JOB_1.setEnable(false);
        CRON_JOB_1.setDelete(false);
        CRON_JOB_1.setLastModifyUser(UUID.randomUUID().toString());
        CRON_JOB_1.setLastModifyTime(DateUtils.currentTimeSeconds());
        CRON_JOB_1.setEndTime(getRandomPositiveLong());
        CRON_JOB_1.setNotifyOffset(getRandomPositiveLong());
        UserRoleInfoDTO userRoleInfo = new UserRoleInfoDTO();
        userRoleInfo.setUserList(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        userRoleInfo.setRoleList(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        CRON_JOB_1.setNotifyUser(userRoleInfo);
        CRON_JOB_1.setNotifyChannel(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        assertThat(cronJobDAO.updateCronJobById(CRON_JOB_1)).isTrue();
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isNotEqualTo(CRON_JOB_1);
        CRON_JOB_1.setExecuteTime(null);
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isEqualTo(CRON_JOB_1);

        CRON_JOB_1.setCronExpression(null);
        CRON_JOB_1.setExecuteTime(DateUtils.currentTimeSeconds());
        assertThat(cronJobDAO.updateCronJobById(CRON_JOB_1)).isTrue();
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isEqualTo(CRON_JOB_1);
    }

    @Test
    void giveCronJobIdReturnDeleteSuccess() {
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isEqualTo(CRON_JOB_1);
        assertThat(cronJobDAO.deleteCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isTrue();
        assertThat(cronJobDAO.deleteCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isFalse();
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isNull();
    }

    private long getRandomPositiveLong() {
        long value = random.nextLong();
        if (value == Long.MIN_VALUE || value == 1L) {
            return 1;
        } else if (value < 0) {
            return -value;
        }
        return value;
    }

    @Test
    void insertCronJobReturnCorrectId() {
        CRON_JOB_1.setId(null);
        CRON_JOB_1.setName(UUID.randomUUID().toString());
        CRON_JOB_1.setCreator(UUID.randomUUID().toString());
        CRON_JOB_1.setTaskTemplateId(getRandomPositiveLong());
        CRON_JOB_1.setTaskPlanId(getRandomPositiveLong());
        CRON_JOB_1.setScriptId(UUID.randomUUID().toString().replace("-", ""));
        CRON_JOB_1.setScriptVersionId(getRandomPositiveLong());
        CRON_JOB_1.setCronExpression(UUID.randomUUID().toString());
        CRON_JOB_1.setExecuteTime(DateUtils.currentTimeSeconds());
        CRON_JOB_1.setVariableValue(Collections.singletonList(VARIABLE_1));
        CRON_JOB_1.setLastExecuteStatus(1);
        CRON_JOB_1.setEnable(false);
        CRON_JOB_1.setDelete(false);
        CRON_JOB_1.setCreateTime(DateUtils.currentTimeSeconds());
        CRON_JOB_1.setLastModifyUser(UUID.randomUUID().toString());
        CRON_JOB_1.setLastModifyTime(DateUtils.currentTimeSeconds());
        CRON_JOB_1.setEndTime(getRandomPositiveLong());
        CRON_JOB_1.setNotifyOffset(getRandomPositiveLong());
        UserRoleInfoDTO userRoleInfo = new UserRoleInfoDTO();
        userRoleInfo.setUserList(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        userRoleInfo.setRoleList(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        CRON_JOB_1.setNotifyUser(userRoleInfo);
        CRON_JOB_1.setNotifyChannel(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        CRON_JOB_1.setId(cronJobDAO.insertCronJob(CRON_JOB_1));
        assertThat(cronJobDAO.getCronJobById(CRON_JOB_1.getAppId(), CRON_JOB_1.getId())).isEqualTo(CRON_JOB_1);
    }

    @Test
    void listCronJobByCondition() {
        CronJobInfoDTO cronJobCondition = new CronJobInfoDTO();
        cronJobCondition.setAppId(CRON_JOB_1.getAppId());
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(2);
        baseSearchCondition.setOrder(0);
        baseSearchCondition.setOrderField("last_modify_user");
        baseSearchCondition.setCreator(CRON_JOB_1.getCreator());
        PageData<CronJobInfoDTO> cronJobInfoPageData =
            cronJobDAO.listPageCronJobsByCondition(cronJobCondition, baseSearchCondition);
        assertThat(cronJobInfoPageData.getStart()).isEqualTo(0);
        assertThat(cronJobInfoPageData.getPageSize()).isEqualTo(2);
        assertThat(cronJobInfoPageData.getTotal()).isEqualTo(4);
        assertThat(cronJobInfoPageData.getData()).contains(CRON_JOB_5);
        assertThat(cronJobInfoPageData.getData()).contains(CRON_JOB_1);

        baseSearchCondition.setStart(2);
        cronJobInfoPageData = cronJobDAO.listPageCronJobsByCondition(cronJobCondition, baseSearchCondition);
        assertThat(cronJobInfoPageData.getStart()).isEqualTo(2);
        assertThat(cronJobInfoPageData.getPageSize()).isEqualTo(2);
        assertThat(cronJobInfoPageData.getTotal()).isEqualTo(4);
        assertThat(cronJobInfoPageData.getData()).contains(CRON_JOB_9);
        assertThat(cronJobInfoPageData.getData()).contains(CRON_JOB_3);
        System.out.println(cronJobInfoPageData.getData());
        baseSearchCondition.setStart(0);

        baseSearchCondition.setOrder(1);
        cronJobInfoPageData = cronJobDAO.listPageCronJobsByCondition(cronJobCondition, baseSearchCondition);
        assertThat(cronJobInfoPageData.getStart()).isEqualTo(0);
        assertThat(cronJobInfoPageData.getPageSize()).isEqualTo(2);
        assertThat(cronJobInfoPageData.getTotal()).isEqualTo(4);
        assertThat(cronJobInfoPageData.getData()).contains(CRON_JOB_9);
        assertThat(cronJobInfoPageData.getData()).contains(CRON_JOB_3);
        System.out.println(cronJobInfoPageData.getData());
        baseSearchCondition.setOrder(0);
    }

    @Test
    void testValidateCronExpression() throws ParseException {
        assertThatExceptionOfType(ParseException.class).isThrownBy(() -> new CronExpression("* * * * * *"));
        assertThatExceptionOfType(ParseException.class).isThrownBy(() -> new CronExpression("* * * * * 2"));
        assertThatExceptionOfType(ParseException.class).isThrownBy(() -> new CronExpression("* * * 2 * *"));
        assertThatExceptionOfType(ParseException.class).isThrownBy(() -> new CronExpression("* * * 2 * 2"));
        assertThatExceptionOfType(ParseException.class).isThrownBy(() -> new CronExpression("* * * ? * ?"));
        new CronExpression("* * * ? * *");
        new CronExpression("* * * * * ?");
        new CronExpression("* * * ? * 2");
        new CronExpression("* * * 2 * ?");
        new CronExpression(CronExpressionUtil.fixExpressionForQuartz("* * * * *"));
        new CronExpression(CronExpressionUtil.fixExpressionForQuartz("* * * * 2"));
        new CronExpression(CronExpressionUtil.fixExpressionForQuartz("* * 2 * *"));
    }

    //    @Test
    void testRedisLock() {
        String firstRequestId = UUID.randomUUID().toString();
        String secondRequestId = UUID.randomUUID().toString();
        String lockKey = "redis_lock_test";
        assertThat(LockUtils.tryGetDistributedLock(lockKey, firstRequestId, 60_000L)).isTrue();
        assertThat(LockUtils.tryGetDistributedLock(lockKey, firstRequestId, 60_000L)).isFalse();
        assertThat(LockUtils.tryGetDistributedLock(lockKey, secondRequestId, 60_000L)).isFalse();

        assertThat(LockUtils.releaseDistributedLock(lockKey, secondRequestId)).isFalse();
        assertThat(LockUtils.releaseDistributedLock(lockKey, firstRequestId)).isTrue();

        assertThat(LockUtils.tryGetDistributedLock(lockKey, secondRequestId, 60_000L)).isTrue();
        assertThat(LockUtils.releaseDistributedLock(lockKey, secondRequestId)).isTrue();
    }
}
