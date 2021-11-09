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

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptRelatedTaskPlanDTO;
import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskPlanDTO;
import com.tencent.bk.job.manage.model.dto.script.ScriptCitedTaskTemplateDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/**
 * 脚本服务
 *
 * @date 2019/09/19
 */
public interface ScriptService {

    /**
     * 根据ID查询脚本版本
     *
     * @param operator        操作者
     * @param appId           业务ID
     * @param scriptVersionId 脚本ID
     * @return 脚本版本
     * @throws ServiceException 业务异常
     */
    ScriptDTO getScriptVersion(String operator, Long appId, Long scriptVersionId) throws ServiceException;

    /**
     * 根据ID查询脚本版本
     *
     * @param operator 操作者
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @param version  脚本版本
     * @return 脚本版本
     * @throws ServiceException 业务异常
     */
    ScriptDTO getByScriptIdAndVersion(
        String operator,
        Long appId,
        String scriptId,
        String version
    ) throws ServiceException;

    /**
     * 根据ID查询脚本版本
     *
     * @param scriptVersionId 脚本版本ID
     * @return 脚本
     * @throws ServiceException
     */
    ScriptDTO getScriptVersion(Long scriptVersionId) throws ServiceException;

    /**
     * 根据scriptId查询脚本
     *
     * @param operator 操作者
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @return 脚本
     * @throws ServiceException 业务异常
     */
    ScriptDTO getScript(String operator, Long appId, String scriptId) throws ServiceException;

    /**
     * 根据scriptId查询脚本
     *
     * @param scriptId 脚本ID
     * @return 脚本
     * @throws ServiceException 业务异常
     */
    ScriptDTO getScriptByScriptId(String scriptId) throws ServiceException;

    /**
     * 根据scriptId查询脚本基本信息,不包含标签信息
     *
     * @param scriptId 脚本ID
     * @return 脚本
     * @throws ServiceException 业务异常
     */
    ScriptDTO getScriptWithoutTagByScriptId(String scriptId) throws ServiceException;

    /**
     * 根据脚本ID查询所有版本的脚本
     *
     * @param operator 操作者
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @return 脚本版本列表
     * @throws ServiceException 业务异常
     */
    List<ScriptDTO> listScriptVersion(String operator, Long appId, String scriptId) throws ServiceException;

    /**
     * 分页查询脚本列表
     *
     * @param scriptCondition     查询条件
     * @param baseSearchCondition 基本查询条件
     * @return 分页脚本列表
     * @throws ServiceException 业务异常
     */
    PageData<ScriptDTO> listPageScript(
        ScriptQuery scriptCondition,
        BaseSearchCondition baseSearchCondition
    ) throws ServiceException;

    /**
     * 根据条件查询脚本
     *
     * @param scriptQuery 查询条件
     * @return 脚本列表
     */
    List<ScriptDTO> listScripts(ScriptQuery scriptQuery);

    /**
     * 保存脚本
     *
     * @param operator 操作者
     * @param appId    业务ID
     * @param script   脚本信息
     * @throws ServiceException 业务异常
     */
    ScriptDTO saveScript(String operator, Long appId, ScriptDTO script) throws ServiceException;

    /**
     * 指定版本Id创建脚本版本
     *
     * @param operator 操作者
     * @param appId    业务ID
     * @param script   脚本信息
     * @throws ServiceException 业务异常
     */
    Pair<String, Long> createScriptWithVersionId(
        String operator,
        Long appId,
        ScriptDTO script,
        Long createTime,
        Long lastModifyTime
    ) throws ServiceException;

    /**
     * 查询引用脚本的作业列表
     *
     * @param scriptId 脚本ID
     * @return 引用的作业列表
     * @throws ServiceException 业务异常
     */
    List<ScriptRelatedTaskPlanDTO> listScriptRelatedTasks(String scriptId) throws ServiceException;

    /**
     * 查询引用脚本版本的作业列表
     *
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @return 引用的作业列表
     * @throws ServiceException 业务异常
     */
    List<ScriptRelatedTaskPlanDTO> listScriptVersionRelatedTasks(
        String scriptId,
        Long scriptVersionId
    ) throws ServiceException;

    /**
     * 删除脚本
     *
     * @param operator 操作者
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @throws ServiceException 业务异常
     */
    void deleteScript(String operator, Long appId, String scriptId) throws ServiceException;

    /**
     * 删除脚本版本
     *
     * @param operator        操作者
     * @param appId           业务ID
     * @param scriptVersionId 脚本版本ID
     * @throws ServiceException 业务异常
     */
    void deleteScriptVersion(String operator, Long appId, Long scriptVersionId) throws ServiceException;

    /**
     * 上线脚本
     *
     * @param appId           业务ID
     * @param operator        操作者
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @throws ServiceException 业务异常
     */
    void publishScript(Long appId, String operator, String scriptId, Long scriptVersionId) throws ServiceException;

