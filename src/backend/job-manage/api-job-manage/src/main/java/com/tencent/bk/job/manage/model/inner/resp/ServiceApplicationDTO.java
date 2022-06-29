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

package com.tencent.bk.job.manage.model.inner.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationAttrsDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 业务
 */
@Data
@ApiModel("业务")
public class ServiceApplicationDTO {


    @ApiModelProperty("业务ID")
    private Long id;

    /**
     * 资源范围类型
     */
    private String scopeType;
    /**
     * 资源范围ID,比如cmdb业务ID、cmdb业务集ID
     */
    private String scopeId;

    /**
     * 业务名称
     */
    @ApiModelProperty("业务名称")
    private String name;

    /**
     * 业务类型
     */
    @ApiModelProperty("业务类型")
    @CompatibleImplementation(explain = "兼容字段，等发布完成之后可以删除", version = "3.5.x")
    private Integer appType;

    /**
     * 运维
     */
    private String maintainers;

    /**
     * 子业务
     */
    @ApiModelProperty("子业务ID")
    @CompatibleImplementation(explain = "兼容字段，等发布完成之后可以删除", version = "3.5.x")
    @JsonProperty("subAppIds")
    private List<Long> subBizIds;

    @ApiModelProperty("开发商")
    private String owner;

    /**
     * 初始运维部门Id
     */
    @CompatibleImplementation(explain = "兼容字段，等发布完成之后可以删除", version = "3.5.x")
    private Long operateDeptId;

    /**
     * 时区
     */
    private String timeZone;

    /**
     * 语言
     */
    private String language;

    /**
     * 业务属性
     */
    private ServiceApplicationAttrsDTO attrs;

    /**
     * 将服务间调用的业务对象转为通用Job业务对象
     *
     * @param serviceAppDTO 服务间调用业务对象
     * @return 通用Job业务对象
     */
    public static ApplicationDTO toApplicationInfoDTO(ServiceApplicationDTO serviceAppDTO) {
        ApplicationDTO applicationInfo = new ApplicationDTO();
        applicationInfo.setId(serviceAppDTO.getId());
        applicationInfo.setName(serviceAppDTO.getName());
        applicationInfo.setScope(new ResourceScope(serviceAppDTO.getScopeType(), serviceAppDTO.getScopeId()));
        applicationInfo.setSubBizIds(serviceAppDTO.getSubBizIds());
        applicationInfo.setLanguage(serviceAppDTO.getLanguage());
        return applicationInfo;
    }

    /**
     * 将通用Job业务对象转为服务间调用的业务对象
     *
     * @param appDTO 通用Job业务对象
     * @return 服务间调用业务对象
     */
    public static ServiceApplicationDTO fromApplicationDTO(ApplicationDTO appDTO) {
        if (appDTO == null) {
            return null;
        }
        ServiceApplicationDTO app = new ServiceApplicationDTO();
        app.setId(appDTO.getId());
        app.setSubBizIds(appDTO.getSubBizIds());
        app.setName(appDTO.getName());
        app.setScopeType(appDTO.getScope().getType().getValue());
        app.setScopeId(appDTO.getScope().getId());
        app.setOwner(appDTO.getBkSupplierAccount());
        app.setTimeZone(appDTO.getTimeZone());
        // 暂时保留appType/maintainers/operateDeptId三个参数
        app.setAppType(appDTO.getAppType().getValue());
        app.setMaintainers(appDTO.getMaintainers());
        app.setOperateDeptId(appDTO.getOperateDeptId());
        return app;
    }

    public boolean isBiz() {
        return ResourceScopeTypeEnum.BIZ.getValue().equals(scopeType);
    }
}
