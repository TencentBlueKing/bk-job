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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.dao.TenantHostDAO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 指定租户的主机DAO
 */
@Slf4j
@Repository
public class TenantHostDAOImpl extends AbstractBaseHostDAO implements TenantHostDAO {

    @Autowired
    public TenantHostDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context,
                             ApplicationDAO applicationDAO,
                             HostTopoDAO hostTopoDAO,
                             TopologyHelper topologyHelper) {
        super(context, applicationDAO, hostTopoDAO, topologyHelper);
    }

    @Override
    protected List<Condition> getBasicConditions() {
        return new ArrayList<>();
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostIds(String tenantId, Collection<Long> hostIds) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.TENANT_ID.eq(tenantId));
        conditions.add(TABLE.HOST_ID.in(hostIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostsByCloudIps(String tenantId, Collection<String> cloudIps) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.TENANT_ID.eq(tenantId));
        conditions.add(TABLE.CLOUD_IP.in(cloudIps));
        return queryHostsByCondition(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByCloudIpv6(String tenantId, Long cloudAreaId, String ipv6) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TENANT_ID.eq(tenantId));
        if (cloudAreaId != null) {
            conditions.add(TABLE.CLOUD_ID.eq(cloudAreaId));
        }
        conditions.add(TABLE.IP_V6.like("%" + ipv6 + "%"));
        return listHostInfoByConditions(conditions);
    }
}
