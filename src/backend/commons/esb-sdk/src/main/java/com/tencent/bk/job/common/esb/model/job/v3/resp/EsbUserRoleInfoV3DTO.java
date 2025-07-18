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

package com.tencent.bk.job.common.esb.model.job.v3.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import lombok.Data;

import java.util.List;

/**
 * 用户、角色信息
 *
 * @since 17/11/2020 21:51
 */
@Data
public class EsbUserRoleInfoV3DTO {
    /**
     * 用户名列表
     */
    @JsonProperty("user_list")
    @JsonPropertyDescription("User list")
    private List<String> userList;

    /**
     * 角色 ID 列表
     */
    @JsonProperty("role_list")
    @JsonPropertyDescription("Job role list ")
    private List<String> roleList;

    public static EsbUserRoleInfoV3DTO fromUserRoleInfo(UserRoleInfoDTO approvalUser) {
        if (approvalUser == null) {
            return null;
        }
        EsbUserRoleInfoV3DTO esbUserRoleInfo = new EsbUserRoleInfoV3DTO();
        esbUserRoleInfo.setUserList(approvalUser.getUserList());
        esbUserRoleInfo.setRoleList(approvalUser.getRoleList());
        return esbUserRoleInfo;
    }
}
