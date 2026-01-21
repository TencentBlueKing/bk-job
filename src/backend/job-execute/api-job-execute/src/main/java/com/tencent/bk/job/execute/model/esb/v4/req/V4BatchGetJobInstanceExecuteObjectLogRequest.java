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
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.execute.model.esb.v4.req.validator.ExecuteObjectsLogGroupSequenceProvider;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@GroupSequenceProvider(ExecuteObjectsLogGroupSequenceProvider.class)
public class V4BatchGetJobInstanceExecuteObjectLogRequest extends EsbAppScopeReq {

    @JsonProperty("job_instance_id")
    @NotNull(message = "{validation.constraints.InvalidJobInstanceId.message}")
    @Min(value = 1L, message = "{validation.constraints.InvalidJobInstanceId.message}")
    private Long jobInstanceId;

    @JsonProperty("step_instance_id")
    @NotNull(message = "{validation.constraints.InvalidStepInstanceId.message}")
    @Min(value = 1L, message = "{validation.constraints.InvalidStepInstanceId.message}")
    private Long stepInstanceId;

    /**
     * 要拉取作业日志的主机ID列表，不为空则忽略ip_list
     */
    @JsonProperty("host_id_list")
    @NotNull(message = "{validation.constraints.Host_empty.message}", groups = ValidateGroup.HostIdList.class)
    @Size(
        min = 1, max = 50,
        message = "{validation.constraints.InvalidExecuteObjectsSize.message}",
        groups = ValidateGroup.HostIdList.class
    )
    private List<Long> hostIdList;

    /**
     * 要拉取作业日志的ip列表
     */
    @JsonProperty("ip_list")
    @NotNull(message = "{validation.constraints.Host_empty.message}", groups = ValidateGroup.IpList.class)
    @Size(
        min = 1, max = 50,
        message = "{validation.constraints.InvalidExecuteObjectsSize.message}",
        groups = ValidateGroup.IpList.class
    )
    @Valid
    private List<OpenApiV4HostDTO> ipList;

    /**
     * 要拉取作业日志的容器ID列表
     */
    @JsonProperty("container_id_list")
    @Size(
        min = 1, max = 50,
        message = "{validation.constraints.InvalidExecuteObjectsSize.message}",
        groups = ValidateGroup.ContainerIdList.class
    )
    private List<Long> containerIdList;

    public interface ValidateGroup {
        interface HostIdList {
        }

        interface IpList {
        }

        interface ContainerIdList {
        }
    }
}
