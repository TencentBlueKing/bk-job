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

package com.tencent.bk.job.manage.dao.template;

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import org.jooq.types.ULong;

import java.util.List;
import java.util.Map;

/**
 * @since 27/9/2019 12:28
 */
public interface TaskTemplateDAO {

    /**
     * 根据参数拉取模版基本信息列表
     *
     * @param query 查询条件
     * @return 分页后的模版基本信息列表
     */
    PageData<TaskTemplateInfoDTO> listPageTaskTemplates(TaskTemplateQuery query);

    /**
     * 批量查询模版数据
     *
     * @param query 查询条件
     * @return 模版信息
     */
    List<TaskTemplateInfoDTO> listTaskTemplates(TaskTemplateQuery query);

    /**
     * 根据模版 ID 查询模版数据
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 模版信息
     */
    TaskTemplateInfoDTO getTaskTemplateById(Long appId, Long templateId);

    /**
     * 根据模版 ID 查询模版数据
     *
     * @param templateId 模版 ID
     * @return 模版信息
     */
    TaskTemplateInfoDTO getTaskTemplateById(Long templateId);

    /**
     * 查询已删除的模板信息
     *
     * @param templateId 模版 ID
     * @return 模版信息
     */
    TaskTemplateInfoDTO getDeletedTaskTemplateById(Long templateId);

    /**
     * 新增模版信息
     *
     * @param templateInfo 模版信息
     * @return 新模版 ID
     */
    Long insertTaskTemplate(TaskTemplateInfoDTO templateInfo);

    /**
     * 根据 ID 更新模版信息
     *
     * @param templateInfo 模版信息
     * @param bumpVersion  是否更新版本号
     * @return 是否更新成功
     */
    boolean updateTaskTemplateById(TaskTemplateInfoDTO templateInfo, boolean bumpVersion);

    /**
     * 根据 ID 删除模版
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 是否删除成功
     */
    boolean deleteTaskTemplateById(Long appId, Long templateId);

    /**
     * 获取模版总个数
     *
     * @param appId 业务 ID
     * @return 模版总个数
     */
    Long getAllTemplateCount(Long appId);

    /**
     * 获取待更新的模版个数
     *
     * @param appId 业务 ID
     * @return 待更新的模版个数
     */
    Long getNeedUpdateTemplateCount(Long appId);

    /**
     * 检查模版名称是否可用
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @param name       模版名称
     * @return 是否可用
     */
    boolean checkTemplateName(Long appId, Long templateId, String name);

    /**
     * 根据模版 ID 获取模版版本号
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 模版版本号
     */
    String getTemplateVersionById(Long appId, Long templateId);

    /**
     * 根据模版名称查询模版信息
     *
     * @param appId 业务 ID
     * @param name  模版名称 完全匹配
     * @return 模版信息
     */
    TaskTemplateInfoDTO getTaskTemplateByName(Long appId, String name);

    /**
     * 根据模版 ID 查询模版信息
     *
     * @param templateId 模版 ID
     * @return 模版信息
     */
    TaskTemplateInfoDTO getTemplateById(long templateId);

    /**
     * 根据模版 ID 查询模版名称
     *
     * @param templateId 模版 ID
     * @return 模版名称
     */
    String getTemplateName(long templateId);

    /**
     * 检查模版 ID 是否被占用
     *
     * @param templateId 模版 ID
     * @return 是否被占用
     */
    boolean checkTemplateId(Long templateId);

    /**
     * 保留模版 ID 插入模版信息
     *
     * @param templateInfo 模版信息
     * @return 是否插入成功
     */
    boolean insertTaskTemplateWithId(TaskTemplateInfoDTO templateInfo);

    /**
     * 根据 ID 更新模版版本
     *
     * @param appId      业务ID
     * @param templateId 模版ID
     * @param version    模板版本
     * @return 是否更新成功
     */
    boolean updateTaskTemplateVersion(long appId, long templateId, String version);

    /**
     * 查询指定业务下是否存在有效模版
     *
     * @param appId 业务 ID
     * @return 是否存在有效模版
     */
    boolean isExistAnyAppTemplate(Long appId);

    void updateTemplateStatus(ULong templateId, int scriptStatus);

    Integer countTemplates(Long appId);

    List<Long> listAllTemplateId();

    List<Long> listAllAppTemplateId(Long appId);

    /**
     * 获取模板标签(兼容老版本)
     *
     * @return Map<TemplateId, List<TagId>>
     */
    Map<Long, List<Long>> listAllTemplateTagsCompatible();
}
