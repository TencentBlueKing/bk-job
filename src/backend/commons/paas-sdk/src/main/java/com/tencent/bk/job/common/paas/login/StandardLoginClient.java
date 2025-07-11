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

package com.tencent.bk.job.common.paas.login;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.sdk.BkApiClient;
import com.tencent.bk.job.common.exception.InternalUserManageException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.exception.AppPermissionDeniedException;
import com.tencent.bk.job.common.paas.model.EsbUserDto;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.ESB_BK_LOGIN_API;

@Slf4j
public class StandardLoginClient extends BkApiClient implements ILoginClient {

    // 用户认证失败，即用户登录态无效
    private static final Integer ESB_CODE_USER_NOT_LOGIN = 1302100;
    // 用户不存在
    private static final Integer ESB_CODE_USER_NOT_EXIST = 1302103;
    // 用户认证成功，但用户无应用访问权限
    private static final Integer ESB_CODE_USER_NO_APP_PERMISSION = 1302403;

    private static final String API_GET_USER_INFO = "/api/c/compapi/v2/bk_login/get_user/";

    private final AppProperties appProperties;

    public StandardLoginClient(EsbProperties esbProperties, AppProperties appProperties, MeterRegistry meterRegistry) {
        super(meterRegistry, ESB_BK_LOGIN_API, esbProperties.getService().getUrl(),
            HttpHelperFactory.getDefaultHttpHelper());
        this.appProperties = appProperties;
    }

    /**
     * 获取指定用户信息
     *
     * @param bkToken 用户登录 token
     * @return 用户信息
     */
    @Override
    public BkUserDTO getUserInfoByToken(String bkToken) {
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.ESB_BK_LOGIN_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(
                Tag.of("api_name", API_GET_USER_INFO)
            );
            EsbResp<EsbUserDto> esbResp = doRequest(
                OpenApiRequestInfo.builder()
                    .method(HttpMethodEnum.GET)
                    .uri(API_GET_USER_INFO)
                    .addQueryParam("bk_token", bkToken)
                    .authorization(BkApiAuthorization.bkTokenUserAuthorization(
                        appProperties.getCode(), appProperties.getSecret(), bkToken))
                    .build(),
                new TypeReference<EsbResp<EsbUserDto>>() {
                }
            );
            Integer code = esbResp.getCode();
            if (ErrorCode.RESULT_OK == code) {
                return convertToBkUserDTO(esbResp.getData());
            } else {
                handleNotOkResp(esbResp);
                return null;
            }
        } catch (Exception e) {
            String errorMsg = "Get " + API_GET_USER_INFO + " error";
            log.error(errorMsg, e);
            throw new InternalUserManageException(errorMsg, e, ErrorCode.USER_MANAGE_API_ACCESS_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    private void handleNotOkResp(EsbResp<EsbUserDto> esbResp) {
        Integer code = esbResp.getCode();
        if (ESB_CODE_USER_NO_APP_PERMISSION.equals(code)) {
            throw new AppPermissionDeniedException(esbResp.getMessage());
        } else if (ESB_CODE_USER_NOT_LOGIN.equals(code)) {
            log.info("User not login, esbResp.code={}, esbResp.message={}", esbResp.getCode(), esbResp.getMessage());
        } else if (ESB_CODE_USER_NOT_EXIST.equals(code)) {
            log.info("User not exist, esbResp.code={}, esbResp.message={}", esbResp.getCode(), esbResp.getMessage());
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
