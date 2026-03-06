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

package com.tencent.bk.job.crontab.model.inner;

import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.model.vo.ContainerVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定时任务容器信息 DTO
 */
@Data
@NoArgsConstructor
@PersistenceObject
@ApiModel("容器信息")
@AllArgsConstructor
public class CronJobContainerDTO implements Cloneable {

    @ApiModelProperty("CMDB中存的容器 ID")
    private Long id;

    public static CronJobContainerDTO fromContainerVO(ContainerVO containerVO) {
        if (containerVO == null) {
            return null;
        }
        CronJobContainerDTO dto = new CronJobContainerDTO();
        dto.setId(containerVO.getId());
        return dto;
    }

    public static ContainerVO toContainerVO(CronJobContainerDTO dto) {
        if (dto == null) {
            return null;
        }
        ContainerVO vo = new ContainerVO();
        vo.setId(dto.getId());
        return vo;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public CronJobContainerDTO clone() {
        CronJobContainerDTO clone = new CronJobContainerDTO();
        clone.setId(this.id);
        return clone;
    }
}

