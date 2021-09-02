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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.dto.TagDTO;

import java.util.List;
import java.util.Map;

/**
 * @since 30/9/2019 16:48
 */
public interface TagService {
    /**
     * 根据业务 ID 和 Tag ID 查询 Tag 信息
     *
     * @param tagId Tag ID
     * @return TagDTO Tag 信息
     */
    TagDTO getTagInfoById(Long tagId);

    /**
     * 根据业务 ID 和 Tag ID 查询 Tag 信息
     *
     * @param appId 业务 ID
     * @param tagId Tag ID
     * @return TagDTO Tag 信息
     */
    TagDTO getTagInfoById(Long appId, Long tagId);

    /**
     * 根据业务 ID 查询所有 Tag
     *
     * @param appId 业务 ID
     * @return Tag 信息列表
     */
    List<TagDTO> listTagsByAppId(Long appId);

    /**
     * 根据业务ID和tagIdList批量查询标签
     *
     * @param appId
     * @param tagIdList
     * @return
     */
    List<TagDTO> listTagsByAppIdAndTagIdList(Long appId, List<Long> tagIdList);

    /**
     * 新增 Tag
     *
     * @param appId    业务 ID
     * @param tagName  Tag 名称
     * @param username 操作人
     * @return Tag ID
     */
    Long insertNewTag(Long appId, String tagName, String username);

    /**
     * 更新 Tag 信息
     *
     * @param appId    业务 ID
     * @param tagId    Tag ID
     * @param tagName  Tag 名称
     * @param username 操作人
     * @return 是否成功
     */
    Boolean updateTagById(Long appId, Long tagId, String tagName, String username);

    /**
     * 根据tag id字符串返回格式化之后的标签名称
     * 例如：
     * tagIdsStr="<1>,<2>",返回"tag1,tag2"
     *
     * @param appId
     * @param tagIdsStr
     * @return
     */
    String getFormattedTagNames(Long appId, String tagIdsStr);

    /**
     * 批量根据tag id字符串返回格式化之后的标签名称
     * 例如：
     * tagIdsStrList=["<1>,<2>","<2>,<3>"],返回{"<1>,<2>":"tag1,tag2","<2>,<3>":"tag2,tag3"}
     *
     * @param appId
     * @param tagIdsStrList
     * @return
     */
    Map<String, String> batchGetFormattedTagNames(Long appId, List<String> tagIdsStrList);

    /**
     * 批量创建不存在的标签
     *
     * @param tags
     * @return
     */
    List<TagDTO> createNewTagIfNotExist(List<TagDTO> tags, Long appId, String username);

    /**
     * 标签通用查询
     *
     * @param appId
     * @param tagNameKeyword
     * @return
     */
    List<TagDTO> listTags(Long appId, String tagNameKeyword);

    /**
     * 标签通用分页查询
     *
     * @param tagCondition
     * @param baseSearchCondition
     * @return
     */
    PageData<TagDTO> listTags(TagDTO tagCondition, BaseSearchCondition baseSearchCondition);
}
