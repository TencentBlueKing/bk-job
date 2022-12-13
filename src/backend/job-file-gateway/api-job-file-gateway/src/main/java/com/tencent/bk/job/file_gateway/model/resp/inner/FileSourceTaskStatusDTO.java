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

package com.tencent.bk.job.file_gateway.model.resp.inner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FileSourceTaskStatusDTO {
    String taskId;
    Byte status;
    String message;
    Long cloudId;
    String ipProtocol;
    String ip;
    Boolean fileCleared;
    /**
     * 文件源文件路径->文件在机器上的真实路径
     */
    Map<String, String> filePathMap;
    List<ThirdFileSourceTaskLogDTO> logList;
    Boolean logEnd;

    public boolean isDone() {
        return TaskStatusEnum.isDone(status);
    }

    /**
     * 通过一些关键字段描述对象，通常用于日志打印忽略非关键字段
     *
     * @return 描述信息
     */
    @JsonIgnore
    public String getSimpleDesc() {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", taskId);
        map.put("status", status);
        map.put("message", message);
        return JsonUtils.toJson(map);
    }
}
