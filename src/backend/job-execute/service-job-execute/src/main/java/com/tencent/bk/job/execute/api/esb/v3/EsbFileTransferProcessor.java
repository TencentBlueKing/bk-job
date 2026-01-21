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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.job.v3.EsbAccountV3BasicDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.FilePathValidateUtil;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class EsbFileTransferProcessor extends EsbCommonExecuteTaskProcessor {

    protected ValidateResult validateFilePath(String filePath) {
        if (!FilePathValidateUtil.validateFileSystemAbsolutePath(filePath)) {
            log.warn("Fast transfer file, target path is invalid!path={}", filePath);
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "file_target_path");
        }
        return ValidateResult.pass();
    }

    protected ValidateResult validateFileSources(List<EsbFileSourceV3DTO> fileSources) {
        if (fileSources == null || fileSources.isEmpty()) {
            log.warn("Fast transfer file, file source list is null or empty!");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source_list");
        }
        for (EsbFileSourceV3DTO fileSource : fileSources) {
            ValidateResult result = validateSingleFileSource(fileSource);
            if (result != null) {
                return result;
            }
        }
        return ValidateResult.pass();
    }

    protected ValidateResult validateSingleFileSource(EsbFileSourceV3DTO fileSource) {
        Integer fileType = fileSource.getFileType();
        // fileType是后加的字段，为null则默认为服务器文件不校验
        if (fileType != null && !TaskFileTypeEnum.isValid(fileType)) {
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_source.file_type");
        }
        List<String> files = fileSource.getTrimmedFiles();
        if (files == null || files.isEmpty()) {
            log.warn("File source contains empty file list");
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.file_list");
        }
        for (String file : files) {
            if ((fileType == null
                || TaskFileTypeEnum.SERVER.getType() == fileType)
                && !FilePathValidateUtil.validateFileSystemAbsolutePath(file)) {
                log.warn("Invalid path:{}", file);
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "file_source.file_list");
            }
        }
        if (null == fileType || TaskFileTypeEnum.SERVER.getType() == fileType) {
            //对文件源类型为服务器文件的文件源校验账号和服务器信息
            EsbAccountV3BasicDTO account = fileSource.getAccount();
            if (account == null) {
                log.warn("File source account is null!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.account");
            }
            if ((account.getId() == null || account.getId() < 1L) && StringUtils.isBlank(account.getAlias())) {
                log.warn("File source account is empty!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                    "file_source.account.account_id|file_source.account.account_alias");
            }
            if (!validFileSourceServer(fileSource.getServer()).isPass()) {
                log.warn("File source server is empty!");
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "file_source.server");
            }
        } else if (TaskFileTypeEnum.FILE_SOURCE.getType() == fileType) {
            //对文件源类型为第三方文件源的文件源校验Id与Code
            Integer fileSourceId = fileSource.getFileSourceId();
            String fileSourceCode = fileSource.getFileSourceCode();
            if ((fileSourceId == null || fileSourceId <= 0) && StringUtils.isBlank(fileSourceCode)) {
                return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
                    "file_source.file_source_id/file_source.file_source_code");
            }
        }
        return null;
    }

    protected ValidateResult validFileSourceServer(EsbServerV3DTO server) {
        if (server == null) {
            return ValidateResult.fail(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, "target_server");
        }

        if (!server.checkHostParamsNonEmpty()) {
            return ValidateResult.fail(ErrorCode.SERVER_EMPTY);
        }
        return ValidateResult.pass();
    }

    protected ExecuteTargetDTO buildFileSourceServer(EsbServerV3DTO server) {
        if (server == null) {
            return null;
        }
        ExecuteTargetDTO executeTargetDTO = new ExecuteTargetDTO();

        // 拓扑节点
        if (CollectionUtils.isNotEmpty(server.getTopoNodes())) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            server.getTopoNodes().forEach(topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(),
                topoNode.getNodeType())));
            executeTargetDTO.setTopoNodes(topoNodes);
        }

        // 动态分组
        if (CollectionUtils.isNotEmpty(server.getDynamicGroups())) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            server.getDynamicGroups().forEach(
                group -> dynamicServerGroups.add(new DynamicServerGroupDTO(group.getId())));
            executeTargetDTO.setDynamicServerGroups(dynamicServerGroups);
        }

        if (CollectionUtils.isNotEmpty(server.getHostIds())) {
            executeTargetDTO.setStaticIpList(
                server.getHostIds().stream().map(HostDTO::fromHostId).collect(Collectors.toList()));
        } else if (CollectionUtils.isNotEmpty(server.getIps())) {
            executeTargetDTO.setStaticIpList(
                server.getIps().stream()
                    .map(host -> new HostDTO(host.getBkCloudId(), host.getIp()))
                    .collect(Collectors.toList()));
        }

        return executeTargetDTO;

    }

}
