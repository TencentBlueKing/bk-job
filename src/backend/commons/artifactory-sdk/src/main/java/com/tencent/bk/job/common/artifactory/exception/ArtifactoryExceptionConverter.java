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

package com.tencent.bk.job.common.artifactory.exception;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.artifactory.constants.ArtifactoryInterfaceConsts;
import com.tencent.bk.job.common.artifactory.model.dto.ArtifactoryResp;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.HttpStatusException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.json.JsonUtils;

/**
 * 制品库接口异常转换器：把 bkrepo 的 HTTP 状态 / 业务错误码统一翻译成类型化异常。
 * 这里是「业务错误码 -> 异常类型」映射的唯一维护点，调用方应基于返回的异常类型做后续判定。
 */
public class ArtifactoryExceptionConverter {

    /**
     * 将Http状态等异常转换为类型化的制品库异常。
     * 只负责构造异常并返回、不抛出，由调用方决定抛出时机。
     *
     * @param e 原始异常
     * @return 转换后的类型化异常
     */
    public static ServiceException convertException(Exception e) {
        if (e instanceof HttpStatusException) {
            HttpStatusException httpStatusException = (HttpStatusException) e;
            int httpStatus = httpStatusException.getHttpStatus();
            // bkrepo 返回 401 表示凭证无效（如用户名/密码或访问令牌错误），
            // 引导用户检查文件源凭证配置；原始 bkrepo 报错信息保留在 cause 中可在日志中查看
            if (httpStatus == HTTP_STATUS_UNAUTHORIZED) {
                return new ArtifactoryAuthFailException(e);
            }
            String httpStatusExceptionRespStr = httpStatusException.getRespBodyStr();
            ArtifactoryResp<Object> artifactoryResp = JsonUtils.fromJson(httpStatusExceptionRespStr,
                new TypeReference<ArtifactoryResp<Object>>() {
                });
            if (artifactoryResp == null) {
                // 响应体中没有详细的报错信息，返回粗粒度接口访问异常
                return new ArtifactoryException(e);
            }
            if (artifactoryResp.getCode() == ArtifactoryInterfaceConsts.RESULT_CODE_NODE_NOT_FOUND) {
                return new NodeNotFoundException(
                    e,
                    ErrorCode.CAN_NOT_FIND_NODE_IN_ARTIFACTORY,
                    new String[]{
                        artifactoryResp.getMessage()
                    }
                );
            } else if (artifactoryResp.getCode() == ArtifactoryInterfaceConsts.RESULT_CODE_PROJECT_EXISTED) {
                // 项目已存在
                return new ProjectExistedException(e);
            } else if (artifactoryResp.getCode() == ArtifactoryInterfaceConsts.RESULT_CODE_REPO_NOT_FOUND) {
                // 仓库不存在
                return new RepoNotFoundException(e);
            } else {
                // 暂未识别的异常
                return new ArtifactoryException(e);
            }
        } else {
            // 未收到正常的HTTP响应，返回粗粒度接口访问异常
            return new ArtifactoryException(e);
        }
    }

    private static final int HTTP_STATUS_UNAUTHORIZED = 401;
}
