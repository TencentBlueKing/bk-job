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

package com.tencent.bk.job.common.cc.sdk;

import com.google.common.collect.Maps;
import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.esb.config.EsbConfig;
import com.tencent.bk.job.common.esb.constants.EsbLang;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;

import java.util.Map;

@Slf4j
@DependsOn({"applicationContextRegister", "cmdbConfigSetter"})
public class CmdbClientFactory {

    private static final Map<String, IBizCmdbClient> CMDB_CLIENT_MAPS = Maps.newHashMap();

    static {
        EsbConfig esbConfig = null;
        CmdbConfig cmdbConfig = null;
        QueryAgentStatusClient queryAgentStatusClient = null;
        MeterRegistry meterRegistry = null;
        try {
            esbConfig = ApplicationContextRegister.getBean(EsbConfig.class);
            cmdbConfig = ApplicationContextRegister.getBean(CmdbConfig.class);
            queryAgentStatusClient = ApplicationContextRegister.getBean(QueryAgentStatusClient.class);
            meterRegistry = ApplicationContextRegister.getBean(MeterRegistry.class);
        } catch (Throwable e) {
            log.error("Error while initialize bk config!", e);
            throw e;
        }
        CMDB_CLIENT_MAPS.put(LocaleUtils.LANG_ZH_CN,
            new BizCmdbClient(esbConfig, cmdbConfig, EsbLang.CN, queryAgentStatusClient, meterRegistry)
        );
        CMDB_CLIENT_MAPS.put(LocaleUtils.LANG_EN,
            new BizCmdbClient(esbConfig, cmdbConfig, EsbLang.EN, queryAgentStatusClient, meterRegistry)
        );
        CMDB_CLIENT_MAPS.put(LocaleUtils.LANG_ZH,
            new BizCmdbClient(esbConfig, cmdbConfig, EsbLang.CN, queryAgentStatusClient, meterRegistry)
        );
        CMDB_CLIENT_MAPS.put(LocaleUtils.LANG_EN_US,
            new BizCmdbClient(esbConfig, cmdbConfig, EsbLang.EN, queryAgentStatusClient, meterRegistry)
        );
    }

    public static IBizCmdbClient getCcClient() {
        return getCcClient(LocaleUtils.LANG_EN_US);
    }

    public static IBizCmdbClient getCcClient(String language) {
        if (language == null) {
            language = LocaleUtils.LANG_EN_US;
        }
        IBizCmdbClient iccClient = CMDB_CLIENT_MAPS.get(language);
        if (iccClient == null) {
            iccClient = CMDB_CLIENT_MAPS.get(LocaleUtils.LANG_EN_US);
        }
        return iccClient;
    }
}
