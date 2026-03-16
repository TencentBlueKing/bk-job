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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TemplateBasicInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TemplateTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;

/**
 * @since 15/10/2019 17:16
 */
@Tag(name = "job-manage:web:Task_Template_Management")
@RequestMapping("/web/scope/{scopeType}/{scopeId}/task/template")
@RestController
@WebAPI
public interface WebTaskTemplateResource {

    @Operation(summary = "获取模版基本信息列表")
    @GetMapping
    Response<PageData<TaskTemplateVO>> listPageTemplates(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID")
        @RequestParam(value = "templateId", required = false)
            Long templateId,
        @Parameter(description = "模版名称")
        @RequestParam(value = "name", required = false)
            String name,
        @Parameter(description = "模版状态")
        @RequestParam(value = "status", required = false)
            Integer status,
        @Parameter(description = "模版标签")
        @RequestParam(value = "tags", required = false)
            String tags,
        @Parameter(description = "左侧模版标签")
        @RequestParam(value = "panelTag", required = false)
            Long panelTag,
        @Parameter(description = "模版类型 1 - 全部 2 - 未分类 3 - 待更新")
        @RequestParam(value = "type", required = false)
            Integer type,
        @Parameter(description = "创建人")
        @RequestParam(value = "creator", required = false)
            String creator,
        @Parameter(description = "更新人")
        @RequestParam(value = "lastModifyUser", required = false)
            String lastModifyUser,
        @Parameter(description = "分页-开始 -1 不分页")
        @RequestParam(value = "start", required = false)
            Integer start,
        @Parameter(description = "分页-每页大小 -1 不分页")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @Parameter(description = "排序字段")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @Parameter(description = "排序顺序 0-降序 1-升序")
        @RequestParam(value = "order", required = false)
            Integer order
    );

    @Operation(summary = "根据模版 ID 获取模版信息")
    @GetMapping("/{templateId}")
    Response<TaskTemplateVO> getTemplateById(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId
    );


    @Operation(summary = "新建模版")
    @PostMapping
    Response<TaskTemplateVO> createTemplate(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "新增模版对象", name = "request", required = true)
        @RequestBody
        @Validated
            TaskTemplateCreateUpdateReq request
    );

    @Operation(summary = "更新模版")
    @PutMapping("/{templateId}")
    Response<TaskTemplateVO> updateTemplate(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "新增/更新的模版对象", name = "request", required = true)
        @RequestBody
        @Validated
            TaskTemplateCreateUpdateReq request
    );

    @Operation(summary = "删除模版")
    @DeleteMapping("/{templateId}")
    Response<Boolean> deleteTemplate(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID", required = true)
        @PathVariable(value = "templateId")
            Long templateId
    );

    @Operation(summary = "获取业务下标签关联的模版数量")
    @GetMapping("/tag/count")
    Response<TagCountVO> getTagTemplateCount(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId
    );

    @Operation(summary = "更新模版元数据，如模版描述、名称、标签")
    @PutMapping("/{templateId}/basic")
    Response<Boolean> updateTemplateBasicInfo(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版ID", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "模版元数据更新请求报文", name = "request", required = true)
        @RequestBody
            TemplateBasicInfoUpdateReq request
    );

    @Operation(summary = "新增收藏")
    @PutMapping("/{templateId}/favorite")
    Response<Boolean> addFavorite(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版ID", required = true)
        @PathVariable("templateId")
            Long templateId
    );

    @Operation(summary = "删除收藏")
    @DeleteMapping("/{templateId}/favorite")
    Response<Boolean> removeFavorite(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版ID", required = true)
        @PathVariable("templateId")
            Long templateId
    );

    @Operation(summary = "检查作业模版名称是否已占用")
    @GetMapping("/{templateId}/check_name")
    Response<Boolean> checkTemplateName(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "作业模版 ID，新建时填 0", required = true)
        @PathVariable("templateId")
            Long templateId,
        @Parameter(description = "名称", required = true)
        @RequestParam(value = "name")
            String name
    );

    @Operation(summary = "根据 ID 批量获取模版基本信息列表")
    @GetMapping("/basic")
    Response<List<TaskTemplateVO>> listTemplateBasicInfoByIds(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版 ID 列表，逗号分隔")
        @RequestParam("ids")
            List<Long> templateIds
    );

    @Operation(summary = "批量更新模板标签-Patch方式")
    @PutMapping("/tag")
    Response<Boolean> batchPatchTemplateTags(
        @Parameter(description = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @Hidden
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @Parameter(description = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @Parameter(description = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @Parameter(description = "模版标签批量更新请求报文", name = "tagBatchPatchReq", required = true)
        @RequestBody
            TemplateTagBatchPatchReq req
    );

}
