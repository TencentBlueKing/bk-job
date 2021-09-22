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
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.manage.model.web.request.ScriptCheckReq;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.request.ScriptSyncReq;
import com.tencent.bk.job.manage.model.web.request.ScriptTagBatchPatchReq;
import com.tencent.bk.job.manage.model.web.vo.BasicScriptVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptCheckResultItemVO;
import com.tencent.bk.job.manage.model.web.vo.ScriptVO;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteCountVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptCiteInfoVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptRelatedTemplateStepVO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptSyncResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 脚本管理API-前端调用
 *
 * @date 2019/09/19
 */
@Api(value = "脚本管理", tags = {"job-manage:web:Script_Management"})
@RequestMapping("/web/script")
@RestController
@WebAPI
public interface WebScriptResource {

    @ApiOperation(value = "根据脚本版本ID获取脚本版本详情", produces = "application/json")
    @GetMapping("/app/{appId}/scriptVersion/{scriptVersionId}")
    ServiceResponse<ScriptVO> getScriptVersionDetail(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "脚本版本ID", required = true, example = "1")
        @PathVariable("scriptVersionId") Long scriptVersionId);

    @ApiOperation(value = "根据脚本ID获取脚本详情", produces = "application/json")
    @GetMapping("/app/{appId}/script/{scriptId}")
    ServiceResponse<ScriptVO> getScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "脚本ID", required = true, example = "1")
        @PathVariable("scriptId") String scriptId);

    @ApiOperation(value = "根据脚本ID获取脚本基本信息", produces = "application/json")
    @GetMapping("/app/{appId}/script/basic/{scriptId}")
    ServiceResponse<ScriptVO> getScriptBasicInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "脚本ID", required = true, example = "1")
        @PathVariable("scriptId") String scriptId);

    @ApiOperation(value = "根据脚本ID获取已上线脚本", produces = "application/json")
    @GetMapping("/app/{appId}/scriptVersion/online/{scriptId}")
    ServiceResponse<ScriptVO> getOnlineScriptVersionByScriptId(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "脚本ID", required = true, example = "1")
        @PathVariable("scriptId") String scriptId,
        @ApiParam(value = "是否公共脚本")
        @RequestParam(value = "publicScript", required = false, defaultValue = "false")
            Boolean publicScript);

    @ApiOperation(value = "获取脚本列表", produces = "application/json")
    @GetMapping("/app/{appId}/script/list")
    ServiceResponse<PageData<ScriptVO>> listPageScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId")
            Long appId,
        @ApiParam(value = "是否公共脚本")
        @RequestParam(value = "publicScript", required = false, defaultValue = "false")
            Boolean publicScript,
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
    @GetMapping("/app/{appId}/script/basic/list")
    ServiceResponse<List<ScriptVO>> listScriptBasicInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true)
        @PathVariable("appId") Long appId,
        @RequestParam("ids") List<String> scriptIds);

    @ApiOperation(value = "获取脚本的所有版本", produces = "application/json")
    @GetMapping("/app/{appId}/script/{scriptId}/scriptVersion/list")
    ServiceResponse<List<ScriptVO>> listScriptVersion(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId")
            Long appId,
        @ApiParam("脚本ID")
        @PathVariable("scriptId")
            String scriptId);

    @ApiOperation(value = "更新脚本元数据，比如脚本描述、名称、标签", produces = "application/json")
    @PutMapping("/app/{appId}/script/{scriptId}/info")
    ServiceResponse updateScriptInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId")
            Long appId,
        @ApiParam(value = "脚本ID", required = true, example = "uuid")
        @PathVariable("scriptId")
            String scriptId,
        @ApiParam(value = "脚本元数据更新请求报文", name = "scriptInfoUpdateReq", required = true)
        @RequestBody ScriptInfoUpdateReq scriptInfoUpdateReq);


    @ApiOperation(value = "更新脚本", produces = "application/json")
    @PostMapping("/app/{appId}/script")
    ServiceResponse<ScriptVO> saveScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId")
            Long appId,
        @ApiParam(value = "新增/更新的脚本对象", name = "scriptCreateUpdateReq", required = true)
        @RequestBody ScriptCreateUpdateReq scriptCreateUpdateReq);

    @ApiOperation(value = "上线脚本", produces = "application/json")
    @PutMapping("/app/{appId}/script/{scriptId}/scriptVersion/{scriptVersionId}/publish")
    ServiceResponse publishScriptVersion(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("业务ID")
        @PathVariable("appId")
            Long appId,
        @ApiParam("脚本ID")
        @PathVariable("scriptId")
            String scriptId,
        @ApiParam("脚本版本ID")
        @PathVariable("scriptVersionId")
            Long scriptVersionId);

    @ApiOperation(value = "下线脚本", produces = "application/json")
    @PutMapping("/app/{appId}/script/{scriptId}/scriptVersion/{scriptVersionId}/disable")
    ServiceResponse disableScriptVersion(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("业务ID")
        @PathVariable("appId")
            Long appId,
        @ApiParam("脚本ID")
        @PathVariable("scriptId")
            String scriptId,
        @ApiParam("脚本版本ID")
        @PathVariable("scriptVersionId")
            Long scriptVersionId);

    @ApiOperation(value = "删除脚本", produces = "application/json")
    @DeleteMapping("/app/{appId}/script/{scriptId}")
    ServiceResponse deleteScriptByScriptId(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("业务ID")
        @PathVariable("appId")
            Long appId,
        @ApiParam("脚本ID")
        @PathVariable("scriptId")
            String scriptId);

    @ApiOperation(value = "删除某个版本的脚本", produces = "application/json")
    @DeleteMapping("/app/{appId}/scriptVersion/{scriptVersionId}")
    ServiceResponse deleteScriptByScriptVersionId(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("业务ID")
        @PathVariable("appId")
            Long appId,
        @ApiParam("脚本版本ID")
        @PathVariable("scriptVersionId")
            Long scriptVersionId);

    @ApiOperation(value = "根据条件查询业务下的脚本名称列表", produces = "application/json")
    @GetMapping("/app/{appId}/scriptNames")
    ServiceResponse listAppScriptNames(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("业务ID")
        @PathVariable("appId")
            Long appId,
        @ApiParam("脚本名称")
        @RequestParam("scriptName")
            String scriptName);

    @ApiOperation(value = "获取业务下面的已在线脚本列表", produces = "application/json")
    @GetMapping("/app/{appId}/scripts/online")
    ServiceResponse<List<BasicScriptVO>> listScriptOnline(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("业务ID")
        @PathVariable("appId")
            Long appId,
        @ApiParam(value = "publicScript", required = false, defaultValue = "false")
        @RequestParam(value = "publicScript", required = false, defaultValue = "false") Boolean publicScript);

    @ApiOperation(value = "检查脚本内容", produces = "application/json")
    @PutMapping("/check")
    ServiceResponse<List<ScriptCheckResultItemVO>> checkScript(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "脚本检查请求报文", name = "scriptCheckReq", required = true)
        @RequestBody ScriptCheckReq scriptCheckReq);

    @ApiOperation(value = "上传脚本获取内容", produces = "application/json")
    @PostMapping("/upload")
    ServiceResponse<ScriptVO> uploadScript(@ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
                                           @ApiParam("脚本文件") @RequestParam("script") MultipartFile scriptFile);

    @ApiOperation(value = "获取脚本可以同步的模板与步骤信息", produces = "application/json")
    @GetMapping("/app/{appId}/script/{scriptId}/scriptVersion/{scriptVersionId}/syncTemplateSteps")
    ServiceResponse<List<ScriptRelatedTemplateStepVO>> listScriptSyncTemplateSteps(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam("业务ID")
        @PathVariable("appId")
            Long appId,
        @ApiParam(value = "scriptId", required = true)
        @PathVariable(value = "scriptId") String scriptId,
        @ApiParam(value = "scriptVersionId", required = true)
        @PathVariable(value = "scriptVersionId") Long scriptVersionId);

    @ApiOperation(value = "同步脚本", produces = "application/json")
    @PostMapping("/app/{appId}/script/{scriptId}/scriptVersion/{scriptVersionId}/sync")
    ServiceResponse<List<ScriptSyncResultVO>> syncScripts(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true)
        @PathVariable(value = "appId") Long appId,
        @ApiParam(value = "脚本ID", required = true)
        @PathVariable(value = "scriptId") String scriptId,
        @ApiParam(value = "脚本版本ID", required = true)
        @PathVariable(value = "scriptVersionId") Long scriptVersionId,
        @ApiParam(value = "脚本检查请求报文", name = "scriptSyncReq", required = true)
        @RequestBody ScriptSyncReq scriptSyncReq);

    @ApiOperation(value = "根据脚本ID/脚本版本ID获取脚本被引次数", produces = "application/json")
    @GetMapping("/app/{appId}/citeCount")
    ServiceResponse<ScriptCiteCountVO> getScriptCiteCount(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "脚本ID", required = false, example = "1")
        @RequestParam(value = "scriptId") String scriptId,
        @ApiParam(value = "脚本版本ID", required = false, example = "1")
        @RequestParam(value = "scriptVersionId", required = false) Long scriptVersionId
    );

    @ApiOperation(value = "根据脚本ID/脚本版本ID获取脚本引用信息", produces = "application/json")
    @GetMapping("/app/{appId}/citeInfo")
    ServiceResponse<ScriptCiteInfoVO> getScriptCiteInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "1")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "脚本ID", required = false, example = "1")
        @RequestParam("scriptId") String scriptId,
        @ApiParam(value = "脚本版本ID", required = false, example = "1")
        @RequestParam(value = "scriptVersionId", required = false) Long scriptVersionId
    );

    @ApiOperation(value = "批量更新脚本标签-Patch方式", produces = "application/json")
    @PutMapping("/app/{appId}/tag")
    ServiceResponse<?> batchUpdateScriptTags(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam(value = "脚本标签批量更新请求报文", name = "tagBatchUpdateReq", required = true)
        @RequestBody ScriptTagBatchPatchReq tagBatchUpdateReq
    );

    @ApiOperation(value = "获取业务下标签关联的脚本数量", produces = "application/json")
    @GetMapping("/app/{appId}/tag/count")
    ServiceResponse<TagCountVO> getTagScriptCount(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务ID", required = true, example = "2")
        @PathVariable("appId") Long appId
    );

}
