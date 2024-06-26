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

package com.tencent.bk.job.execute.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.validation.ValidFieldsStrictValue;
import com.tencent.bk.job.common.validation.ValidCollectionSizeOutOfLimit;
import com.tencent.bk.job.common.validation.ValidationConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@ValidFieldsStrictValue(fieldNames = {"ipList", "hostIdList"})
@ValidCollectionSizeOutOfLimit(fieldNames = {"ipList", "hostIdList"})
public class EsbBatchGetJobInstanceIpLogV3Request extends EsbAppScopeReq {

    /**
     * 作业执行实例 ID
     */
    @JsonProperty("job_instance_id")
    @NotNull(message = "{validation.constraints.InvalidJobInstanceId.message}")
    @Min(value = ValidationConstants.COMMON_MIN_1, message = "{validation.constraints.InvalidJobInstanceId.message}")
    private Long taskInstanceId;

    /**
     * 作业步骤实例ID
     */
    @JsonProperty("step_instance_id")
    @NotNull(message = "{validation.constraints.InvalidStepInstanceId.message}")
    @Min(value = ValidationConstants.COMMON_MIN_1, message = "{validation.constraints.InvalidStepInstanceId.message}")
    private Long stepInstanceId;

    /**
     * 目标服务器IP列表
     */
    @JsonProperty("ip_list")
    @Valid
    private List<EsbIpDTO> ipList;

    /**
     * 目标主机ID列表
     */
    @JsonProperty("host_id_list")
    private List<Long> hostIdList;
}
