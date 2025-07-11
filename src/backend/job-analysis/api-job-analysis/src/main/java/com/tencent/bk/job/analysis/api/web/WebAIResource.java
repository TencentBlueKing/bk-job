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

package com.tencent.bk.job.analysis.api.web;

import com.tencent.bk.job.analysis.model.web.req.AIAnalyzeErrorReq;
import com.tencent.bk.job.analysis.model.web.req.AICheckScriptReq;
import com.tencent.bk.job.analysis.model.web.req.AIGeneralChatReq;
import com.tencent.bk.job.analysis.model.web.req.GenerateChatStreamReq;
import com.tencent.bk.job.analysis.model.web.req.TerminateChatReq;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.model.web.resp.ClearChatHistoryResp;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

@Validated
@Api(tags = {"job-analysis:web:AI"})
@RequestMapping("/web/ai/scope/{scopeType}/{scopeId}")
@RestController
@WebAPI
public interface WebAIResource {

    @ApiOperation(value = "获取AI相关的配置参数，取值：\n" +
        "enabled：表示是否启用AI功能；\n" +
        "analyzeErrorLogMaxLength：表示分析报错信息时支持的最大日志长度，单位为字符；",
        produces = "application/json")
    @GetMapping("/config")
    Response<Map<String, Object>> getAIConfig(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId
    );

    @ApiOperation(value = "获取最近的AI对话记录历史（按产生时间倒序排列）", produces = "application/json")
    @GetMapping("/latestChatHistoryList")
    Response<List<AIChatRecord>> getLatestChatHistoryList(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @ApiParam(value = "对话记录起始位置，不传默认为0")
        @RequestParam(value = "start", defaultValue = "0")
        @Min(value = 0L, message = "{validation.constraints.AIInvalidHistoryStart.message}")
        Integer start,
        @ApiParam(value = "需要获取的对话记录条数，最大200条，不传默认20条")
        @RequestParam(value = "length", defaultValue = "20")
        @Range(max = 200L, message = "{validation.constraints.AIInvalidHistoryLength.message}")
        Integer length
    );

    @ApiOperation(value = "通用聊天接口", produces = "application/json")
    @PostMapping("/general/chat")
    Response<AIChatRecord> generalChat(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @ApiParam(value = "AI通用聊天参数", required = true)
        @Validated
        @RequestBody AIGeneralChatReq req
    );

    @ApiOperation(value = "检查脚本", produces = "application/json")
    @PostMapping("/checkScript")
    Response<AIChatRecord> checkScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @ApiParam(value = "AI检查脚本参数", required = true)
        @Validated
        @RequestBody AICheckScriptReq req
    );

    @ApiOperation(value = "分析报错信息", produces = "application/json")
    @PostMapping("/analyzeError")
    Response<AIChatRecord> analyzeError(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @ApiParam(value = "AI分析报错信息参数", required = true)
        @Validated
        @RequestBody AIAnalyzeErrorReq req
    );

    @ApiOperation(value = "获取单次对话流式数据（流式接口），返回换行符分隔的多条JSON数据，可分块读取，单条JSON数据格式：{\"success\":true,\"code\":0," +
        "\"errorMsg\":\"成功\",\"data\":{\"errorCode\":\"0\",\"errorMessage\":null,\"content\":\"hello world\"," +
        "\"time\":\"2024-08-14 12:00:00\"},\"requestId\":\"fb991170da868b2a1eb5835bc426e992\",\"authResult\": null," +
        "\"errorDetail\": null}", produces = "application/json")
    @PostMapping("/chatStream")
    ResponseEntity<StreamingResponseBody> generateChatStream(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @ApiParam(value = "生成流式响应数据请求参数", required = true)
        @Validated
        @RequestBody GenerateChatStreamReq req
    );

    @ApiOperation(value = "终止对话", produces = "application/json")
    @PutMapping("/terminateChat")
    Response<Boolean> terminateChat(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId,
        @ApiParam(value = "终止对话请求参数", required = true)
        @Validated
        @RequestBody TerminateChatReq req
    );

    @ApiOperation(value = "清空聊天记录", produces = "application/json")
    @DeleteMapping("/clearChatHistory")
    Response<ClearChatHistoryResp> clearChatHistory(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
        String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
        AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
        String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
        String scopeId
    );
}
