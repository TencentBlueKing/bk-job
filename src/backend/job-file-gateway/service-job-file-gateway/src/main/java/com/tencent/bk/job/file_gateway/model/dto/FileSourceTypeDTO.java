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
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.file_gateway.model.resp.web.FileSourceTypeVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 文件源类型
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class FileSourceTypeDTO {
    /**
     * id
     */
    private Integer id;
    /**
     * workerId
     */
    private Long workerId;
    /**
     * 文件源存储类型
     */
    private String storageType;
    /**
     * 文件源类型CODE
     */
    private String code;
    /**
     * 文件源类型名称
     */
    private String name;
    /**
     * 文件源类型图标Base64编码
     */
    private String icon;
    /**
     * 更新人
     */
    private String lastModifier;
    /**
     * 更新时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    public static FileSourceTypeVO toVO(FileSourceTypeDTO fileSourceTypeDTO) {
        if (fileSourceTypeDTO == null) {
            return null;
        }
        FileSourceTypeVO fileSourceTypeVO = new FileSourceTypeVO();
        fileSourceTypeVO.setId(fileSourceTypeDTO.getId());
        fileSourceTypeVO.setWorkerId(fileSourceTypeDTO.getWorkerId());
        fileSourceTypeVO.setStorageType(fileSourceTypeDTO.getStorageType());
        fileSourceTypeVO.setCode(fileSourceTypeDTO.getCode());
        fileSourceTypeVO.setName(fileSourceTypeDTO.getName());
        fileSourceTypeVO.setIcon(fileSourceTypeDTO.getIcon());
        return fileSourceTypeVO;
    }
}
