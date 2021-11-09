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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.artifactory.model.dto.TempUrlInfo;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.constants.EsbConsts;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.check.ParamCheckUtil;
import com.tencent.bk.job.manage.api.esb.v3.EsbLocalFileV3Resource;
import com.tencent.bk.job.manage.config.LocalFileConfigForManage;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGenLocalFileUploadUrlV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbUploadUrlV3DTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@Slf4j
public class EsbLocalFileResourceV3Impl implements EsbLocalFileV3Resource {

    private final LocalFileConfigForManage localFileConfigForManage;
    private final ArtifactoryClient artifactoryClient;

    @Autowired
    public EsbLocalFileResourceV3Impl(
        LocalFileConfigForManage localFileConfigForManage,
        ArtifactoryClient artifactoryClient
    ) {
        this.localFileConfigForManage = localFileConfigForManage;
        this.artifactoryClient = artifactoryClient;
    }

    @Override
    public EsbResp<EsbUploadUrlV3DTO> generateLocalFileUploadUrl(EsbGenLocalFileUploadUrlV3Req req) {
        // 参数检查
        // appId
        Long appId = req.getAppId();
        ParamCheckUtil.checkAppId(appId, EsbConsts.PARAM_BK_BIZ_ID);
        // fileNameList
        List<String> fileNameList = req.getFileNameList();
        String fileNameDesc = "fileName in " + EsbConsts.PARAM_FILE_NAME_LIST;
        if (fileNameList == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{EsbConsts.PARAM_FILE_NAME_LIST, fileNameDesc + " cannot be null"});
        }
        fileNameList.forEach(fileName -> {
            ParamCheckUtil.checkLocalUploadFileName(fileName, fileNameDesc);
        });
        // 权限校验：在切面层已实现
        List<String> filePathList = new ArrayList<>();
        fileNameList.forEach(fileName -> {
            StringBuilder sb = new StringBuilder();
            sb.append(appId);
            sb.append(File.separatorChar);
            sb.append(Utils.getUUID());
            sb.append(File.separator);
            sb.append(req.getUserName());
            sb.append(File.separatorChar);
            sb.append(fileName);
            String filePath = sb.toString();
            filePathList.add(filePath);
        });
        List<TempUrlInfo> urlInfoList = artifactoryClient.createTempUrls(
            localFileConfigForManage.getArtifactoryJobProject(),
            localFileConfigForManage.getArtifactoryJobLocalUploadRepo(),
            filePathList
        );
        Map<String, TempUrlInfo> urlInfoMap = new HashMap<>();
        urlInfoList.forEach(urlInfo -> {
            urlInfoMap.put(
                StringUtil.removePrefix(urlInfo.getFullPath(), "/"),
                urlInfo
            );
        });
        EsbUploadUrlV3DTO esbUploadUrlV3DTO = new EsbUploadUrlV3DTO();
        Map<String, Map<String, String>> urlMap = new HashMap<>();
        int size = fileNameList.size();
        for (int i = 0; i < size; i++) {
            String fileName = fileNameList.get(i);
            String filePath = filePathList.get(i);
            TempUrlInfo urlInfo = urlInfoMap.get(StringUtil.removePrefix(filePath, "/"));
            if (urlInfo != null) {
                String uploadUrl = urlInfo.getUrl();
                Map<String, String> map = new HashMap<>();
                map.put("upload_url", uploadUrl);
                map.put("path", filePath);
                urlMap.put(fileName, map);
            } else {
                log.error("Fail to create uploadUrl for {}", filePath);
            }
        }
        esbUploadUrlV3DTO.setUrlMap(urlMap);
        return EsbResp.buildSuccessResp(esbUploadUrlV3DTO);
    }
}
