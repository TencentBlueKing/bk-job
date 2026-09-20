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

package com.tencent.bk.job.dao;

import com.tencent.bk.job.model.VersionBranchDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 版本分支信息 Mapper。
 * 当前工程未开启 MyBatis mapUnderscoreToCamelCase，SELECT 必须使用列别名映射到 Java camelCase 属性。
 */
@Mapper
public interface VersionBranchMapper {

    @Insert("INSERT INTO version_branch ("
        + "version_branch, description, dev_branch, "
        + "dev_branch_deploy_pipeline_cmd, dev_branch_deploy_pipeline_cmd_desc, "
        + "create_time, last_modify_time"
        + ") VALUES ("
        + "#{versionBranch}, #{description}, #{devBranch}, "
        + "#{devBranchDeployPipelineCmd}, #{devBranchDeployPipelineCmdDesc}, "
        + "#{createTime}, #{lastModifyTime}"
        + ")")
    int insert(VersionBranchDTO dto);

    @Select("SELECT "
        + "version_branch AS versionBranch, "
        + "description AS description, "
        + "dev_branch AS devBranch, "
        + "dev_branch_deploy_pipeline_cmd AS devBranchDeployPipelineCmd, "
        + "dev_branch_deploy_pipeline_cmd_desc AS devBranchDeployPipelineCmdDesc, "
        + "create_time AS createTime, "
        + "last_modify_time AS lastModifyTime "
        + "FROM version_branch "
        + "WHERE version_branch = #{versionBranch}")
    VersionBranchDTO selectByVersionBranch(@Param("versionBranch") String versionBranch);

    @Select("SELECT COUNT(1) FROM version_branch WHERE version_branch = #{versionBranch}")
    int countByVersionBranch(@Param("versionBranch") String versionBranch);

    @Select("SELECT "
        + "version_branch AS versionBranch, "
        + "description AS description, "
        + "dev_branch AS devBranch, "
        + "dev_branch_deploy_pipeline_cmd AS devBranchDeployPipelineCmd, "
        + "dev_branch_deploy_pipeline_cmd_desc AS devBranchDeployPipelineCmdDesc, "
        + "create_time AS createTime, "
        + "last_modify_time AS lastModifyTime "
        + "FROM version_branch "
        + "ORDER BY version_branch ASC")
    List<VersionBranchDTO> selectAll();

    @Update("UPDATE version_branch SET "
        + "description = #{description}, "
        + "dev_branch = #{devBranch}, "
        + "dev_branch_deploy_pipeline_cmd = #{devBranchDeployPipelineCmd}, "
        + "dev_branch_deploy_pipeline_cmd_desc = #{devBranchDeployPipelineCmdDesc}, "
        + "last_modify_time = #{lastModifyTime} "
        + "WHERE version_branch = #{versionBranch}")
    int updateByVersionBranch(VersionBranchDTO dto);

    @Delete("DELETE FROM version_branch WHERE version_branch = #{versionBranch}")
    int deleteByVersionBranch(@Param("versionBranch") String versionBranch);
}
