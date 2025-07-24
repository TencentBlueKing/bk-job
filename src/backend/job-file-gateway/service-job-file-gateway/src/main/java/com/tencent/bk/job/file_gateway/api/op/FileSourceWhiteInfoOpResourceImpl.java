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

package com.tencent.bk.job.file_gateway.api.op;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.file_gateway.consts.FileSourceWhiteInfoTypeConsts;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceWhiteInfoDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceWhiteInfoDTO;
import com.tencent.bk.job.file_gateway.model.req.op.AddBkArtifactoryWhiteBaseUrlReq;
import com.tencent.bk.job.file_gateway.model.req.op.BatchDeleteFileSourceWhiteInfoReq;
import com.tencent.bk.job.file_gateway.model.resp.op.FileSourceWhiteInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestController("jobFileGatewayFileSourceWhiteInfoOpResourceImpl")
public class FileSourceWhiteInfoOpResourceImpl implements FileSourceWhiteInfoOpResource {

    private final FileSourceWhiteInfoDAO fileSourceWhiteInfoDAO;

    @Autowired
    public FileSourceWhiteInfoOpResourceImpl(FileSourceWhiteInfoDAO fileSourceWhiteInfoDAO) {
        this.fileSourceWhiteInfoDAO = fileSourceWhiteInfoDAO;
    }

    @Override
    public Response<Integer> addBkArtifactoryWhiteBaseUrl(String username, AddBkArtifactoryWhiteBaseUrlReq req) {
        if(fileSourceWhiteInfoDAO.exists(FileSourceWhiteInfoTypeConsts.BK_ARTIFACTORY_BASE_URL, req.getBaseUrl())){
            return Response.buildCommonFailResp(ErrorCode.FILE_SOURCE_WHITE_INFO_ALREADY_EXISTS);
        }
        FileSourceWhiteInfoDTO fileSourceWhiteInfoDTO = new FileSourceWhiteInfoDTO();
        fileSourceWhiteInfoDTO.setType(FileSourceWhiteInfoTypeConsts.BK_ARTIFACTORY_BASE_URL);
        fileSourceWhiteInfoDTO.setContent(req.getBaseUrl());
        fileSourceWhiteInfoDTO.setRemark(req.getRemark());
        fileSourceWhiteInfoDTO.setCreator(username);
        Integer id = fileSourceWhiteInfoDAO.insert(fileSourceWhiteInfoDTO);
        return Response.buildSuccessResp(id);
    }

    @Override
    public Response<List<FileSourceWhiteInfoVO>> list(String username, Integer start, Integer length) {
        List<FileSourceWhiteInfoDTO> fileSourceWhiteInfoDTOList = fileSourceWhiteInfoDAO.list(start, length);
        List<FileSourceWhiteInfoVO> resultList = fileSourceWhiteInfoDTOList.stream()
            .map(FileSourceWhiteInfoDTO::toFileSourceWhiteInfoVO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(resultList);
    }

    @Override
    public Response<Integer> batchDelete(String username, BatchDeleteFileSourceWhiteInfoReq req) {
        int affectedNum = fileSourceWhiteInfoDAO.delete(req.getIdList());
        return Response.buildSuccessResp(affectedNum);
    }
}
