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

package com.tencent.bk.job.manage.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TemplateBasicInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TemplateTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @since 15/10/2019 17:16
 */
@Api(tags = {"job-manage:web:Task_Template_Management"})
@RequestMapping("/web/app/{appId}/task/template")
@RestController
@WebAPI
public interface WebTaskTemplateResource {

    @ApiOperation(value = "获取模版基本信息列表", produces = "application/json")
    @GetMapping
    Response<PageData<TaskTemplateVO>> listPageTemplates(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID")
        @RequestParam(value = "templateId", required = false) Long templateId,
        @ApiParam(value = "模版名称")
        @RequestParam(value = "name", required = false) String name,
        @ApiParam(value = "模版状态")
        @RequestParam(value = "status", required = false) Integer status,
        @ApiParam(value = "模版标签")
        @RequestParam(value = "tags", required = false) String tags,
        @ApiParam(value = "左侧模版标签")
        @RequestParam(value = "panelTag", required = false) Long panelTag,
        @ApiParam(value = "模版类型 1 - 全部 2 - 未分类 3 - 待更新")
        @RequestParam(value = "type", required = false) Integer type,
        @ApiParam(value = "创建人")
        @RequestParam(value = "creator", required = false) String creator,
        @ApiParam(value = "更新人")
        @RequestParam(value = "lastModifyUser", required = false) String lastModifyUser,
        @ApiParam(value = "分页-开始 -1 不分页")
        @RequestParam(value = "start", required = false) Integer start,
        @ApiParam(value = "分页-每页大小 -1 不分页")
        @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @ApiParam(value = "排序字段")
        @RequestParam(value = "orderField", required = false) String orderField,
        @ApiParam(value = "排序顺序 0-降序 1-升序")
        @RequestParam(value = "order", required = false) Integer order
    );

    @ApiOperation(value = "根据模版 ID 获取模版信息", produces = "application/json")
    @GetMapping("/{templateId}")
    Response<TaskTemplateVO> getTemplateById(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable("templateId") Long templateId
    );

    @ApiOperation(value = "更新模版", produces = "application/json")
    @PutMapping("/{templateId}")
    Response<Long> saveTemplate(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID 新建填 0", required = true)
        @PathVariable("templateId") Long templateId,
        @ApiParam(value = "新增/更新的模版对象", name = "templateCreateUpdateReq", required = true)
        @RequestBody @Validated TaskTemplateCreateUpdateReq taskTemplateCreateUpdateReq
    );

    @ApiOperation(value = "删除模版", produces = "application/json")
    @DeleteMapping("/{templateId}")
    Response<Boolean> deleteTemplate(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID", required = true)
        @PathVariable(value = "templateId") Long templateId
    );

    @ApiOperation(value = "获取业务下标签关联的模版数量", produces = "application/json")
    @GetMapping("/tag/count")
    Response<TagCountVO> getTagTemplateCount(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId") Long appId
    );

    @ApiOperation(value = "更新模版元数据，如模版描述、名称、标签", produces = "application/json")
    @PutMapping("/{templateId}/basic")
    Response<Boolean> updateTemplateBasicInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版ID", required = true)
        @PathVariable("templateId") Long templateId,
        @ApiParam(value = "模版元数据更新请求报文", name = "templateBasicInfoUpdateReq", required = true)
        @RequestBody TemplateBasicInfoUpdateReq templateBasicInfoUpdateReq
    );

    @ApiOperation(value = "新增收藏", produces = "application/json")
    @PutMapping("/{templateId}/favorite")
    Response<Boolean> addFavorite(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版ID", required = true)
        @PathVariable("templateId") Long templateId
    );

    @ApiOperation(value = "删除收藏", produces = "application/json")
    @DeleteMapping("/{templateId}/favorite")
    Response<Boolean> removeFavorite(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版ID", required = true)
        @PathVariable("templateId") Long templateId
    );

    @ApiOperation(value = "检查作业模版名称是否已占用", produces = "application/json")
    @GetMapping("/{templateId}/check_name")
    Response<Boolean> checkTemplateName(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "作业模版 ID，新建时填 0", required = true)
        @PathVariable("templateId") Long templateId,
        @ApiParam(value = "名称", required = true)
        @RequestParam(value = "name") String name
    );

    @ApiOperation(value = "根据 ID 批量获取模版基本信息列表", produces = "application/json")
    @GetMapping("/basic")
    Response<List<TaskTemplateVO>> listTemplateBasicInfoByIds(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版 ID 列表，逗号分隔")
        @RequestParam("ids") List<Long> templateIds
    );

    @ApiOperation(value = "批量更新模板标签-Patch方式", produces = "application/json")
    @PutMapping("/tag")
    Response<Boolean> batchPatchTemplateTags(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "模版标签批量更新请求报文", name = "tagBatchPatchReq", required = true)
        @RequestBody TemplateTagBatchPatchReq req
    );

}
