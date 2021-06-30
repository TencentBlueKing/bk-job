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

package com.tencent.bk.job.common.model.dto;

import com.tencent.bk.job.common.model.vo.UserRoleInfoVO;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @since 28/4/2020 18:06
 */
@Data
public class UserRoleInfoDTO {
    private List<String> userList;

    private List<String> roleList;

    public static UserRoleInfoVO toVO(UserRoleInfoDTO approvalUser) {
        if (approvalUser == null) {
            return null;
        }
        UserRoleInfoVO userRoleInfoVO = new UserRoleInfoVO();
        userRoleInfoVO.setUserList(approvalUser.getUserList());
        userRoleInfoVO.setRoleList(approvalUser.getRoleList());
        return userRoleInfoVO;
    }

    public static UserRoleInfoDTO fromVO(UserRoleInfoVO approvalUserVO) {
        if (approvalUserVO == null) {
            return null;
        }
        UserRoleInfoDTO userRoleInfo = new UserRoleInfoDTO();
        userRoleInfo.setUserList(approvalUserVO.getUserList());
        userRoleInfo.setRoleList(approvalUserVO.getRoleList());
        return userRoleInfo;
    }

    public boolean validate() {
        if (CollectionUtils.isEmpty(userList) && CollectionUtils.isEmpty(roleList)) {
            return false;
        }
        userList = userList.parallelStream().filter(Objects::nonNull).collect(Collectors.toList());
        roleList = roleList.parallelStream().filter(Objects::nonNull).collect(Collectors.toList());
        return true;
    }
}
