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

package com.tencent.bk.job.analysis.model.web.req;

import com.tencent.bk.job.analysis.validation.CheckSceneResourceId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "保存AI场景会话请求体")
@CheckSceneResourceId
@Data
public class SaveAIChatSessionReq {

    @Schema(description = "场景类型: 1-任务报错分析, 2-脚本管理, 3-自由对话")
    @NotNull(message = "{validation.constraints.AIChatSession_sceneTypeEmpty.message}")
    private Integer sceneType;

    @Schema(description = "场景资源标识(stepInstanceId/scriptId等)，自由对话场景可不传")
    private String sceneResourceId;

    @Schema(description = "AI智能体会话ID")
    @NotBlank(message = "{validation.constraints.AIChatSession_aiSessionIdEmpty.message}")
    private String aiSessionId;

    @Schema(description = "会话名称")
    @NotBlank(message = "{validation.constraints.AIChatSession_sessionNameEmpty.message}")
    private String sessionName;
}
