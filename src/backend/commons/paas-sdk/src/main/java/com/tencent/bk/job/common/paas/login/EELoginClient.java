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

package com.tencent.bk.job.common.paas.login;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.sdk.AbstractEsbSdkClient;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.model.EsbUserDto;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EELoginClient extends AbstractEsbSdkClient implements ILoginClient {
    private final static String API_GET_USER_INFO = "/api/c/compapi/v2/bk_login/get_user/";

    public EELoginClient(String esbHostUrl, String appCode, String appSecret, String lang, boolean useEsbTestEnv) {
        super(esbHostUrl, appCode, appSecret, lang, useEsbTestEnv);
    }

    public EELoginClient(String esbHostUrl, String appCode, String appSecret, boolean useEsbTestEnv) {
        super(esbHostUrl, appCode, appSecret, "en", useEsbTestEnv);
    }

    /**
     * 获取指定用户信息
     *
     * @param bkToken
     * @return
     */
    @Override
    public BkUserDTO getUserInfoByToken(String bkToken) {
        EsbReq esbReq = makeBaseReqByWeb(EsbReq.class, bkToken);
        return getUserInfo(esbReq);
    }

    @Override
    public BkUserDTO getUserInfoByUserName(String userName) {
        EsbReq esbReq = makeBaseReqByWeb(EsbReq.class, null, userName, null);
        return getUserInfo(esbReq);
    }

    private BkUserDTO getUserInfo(EsbReq esbReq) {
        try {
            String retStr = doHttpGet(API_GET_USER_INFO, esbReq);
            EsbResp<EsbUserDto> resp = JsonUtils.fromJson(retStr, new TypeReference<EsbResp<EsbUserDto>>() {
            });
            int SUCCESS_CODE = 0;
            if (resp == null || resp.getCode() != SUCCESS_CODE || resp.getData() == null) {
                return null;
            }
            return convertToBkUserDTO(resp.getData());
        } catch (Exception e) {
            log.error("Get user info from paas fail", e);
            return null;
        }
    }

    private BkUserDTO convertToBkUserDTO(EsbUserDto esbUserDto) {
        BkUserDTO bkUserDTO = new BkUserDTO();
        bkUserDTO.setUsername(esbUserDto.getUsername());
        bkUserDTO.setEmail(esbUserDto.getEmail());
        bkUserDTO.setPhone(esbUserDto.getPhone());
        bkUserDTO.setWxUserId(esbUserDto.getWxUserId());
        bkUserDTO.setTimeZone(esbUserDto.getTimeZone());
        return bkUserDTO;
    }
}
