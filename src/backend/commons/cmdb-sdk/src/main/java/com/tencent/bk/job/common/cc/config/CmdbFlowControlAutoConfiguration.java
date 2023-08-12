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

package com.tencent.bk.job.common.cc.config;

import com.tencent.bk.job.common.redis.config.JobRedisAutoConfiguration;
import com.tencent.bk.job.common.redis.util.RedisSlideWindowFlowController;
import com.tencent.bk.job.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Configuration(proxyBeanMethods = false)
@Import({CmdbConfig.class})
@Slf4j
@ConditionalOnProperty(value = "cmdb.interface.flowControl.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter({JobRedisAutoConfiguration.class})
public class CmdbFlowControlAutoConfiguration {
    @Bean
    @ConditionalOnClass(RedisSlideWindowFlowController.class)
    public RedisSlideWindowFlowController cmdbGlobalFlowController(
        ObjectProvider<StringRedisTemplate> redisTemplateProvider,
        CmdbConfig cmdbConfig) {
        log.info("Init Cmdb global flow controller!");
        return initCMDBGlobalFlowController(redisTemplateProvider.getIfAvailable(), cmdbConfig);
    }

    private RedisSlideWindowFlowController initCMDBGlobalFlowController(StringRedisTemplate redisTemplate,
                                                                        CmdbConfig cmdbConfig) {
        RedisSlideWindowFlowController flowController = new RedisSlideWindowFlowController();

        String flowControlResourcesStr = cmdbConfig.getFlowControlResourcesStr();
        flowControlResourcesStr = flowControlResourcesStr.trim();
        Map<String, Integer> map = new HashMap<>();
        try {
            List<String> resourceLimitList = StringUtil.strToList(flowControlResourcesStr, String.class, ",");
            for (String resourceLimitStr : resourceLimitList) {
                String[] arr = resourceLimitStr.split(":");
                String resourceId = arr[0];
                Integer limitNum = Integer.parseInt(arr[1]);
                map.put(resourceId, limitNum);
            }
        } catch (Throwable t) {
            log.error("CMDB Flow control resources config invalid:{}, right format:{resourceId1}:{limit1},...," +
                "{resourceId2}:{limit2}", flowControlResourcesStr, t);
        }
        try {
            log.info("CMDB Flow control initializing,map={},flowControlDefaultLimit={},getFlowControlPrecision={}",
                map, cmdbConfig.getFlowControlDefaultLimit(), cmdbConfig.getFlowControlPrecision());
            flowController.init(redisTemplate,
                map,
                cmdbConfig.getFlowControlDefaultLimit(),
                cmdbConfig.getFlowControlPrecision());
        } catch (Exception e) {
            log.error("Fail to init globalFlowController", e);
        }


        return flowController;
    }
}
