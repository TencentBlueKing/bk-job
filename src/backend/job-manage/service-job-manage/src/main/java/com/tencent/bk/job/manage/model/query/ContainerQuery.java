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

package com.tencent.bk.job.manage.model.query;

import com.tencent.bk.job.common.cc.model.BaseRuleDTO;
import com.tencent.bk.job.common.cc.model.PropertyFilterDTO;
import com.tencent.bk.job.common.cc.model.req.ListKubeContainerByTopoReq;
import com.tencent.bk.job.common.cc.model.req.Page;
import com.tencent.bk.job.common.cc.model.req.field.ContainerFields;
import com.tencent.bk.job.common.cc.model.req.field.PodFields;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * 容器查询
 */
@Getter
@ToString
@NoArgsConstructor
@Builder
public class ContainerQuery {
    private Long bizId;

    private Long clusterId;

    private Long namespaceId;

    private Long workloadId;

    private String containerUID;

    private String containerName;

    private String podName;

    private BaseSearchCondition baseSearchCondition;

    public ListKubeContainerByTopoReq toListKubeContainerByTopoReq() {
        ListKubeContainerByTopoReq req = new ListKubeContainerByTopoReq();
        req.setBizId(bizId);
        req.setClusterId(clusterId);
        req.setNamespaceId(namespaceId);
        req.setWorkloadId(workloadId);

        setContainerFilterIfNecessary(req);

        setPodFilterIfNecessary(req);

        setPageIfNecessary(req);

        return req;
    }

    private void setContainerFilterIfNecessary(ListKubeContainerByTopoReq req) {
        if (StringUtils.isNotEmpty(containerUID) || StringUtils.isNotEmpty(containerName)) {
            PropertyFilterDTO containerFilter = new PropertyFilterDTO();
            containerFilter.setCondition("AND");

            if (StringUtils.isNotEmpty(containerUID)) {
                BaseRuleDTO rule = new BaseRuleDTO();
                rule.setField(ContainerFields.CONTAINER_UID);
                rule.setOperator("equal");
                rule.setValue(containerUID);
                containerFilter.addRule(rule);
            }

            if (StringUtils.isNotEmpty(containerName)) {
                BaseRuleDTO rule = new BaseRuleDTO();
                rule.setField(ContainerFields.NAME);
                rule.setOperator("equal");
                rule.setValue(containerName);
                containerFilter.addRule(rule);
            }
            req.setContainerFilter(containerFilter);
        }
    }

    private void setPodFilterIfNecessary(ListKubeContainerByTopoReq req) {
        if (StringUtils.isNotEmpty(podName)) {
            PropertyFilterDTO podFilter = new PropertyFilterDTO();
            podFilter.setCondition("AND");

            BaseRuleDTO rule = new BaseRuleDTO();
            rule.setField(PodFields.NAME);
            rule.setOperator("equal");
            rule.setValue(podName);
            podFilter.addRule(rule);

            req.setPodFilter(podFilter);
        }
    }

    private void setPageIfNecessary(ListKubeContainerByTopoReq req) {
        if (baseSearchCondition != null) {
            Page page = new Page();
            page.setStart(baseSearchCondition.getStartOrDefault(0));
            page.setLimit(baseSearchCondition.getLengthOrDefault(10));
            page.setSort(ContainerFields.ID);
            req.setPage(page);
        }
    }

}
