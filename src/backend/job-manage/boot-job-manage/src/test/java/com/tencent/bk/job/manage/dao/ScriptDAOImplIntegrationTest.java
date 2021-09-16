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
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import org.jooq.generated.tables.Script;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_script_data.sql"})
@DisplayName("脚本管理DAO集成测试")
class ScriptDAOImplIntegrationTest {

    @Autowired
    private ScriptDAO scriptDAO;

    @Test
    @DisplayName("根据scriptVersionId获取脚本版本，返回结果")
    void whenGetScriptVersionByIdThenReturn() {
        ScriptDTO script = scriptDAO.getScriptVersionById(1);
        assertThat(script.getScriptVersionId()).isEqualTo(1L);
        assertThat(script.getAppId()).isEqualTo(2);
        assertThat(script.getId()).isEqualTo("dc65a20cd91811e993a2309c2357fc12");
        assertThat(script.getName()).isEqualTo("test1");
        assertThat(script.getContent()).isEqualTo("df");
        assertThat(script.getCategory()).isEqualTo(1);
        assertThat(script.getType()).isEqualTo(1);
        assertThat(script.isPublicScript()).isEqualTo(false);
        assertThat(script.getVersion()).isEqualTo("user1.20190917170000");
        assertThat(script.getCreator()).isEqualTo("user1");
        assertThat(script.getCreateTime()).isNotNull();
        assertThat(script.getLastModifyUser()).isNotEmpty();
        assertThat(script.getLastModifyTime()).isNotNull();
        assertThat(script.getStatus()).isEqualTo(0);
        assertThat(script.getVersionDesc()).isEqualTo("version_desc1");
        assertThat(script.getDescription()).isEqualTo("desc1");
    }

    @Test
    void givenNotExistIdWhenGetScriptVersionThenReturnNull() {
        ScriptDTO script = scriptDAO.getScriptVersionById(404);
        assertThat(script).isNull();
    }

    @Test
    void whenListPageScriptThenReturnOrderedPageResult() {
        ScriptQuery scriptCondition = new ScriptQuery();
        scriptCondition.setAppId(2L);
        scriptCondition.setType(1);
        scriptCondition.setName("test");

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(1);
        baseSearchCondition.setOrderField(Script.SCRIPT.LAST_MODIFY_TIME.getName());
        baseSearchCondition.setOrder(1);
        baseSearchCondition.setCreator("user1");

        PageData<ScriptDTO> pageData = scriptDAO.listPageScript(scriptCondition, baseSearchCondition);
        assertThat(pageData).isNotNull();
        assertThat(pageData.getPageSize()).isEqualTo(1);
        assertThat(pageData.getStart()).isEqualTo(0);
        assertThat(pageData.getTotal()).isEqualTo(2);

        List<ScriptDTO> resultScriptList = pageData.getData();
        assertThat(resultScriptList).isNotEmpty();
        assertThat(resultScriptList.size()).isEqualTo(1);

        ScriptDTO actual = resultScriptList.get(0);
        assertThat(actual.getAppId()).isEqualTo(2);
        assertThat(actual.getId()).isEqualTo("d68700a6db8711e9ac466c92bf62a896");
        assertThat(actual.getName()).isEqualTo("test2");
        assertThat(actual.getCategory()).isEqualTo(1);
        assertThat(actual.getType()).isEqualTo(1);
        assertThat(actual.isPublicScript()).isEqualTo(false);
        assertThat(actual.getCreator()).isEqualTo("user1");
        assertThat(actual.getCreateTime()).isNotNull();
        assertThat(actual.getLastModifyUser()).isEqualTo("user1");
        assertThat(actual.getLastModifyTime()).isNotNull();
    }

    @Test
    public void givenNoMatchScriptWhenListScriptThenReturnEmptyPageData() {
        ScriptQuery scriptCondition = new ScriptQuery();
        scriptCondition.setAppId(2L);
        scriptCondition.setName("notexistscript");

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);

