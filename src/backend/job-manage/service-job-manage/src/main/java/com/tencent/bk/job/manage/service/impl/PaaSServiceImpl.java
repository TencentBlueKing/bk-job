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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.model.EsbNotifyChannelDTO;
import com.tencent.bk.job.common.paas.user.IPaasClient;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.common.client.PAASClientFactory;
import com.tencent.bk.job.manage.service.PaaSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tencent.bk.job.common.i18n.locale.LocaleUtils.COMMON_LANG_HEADER;

/**
 * @Description
 * @Date 2020/1/3
 * @Version 1.0
 */
@Service
public class PaaSServiceImpl implements PaaSService {
    private static final Logger logger = LoggerFactory.getLogger(PaaSServiceImpl.class);

    private IPaasClient getClientWithCurrentLang() {
        try {
            HttpServletRequest request = JobContextUtil.getRequest();
            String lang = null;
            if (request != null) {
                lang = request.getHeader(COMMON_LANG_HEADER);
            }
            logger.info(String.format("Current Locale:%s", lang));
            return PAASClientFactory.getClient(lang);
        } catch (Exception e) {
            logger.info("Locale not set, backend task, use default locale");
            return PAASClientFactory.getClient();
        }
    }

    @Override
    public List<BkUserDTO> getAllUserList(String bkToken, String uin) {
        IPaasClient paasClient = getClientWithCurrentLang();
        try {
            return paasClient.getUserList("id,username,display_name,logo", bkToken, uin);
        } catch (Exception e) {
            logger.error("Exception while get user list!", e);
        }
        logger.error("Get user list failed!|{}", uin);
        return Collections.emptyList();
    }

    @Override
    public List<EsbNotifyChannelDTO> getAllChannelList(String bkToken, String uin) {
        IPaasClient paasClient = getClientWithCurrentLang();
        return paasClient.getNotifyChannelList(uin);
    }

    @Override
    @EsbApiTimed
    public boolean sendMsg(
        String msgType,
        String sender,
        Set<String> receivers,
        String title,
        String content
    ) throws Exception {
        IPaasClient paasClient = getClientWithCurrentLang();
        return paasClient.sendMsg(msgType, sender, receivers, title, content);
    }
}
