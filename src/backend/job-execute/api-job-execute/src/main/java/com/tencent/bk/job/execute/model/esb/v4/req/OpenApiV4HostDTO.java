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

package com.tencent.bk.job.execute.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.model.esb.v4.req.validator.V4HostGroupSequenceProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@GroupSequenceProvider(V4HostGroupSequenceProvider.class)
public class OpenApiV4HostDTO {

    @JsonProperty("bk_cloud_id")
    @NotNull(
        message = "{validation.constraints.InvalidBkCloudId.message}",
        groups = ValidationGroups.HostType.CloudIdIp.class
    )
    private Long bkCloudId;

    @JsonProperty("ip")
    @Pattern(regexp = "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)"
        + "\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)"
        + "\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b",
        message = "{validation.constraints.InvalidIp.message}",
        groups = ValidationGroups.HostType.CloudIdIp.class
    )
    @NotNull(
        message = "{validation.constraints.InvalidIp.message}",
        groups = ValidationGroups.HostType.CloudIdIp.class
    )
    private String ip;

    @JsonProperty("bk_host_id")
    @NotNull(
        message = "{validation.constraints.BkHostId_null.message}",
        groups = ValidationGroups.HostType.HostId.class
    )
    private Long bkHostId;

    public OpenApiV4HostDTO(HostDTO hostDTO) {
        this.bkHostId = hostDTO.getHostId();
        this.bkCloudId = hostDTO.getBkCloudId();
        this.ip = hostDTO.getIp();
    }
}
