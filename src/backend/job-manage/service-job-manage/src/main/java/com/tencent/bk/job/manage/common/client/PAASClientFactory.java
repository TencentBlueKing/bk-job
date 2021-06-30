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

package com.tencent.bk.job.manage.common.client;

import com.google.common.collect.Maps;
import com.tencent.bk.job.common.esb.config.EsbConfig;
import com.tencent.bk.job.common.paas.user.EEPaasClient;
import com.tencent.bk.job.common.paas.user.IPaasClient;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PAASClientFactory {

    private static final String CHINESE = "zh_CN";
    private static final String ENGLISH = "en_US";
    private static final String BK_LANG_CN = "zh-cn";
    private static final String BK_LANG_EN = "en";
    private static final String BK_LANG_ALL = "all";

    private static final Map<String, IPaasClient> paasClients = Maps.newHashMap();

    static {
        EsbConfig esbConfig;
        MeterRegistry meterRegistry;
        try {
            esbConfig = ApplicationContextRegister.getBean(EsbConfig.class);
            meterRegistry = ApplicationContextRegister.getBean(MeterRegistry.class);
        } catch (Exception e) {
            log.error("Error while initialize bk config!", e);
            throw e;
        }

        paasClients.put(CHINESE, new EEPaasClient(esbConfig.getEsbUrl(), esbConfig.getAppCode(),
            esbConfig.getAppSecret(), BK_LANG_CN, esbConfig.isUseEsbTestEnv(), meterRegistry));
        paasClients.put(ENGLISH, new EEPaasClient(esbConfig.getEsbUrl(), esbConfig.getAppCode(),
            esbConfig.getAppSecret(), BK_LANG_EN, esbConfig.isUseEsbTestEnv(), meterRegistry));
        paasClients.put(BK_LANG_CN, new EEPaasClient(esbConfig.getEsbUrl(), esbConfig.getAppCode(),
            esbConfig.getAppSecret(), BK_LANG_CN, esbConfig.isUseEsbTestEnv(), meterRegistry));
        paasClients.put(BK_LANG_EN, new EEPaasClient(esbConfig.getEsbUrl(), esbConfig.getAppCode(),
            esbConfig.getAppSecret(), BK_LANG_EN, esbConfig.isUseEsbTestEnv(), meterRegistry));
        paasClients.put(BK_LANG_ALL, new EEPaasClient(esbConfig.getEsbUrl(), esbConfig.getAppCode(),
            esbConfig.getAppSecret(), BK_LANG_ALL, esbConfig.isUseEsbTestEnv(), meterRegistry));
    }

    public static IPaasClient getClient(String language) {
        if (language == null) {
            language = BK_LANG_EN;
        }
        IPaasClient paasClient = paasClients.get(language);
        if (paasClient == null) {
            paasClient = paasClients.get(BK_LANG_EN);
        }
        return paasClient;
    }


    public static IPaasClient getClient() {
        return getClient(BK_LANG_EN);
    }

    public static void resetTodayStatistics() {
        paasClients.entrySet().forEach(entry -> {
            IPaasClient paasClient = entry.getValue();
            paasClient.resetTodayStatistics();
        });
    }
}
