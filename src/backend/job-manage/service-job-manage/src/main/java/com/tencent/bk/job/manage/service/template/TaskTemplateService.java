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

package com.tencent.bk.job.manage.service.template;

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;

import java.util.List;
import java.util.Set;

public interface TaskTemplateService {
    /**
     * 分页查询模版列表
     *
     * @param query 查询条件
     * @return 分页后的模版列表
     */
    PageData<TaskTemplateInfoDTO> listPageTaskTemplates(TaskTemplateQuery query);

    /**
     * 分页查询模版基本信息列表
     *
     * @param query                 查询条件
     * @param favoredTemplateIdList 收藏的模板ID列表,需要优先展示
     * @return 分页后的模版基本列表
     */
    PageData<TaskTemplateInfoDTO> listPageTaskTemplatesBasicInfo(TaskTemplateQuery query,
                                                                 List<Long> favoredTemplateIdList);

    /**
     * 根据 ID 查询模版信息
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 模版信息
     */
    TaskTemplateInfoDTO getTaskTemplateById(Long appId, Long templateId);

    /**
     * 新增、保存模版信息
     *
     * @param taskTemplateInfo 待新增、保存的模版信息
     * @return 模版 ID
     */
    Long saveTaskTemplate(TaskTemplateInfoDTO taskTemplateInfo);

    /**
     * 删除模版
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 是否删除成功
     */
    Boolean deleteTaskTemplate(Long appId, Long templateId);

    /**
     * 获取标签关联的模版数量
     *
     * @param appId 业务 ID
     * @return 标签模版数量
     */
    TagCountVO getTagTemplateCount(Long appId);

    /**
     * 更新模版基础信息
     * <p>
     * 仅更新名称、描述、标签
     *
     * @param taskTemplateInfo 模版信息
     * @return 是否更新成功
     */
    Boolean saveTaskTemplateBasicInfo(TaskTemplateInfoDTO taskTemplateInfo);

    /**
     * 根据作业模版 ID 查询模版基础信息
     * <p>
     * 不包含步骤、变量信息
     *
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @return 模版基础信息
     */
    TaskTemplateInfoDTO getTaskTemplateBasicInfoById(Long appId, Long templateId);

    /**
     * 根据作业模版 ID 查询模版基础信息
     * <p>
     * 不包含步骤、变量信息
     *
     * @param templateId 作业模版 ID
     * @return 模版基础信息
     */
    TaskTemplateInfoDTO getTaskTemplateBasicInfoById(Long templateId);

    /**
     * 根据作业模版 IDs 批量查询模版基础信息
     * <p>
     * 不包含步骤、变量信息
     *
     * @param templateIds 作业模版 IDs
     * @return 模版基础信息列表
     */
    List<TaskTemplateInfoDTO> listTaskTemplateBasicInfoByIds(List<Long> templateIds);

    /**
     * 根据模版 ID 列表批量查询模版基础信息
     * <p>
     * 不包含步骤、变量信息
     *
     * @param appId          业务 ID
     * @param templateIdList 模版 ID 列表
     * @return 模版基础信息列表
     */
    List<TaskTemplateInfoDTO> listTaskTemplateBasicInfoByIds(Long appId, List<Long> templateIdList);

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
     * 批量更新模版中的脚本引用状态
     * <p>
     * 当脚本版本变化时由脚本管理服务调用此接口后台更新模版中的脚本引用状态
     *
     * @param appId           业务 ID
     * @param scriptId        脚本 ID
     * @param scriptVersionId 脚本版本 ID
     * @param status          引用状态
     * @return 更新任务是否入队成功
     */
    boolean updateScriptStatus(Long appId, String scriptId, Long scriptVersionId, JobResourceStatusEnum status);

    /**
     * 获取用户收藏的作业模版基本信息列表
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @return 用户收藏的作业模版基本信息列表
     */
    List<TaskTemplateInfoDTO> getFavoredTemplateBasicInfo(Long appId, String username);

    /**
     * 保存作业模版（仅数据迁移用）
     *
     * @param taskTemplateInfo 作业模版信息
     * @param createTime       创建时间
     * @param lastModifyTime   最后更新时间
     * @param lastModifyUser   最后更新人
     * @return 是否保存成功
     */
    Long saveTaskTemplateForMigration(
        TaskTemplateInfoDTO taskTemplateInfo, Long createTime, Long lastModifyTime, String lastModifyUser
    );

    /**
     * 为模板创建标签
     *
     * @param taskTemplateInfo 模版信息
     */
    void createNewTagForTemplateIfNotExist(TaskTemplateInfoDTO taskTemplateInfo);

    /**
     * 更新模版步骤信息
     *
     * @param taskTemplateInfo 模版信息
     */
    void processTemplateStep(TaskTemplateInfoDTO taskTemplateInfo);

    /**
     * 新增模版信息
     *
     * @param taskTemplateInfo 模版信息
     * @return 新增的模版 ID
     */
    Long insertNewTemplate(TaskTemplateInfoDTO taskTemplateInfo);

    boolean insertNewTemplateWithTemplateId(TaskTemplateInfoDTO taskTemplateInfo);

    /**
     * 根据模版 ID 查询模版名称
     *
     * @param templateId 模版 ID
     * @return 模版名称
     */
    String getTemplateName(long templateId);

    /**
     * 根据模版 ID 查询模版信息
     *
     * @param templateId 模版 ID
     * @return 模版信息
     */
    TaskTemplateInfoDTO getTemplateById(long templateId);

    /**
     * 检查模版 ID 是否被占用
     *
     * @param templateId 模版 ID
     * @return 是否被占用
     */
    boolean checkTemplateId(Long templateId);

    /**
     * 查询指定业务下是否存在有效模版
     *
     * @param appId 业务 ID
     * @return 是否存在有效模版
     */
    boolean isExistAnyAppTemplate(Long appId);

    Integer countTemplates(Long appId);

    Integer countTemplateSteps(Long appId, TaskStepTypeEnum taskStepType, TaskScriptSourceEnum scriptSource,
                               TaskFileTypeEnum fileType);

    Integer countCiteScriptSteps(Long appId, List<String> scriptIdList);

    Set<String> listLocalFiles();
}
