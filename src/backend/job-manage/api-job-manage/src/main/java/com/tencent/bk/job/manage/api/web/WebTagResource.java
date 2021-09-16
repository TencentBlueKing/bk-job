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
import com.tencent.bk.job.manage.model.web.request.BatchPatchResourceTagReq;
import com.tencent.bk.job.manage.model.web.request.TagCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
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

import java.util.List;

/**
 * 标签管理API-前端调用
 *
 * @date 2019/10/10
 */
@Api(tags = {"job-manage:web:Tag_Management"})
@RequestMapping("/web/tag/app/{appId}")
@RestController
@WebAPI
public interface WebTagResource {

    @ApiOperation(value = "根据条件获取业务下的所有标签", produces = "application/json")
    @GetMapping("/tag/list")
    ServiceResponse<PageData<TagVO>> listPageTags(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam("标签名称,支持模糊查询")
        @RequestParam(value = "name", required = false) String name,
        @ApiParam("创建人")
        @RequestParam(value = "creator", required = false) String creator,
        @ApiParam("更新人")
        @RequestParam(value = "lastModifyUser", required = false) String lastModifyUser,
        @ApiParam("分页-开始")
        @RequestParam(value = "start", required = false) Integer start,
        @ApiParam("分页-每页大小")
        @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @ApiParam("排序字段,标签名:name,创建时间:createTime,更新时间:lastModifyTime")
        @RequestParam(value = "orderField", required = false) String orderField,
        @ApiParam("排序顺序,0:降序;1:升序")
        @RequestParam(value = "order", required = false) Integer order);

    @ApiOperation(value = "根据条件获取业务下的所有标签,仅返回基础信息(id/name)", produces = "application/json")
    @GetMapping("/tag/basic/list")
    ServiceResponse<List<TagVO>> listTagsBasic(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2")
        @PathVariable("appId") Long appId,
        @ApiParam("标签名称")
        @RequestParam(value = "name", required = false) String name);

    @ApiOperation(value = "更新标签名称", produces = "application/json")
    @PutMapping("/tag/{tagId}")
    ServiceResponse<Boolean> updateTagInfo(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable(
            "appId") Long appId,
        @ApiParam(value = "标签 ID", required = true) @PathVariable("tagId") Long tagId,
        @ApiParam("标签修改请求") @RequestBody TagCreateUpdateReq tagCreateUpdateReq);

    @ApiOperation(value = "创建标签", produces = "application/json")
    @PostMapping("/tag")
    ServiceResponse<TagVO> saveTagInfo(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam("标签创建请求") @RequestBody TagCreateUpdateReq tagCreateUpdateReq);

    @ApiOperation(value = "删除标签", produces = "application/json")
    @DeleteMapping("/tag/{tagId}")
    ServiceResponse<Boolean> deleteTag(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "标签 ID", required = true) @PathVariable("tagId") Long tagId);


    @ApiOperation(value = "批量流转标签", produces = "application/json")
    @PutMapping("/tag/{tagId}/resources")
    ServiceResponse<?> patchTagRefResourceTags(
        @ApiParam("用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable(
            "appId") Long appId,
        @ApiParam(value = "标签 ID", required = true) @PathVariable("tagId") Long tagId,
        @ApiParam("批量修改资源引用的标签的请求") @RequestBody BatchPatchResourceTagReq tagBatchUpdateReq);
}
