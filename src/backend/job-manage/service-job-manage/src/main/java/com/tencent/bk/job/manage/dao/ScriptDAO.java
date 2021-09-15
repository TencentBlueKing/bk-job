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
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ScriptDAO {
    /**
     * 根据条件查询脚本
     *
     * @param scriptQuery
     * @param baseSearchCondition
     * @return
     */
    PageData<ScriptDTO> listPageScript(ScriptQuery scriptQuery, BaseSearchCondition baseSearchCondition);

    /**
     * 根据scriptId获取脚本信息
     *
     * @param scriptId
     * @return
     */
    ScriptDTO getScriptByScriptId(String scriptId);

    /**
     * 通过id查询脚本版本
     *
     * @param id 脚本id
     * @return ScriptDTO
     */
    ScriptDTO getScriptVersionById(long id);

    /**
     * 通过id查询脚本版本
     *
     * @return ScriptDTO
     */
    ScriptDTO getByScriptIdAndVersion(Long appId, String scriptId, String version);

    /**
     * 通过脚本版本ID批量查询脚本版本
     *
     * @param scriptVersionIds 脚本版本id
     * @return ScriptDTO
     */
    List<ScriptDTO> batchGetScriptVersionsByIds(Collection<Long> scriptVersionIds);

    /**
     * 新增脚本
     *
     * @param script
     * @
     */
    String saveScript(ScriptDTO script);

    /**
     * 新增脚本
     *
     * @param script
     * @
     */
    String saveScript(ScriptDTO script, long createTime, long lastModifyTime);

    /**
     * 新增脚本
     *
     * @param script
     * @
     */
    String saveScript(DSLContext dslContext, ScriptDTO script, long createTime, long lastModifyTime);

    /**
     * 更新脚本
     *
     * @param script
     */
    void updateScript(ScriptDTO script);

    /**
     * 更新脚本
     *
     * @param script
     */
    void updateScript(ScriptDTO script, long lastModifyTime);

    /**
     * 更新脚本
     *
     * @param script
     */
    void updateScript(DSLContext dslContext, ScriptDTO script, long lastModifyTime);

    /**
     * 删除脚本
     *
     * @param scriptId
     */
    void deleteScript(String scriptId);

    /**
     * 新增脚本版本
     *
     * @param scriptVersion
     */
    Long saveScriptVersion(ScriptDTO scriptVersion);

    /**
     * 新增脚本版本
     *
     * @param scriptVersion
     */
    Long saveScriptVersion(ScriptDTO scriptVersion, long createTime, long lastModifyTime);

    /**
     * 新增脚本版本
     *
     * @param scriptVersion
     */
    Long saveScriptVersion(DSLContext dslContext, ScriptDTO scriptVersion, long createTime, long lastModifyTime);

    /**
     * 根据脚本ID查询所有的版本
     *
     * @param scriptId
     * @return
     */
    List<ScriptDTO> listScriptVersionsByScriptId(String scriptId);

    /**
     * 业务下是否存在相同脚本Id
     *
     * @param appId
     * @param scriptId
     * @return
     */
    boolean isExistDuplicateScriptId(Long appId, String scriptId);

    /**
     * 业务下是否存在同名脚本
     *
     * @param appId
     * @param scriptName
     * @return
     */
    boolean isExistDuplicateName(Long appId, String scriptName);

    /**
     * 更新脚本描述
     *
     * @param scriptVersionId
     * @param desc
     */
    void updateScriptVersionDesc(Long scriptVersionId, String desc);

    /**
     * 删除脚本版本
     *
     * @param scriptVersionId
     */
    void deleteScriptVersion(Long scriptVersionId);

    /**
     * 删除脚本下的所有版本
     *
     * @param scriptId
     */
    void deleteScriptVersionByScriptId(String scriptId);

    /**
     * 更新脚本版本状态
     *
     * @param scriptVersionId
     * @param status
     */
    void updateScriptVersionStatus(Long scriptVersionId, Integer status);

    /**
     * 批量根据scriptId获取已上线的脚本版本
     *
     * @param scriptIds
     * @return
     */
    Map<String, ScriptDTO> batchGetOnlineByScriptIds(List<String> scriptIds);

    /**
     * 更新脚本描述
     *
     * @param operator
     * @param scriptId
     * @param desc
     */
    void updateScriptDesc(String operator, String scriptId, String desc);

    /**
     * 更新脚本名称
     *
     * @param operator
     * @param scriptId
     * @param name
     */
    void updateScriptName(String operator, String scriptId, String name);

    /**
     * 更新脚本版本信息
     *
     * @param operator
     * @param scriptVersionId
     * @param scriptVersion
     */
    void updateScriptVersion(String operator, Long scriptVersionId, ScriptDTO scriptVersion);

    /**
     * 根据脚本名称模糊查询业务下的脚本名
     *
     * @param appId
     * @param keyword
     * @return
     */
    List<String> listScriptNames(Long appId, String keyword);

    /**
     * 获取业务下的已上线脚本列表
     *
     * @param appId
     * @return
     */
    List<ScriptDTO> listOnlineScriptForApp(long appId);

    /**
     * 获取已上线脚本列表
     *
     * @param scriptCondition     查询条件
     * @param baseSearchCondition 基本查询条件
     * @return 脚本列表
     */
    PageData<ScriptDTO> listPageOnlineScript(ScriptQuery scriptCondition,
                                             BaseSearchCondition baseSearchCondition);

    /**
     * 分页查询脚本版本
     *
     * @param scriptQuery
     * @param baseSearchCondition
     * @return
     */
    PageData<ScriptDTO> listPageScriptVersion(ScriptQuery scriptQuery, BaseSearchCondition baseSearchCondition);

    /**
     * 获取脚本已上线版本信息
     *
     * @param appId    业务 ID
     * @param scriptId 脚本 ID
     * @return 脚本版本
     */
    ScriptDTO getOnlineScriptVersionByScriptId(long appId, String scriptId);

    /**
     * 获取脚本已上线版本信息
     *
     * @param scriptId 脚本 ID
     * @return 脚本版本
     */
    ScriptDTO getOnlineScriptVersionByScriptId(String scriptId);

    /**
     * 获取业务脚本数量
     *
     * @param appId 业务 ID
     * @return 脚本数
     */
    long countScriptByAppId(long appId);

    /**
     * 脚本版本号是否重复
     *
     * @param scriptId 脚本ID
     * @param version  脚本版本
     * @return 是否重复
     */
    boolean isExistDuplicateVersion(String scriptId, String version);

    /**
     * 脚本版本Id是否重复
     *
     * @param scriptVersionId 脚本版本Id
     * @return 是否重复
     */
    boolean isExistDuplicateScriptId(Long scriptVersionId);

    /**
     * 业务下是否存在任意脚本
     *
     * @param appId 业务ID
     */
    boolean isExistAnyScript(Long appId);

    /**
     * 是否存在任意公共脚本
     */
    boolean isExistAnyPublicScript();

    /**
     * 脚本总量统计
     *
     * @return
     */
    Integer countScripts();

    /**
     * 脚本统计
     *
     * @param appId                 业务Id
     * @param scriptTypeEnum        脚本类型
     * @param jobResourceStatusEnum 脚本版本状态
     * @return
     */
    Integer countScripts(Long appId, ScriptTypeEnum scriptTypeEnum, JobResourceStatusEnum jobResourceStatusEnum);

    /**
     * 脚本版本统计
     *
     * @param appId                 业务Id
     * @param scriptTypeEnum        脚本类型
     * @param jobResourceStatusEnum 脚本版本状态
     * @return
     */
    Integer countScriptVersions(Long appId, ScriptTypeEnum scriptTypeEnum, JobResourceStatusEnum jobResourceStatusEnum);

    /**
     * 查询某业务的所有脚本Id
     *
     * @param appId 业务Id
     * @return
     */
    List<String> listAppScriptIds(Long appId);

    /**
     * 获取脚本标签(兼容老版本)
     *
     * @return Map<ScriptId, List<TagId>>
     */
    Map<String, List<Long>> listAllScriptTagsCompatible();
}
