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
import com.tencent.bk.job.common.util.json.JsonUtils;

/**
 * 制品库接口异常转换器
 */
public class ArtifactoryExceptionConverter {

    /**
     * 将Http状态等异常转换为ArtifactoryException
     *
     * @param e   原始异常
     * @param <R> 返回数据类型
     * @return 转换后的异常
     */
    public static <R> R convertException(Exception e) {
        if (e instanceof HttpStatusException) {
            String httpStatusExceptionRespStr = ((HttpStatusException) e).getRespBodyStr();
            ArtifactoryResp<Object> artifactoryResp = JsonUtils.fromJson(httpStatusExceptionRespStr,
                new TypeReference<ArtifactoryResp<Object>>() {
                });
            if (artifactoryResp == null) {
                // 响应体中没有详细的报错信息，抛出粗粒度接口访问异常
                throw new ArtifactoryException(e);
            }
            if (artifactoryResp.getCode() == ArtifactoryInterfaceConsts.RESULT_CODE_NODE_NOT_FOUND) {
                throw new NodeNotFoundException(
                    e,
                    ErrorCode.CAN_NOT_FIND_NODE_IN_ARTIFACTORY,
                    new String[]{
                        artifactoryResp.getMessage()
                    }
                );
            } else if (artifactoryResp.getCode() == ArtifactoryInterfaceConsts.RESULT_CODE_PROJECT_EXISTED) {
                // 项目不存在
                throw new ProjectExistedException(e);
            } else if (artifactoryResp.getCode() == ArtifactoryInterfaceConsts.RESULT_CODE_REPO_NOT_FOUND) {
                // 仓库不存在
                throw new RepoNotFoundException(e);
            } else {
                // 暂未识别的异常
                throw new ArtifactoryException(e);
            }
        } else {
            // 未收到正常的HTTP响应，抛出粗粒度接口访问异常
            throw new ArtifactoryException(e);
        }
    }
}
