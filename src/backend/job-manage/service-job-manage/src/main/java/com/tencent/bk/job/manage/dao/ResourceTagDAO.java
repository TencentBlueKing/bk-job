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

import com.tencent.bk.job.common.exception.DAOException;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;

import java.util.List;

public interface ResourceTagDAO {

    boolean batchSaveResourceTags(List<ResourceTagDTO> resourceTags) throws DAOException;

    List<ResourceTagDTO> listAllResourceTags();

    List<ResourceTagDTO> listResourceTags(Long tagId);

    List<ResourceTagDTO> listResourceTags(List<Long> tagIds);

    List<ResourceTagDTO> listResourceTags(Long tagId, Integer resourceType);

    List<ResourceTagDTO> listResourceTags(List<Long> tagIds, Integer resourceType);

    List<ResourceTagDTO> listResourceTags(Integer resourceType, List<String> resourceIds);

    List<ResourceTagDTO> listResourceTags(Integer resourceType, String resourceId);

    boolean deleteResourceTags(Integer resourceType, String resourceId, List<Long> tagIds);

    boolean deleteResourceTag(Integer resourceType, String resourceId, Long tagId);

    boolean deleteResourceTags(Integer resourceType, String resourceId);

    boolean deleteResourceTags(List<ResourceTagDTO> resourceTags);

    boolean deleteResourceTags(Long tagId);

}