    /**
     * 下线脚本
     *
     * @param appId           业务ID
     * @param operator        操作者
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @throws ServiceException 业务异常
     */
    void disableScript(Long appId, String operator, String scriptId, Long scriptVersionId) throws ServiceException;

    /**
     * 批量获取脚本的在线版本
     *
     * @param scriptIdList 脚本ID列表
     * @return 脚本信息
     * @throws ServiceException 业务异常
     */
    Map<String, ScriptDTO> batchGetOnlineScriptVersionByScriptIds(List<String> scriptIdList) throws ServiceException;

    /**
     * 更新脚本描述
     *
     * @param appId    业务ID
     * @param operator 操作者
     * @param scriptId 脚本ID
     * @param desc     脚本描述
     * @throws ServiceException 业务异常
     */
    void updateScriptDesc(Long appId, String operator, String scriptId, String desc) throws ServiceException;

    /**
     * 更新脚本名称
     *
     * @param appId    业务ID
     * @param operator 操作者
     * @param scriptId 脚本ID
     * @param newName  脚本名称
     * @throws ServiceException 业务异常
     */
    void updateScriptName(Long appId, String operator, String scriptId, String newName) throws ServiceException;

    /**
     * 更新脚本标签
     *
     * @param appId    业务ID
     * @param operator 操作者
     * @param scriptId 脚本ID
     * @param tags     脚本标签列表
     * @throws ServiceException 业务异常
     */
    void updateScriptTags(Long appId, String operator, String scriptId, List<TagDTO> tags) throws ServiceException;

    /**
     * 根据脚本名称模糊查询业务下的脚本名
     *
     * @param appId   业务ID
     * @param keyword 关键字
     * @return 脚本名称列表
     * @throws ServiceException 业务异常
     */
    List<String> listScriptNames(Long appId, String keyword) throws ServiceException;

    /**
     * 获取业务下的已上线脚本列表
     *
     * @param operator 操作者
     * @param appId    业务ID
     * @return 脚本列表
     * @throws ServiceException 业务异常
     */
    List<ScriptDTO> listOnlineScriptForApp(String operator, long appId) throws ServiceException;

    /**
     * 获取脚本列表
     *
     * @param scriptCondition     查询条件
     * @param baseSearchCondition 基本查询条件
     * @return 脚本列表
     * @throws ServiceException 业务异常
     */
    PageData<ScriptDTO> listPageOnlineScript(ScriptQuery scriptCondition,
                                             BaseSearchCondition baseSearchCondition) throws ServiceException;

    /**
     * 获取脚本已上线脚本版本
     *
     * @param operator 操作者
     * @param appId    业务 ID
     * @param scriptId 脚本 ID
     * @return 已上线版本，如果没有返回null
     * @throws ServiceException
     */
    ScriptDTO getOnlineScriptVersionByScriptId(String operator, long appId, String scriptId) throws ServiceException;

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
     * @param scriptQuery         查询条件
     * @param baseSearchCondition 基本查询条件
     * @return
     */
    PageData<ScriptDTO> listPageScriptVersion(ScriptQuery scriptQuery, BaseSearchCondition baseSearchCondition);

    /**
     * 获取引用脚本的模板与步骤
     *
     * @param username 用户名
     * @param appId    业务ID
     * @param scriptId 脚本ID
     * @return 引用脚本的模板与步骤
     */
    List<ScriptSyncTemplateStepDTO> listScriptSyncTemplateSteps(String username, Long appId, String scriptId);

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
    List<SyncScriptResultDTO> syncScriptToTaskTemplate(String username, Long appId, String scriptId,
                                                       Long syncScriptVersionId,
                                                       List<TemplateStepIDDTO> templateStepIDs)
        throws PermissionDeniedException;

    /**
     * 获取引用脚本的模板数量
     *
     * @param username
     * @param appId
     * @param scriptId
     * @param scriptVersionId
     * @return
     */
    Integer getScriptTemplateCiteCount(String username, Long appId, String scriptId, Long scriptVersionId);

    /**
     * 获取引用脚本的执行方案数量
     *
     * @param username
     * @param appId
     * @param scriptId
     * @param scriptVersionId
     * @return
     */
    Integer getScriptTaskPlanCiteCount(String username, Long appId, String scriptId, Long scriptVersionId);

    /**
     * 获取引用脚本的模板信息
     *
     * @param username        用户名
     * @param appId           业务ID
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @return
     */
    List<ScriptCitedTaskTemplateDTO> getScriptCitedTemplates(String username, Long appId, String scriptId,
                                                             Long scriptVersionId);

    /**
     * 获取引用脚本的执行方案信息
     *
     * @param username        用户名
     * @param appId           业务ID
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     * @return
     */
    List<ScriptCitedTaskPlanDTO> getScriptCitedTaskPlans(String username, Long appId, String scriptId,
                                                         Long scriptVersionId);

    /**
     * 业务下是否存在任意脚本
     *
     * @param appId 业务ID
     */
    boolean isExistAnyAppScript(Long appId);

    /**
     * 是否存在任意公共脚本
     */
    boolean isExistAnyPublicScript();

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

}
