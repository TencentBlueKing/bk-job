/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.SyncScriptResultDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.TemplateStepIDDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 公共脚本服务
 */
public interface PublicScriptService {

    /**
     * 根据 `租户 ID + 脚本版本ID` 查询脚本版本
     *
     * @param tenantId        租户 ID
     * @param scriptVersionId 脚本ID
     * @return 脚本版本
     */
    ScriptDTO getScriptVersion(String tenantId, Long scriptVersionId);

    /**
     * 根据ID查询脚本版本
     *
     * @param scriptVersionId 脚本ID
     * @return 脚本版本
     */
    ScriptDTO getScriptVersion(Long scriptVersionId);

    /**
     * 根据scriptId查询脚本
     *
     * @param scriptId 脚本ID
     * @return 脚本
     */
    ScriptDTO getScript(String scriptId);

    /**
     * 根据`租户 id + scriptId` 查询脚本
     *
     * @param tenantId 租户 ID
     * @param scriptId 脚本ID
     * @return 脚本
     */
    ScriptDTO getScript(String tenantId, String scriptId);


    /**
     * 根据scriptIds批量查询脚本基础信息
     *
     * @param scriptIds 脚本ID集合
     * @return 脚本
     */
    List<ScriptBasicDTO> listScriptBasicInfoByScriptIds(Collection<String> scriptIds);

    /**
     * 根据脚本ID查询所有版本的脚本
     *
     * @param scriptId 脚本ID
     * @return 脚本版本列表
     */
    List<ScriptDTO> listScriptVersion(String scriptId);

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
     * 保存脚本
     *
     * @param user   用户
     * @param script 脚本信息
     */
    ScriptDTO saveScript(User user, ScriptDTO script);

    /**
     * 保存脚本版本
     *
     * @param user          用户
     * @param scriptVersion 脚本版本
     */
    ScriptDTO saveScriptVersion(User user, ScriptDTO scriptVersion);

    /**
     * 更新脚本版本
     *
     * @param user          用户
     * @param scriptVersion 脚本版本
     */
    ScriptDTO updateScriptVersion(User user, ScriptDTO scriptVersion);

    /**
     * 删除脚本
     *
     * @param user     用户
     * @param scriptId 脚本ID
     */
    void deleteScript(User user, String scriptId);

    /**
     * 删除脚本版本
     *
     * @param user            用户
     * @param scriptVersionId 脚本版本ID
     */
    void deleteScriptVersion(User user, Long scriptVersionId);

    /**
     * 上线脚本
     *
     * @param user            用户
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     */
    void publishScript(User user, String scriptId, Long scriptVersionId);

    /**
     * 下线脚本
     *
     * @param user            用户
     * @param scriptId        脚本ID
     * @param scriptVersionId 脚本版本ID
     */
    void disableScript(User user, String scriptId, Long scriptVersionId);

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
     * @param user     操作人
     * @param scriptId 脚本ID
     * @param desc     脚本描述
     */
    ScriptDTO updateScriptDesc(User user, String scriptId, String desc);

    /**
     * 更新脚本名称
     *
     * @param user     操作人
     * @param scriptId 脚本ID
     * @param newName  脚本名称
     */
    ScriptDTO updateScriptName(User user, String scriptId, String newName);

    /**
     * 更新脚本标签
     *
     * @param user     操作人
     * @param scriptId 脚本ID
     * @param tags     脚本标签列表
     */
    ScriptDTO updateScriptTags(User user, String scriptId, List<TagDTO> tags);

    /**
     * 根据脚本名称模糊查询业务下的脚本名
     *
     * @param tenantId 租户 ID
     * @param keyword  关键字
     * @return 脚本名称列表
     */
    List<String> listScriptNames(String tenantId, String keyword);

    /**
     * 获取已上线的公共脚本
     *
     * @param tenantId 租户 ID
     * @return 脚本列表
     */
    List<ScriptDTO> listOnlineScript(String tenantId);


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
     * 批量同步脚本到作业模板
     *
     * @param user                操作人
     * @param syncScriptVersionId 需要同步的脚本版本ID
     * @param templateStepIDs     作业模板与步骤信息
     * @return 同步结果
     */
    List<SyncScriptResultDTO> syncScriptToTaskTemplate(User user,
                                                       String scriptId,
                                                       Long syncScriptVersionId,
                                                       List<TemplateStepIDDTO> templateStepIDs);

    /**
     * 是否存在任意公共脚本
     *
     * @param tenantId 租户 ID
     */
    boolean isExistAnyPublicScript(String tenantId);

    List<String> listScriptIds(String tenantId);

    /**
     * 获取标签关联的模版数量
     *
     * @return 标签模版数量
     */
    TagCountVO getTagScriptCount(String tenantId);

    /**
     * 根据脚本ID/版本号查询脚本
     *
     * @param tenantId 租户 ID
     * @param scriptId 脚本ID
     * @param version  脚本版本
     * @return 脚本版本
     */
    ScriptDTO getByScriptIdAndVersion(String tenantId, String scriptId, String version);

}
