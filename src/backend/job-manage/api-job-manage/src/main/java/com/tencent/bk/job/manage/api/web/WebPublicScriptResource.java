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
import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.common.validation.Update;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptSyncReq;
import com.tencent.bk.job.manage.model.web.request.ScriptTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.request.ScriptVersionCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.BasicScriptVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptVO;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteInfoVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptRelatedTemplateStepVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptSyncResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共脚本管理API-前端调用
 */
@Api(value = "公共脚本管理", tags = {"job-manage:web:Public_Script_Management"})
@RequestMapping("/web/public_script")
@RestController
@WebAPI
public interface WebPublicScriptResource {

    @ApiOperation(value = "根据脚本版本ID获取脚本版本详情", produces = "application/json")
    @GetMapping("/scriptVersion/{scriptVersionId}")
    Response<ScriptVO> getScriptVersionDetail(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本版本ID", required = true, example = "1")
        @PathVariable("scriptVersionId") Long scriptVersionId);

    @ApiOperation(value = "根据脚本ID获取脚本详情", produces = "application/json")
    @GetMapping("/script/{scriptId}")
    Response<ScriptVO> getScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本ID", required = true, example = "1")
        @PathVariable("scriptId") String scriptId);

    @ApiOperation(value = "根据脚本ID获取脚本基本信息", produces = "application/json")
    @GetMapping("/script/basic/{scriptId}")
    Response<ScriptVO> getScriptBasicInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本ID", required = true, example = "1")
        @PathVariable("scriptId") String scriptId);

    @ApiOperation(value = "根据脚本ID获取已上线脚本", produces = "application/json")
    @GetMapping("/scriptVersion/online/{scriptId}")
    Response<ScriptVO> getOnlineScriptVersionByScriptId(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本ID", required = true, example = "1")
        @PathVariable("scriptId") String scriptId);

    @ApiOperation(value = "获取公共脚本列表", produces = "application/json")
    @GetMapping("/script/list")
    Response<PageData<ScriptVO>> listPageScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本名称")
        @RequestParam(value = "name", required = false)
            String name,
        @ApiParam("脚本类型")
        @RequestParam(value = "type", required = false)
            Integer type,
        @ApiParam("脚本标签")
        @RequestParam(value = "tags", required = false)
            String tags,
        @ApiParam(value = "左侧模版标签")
        @RequestParam(value = "panelTag", required = false)
            Long panelTag,
        @ApiParam(value = "脚本在导航栏的分类,1-全部,2-未分类")
        @RequestParam(value = "panelType", required = false)
            Integer panelType,
        @ApiParam("创建人")
        @RequestParam(value = "creator", required = false)
            String creator,
        @ApiParam("更新人")
        @RequestParam(value = "lastModifyUser", required = false)
            String lastModifyUser,
        @ApiParam("脚本ID")
        @RequestParam(value = "scriptId", required = false)
            String scriptId,
        @ApiParam("脚本内容关键字,支持模糊搜索")
        @RequestParam(value = "content", required = false)
            String content,
        @ApiParam("分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam("分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @ApiParam("排序字段,脚本名:name,脚本类型:type,标签:tags,创建人:creator")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @ApiParam("排序顺序,0:降序;1:升序")
        @RequestParam(value = "order", required = false)
            Integer order);

    @ApiOperation(value = "获取脚本列表(仅包含基础信息)", produces = "application/json")
    @GetMapping("/script/basic/list")
    Response<List<ScriptVO>> listScriptBasicInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本ID列表，多个ID之间用,分隔", required = true)
        @RequestParam("ids") List<String> scriptIds);

    @ApiOperation(value = "获取脚本的所有版本", produces = "application/json")
    @GetMapping("/script/{scriptId}/scriptVersion/list")
    Response<List<ScriptVO>> listScriptVersion(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("脚本ID")
        @PathVariable("scriptId")
            String scriptId);

    @ApiOperation(value = "根据条件查询业务下的脚本名称列表", produces = "application/json")
    @GetMapping("/scriptNames")
    Response<List<String>> listPublicScriptNames(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("脚本名称")
        @RequestParam("scriptName")
            String scriptName);

    @ApiOperation(value = "获取业务下面的已在线脚本列表", produces = "application/json")
    @GetMapping("/scripts/online")
    Response<List<BasicScriptVO>> listScriptOnline(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username
    );

    @ApiOperation(value = "更新脚本元数据，比如脚本描述、名称、标签", produces = "application/json")
    @PutMapping("/script/{scriptId}/info")
    Response<ScriptVO> updateScriptInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本ID", required = true, example = "uuid")
        @PathVariable("scriptId")
            String scriptId,
        @ApiParam(value = "脚本元数据更新请求报文", name = "scriptInfoUpdateReq", required = true)
        @RequestBody ScriptInfoUpdateReq scriptInfoUpdateReq);


    @ApiOperation(value = "创建脚本", produces = "application/json")
    @PostMapping("/script")
    Response<ScriptVO> saveScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "新增/更新的脚本对象", name = "request", required = true)
        @RequestBody
        @Validated
            ScriptCreateReq request
    );

    @ApiOperation(value = "新增脚本版本", produces = "application/json")
    @PostMapping("/script/{scriptId}/scriptVersion")
    Response<ScriptVO> saveScriptVersion(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @PathVariable(value = "scriptId")
            String scriptId,
        @ApiParam(value = "新增脚本版本请求", name = "request", required = true)
        @RequestBody
        @Validated(Create.class)
            ScriptVersionCreateUpdateReq request
    );

    @ApiOperation(value = "更新脚本版本", produces = "application/json")
    @PutMapping("/script/{scriptId}/scriptVersion/{scriptVersionId}")
    Response<ScriptVO> updateScriptVersion(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @PathVariable(value = "scriptId")
            String scriptId,
        @PathVariable(value = "scriptVersionId")
            Long scriptVersionId,
        @ApiParam(value = "更新脚本版本请求", name = "request", required = true)
        @RequestBody
        @Validated(Update.class)
            ScriptVersionCreateUpdateReq request
    );

    @ApiOperation(value = "上线脚本", produces = "application/json")
    @PutMapping("/script/{scriptId}/scriptVersion/{scriptVersionId}/publish")
    Response publishScriptVersion(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("脚本ID")
        @PathVariable("scriptId")
            String scriptId,
        @ApiParam("脚本版本ID")
        @PathVariable("scriptVersionId")
            Long scriptVersionId);

    @ApiOperation(value = "下线脚本", produces = "application/json")
    @PutMapping("/script/{scriptId}/scriptVersion/{scriptVersionId}/disable")
    Response disableScriptVersion(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("脚本ID")
        @PathVariable("scriptId")
            String scriptId,
        @ApiParam("脚本版本ID")
        @PathVariable("scriptVersionId")
            Long scriptVersionId);

    @ApiOperation(value = "删除脚本", produces = "application/json")
    @DeleteMapping("/script/{scriptId}")
    Response deleteScriptByScriptId(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("脚本ID")
        @PathVariable("scriptId")
            String scriptId);

    @ApiOperation(value = "删除某个版本的脚本", produces = "application/json")
    @DeleteMapping("/scriptVersion/{scriptVersionId}")
    Response deleteScriptByScriptVersionId(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("脚本版本ID")
        @PathVariable("scriptVersionId")
            Long scriptVersionId);


    @ApiOperation(value = "获取脚本可以同步的模板与步骤信息", produces = "application/json")
    @GetMapping("/script/{scriptId}/scriptVersion/{scriptVersionId}/syncTemplateSteps")
    Response<List<ScriptRelatedTemplateStepVO>> listScriptSyncTemplateSteps(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "scriptId", required = true)
        @PathVariable(value = "scriptId") String scriptId,
        @ApiParam(value = "scriptVersionId", required = true)
        @PathVariable(value = "scriptVersionId") Long scriptVersionId);

    @ApiOperation(value = "同步脚本", produces = "application/json")
    @PostMapping("/script/{scriptId}/scriptVersion/{scriptVersionId}/sync")
    Response<List<ScriptSyncResultVO>> syncScripts(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本ID", required = true)
        @PathVariable(value = "scriptId") String scriptId,
        @ApiParam(value = "脚本版本ID", required = true)
        @PathVariable(value = "scriptVersionId") Long scriptVersionId,
        @ApiParam(value = "脚本检查请求报文", name = "scriptSyncReq", required = true)
        @RequestBody ScriptSyncReq scriptSyncReq);

    @ApiOperation(value = "根据脚本ID/脚本版本ID获取脚本被引次数", produces = "application/json")
    @GetMapping("/citeCount")
    Response<ScriptCiteCountVO> getPublicScriptCiteCount(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本ID", example = "1")
        @RequestParam(value = "scriptId") String scriptId,
        @ApiParam(value = "脚本版本ID", example = "1")
        @RequestParam(value = "scriptVersionId", required = false) Long scriptVersionId
    );

    @ApiOperation(value = "根据脚本ID/脚本版本ID获取脚本引用信息", produces = "application/json")
    @GetMapping("/citeInfo")
    Response<ScriptCiteInfoVO> getPublicScriptCiteInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本ID", example = "1")
        @RequestParam("scriptId") String scriptId,
        @ApiParam(value = "脚本版本ID", example = "1")
        @RequestParam(value = "scriptVersionId", required = false) Long scriptVersionId
    );

    @ApiOperation(value = "批量更新脚本标签-Patch方式", produces = "application/json")
    @PutMapping("/tag")
    Response<?> batchUpdatePublicScriptTags(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本标签批量更新请求报文", name = "tagBatchUpdateReq", required = true)
        @RequestBody ScriptTagBatchPatchReq tagBatchUpdateReq
    );

    @ApiOperation(value = "获取业务下标签关联的脚本数量", produces = "application/json")
    @GetMapping("/tag/count")
    Response<TagCountVO> getTagPublicScriptCount(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username
    );
}
