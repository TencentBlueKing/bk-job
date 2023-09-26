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
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.common.util.json.SkipLogFields;
import com.tencent.bk.job.file_gateway.consts.FileGatewayConsts;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceMetaData;
import com.tencent.bk.job.file_gateway.model.req.common.FileWorkerConfig;
import com.tencent.bk.job.file_gateway.model.req.inner.HeartBeatReq;
import com.tencent.bk.job.file_gateway.model.resp.web.BaseFileWorkerVO;
import com.tencent.bk.job.file_gateway.model.resp.web.FileWorkerVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 文件Worker
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileWorkerDTO {
    /**
     * id
     */
    private Long id;
    /**
     * appId
     */
    private Long appId;
    /**
     * 名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;
    /**
     * 密钥
     */
    @SkipLogFields
    private String token;
    /**
     * 访问Host
     */
    private String accessHost;
    /**
     * 访问端口
     */
    private Integer accessPort;
    /**
     * 所在机器云区域Id
     */
    private Long cloudAreaId;
    /**
     * 内网IP协议，取值：v4/v6
     */
    private String innerIpProtocol;
    /**
     * 内网IP
     */
    private String innerIp;
    /**
     * 标签列表
     */
    private List<String> tagList;
    /**
     * 能力标签
     */
    private List<String> abilityTagList;
    /**
     * Ping延迟
     */
    private Integer latency;
    /**
     * CPU负载
     */
    private Float cpuOverload;
    /**
     * 内存使用率
     */
    private Float memRate;
    /**
     * 内存剩余
     */
    private Float memFreeSpace;
    /**
     * 磁盘使用率
     */
    private Float diskRate;
    /**
     * 磁盘剩余
     */
    private Float diskFreeSpace;
    /**
     * Worker版本
     */
    private String version;
    /**
     * Worker在线状态
     */
    private Byte onlineStatus;
    /**
     * 上一次心跳时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastHeartBeat;
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
     * 最后修改人
     */
    private String lastModifyUser;
    /**
     * 最后修改时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;
    /**
     * 配置JSON串
     */
    private String configStr;

    public static FileWorkerDTO fromReq(HeartBeatReq heartBeatReq) {
        FileWorkerDTO fileWorkerDTO = new FileWorkerDTO();
        fileWorkerDTO.setId(heartBeatReq.getId());
        fileWorkerDTO.setName(heartBeatReq.getName());
        fileWorkerDTO.setTagList(heartBeatReq.getTagList());
        fileWorkerDTO.setAppId(heartBeatReq.getAppId());
        fileWorkerDTO.setToken(heartBeatReq.getToken());
        fileWorkerDTO.setAccessHost(heartBeatReq.getAccessHost());
        fileWorkerDTO.setAccessPort(heartBeatReq.getAccessPort());
        fileWorkerDTO.setCloudAreaId(heartBeatReq.getCloudAreaId());
        fileWorkerDTO.setInnerIpProtocol(heartBeatReq.getInnerIpProtocol());
        fileWorkerDTO.setInnerIp(heartBeatReq.getInnerIp());
        FileWorkerConfig fileWorkerConfig = heartBeatReq.getFileWorkerConfig();
        List<FileSourceMetaData> fileSourceMetaDataList = fileWorkerConfig.getFileSourceMetaDataList();
        // 能力标签合并、去重
        Set<String> tagSet = new HashSet<>(heartBeatReq.getAbilityTagList());
        for (FileSourceMetaData fileSourceMetaData : fileSourceMetaDataList) {
            tagSet.add(FileGatewayConsts.ABILITY_TAG_KEY_FILE_SOURCE_TYPE_CODE
                + "=" + fileSourceMetaData.getFileSourceTypeCode());
        }
        fileWorkerDTO.setAbilityTagList(new ArrayList<>(tagSet));
        fileWorkerDTO.setCpuOverload(heartBeatReq.getCpuOverload());
        fileWorkerDTO.setMemRate(heartBeatReq.getMemRate());
        fileWorkerDTO.setMemFreeSpace(heartBeatReq.getMemFreeSpace());
        fileWorkerDTO.setDiskRate(heartBeatReq.getDiskRate());
        fileWorkerDTO.setDiskFreeSpace(heartBeatReq.getDiskFreeSpace());
        fileWorkerDTO.setVersion(heartBeatReq.getVersion());
        fileWorkerDTO.setOnlineStatus(heartBeatReq.getOnlineStatus());
        fileWorkerDTO.setLastHeartBeat(System.currentTimeMillis());
        fileWorkerDTO.setConfigStr(JsonUtils.toJson(heartBeatReq.getFileWorkerConfig()));
        return fileWorkerDTO;
    }

    public FileWorkerVO toVO() {
        FileWorkerVO fileWorkerVO = new FileWorkerVO();
        fileWorkerVO.setId(id);

        if (appId != null && appId > 0) {
            // 具体的业务/业务集
            AppScopeMappingService appScopeMappingService =
                ApplicationContextRegister.getBean(AppScopeMappingService.class);
            ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
            fileWorkerVO.setScopeType(resourceScope.getType().getValue());
            fileWorkerVO.setScopeId(resourceScope.getId());
        } else {
            // 非具体业务的公共FileWorker
            fileWorkerVO.setScopeType(null);
            fileWorkerVO.setScopeId(null);
        }

        fileWorkerVO.setName(name);
        fileWorkerVO.setDescription(description);
        fileWorkerVO.setCloudAreaId(cloudAreaId);
        fileWorkerVO.setInnerIp(innerIp);
        fileWorkerVO.setAbilityTagList(abilityTagList);
        fileWorkerVO.setLatency(latency);
        fileWorkerVO.setCpuOverload(cpuOverload);
        fileWorkerVO.setMemRate(memRate);
        fileWorkerVO.setMemFreeSpace(memFreeSpace);
        fileWorkerVO.setDiskRate(diskRate);
        fileWorkerVO.setDiskFreeSpace(diskFreeSpace);
        fileWorkerVO.setVersion(version);
        fileWorkerVO.setOnlineStatus(onlineStatus);
        fileWorkerVO.setLastHeartBeat(lastHeartBeat);
        fileWorkerVO.setCreator(creator);
        fileWorkerVO.setCreateTime(createTime);
        fileWorkerVO.setLastModifyUser(lastModifyUser);
        fileWorkerVO.setLastModifyTime(lastModifyTime);
        return fileWorkerVO;
    }

    public BaseFileWorkerVO toBaseVO() {
        BaseFileWorkerVO baseFileWorkerVO = new BaseFileWorkerVO();
        baseFileWorkerVO.setId(id);
        baseFileWorkerVO.setName(name);
        baseFileWorkerVO.setCloudAreaId(cloudAreaId);
        baseFileWorkerVO.setInnerIp(innerIp);
        baseFileWorkerVO.setLatency(latency);
        baseFileWorkerVO.setCpuOverload(cpuOverload);
        baseFileWorkerVO.setMemRate(memRate);
        baseFileWorkerVO.setVersion(version);
        baseFileWorkerVO.setOnlineStatus(onlineStatus);
        return baseFileWorkerVO;
    }

    public boolean isOnline() {
        return (onlineStatus != null && onlineStatus.intValue() == 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileWorkerDTO)) return false;
        FileWorkerDTO that = (FileWorkerDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getBasicDesc() {
        return "(id=" + id + ", appId=" + appId + ", name=" + name + ")";
    }

    public String getCloudIp() {
        return cloudAreaId + ":" + innerIp;
    }
}
