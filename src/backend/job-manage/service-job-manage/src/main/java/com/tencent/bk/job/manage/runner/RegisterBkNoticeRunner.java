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

package com.tencent.bk.job.manage.runner;

import com.tencent.bk.job.common.notice.IBkNoticeClient;
import com.tencent.bk.job.common.notice.model.BkNoticeApp;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.manage.api.common.constants.globalsetting.GlobalSettingKeys;
import com.tencent.bk.job.manage.dao.globalsetting.GlobalSettingDAO;
import com.tencent.bk.job.manage.model.dto.GlobalSettingDTO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 进程启动时向消息中心注册平台信息（幂等操作）
 */
@SuppressWarnings("ConstantConditions")
@Slf4j
@Component
@ConditionalOnProperty(value = "bkNotice.enabled", havingValue = "true", matchIfMissing = true)
public class RegisterBkNoticeRunner implements CommandLineRunner {

    private final IBkNoticeClient bkNoticeClient;
    private final ThreadPoolExecutor initRunnerExecutor;
    private final GlobalSettingDAO globalSettingDAO;

    @Autowired
    public RegisterBkNoticeRunner(IBkNoticeClient bkNoticeClient,
                                  @Qualifier("initRunnerExecutor") ThreadPoolExecutor initRunnerExecutor,
                                  GlobalSettingDAO globalSettingDAO) {
        this.bkNoticeClient = bkNoticeClient;
        this.initRunnerExecutor = initRunnerExecutor;
        this.globalSettingDAO = globalSettingDAO;
    }

    @Override
    public void run(String... args) {
        initRunnerExecutor.submit(() -> {
            boolean registerSuccess = false;
            // 最多重试30min，覆盖整个蓝鲸部署时间
            int maxRetryTimes = 180;
            int retryTimes = 0;
            while (!registerSuccess && retryTimes < maxRetryTimes) {
                try {
                    BkNoticeApp bkNoticeApp = bkNoticeClient.registerApplication();
                    log.info("registerApplication result:{}", bkNoticeApp);
                    registerSuccess = true;
                } catch (Exception e) {
                    retryTimes++;
                    if (retryTimes < maxRetryTimes) {
                        String msg = MessageFormatter.format(
                            "Fail to registerApplication, retry {}",
                            retryTimes
                        ).getMessage();
                        log.warn(msg, e);
                        ThreadUtils.sleep(10000);
                    } else {
                        log.warn("Fail to registerApplication finally", e);
                    }
                }
            }
            // 将注册结果写入DB中
            if (registerSuccess) {
                int affectedNum = globalSettingDAO.upsertGlobalSetting(buildRegisterResult(registerSuccess));
                log.info("Write to db, registerSuccess={}, affectedNum={}", registerSuccess, affectedNum);
            }
        });
    }

    private GlobalSettingDTO buildRegisterResult(boolean registerSuccess) {
        GlobalSettingDTO globalSettingDTO = new GlobalSettingDTO();
        globalSettingDTO.setKey(GlobalSettingKeys.KEY_BK_NOTICE_REGISTERED_SUCCESS);
        globalSettingDTO.setValue(String.valueOf(registerSuccess));
        globalSettingDTO.setDescription("Updated at " + TimeUtil.getCurrentTimeStr("yyyy-MM-dd HH:mm:ss.SSS"));
        return globalSettingDTO;
    }
}
