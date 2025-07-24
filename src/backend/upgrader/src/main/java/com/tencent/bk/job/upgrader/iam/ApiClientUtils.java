/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.upgrader.iam;

import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.iam.client.EsbIamClient;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;

import java.util.Properties;

public class ApiClientUtils {

    public static EsbIamClient buildEsbIamClient(Properties properties) {
        EsbProperties esbProperties = new EsbProperties();
        EsbProperties.EsbServiceConfig esbServiceConfig = new EsbProperties.EsbServiceConfig();
        esbServiceConfig.setUrl((String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ESB_SERVICE_URL));
        esbProperties.setService(esbServiceConfig);

        return new EsbIamClient(
            null,
            new AppProperties(
                (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_CODE),
                (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_SECRET)
            ),
            esbProperties
        );
    }

}
