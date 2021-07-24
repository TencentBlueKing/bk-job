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

package com.tencent.bk.job.common.web.config;

import com.ecwid.consul.v1.agent.model.NewService;
import com.tencent.bk.job.common.web.consts.JobConsulConsts;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulManagementRegistrationCustomizer;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistrationCustomizer;
import org.springframework.context.ApplicationContext;

import java.util.List;

public class JobConsulAutoRegistration extends ConsulAutoRegistration {
    public JobConsulAutoRegistration(NewService service,
                                     AutoServiceRegistrationProperties autoServiceRegistrationProperties,
                                     ConsulDiscoveryProperties properties, ApplicationContext context,
                                     HeartbeatProperties heartbeatProperties,
                                     List<ConsulManagementRegistrationCustomizer> managementRegistrationCustomizers) {
        super(service, autoServiceRegistrationProperties, properties, context, heartbeatProperties,
            managementRegistrationCustomizers);
    }


    public static ConsulAutoRegistration registration(
        AutoServiceRegistrationProperties autoServiceRegistrationProperties,
        ConsulDiscoveryProperties properties, ApplicationContext context,
        List<ConsulRegistrationCustomizer> registrationCustomizers,
        List<ConsulManagementRegistrationCustomizer> managementRegistrationCustomizers,
        HeartbeatProperties heartbeatProperties, BuildProperties buildProperties) {
        ConsulAutoRegistration registration = ConsulAutoRegistration.registration(autoServiceRegistrationProperties,
            properties, context, registrationCustomizers, managementRegistrationCustomizers, heartbeatProperties);
        // 将版本号写入Tag中
        registration.getService().getTags().add(JobConsulConsts.TAG_KEY_VERSION + "=" + buildProperties.getVersion());
        // 区分Job后台服务与组件（Redis、MQ等）
        registration.getService().getTags()
            .add(JobConsulConsts.TAG_KEY_TYPE + "=" + JobConsulConsts.TAG_VALUE_TYPE_JOB_BACKEND_SERVICE);
        return registration;
    }
}