        PageData<ScriptDTO> pageData = scriptDAO.listPageScript(scriptCondition, baseSearchCondition);
        assertThat(pageData).isNotNull();
        assertThat(pageData.getPageSize()).isEqualTo(10);
        assertThat(pageData.getStart()).isEqualTo(0);
        assertThat(pageData.getTotal()).isEqualTo(0);
        assertThat(pageData.getData()).isNullOrEmpty();
    }

    @Test
    public void whenListPageScriptByScriptIdThenReturnSingleData() {
        ScriptQuery scriptCondition = new ScriptQuery();
        scriptCondition.setId("dc65a20cd91811e993a2309c2357fc12");

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);

        PageData<ScriptDTO> pageData = scriptDAO.listPageScript(scriptCondition, baseSearchCondition);
        assertThat(pageData).isNotNull();
        assertThat(pageData.getTotal()).isEqualTo(1);
        assertThat(pageData.getData()).isNotNull();
        assertThat(pageData.getData().get(0).getId()).isEqualTo("dc65a20cd91811e993a2309c2357fc12");
    }

    @Test
    public void whenGetScriptByScriptIdThenReturn() {
        ScriptDTO script = scriptDAO.getScriptByScriptId("dc65a20cd91811e993a2309c2357fc12");
        assertThat(script.getId()).isEqualTo("dc65a20cd91811e993a2309c2357fc12");
        assertThat(script.getName()).isEqualTo("test1");
        assertThat(script.getType()).isEqualTo(1);
        assertThat(script.getCategory()).isEqualTo(1);
        assertThat(script.getCreator()).isEqualTo("user1");
        assertThat(script.getLastModifyUser()).isEqualTo("user1");
    }

    @Test
    public void whenSaveScriptThenStore() {
        ScriptDTO script = new ScriptDTO();
        script.setAppId(2L);
        script.setName("new_script");
        script.setCategory(1);
        script.setType(1);
        script.setPublicScript(false);
        script.setCreator("user3");

        String scriptId = scriptDAO.saveScript(script);
        assertThat(scriptId).isNotEmpty();

        ScriptQuery scriptCondition = new ScriptQuery();
        scriptCondition.setId(scriptId);

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);

        PageData<ScriptDTO> pageData = scriptDAO.listPageScript(scriptCondition, baseSearchCondition);
        assertThat(pageData.getTotal()).isEqualTo(1);
        assertThat(pageData.getData()).isNotEmpty();

        ScriptDTO savedScript = pageData.getData().get(0);
        assertThat(savedScript.getAppId()).isEqualTo(2);
        assertThat(savedScript.getName()).isEqualTo(script.getName());
        assertThat(savedScript.getCategory()).isEqualTo(script.getCategory());
        assertThat(savedScript.getType()).isEqualTo(script.getType());
        assertThat(savedScript.getCreator()).isEqualTo(script.getCreator());
        assertThat(savedScript.isPublicScript()).isEqualTo(script.isPublicScript());
    }

    @Test
    public void whenUpdateScriptThenStore() {
        ScriptDTO script = new ScriptDTO();
        script.setLastModifyUser("user2");
        script.setId("dc65a20cd91811e993a2309c2357fc12");
        script.setName("new_name");

        scriptDAO.updateScript(script);

        ScriptDTO updatedScript = getScriptById("dc65a20cd91811e993a2309c2357fc12");
        assertThat(updatedScript).isNotNull();
        assertThat(updatedScript.getLastModifyUser()).isEqualTo(script.getLastModifyUser());
        assertThat(updatedScript.getName()).isEqualTo("new_name");
    }

    private ScriptDTO getScriptById(String scriptId) {
        ScriptQuery scriptCondition = new ScriptQuery();
        scriptCondition.setId(scriptId);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);

        PageData<ScriptDTO> pageData = scriptDAO.listPageScript(scriptCondition, baseSearchCondition);
        if (pageData.getData() == null || pageData.getData().isEmpty()) {
            return null;
        }
        return pageData.getData().get(0);
    }

    @Test
    public void whenDeleteScriptByIdThenDeleted() {
        String id = "dc65a20cd91811e993a2309c2357fc12";
        scriptDAO.deleteScript(id);

        ScriptDTO notExistScript = getScriptById(id);

        assertThat(notExistScript).describedAs("check the script is deleted").isNull();
    }

    @Test
    public void whenSaveScriptVersionThenStore() {
        ScriptDTO scriptVersion = new ScriptDTO();
        scriptVersion.setId(JobUUID.getUUID());
        scriptVersion.setContent("IyEvYmluL2Jhc2gKZWNobyAnMTIzJwo=");
        scriptVersion.setCreator("user1");
        scriptVersion.setVersion("user1.20190927103700");
        scriptVersion.setLastModifyUser("user1");
        scriptVersion.setStatus(JobResourceStatusEnum.ONLINE.getValue());
        scriptVersion.setVersionDesc("description for new script version");

        Long scriptVersionId = scriptDAO.saveScriptVersion(scriptVersion);
        assertThat(scriptVersionId).isNotNull();

    }

    @Test
    public void whenListScriptVersionThenReturnOrderResult() {
        List<ScriptDTO> scripts = scriptDAO.listScriptVersionsByScriptId("dc65a20cd91811e993a2309c2357fc12");
        assertThat(scripts).isNotEmpty();
        assertThat(scripts).hasSize(2);
        // 按照更新时间排序
        assertThat(scripts.get(0).getLastModifyTime()).isGreaterThan(scripts.get(1).getLastModifyTime());
    }

    @Test
    public void givenNotExistScriptIdWhenListScriptVersionThenReturnEmptyList() {
        List<ScriptDTO> scripts = scriptDAO.listScriptVersionsByScriptId("notexistscriptid");
        assertThat(scripts).isNotNull();
        assertThat(scripts).isEmpty();
    }

    @Test
    public void whenExistScriptNameThenReturnTrue() {
        boolean isExist = scriptDAO.isExistDuplicateName(2L, "test1");
        assertThat(isExist).as("check script name duplicate").isEqualTo(true);
    }

    @Test
    public void whenNotExistScriptNameThenReturnFalse() {
        boolean isDuplicateName = scriptDAO.isExistDuplicateName(2L, "test_not_exist_name");
        assertThat(isDuplicateName).as("Check if the script name is duplicated").isEqualTo(false);

        isDuplicateName = scriptDAO.isExistDuplicateName(1L, "test1");
        assertThat(isDuplicateName).as("Check if the script name is duplicated").isEqualTo(false);
    }

    @Test
    public void whenUpdateScriptDescThenStore() {
        scriptDAO.updateScriptVersionDesc(1L, "new_desc");
        ScriptDTO updatedScript = scriptDAO.getScriptVersionById(1L);
        assertThat(updatedScript.getVersionDesc()).isEqualTo("new_desc");
    }

    @Test
    public void whenDeleteScriptVersionThenDeleted() {
        scriptDAO.deleteScriptVersion(1L);
        ScriptDTO deletedScriptVersion = scriptDAO.getScriptVersionById(1L);
        assertThat(deletedScriptVersion).describedAs("check the script version is deleted").isNull();
    }

    @Test
    public void whenDeleteAllScriptVersionThenDeleted() {
        scriptDAO.deleteScriptVersionByScriptId("dc65a20cd91811e993a2309c2357fc12");
        List<ScriptDTO> existScriptVersions = scriptDAO.listScriptVersionsByScriptId("dc65a20cd91811e993a2309c2357fc12");
        assertThat(existScriptVersions).describedAs("check that all script version is deleted").isNullOrEmpty();
    }

    @Test
    public void whenUpdateScriptVersionStatusThenUpdated() {
        Long scriptVersionId = 1L;
        Integer status = JobResourceStatusEnum.DISABLED.getValue();
        scriptDAO.updateScriptVersionStatus(scriptVersionId, status);
        ScriptDTO updatedScriptVersion = scriptDAO.getScriptVersionById(1L);
        assertThat(updatedScriptVersion.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("验证批量根据scriptIdList获取在线脚本")
    public void whenBatchGetOnlineScriptVersionByScriptIdsThenReturn() {
        List<String> scriptIdList = new ArrayList<>();
        scriptIdList.add("dc65a20cd91811e993a2309c2357fc12");
        scriptIdList.add("d68700a6db8711e9ac466c92bf62a896");
        scriptIdList.add("553285c5db8211e9ac466c92bf62a896");
        scriptIdList.add("3507cad7db8411e9ac466c92bf62a896");
        Map<String, ScriptDTO> scripts = scriptDAO.batchGetOnlineByScriptIds(scriptIdList);

        assertThat(scripts).hasSize(3).containsKeys("dc65a20cd91811e993a2309c2357fc12",
            "d68700a6db8711e9ac466c92bf62a896", "553285c5db8211e9ac466c92bf62a896");
    }

    @Test
    @DisplayName("验证更新脚本描述")
    public void whenUpdateScriptDescThenUpdated() {
        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
        String operator = "new_operator";
        String newDesc = "newDesc";
        scriptDAO.updateScriptDesc(operator, scriptId, newDesc);

        ScriptDTO updatedScript = scriptDAO.getScriptByScriptId(scriptId);

        assertThat(updatedScript.getDescription()).isEqualTo(newDesc);
        assertThat(updatedScript.getLastModifyUser()).isEqualTo(operator);
    }

    @Test
    @DisplayName("验证更新脚本名称")
    public void whenUpdateScriptNameThenUpdated() {
        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
        String operator = "new_operator";
        String newName = "newName";
        scriptDAO.updateScriptName(operator, scriptId, newName);

        ScriptDTO updatedScript = scriptDAO.getScriptByScriptId(scriptId);

        assertThat(updatedScript.getName()).isEqualTo(newName);
        assertThat(updatedScript.getLastModifyUser()).isEqualTo(operator);
    }

    @Test
    @DisplayName("验证更新脚本版本")
    public void whenUpdateScriptVersionThenUpdated() {
        Long scriptVersionId = 1L;
        String operator = "new_operator";
        ScriptDTO scriptVersion = new ScriptDTO();
        scriptVersion.setScriptVersionId(scriptVersionId);
        scriptVersion.setContent("new_content");
        scriptVersion.setVersionDesc("new_version_desc");

        scriptDAO.updateScriptVersion(operator, scriptVersionId, scriptVersion);

        ScriptDTO updatedScript = scriptDAO.getScriptVersionById(scriptVersionId);

        assertThat(updatedScript.getContent()).isEqualTo("new_content");
        assertThat(updatedScript.getVersionDesc()).isEqualTo("new_version_desc");
        assertThat(updatedScript.getLastModifyUser()).isEqualTo(operator);
    }

    @Test
    @DisplayName("验证根据关键字查询业务下的脚本名称")
    void whenListScriptNames() {
        String keyword = "test";
        Long appId = 2L;

        List<String> scriptNames = scriptDAO.listScriptNames(appId, keyword);

        assertThat(scriptNames).containsSequence("test1", "test2", "test3");
    }

    @Test
    @DisplayName("测试根据scriptId查询已上线脚本版本")
    public void testGetOnlineScriptVersionByScriptId() {
        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
        long appId = 2L;
        ScriptDTO onlineScriptVersion = scriptDAO.getOnlineScriptVersionByScriptId(appId, scriptId);

        assertThat(onlineScriptVersion.getId()).isEqualTo("dc65a20cd91811e993a2309c2357fc12");
        assertThat(onlineScriptVersion.getScriptVersionId()).isEqualTo(2L);
        assertThat(onlineScriptVersion.getStatus()).isEqualTo(JobResourceStatusEnum.ONLINE.getValue());
    }

    @Test
    @DisplayName("测试分页查询脚本版本列表")
    void whenListPageScriptVersionThenReturnOrderedPageResult() {
        ScriptQuery scriptCondition = new ScriptQuery();
        scriptCondition.setAppId(2L);
        scriptCondition.setStatus(JobResourceStatusEnum.ONLINE.getValue());

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(Integer.MAX_VALUE);

        PageData<ScriptDTO> pageData = scriptDAO.listPageScriptVersion(scriptCondition, baseSearchCondition);
        assertThat(pageData).isNotNull();
        assertThat(pageData.getPageSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(pageData.getStart()).isEqualTo(0);
        assertThat(pageData.getTotal()).isEqualTo(3);

        List<ScriptDTO> resultScriptList = pageData.getData();
        assertThat(resultScriptList).isNotEmpty();
        assertThat(resultScriptList.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("测试批量根据脚本版本ID查询脚本列表")
    void givenScriptVersionIdsThenReturnScripts() {
        List<Long> scriptVersionIds = new ArrayList<>();
        scriptVersionIds.add(1L);
        scriptVersionIds.add(3L);
        List<ScriptDTO> scripts = scriptDAO.batchGetScriptVersionsByIds(scriptVersionIds);

        assertThat(scripts).hasSize(2);
        assertThat(scripts).extracting("scriptVersionId").contains(1L, 3L);
    }

    @Test
    @DisplayName("当存在业务脚本，返回true")
    void whenAppScriptExistThenReturnTrue() {
        boolean existAnyAppScript = scriptDAO.isExistAnyScript(2L);
        assertThat(existAnyAppScript).isEqualTo(true);
    }

    @Test
    @DisplayName("当存在公共脚本，返回true")
    void whenAppPublicExistThenReturnTrue() {
        boolean existAnyPublicScript = scriptDAO.isExistAnyPublicScript();
        assertThat(existAnyPublicScript).isEqualTo(true);
    }

    @Test
    void testListPageOnlineScript() {
        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(2L);
        scriptQuery.setPublicScript(false);
        scriptQuery.setType(ScriptTypeEnum.SHELL.getValue());
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(100);
        PageData<ScriptDTO> pageData = scriptDAO.listPageOnlineScript(scriptQuery, baseSearchCondition);

        assertThat(pageData.getTotal()).isEqualTo(2);
        assertThat(pageData.getStart()).isEqualTo(0);
        assertThat(pageData.getPageSize()).isEqualTo(100);
        assertThat(pageData.getData()).hasSize(2);

    }

    private static class TagListComparator implements Comparator<List<? extends TagDTO>> {
        @Override
        public int compare(List<? extends TagDTO> tagList1, List<? extends TagDTO> tagList2) {
            if (tagList1 == null && tagList2 == null) {
                return 0;
            }
            if (tagList1 != null && tagList2 != null) {
                if (tagList1.size() != tagList2.size()) {
                    return -1;
                }
                boolean isEqual = true;
                for (int i = 0; i < tagList1.size(); i++) {
                    TagDTO tagInList1 = tagList1.get(i);
                    TagDTO tagInList2 = tagList2.get(i);
                    if (!tagInList1.getId().equals(tagInList2.getId())) {
                        isEqual = false;
                        break;
                    }
                }
                return isEqual ? 0 : -1;
            }
            return -1;
        }
    }


}
