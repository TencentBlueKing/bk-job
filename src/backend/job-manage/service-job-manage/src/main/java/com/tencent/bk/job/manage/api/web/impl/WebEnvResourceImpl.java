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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.config.BkConfig;
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.web.WebEnvResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class WebEnvResourceImpl implements WebEnvResource {

    private final EsbProperties esbProperties;
    private final BkConfig bkConfig;

    @Autowired
    public WebEnvResourceImpl(EsbProperties esbProperties, BkConfig bkConfig) {
        this.esbProperties = esbProperties;
        this.bkConfig = bkConfig;
    }

    @Override
    public Response<Map<String, String>> getJobEnvProperties(String username) {
        Map<String, String> properties = new HashMap<>();
        properties.put("esb.url", standardUrl(esbProperties.getService().getPublicUrl()));
        properties.put("bkDomain", bkConfig.getBkDomain());
        return Response.buildSuccessResp(properties);
    }

    private String standardUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        // 移除最后的/
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        } else {
            return url;
        }
    }

}
