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

package com.tencent.bk.job.manage.model.dto.task;

import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.esb.model.job.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskNodeInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @since 12/12/2019 22:12
 */
@PersistenceObject
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskNodeInfoDTO {

    private Long id;

    private String name;

    private String type;

    public static TaskNodeInfoDTO fromVO(TargetNodeVO targetNodeVO) {
        if (targetNodeVO == null) {
            return null;
        }
        TaskNodeInfoDTO nodeInfo = new TaskNodeInfoDTO();
        nodeInfo.setId(targetNodeVO.getInstanceId());
        nodeInfo.setType(targetNodeVO.getObjectId());
        return nodeInfo;
    }

    public static EsbCmdbTopoNodeDTO toEsbCmdbTopoNode(TaskNodeInfoDTO taskNodeInfo) {
        if (taskNodeInfo == null) {
            return null;
        }
        EsbCmdbTopoNodeDTO esbCmdbTopoNodeDTO = new EsbCmdbTopoNodeDTO();
        esbCmdbTopoNodeDTO.setId(taskNodeInfo.getId());
        esbCmdbTopoNodeDTO.setNodeType(taskNodeInfo.getType());
        return esbCmdbTopoNodeDTO;
    }

    public TargetNodeVO toVO() {
        TargetNodeVO targetNodeVO = new TargetNodeVO();
        targetNodeVO.setInstanceId(this.getId());
        targetNodeVO.setObjectId(this.getType());
        return targetNodeVO;
    }

    public ServiceTaskNodeInfoDTO toServiceTaskHostNodeDTO() {
        ServiceTaskNodeInfoDTO serviceTaskNodeInfoDTO = new ServiceTaskNodeInfoDTO();
        serviceTaskNodeInfoDTO.setId(this.getId());
        serviceTaskNodeInfoDTO.setType(this.getType());
        return serviceTaskNodeInfoDTO;
    }
}
