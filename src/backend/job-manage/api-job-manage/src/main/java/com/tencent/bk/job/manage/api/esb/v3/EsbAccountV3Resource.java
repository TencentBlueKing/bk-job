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

package com.tencent.bk.job.manage.api.esb.v3;

import com.tencent.bk.job.common.annotation.EsbAPI;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateAccountV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbDeleteAccountV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetAccountListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbAccountV3DTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号API-V3
 */
@RequestMapping("/esb/api/v3")
@RestController
@EsbAPI
public interface EsbAccountV3Resource {

    @PostMapping("/get_account_list")
    EsbResp<EsbPageDataV3<EsbAccountV3DTO>> getAccountListUsingPost(
        @RequestBody
        @Validated
            EsbGetAccountListV3Req request
    );

    @GetMapping("/get_account_list")
    EsbResp<EsbPageDataV3<EsbAccountV3DTO>> getAccountList(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_biz_id", required = false) Long bizId,
        @RequestParam(value = "bk_scope_type", required = false) String scopeType,
        @RequestParam(value = "bk_scope_id", required = false) String scopeId,
        @RequestParam(value = "category", required = false) Integer category,
        @RequestParam(value = "start", required = false) Integer start,
        @RequestParam(value = "length", required = false) Integer length);

    @GetMapping("/search_account")
    EsbResp<EsbPageDataV3<EsbAccountV3DTO>> searchAccount(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_scope_type", required = false) String scopeType,
        @RequestParam(value = "bk_scope_id", required = false) String scopeId,
        @RequestParam(value = "category", required = false) Integer category,
        @RequestParam(value = "account", required = false) String account,
        @RequestParam(value = "alias", required = false) String alias,
        @RequestParam(value = "start", required = false) Integer start,
        @RequestParam(value = "length", required = false) Integer length);

    @PostMapping("/create_account")
    EsbResp<EsbAccountV3DTO> createAccount(
        @RequestBody
        @Validated
            EsbCreateAccountV3Req req
    );

    @DeleteMapping("/delete_account")
    EsbResp<EsbAccountV3DTO> deleteAccount(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_scope_type", required = false) String scopeType,
        @RequestParam(value = "bk_scope_id", required = false) String scopeId,
        @RequestParam(value = "id") Long id
    );

    @PostMapping("/delete_account")
    EsbResp<EsbAccountV3DTO> deleteAccountUsingPost(
        @RequestBody
        @Validated
            EsbDeleteAccountV3Req req
    );

}
