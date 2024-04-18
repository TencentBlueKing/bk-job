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

package com.tencent.bk.job.file_gateway.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.file_gateway.model.resp.common.SimpleFileSourceVO;
import com.tencent.bk.job.file_gateway.model.resp.esb.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.file_gateway.model.resp.web.FileSourceVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件源
 */
@Slf4j
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class FileSourceDTO {
    /**
     * id
     */
    private Integer id;
    /**
     * appId
     */
    private Long appId;
    /**
     * 文件源标识
     */
    private String code;
    /**
     * 文件源别名
     */
    private String alias;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 类型
     */
    private FileSourceTypeDTO fileSourceType;
    /**
     * 文件源信息Map
     */
    private Map<String, Object> fileSourceInfoMap;
    /**
     * 是否为公共文件源
     */
    private Boolean publicFlag;
    /**
     * 共享的业务Id集合
     */
    private List<Long> sharedAppIdList;
    /**
     * 是否共享到全业务
     */
    private Boolean shareToAllApp;
    /**
     * 凭证Id
     */
    private String credentialId;
    /**
     * 文件前缀
     */
    private String filePrefix;
    /**
     * 接入点选择范围
     */
    private String workerSelectScope;
    /**
     * 接入点选择模式
     */
    private String workerSelectMode;
    /**
     * 接入点Id
     */
    private Long workerId;
    /**
     * 是否启用
     */
    private Boolean enable;
    /**
     * 创建人
     */
    private String creator;
    /**
     * 创建时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;
    /**
     * 更新人
     */
    private String lastModifyUser;
    /**
     * 更新时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    public static SimpleFileSourceVO toSimpleFileSourceVO(FileSourceDTO fileSourceDTO) {
        if (fileSourceDTO == null) {
            return null;
        }
        SimpleFileSourceVO fileSourceVO = new SimpleFileSourceVO();
        fileSourceVO.setId(fileSourceDTO.getId());
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(fileSourceDTO.getAppId());
        fileSourceVO.setScopeType(resourceScope.getType().getValue());
        fileSourceVO.setScopeId(resourceScope.getId());
        fileSourceVO.setCode(fileSourceDTO.getCode());
        fileSourceVO.setAlias(fileSourceDTO.getAlias());
        return fileSourceVO;
    }

    public static EsbFileSourceV3DTO toEsbFileSourceV3DTO(FileSourceDTO fileSourceDTO) {
        if (fileSourceDTO == null) {
            return null;
        }
        EsbFileSourceV3DTO fileSource = new EsbFileSourceV3DTO();
        fileSource.setId(fileSourceDTO.getId());
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(fileSourceDTO.getAppId());
        fileSource.setScopeType(resourceScope.getType().getValue());
        fileSource.setScopeId(resourceScope.getId());
        fileSource.setCode(fileSourceDTO.getCode());
        fileSource.setAlias(fileSourceDTO.getAlias());
        fileSource.setCredentialId(fileSourceDTO.getCredentialId());
        fileSource.setEnable(fileSourceDTO.getEnable());
        fileSource.setFileSourceTypeCode(fileSourceDTO.getFileSourceType() != null ?
            fileSourceDTO.getFileSourceType().getCode() : null);
        fileSource.setPublicFlag(fileSourceDTO.getPublicFlag());
        fileSource.setStatus(fileSourceDTO.getStatus());
        fileSource.setCreateTime(fileSourceDTO.getCreateTime());
        fileSource.setCreator(fileSourceDTO.getCreator());
        fileSource.setLastModifyTime(fileSourceDTO.getLastModifyTime());
        fileSource.setLastModifyUser(fileSourceDTO.getLastModifyUser());
        return fileSource;
    }

    public static FileSourceVO toVO(FileSourceDTO fileSourceDTO) {
        if (fileSourceDTO == null) {
            return null;
        }
        FileSourceVO fileSourceVO = new FileSourceVO();
        fileSourceVO.setId(fileSourceDTO.getId());

        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(fileSourceDTO.getAppId());
        fileSourceVO.setScopeType(resourceScope.getType().getValue());
        fileSourceVO.setScopeId(resourceScope.getId());

        fileSourceVO.setCode(fileSourceDTO.getCode());
        fileSourceVO.setAlias(fileSourceDTO.getAlias());
        Integer status = fileSourceDTO.getStatus();
        if (status == -1) {
            // 找不到合适Worker的情况归纳为异常状态
            status = 0;
        }
        fileSourceVO.setStatus(status);
        fileSourceVO.setFileSourceType(FileSourceTypeDTO.toVO(fileSourceDTO.getFileSourceType()));
        fileSourceVO.setStorageType(fileSourceDTO.getFileSourceType().getStorageType());
        fileSourceVO.setFileSourceInfoMap(fileSourceDTO.getFileSourceInfoMap());
        fileSourceVO.setPublicFlag(fileSourceDTO.getPublicFlag());
        List<Long> sharedAppIdList = fileSourceDTO.getSharedAppIdList();
        List<ResourceScope> sharedScopeList = new ArrayList<>();
        Map<Long, ResourceScope> map = appScopeMappingService.getScopeByAppIds(sharedAppIdList);
        for (Long appId : sharedAppIdList) {
            ResourceScope scope = map.get(appId);
            if (scope == null) {
                log.warn("cannot find scope by appId:{}, ignore", appId);
            } else {
                sharedScopeList.add(scope);
            }
        }
        fileSourceVO.setSharedScopeList(sharedScopeList);
        fileSourceVO.setShareToAllApp(fileSourceDTO.getShareToAllApp());
        fileSourceVO.setCredentialId(fileSourceDTO.getCredentialId());
        fileSourceVO.setFilePrefix(fileSourceDTO.getFilePrefix());
        fileSourceVO.setWorkerSelectScope(fileSourceDTO.getWorkerSelectScope());
        fileSourceVO.setWorkerSelectMode(fileSourceDTO.getWorkerSelectMode());
        fileSourceVO.setWorkerId(fileSourceDTO.getWorkerId());
        fileSourceVO.setEnable(fileSourceDTO.getEnable());
        fileSourceVO.setCreator(fileSourceDTO.getCreator());
        fileSourceVO.setCreateTime(fileSourceDTO.getCreateTime());
        fileSourceVO.setLastModifyUser(fileSourceDTO.getLastModifyUser());
        fileSourceVO.setLastModifyTime(fileSourceDTO.getLastModifyTime());
        return fileSourceVO;
    }

    public String getBasicDesc() {
        return "(id=" + id + ", appId=" + appId + ", code=" + code + ", alias=" + alias + ", enable=" + enable + ")";
    }
}
