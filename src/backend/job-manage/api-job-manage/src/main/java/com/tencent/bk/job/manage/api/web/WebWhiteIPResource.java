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
import com.tencent.bk.job.common.model.PageDataWithManagePermission;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = {"job-manage:web:WhiteIP"})
@RequestMapping("/web/whiteIP")
@RestController
@WebAPI
public interface WebWhiteIPResource {

    @ApiOperation(value = "获取IP白名单列表", produces = "application/json")
    @GetMapping("/list")
    ServiceResponse<PageDataWithManagePermission<WhiteIPRecordVO>> listWhiteIP(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @ApiParam("IP（英文逗号分隔）：模糊搜索")
        @RequestParam(value = "ipStr", required = false)
            String ipStr,
        @ApiParam("业务Id（英文逗号分隔）：精确过滤")
        @RequestParam(value = "appIdStr", required = false)
            String appIdStr,
        @ApiParam("业务名称（英文逗号分隔）：模糊搜索")
        @RequestParam(value = "appNameStr", required = false)
            String appNameStr,
        @ApiParam("生效范围：SCRIPT_EXECUTE/FILE_DISTRIBUTION（英文逗号分隔）：精确过滤")
        @RequestParam(value = "actionScopeStr", required = false)
            String actionScopeStr,
        @ApiParam("创建人：模糊搜索")
        @RequestParam(value = "creator", required = false)
            String creator,
        @ApiParam("更新人：模糊搜索")
        @RequestParam(value = "lastModifier", required = false)
            String lastModifier,
        @ApiParam("分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam("分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @ApiParam("排序字段,脚本名:name,脚本类型:type,标签:tags,创建人:creator")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @ApiParam("排序顺序,0:逆序;1:正序")
        @RequestParam(value = "order", required = false)
            Integer order
    );


    @ApiOperation(value = "新增/更新IP白名单", produces = "application/json")
    @PostMapping("/")
    ServiceResponse<Long> saveWhiteIP(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username")
            String username,
        @ApiParam(value = "创建或更新请求体", required = true)
        @RequestBody
            WhiteIPRecordCreateUpdateReq createUpdateReq
    );

    @ApiOperation(value = "获取IP白名单记录详情", produces = "application/json")
    @GetMapping("/ids/{id}")
    ServiceResponse<WhiteIPRecordVO> getWhiteIPDetailById(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("IP白名单记录ID")
        @PathVariable("id")
            Long id
    );

    @ApiOperation(value = "获取业务下云区域列表", produces = "application/json")
    @GetMapping("/cloudAreas/list")
    ServiceResponse<List<CloudAreaInfoVO>> listCloudAreas(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @ApiOperation(value = "获取生效范围列表", produces = "application/json")
    @GetMapping("/actionScope/list")
    ServiceResponse<List<ActionScopeVO>> listActionScope(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

    @ApiOperation(value = "删除IP白名单", produces = "application/json")
    @DeleteMapping("/ids/{id}")
    ServiceResponse<Long> deleteWhiteIPById(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("IP白名单记录ID")
        @PathVariable("id")
            Long id
    );
}
