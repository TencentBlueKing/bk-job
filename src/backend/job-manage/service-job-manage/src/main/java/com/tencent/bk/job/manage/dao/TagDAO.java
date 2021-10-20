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

package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.dto.TagDTO;

import java.util.List;

/**
 * @since 29/9/2019 16:04
 */
public interface TagDAO {

    /**
     * 根据标签 ID 获取标签信息
     *
     * @param tagId 标签 ID
     * @return 标签信息或 {@code null}
     */
    TagDTO getTagById(long tagId);

    /**
     * 根据业务 ID 和标签 ID 获取标签信息
     *
     * @param appId 业务 ID
     * @param tagId 标签 ID
     * @return 标签信息或 {@code null}
     */
    TagDTO getTagById(long appId, long tagId);

    /**
     * 根据业务 ID 和标签 ID 列表获取标签信息
     *
     * @param appId  业务 ID
     * @param tagIds 标签 ID 列表
     * @return 标签信息列表或空列表
     */
    List<TagDTO> listTagsByIds(long appId, List<Long> tagIds);

    /**
     * 根据标签 ID 列表获取标签信息
     *
     * @param tagIds 标签 ID 列表
     * @return 标签信息列表或空列表
     */
    List<TagDTO> listTagsByIds(List<Long> tagIds);

    /**
     * 根据业务 ID 和标签 ID 列表获取标签信息
     *
     * @param appId  业务 ID
     * @param tagIds 标签 ID 列表
     * @return 标签信息列表或空列表
     * @see TagDAO#listTagsByIds(long, java.util.List)
     */
    List<TagDTO> listTagsByIds(long appId, Long... tagIds);

    /**
     * 根据业务 ID 获取标签信息列表
     *
     * @param appId 业务 ID
     * @return 标签信息列表或空列表
     */
    List<TagDTO> listTagsByAppId(long appId);

    /**
     * 新增 Tag
     *
     * @param tag Tag 信息
     * @return 新增 Tag 的 ID
     */
    Long insertTag(TagDTO tag);

    /**
     * 根据 TagId 更新 Tag name
     *
     * @param tag Tag 信息
     * @return 更新是否成功
     */
    boolean updateTagById(TagDTO tag);

    /**
     * 标签通用查询
     *
     * @param searchCondition 查询条件
     * @return 标签列表
     */
    List<TagDTO> listTags(TagDTO searchCondition);

    /**
     * 标签通用查询
     *
     * @param tagQuery            查询条件
     * @param baseSearchCondition 基本查询条件
     * @return 分页标签列表
     */
    PageData<TagDTO> listPageTags(TagDTO tagQuery, BaseSearchCondition baseSearchCondition);

    /**
     * 根据标签ID删除标签
     *
     * @param tagId 标签ID
     * @return 是否删除成功
     */
    boolean deleteTagById(Long tagId);

    /**
     * 业务下是否存在同名标签
     *
     * @param appId   业务ID
     * @param tagName 标签名称
     * @return 是否存在
     */
    boolean isExistDuplicateName(Long appId, String tagName);

    List<TagDTO> listAllTags();

}
