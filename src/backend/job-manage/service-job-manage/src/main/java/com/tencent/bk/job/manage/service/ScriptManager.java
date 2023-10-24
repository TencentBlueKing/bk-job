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

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskPlanDTO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskTemplateDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 脚本管理通用实现
 */
public interface ScriptManager {

    /**
     * 根据ID查询脚本版本
     *
     * @param appId           业务ID
     * @param scriptVersionId 脚本ID
     * @return 脚本版本
     */
    ScriptDTO getScriptVersion(Long appId, Long scriptVersionId);

    /**
     * 根据ID查询脚本版本
     *
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @param version  脚本版本
     * @return 脚本版本
     */
    ScriptDTO getByScriptIdAndVersion(Long appId, String scriptId, String version);

    /**
     * 根据ID查询脚本版本
     *
     * @param scriptVersionId 脚本版本ID
     * @return 脚本
     */
    ScriptDTO getScriptVersion(Long scriptVersionId);

    /**
     * 根据scriptId查询脚本
     *
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @return 脚本
     */
    ScriptDTO getScript(Long appId, String scriptId);

    /**
     * 根据scriptId查询脚本
     *
     * @param scriptId 脚本ID
     * @return 脚本
     */
    ScriptDTO getScriptByScriptId(String scriptId);

    /**
     * 根据scriptIds批量查询脚本基础信息
     *
     * @param scriptIds 脚本ID集合
     * @return 脚本
     */
    List<ScriptBasicDTO> listScriptBasicInfoByScriptIds(Collection<String> scriptIds);

    /**
     * 根据scriptId查询脚本基本信息,不包含标签信息
     *
     * @param scriptId 脚本ID
     * @return 脚本
     */
    ScriptDTO getScriptWithoutTagByScriptId(String scriptId);

    /**
     * 根据脚本ID查询所有版本的脚本
     *
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @return 脚本版本列表
     */
    List<ScriptDTO> listScriptVersion(Long appId, String scriptId);

    /**
     * 分页查询脚本列表
     *
     * @param scriptQuery 查询条件
     * @return 分页脚本列表
     */
    PageData<ScriptDTO> listPageScript(ScriptQuery scriptQuery);

    /**
     * 根据条件查询脚本
     *
     * @param scriptQuery 查询条件
     * @return 脚本列表
     */
    List<ScriptDTO> listScripts(ScriptQuery scriptQuery);

    /**
     * 创建脚本
     *
     * @param script 脚本信息
     */
    ScriptDTO createScript(ScriptDTO script);

    /**
     * 创建脚本版本
     *
     * @param scriptVersion 脚本版本
     */
    ScriptDTO createScriptVersion(ScriptDTO scriptVersion);

    /**
     * 更新脚本版本
     *
     * @param scriptVersion 脚本版本
     */
    ScriptDTO updateScriptVersion(ScriptDTO scriptVersion);

    /**
     * 指定版本Id创建脚本版本
     *
     * @param appId  业务ID
     * @param script 脚本信息
     */
    Pair<String, Long> createScriptWithVersionId(Long appId,
                                                 ScriptDTO script,
                                                 Long createTime,
                                                 Long lastModifyTime);

    /**
     * 删除脚本
     *
     * @param appId    业务ID
     * @param scriptId 脚本ID
     */
    void deleteScript(Long appId, String scriptId);

    /**
     * 删除脚本版本
     *
     * @param appId           业务ID
     * @param scriptVersionId 脚本版本ID
     */
    void deleteScriptVersion(Long appId, Long scriptVersionId);

    /**
     * 上线脚本
     *
     * @param appId           业务ID
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     */
    void publishScript(Long appId, String scriptId, Long scriptVersionId);

    /**
     * 下线脚本
     *
     * @param appId           业务ID
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     */
    void disableScript(Long appId, String scriptId, Long scriptVersionId);

    /**
     * 批量获取脚本的在线版本
     *
     * @param scriptIdList 脚本ID列表
     * @return 脚本信息
     */
    Map<String, ScriptDTO> batchGetOnlineScriptVersionByScriptIds(List<String> scriptIdList);

    /**
     * 更新脚本描述
     *
     * @param operator 操作人
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @param desc     脚本描述
     */
    ScriptDTO updateScriptDesc(String operator, Long appId, String scriptId, String desc);

