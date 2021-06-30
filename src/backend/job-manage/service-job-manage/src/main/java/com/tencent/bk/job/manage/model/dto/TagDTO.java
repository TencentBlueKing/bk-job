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

package com.tencent.bk.job.manage.model.dto;

import com.tencent.bk.job.manage.model.inner.ServiceTagDTO;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @since 29/9/2019 16:04
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {
    /**
     * 标签 ID
     */
    private Long id;

    /**
     * 业务 ID
     */
    private Long appId;

    /**
     * 标签名
     */
    private String name;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 最后修改者
     */
    private String lastModifyUser;

    public static TagVO toVO(TagDTO tagInfo) {
        return new TagVO(tagInfo.getId(), tagInfo.getName());
    }

    public static TagDTO fromVO(TagVO tagVO) {
        TagDTO tagInfo = new TagDTO();
        tagInfo.setId(tagVO.getId());
        tagInfo.setName(tagVO.getName());
        return tagInfo;
    }

    public static ServiceTagDTO toServiceDTO(TagDTO tagDTO) {
        if (tagDTO == null) {
            return null;
        }
        ServiceTagDTO serviceTag = new ServiceTagDTO();
        serviceTag.setId(tagDTO.getId());
        serviceTag.setName(tagDTO.getName());
        return serviceTag;
    }
}
