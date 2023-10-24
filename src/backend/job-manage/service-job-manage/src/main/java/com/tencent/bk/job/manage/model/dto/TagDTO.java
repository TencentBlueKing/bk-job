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

import com.tencent.bk.job.common.model.dto.BasicDTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbTagV3DTO;
import com.tencent.bk.job.manage.model.inner.ServiceTagDTO;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class TagDTO extends BasicDTO implements Cloneable {
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
     * 描述
     */
    private String description;

    public TagDTO(Long id, Long appId, String name, String description, String creator, String lastModifyUser) {
        this.id = id;
        this.appId = appId;
        this.name = name;
        this.description = description;
        this.setCreator(creator);
        this.setLastModifyUser(lastModifyUser);
    }

    public static TagVO toVO(TagDTO tagInfo) {
        TagVO vo = new TagVO();
        vo.setId(tagInfo.getId());
        vo.setName(tagInfo.getName());
        vo.setCreator(tagInfo.getCreator());
        vo.setLastModifyUser(tagInfo.getLastModifyUser());
        vo.setDescription(tagInfo.getDescription());
        vo.setCreateTime(tagInfo.getCreateTime());
        vo.setLastModifyTime(tagInfo.getLastModifyTime());
        return vo;
    }

    public static TagDTO fromVO(TagVO tagVO) {
        TagDTO tagInfo = new TagDTO();
        tagInfo.setId(tagVO.getId());
        tagInfo.setName(tagVO.getName());
        tagInfo.setCreator(tagVO.getCreator());
        tagInfo.setCreateTime(tagVO.getCreateTime());
        tagInfo.setLastModifyUser(tagVO.getLastModifyUser());
        tagInfo.setLastModifyTime(tagVO.getLastModifyTime());
        tagInfo.setDescription(tagVO.getDescription());
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

    public static EsbTagV3DTO toEsbTagV3DTO(TagDTO tag) {
        if (tag == null) {
            return null;
        }
        EsbTagV3DTO result = new EsbTagV3DTO();
        result.setId(tag.getId());
        result.setName(tag.getName());
        result.setDescription(tag.getDescription());
        return result;
    }

    @Override
    public TagDTO clone() {
        TagDTO tag = new TagDTO();
        tag.setId(id);
        tag.setAppId(appId);
        tag.setName(name);
        tag.setDescription(description);
        tag.setCreator(getCreator());
        tag.setLastModifyUser(getLastModifyUser());
        tag.setCreateTime(getCreateTime());
        tag.setLastModifyTime(getLastModifyTime());
        return tag;
    }
}