    /**
     * 更新脚本名称
     *
     * @param operator 操作人
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @param newName  脚本名称
     */
    ScriptDTO updateScriptName(String operator, Long appId, String scriptId, String newName);

    /**
     * 更新脚本标签
     *
     * @param operator 操作人
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @param tags     脚本标签列表
     */
    ScriptDTO updateScriptTags(String operator, Long appId, String scriptId, List<TagDTO> tags);

    /**
     * 根据脚本名称模糊查询业务下的脚本名
     *
     * @param appId   业务ID
     * @param keyword 关键字
     * @return 脚本名称列表
     */
    List<String> listScriptNames(Long appId, String keyword);

    /**
     * 获取已上线脚本列表
     *
     * @param appId 业务ID
     * @return 脚本列表
     */
    List<ScriptDTO> listOnlineScriptForApp(long appId);

    /**
     * 获取脚本已上线脚本版本
     *
     * @param appId    业务 ID
     * @param scriptId 脚本 ID
     * @return 已上线版本，如果没有返回null
     */
    ScriptDTO getOnlineScriptVersionByScriptId(long appId, String scriptId);

    /**
     * 获取脚本已上线脚本版本
     *
     * @param scriptId 脚本 ID
     * @return 已上线版本，如果没有返回null
     */
    ScriptDTO getOnlineScriptVersionByScriptId(String scriptId);

    /**
     * 分页查询脚本版本列表
     *
     * @param scriptQuery 查询条件
     * @return 脚本版本分页
     */
    PageData<ScriptDTO> listPageScriptVersion(ScriptQuery scriptQuery);

    /**
     * 获取引用脚本的模板与步骤
     *
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @return 引用脚本的模板与步骤
     */
    List<ScriptSyncTemplateStepDTO> listScriptSyncTemplateSteps(Long appId, String scriptId);

    /**
     * 批量同步脚本到作业模板
     *
     * @param username            用户名
     * @param appId               业务ID
     * @param scriptId            脚本ID
     * @param syncScriptVersionId 需要同步的脚本版本ID
     * @param templateStepIDs     作业模板与步骤信息
     * @return 同步结果
     */
    List<SyncScriptResultDTO> syncScriptToTaskTemplate(String username,
                                                       Long appId,
                                                       String scriptId,
                                                       Long syncScriptVersionId,
                                                       List<TemplateStepIDDTO> templateStepIDs);

    /**
     * 获取引用脚本的模板数量
     *
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @return 引用脚本的模板数量
     */
    Integer getScriptTemplateCiteCount(String scriptId, Long scriptVersionId);

    /**
     * 获取引用脚本的执行方案数量
     *
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @return 引用脚本的执行方案数量
     */
    Integer getScriptTaskPlanCiteCount(String scriptId, Long scriptVersionId);

    /**
     * 获取引用脚本的模板信息
     *
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @return 引用脚本的作业模板
     */
    List<ScriptCitedTaskTemplateDTO> getScriptCitedTemplates(String scriptId, Long scriptVersionId);

    /**
     * 获取引用脚本的执行方案信息
     *
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @return 引用脚本的执行方案
     */
    List<ScriptCitedTaskPlanDTO> getScriptCitedTaskPlans(String scriptId, Long scriptVersionId);

    Integer countScripts(Long appId, ScriptTypeEnum scriptTypeEnum, JobResourceStatusEnum jobResourceStatusEnum);

    Integer countScriptVersions(Long appId, ScriptTypeEnum scriptTypeEnum, JobResourceStatusEnum jobResourceStatusEnum);

    List<String> listScriptIds(Long appId);

    Integer countCiteScripts(Long appId);

    /**
     * 获取标签关联的模版数量
     *
     * @param appId 业务 ID
     * @return 标签模版数量
     */
    TagCountVO getTagScriptCount(Long appId);

    /**
     * 当前业务下是否存在任意脚本
     *
     * @param appId 业务 ID
     */
    boolean isExistAnyScript(Long appId);

    /**
     * 脚本版本是否被引用
     *
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     */
    boolean isScriptReferenced(String scriptId, Long scriptVersionId);

}
