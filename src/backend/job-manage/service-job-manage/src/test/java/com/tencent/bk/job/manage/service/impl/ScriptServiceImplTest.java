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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.dao.*;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptQueryDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.job.manage.service.template.impl.TemplateStatusUpdateService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.junit.platform.commons.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("脚本管理服务测试")
public class ScriptServiceImplTest {
    private ScriptServiceImpl scriptService;
    private ScriptDAO scriptDAO;
    private TagService tagService;
    private ScriptRelateTaskPlanDAO scriptRelateTaskPlanDAO;
    private ScriptRelateJobTemplateDAO scriptRelateJobTemplateDAO;
    private TaskTemplateService taskTemplateService;
    private DSLContext dslContext;
    private TaskScriptStepDAO taskScriptStepDAO;
    private TaskTemplateDAO taskTemplateDAO;
    private TemplateStatusUpdateService templateStatusUpdateService;
    private ScriptCitedTaskTemplateDAO scriptCitedTaskTemplateDAO;
    private AuthService authService;
    private MessageI18nService i18nService;

    @BeforeEach
    public void init() {
        dslContext = mock(DSLContext.class);
        scriptDAO = mock(ScriptDAO.class);
        tagService = mock(TagService.class);
        scriptRelateTaskPlanDAO = mock(ScriptRelateTaskPlanDAO.class);
        scriptRelateJobTemplateDAO = mock(ScriptRelateJobTemplateDAO.class);
        taskTemplateService = mock(TaskTemplateService.class);
        taskScriptStepDAO = mock(TaskScriptStepDAO.class);
        taskTemplateDAO = mock(TaskTemplateDAO.class);
        templateStatusUpdateService = mock(TemplateStatusUpdateService.class);
        authService = mock(AuthService.class);
        scriptCitedTaskTemplateDAO = mock(ScriptCitedTaskTemplateDAO.class);
        authService = mock(AuthService.class);
        i18nService = mock(MessageI18nService.class);

        scriptService = new ScriptServiceImpl(dslContext, scriptDAO, tagService, scriptRelateTaskPlanDAO,
            scriptCitedTaskTemplateDAO, taskTemplateService, scriptRelateJobTemplateDAO, taskScriptStepDAO,
            taskTemplateDAO, templateStatusUpdateService, authService, i18nService);
        scriptService.setTaskTemplateService(taskTemplateService);
    }

    @AfterEach
    public void destroy() {
        scriptService = null;
        scriptDAO = null;
    }

    @Test
    @DisplayName("根据脚本版本id获取脚本版本，调用DAO并返回查询结果")
    public void whenGetScriptVersionByIdThenInvokeDAOAndReturn() {

        ScriptDTO expectedScriptForDAO = buildScriptVersionFromDAO();
        when(scriptDAO.getScriptVersionById(1L)).thenReturn(expectedScriptForDAO);

        TagDTO tag1 = new TagDTO();
        tag1.setId(1L);
        tag1.setAppId(2L);
        tag1.setName("tag1");
        TagDTO tag2 = new TagDTO();
        tag2.setId(2L);
        tag2.setAppId(2L);
        tag2.setName("tag2");
        when(tagService.getTagInfoById(2L, 1L)).thenReturn(tag1);
        when(tagService.getTagInfoById(2L, 2L)).thenReturn(tag2);

        ScriptDTO actualScript = scriptService.getScriptVersion("admin", 2L, 1L);

        verify(scriptDAO).getScriptVersionById(1L);
        assertThat(actualScript.getTags()).extracting("name").allMatch((Predicate<Object>) s -> {
            String str = (String) s;
            return StringUtils.isNotBlank(str);
        });
        assertThat(actualScript.getTags()).extracting("name").containsSequence("tag1", "tag2");
    }

    private ScriptDTO buildScriptVersionFromDAO() {
        ScriptDTO scriptVersion = new ScriptDTO();
        scriptVersion.setId("dc65a20cd91811e993a2309c2357fc12");
        scriptVersion.setScriptVersionId(1L);
        scriptVersion.setAppId(2L);
        scriptVersion.setCategory(1);
        scriptVersion.setType(1);
        scriptVersion.setName("test");
        scriptVersion.setCreator("admin");
        scriptVersion.setContent("IyEvYmluL2Jhc2gKZWNobyAnMTIzJwo=");
        scriptVersion.setPublicScript(false);
        scriptVersion.setStatus(JobResourceStatusEnum.ONLINE.getValue());
        scriptVersion.setVersionDesc("desc1");
        scriptVersion.setVersion("admin.20190927101000");
        scriptVersion.setCreateTime(1569574800000L);
        scriptVersion.setLastModifyTime(1569574800000L);
        List<TagDTO> tags = new ArrayList<>();
        TagDTO tag1 = new TagDTO();
        tag1.setId(1L);
        tags.add(tag1);
        TagDTO tag2 = new TagDTO();
        tag2.setId(2L);
        tags.add(tag2);
        scriptVersion.setTags(tags);
        return scriptVersion;
    }

    @Test
    public void whenGetScriptByScriptIdThenInvokeDAO() {
        String scriptId = "dc65a20cd91811e993a2309c2357fc12";

        ScriptDTO script = buildScriptVersionFromDAO();
        when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);

        scriptService.getScript("admin", 2L, scriptId);
        verify(scriptDAO).getScriptByScriptId("dc65a20cd91811e993a2309c2357fc12");
    }

    @Test
    @DisplayName("获取版本列表，调用DAO方法并返回结果")
    public void whenListPageScriptThenInvokeDAOAndReturn() {
        Long appId = 2L;
        ScriptQueryDTO mockScriptCondition = mock(ScriptQueryDTO.class);
        when(mockScriptCondition.getAppId()).thenReturn(appId);
        BaseSearchCondition mockBaseSearchCondition = mock(BaseSearchCondition.class);

        ScriptDTO returnScriptByDAO = buildScriptVersionFromDAO();
        List<ScriptDTO> returnScripts = new ArrayList<>();
        returnScripts.add(returnScriptByDAO);
        PageData<ScriptDTO> returnPageDataByDAO = new PageData<>();
        returnPageDataByDAO.setData(returnScripts);
        returnPageDataByDAO.setStart(0);
        returnPageDataByDAO.setPageSize(10);
        returnPageDataByDAO.setTotal(1L);
        when(scriptDAO.listPageScript(mockScriptCondition, mockBaseSearchCondition)).thenReturn(returnPageDataByDAO);

        List<TagDTO> returnTags = new ArrayList();
        TagDTO tag1 = new TagDTO();
        tag1.setId(1L);
        tag1.setName("tag1");
        returnTags.add(tag1);
        TagDTO tag2 = new TagDTO();
        tag2.setId(2L);
        tag2.setName("tag2");
        returnTags.add(tag2);
        TagDTO tag3 = new TagDTO();
        tag3.setId(3L);
        tag3.setName("tag3");
        returnTags.add(tag3);
        when(tagService.listTagsByAppId(appId)).thenReturn(returnTags);

        PageData<ScriptDTO> pageData = scriptService.listPageScript(mockScriptCondition, mockBaseSearchCondition);

        verify(scriptDAO).listPageScript(mockScriptCondition, mockBaseSearchCondition);
        verify(tagService).listTagsByAppId(appId);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getTags()).as("返回结果包含标签名称").hasSize(2).extracting("name").contains("tag1"
            , "tag2");
    }

    @Test
    @DisplayName("获取脚本版本，调用DAO并返回")
    public void whenListScriptVersionThenInvokeDAOAndReturn() {
        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
        List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
        scriptsReturnByDAO.add(buildScriptVersionFromDAO());
        when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

        List<TagDTO> returnTags = new ArrayList();
        TagDTO tag1 = new TagDTO();
        tag1.setId(1L);
        tag1.setName("tag1");
        returnTags.add(tag1);
        TagDTO tag2 = new TagDTO();
        tag2.setId(2L);
        tag2.setName("tag2");
        returnTags.add(tag2);
        TagDTO tag3 = new TagDTO();
        tag3.setId(3L);
        tag3.setName("tag3");
        returnTags.add(tag3);
        when(tagService.listTagsByAppId(anyLong())).thenReturn(returnTags);

        List<ScriptDTO> scripts = scriptService.listScriptVersion("admin", 2L, "dc65a20cd91811e993a2309c2357fc12");

        verify(scriptDAO, times(1)).listByScriptId("dc65a20cd91811e993a2309c2357fc12");
        assertThat(scripts).hasSize(1);
        assertThat(scripts.get(0).getTags()).hasSize(2).extracting("name").containsSequence("tag1", "tag2");
    }

    @Test
    public void whenDeleteScriptThenBothScriptAndVersionAreDeleted() {
        ScriptDTO toBeDeletedScript = new ScriptDTO();
        toBeDeletedScript.setId("dc65a20cd91811e993a2309c2357fc12");
        toBeDeletedScript.setAppId(2L);
        when(scriptDAO.getScriptByScriptId(anyString())).thenReturn(toBeDeletedScript);

        scriptService.deleteScript("admin", 2L, "dc65a20cd91811e993a2309c2357fc12");

        verify(scriptDAO, times(1)).deleteScript("dc65a20cd91811e993a2309c2357fc12");
        verify(scriptDAO, times(1)).deleteScriptVersionByScriptId("dc65a20cd91811e993a2309c2357fc12");
    }

    @Test
    public void whenScriptIsNotExistThenThrowsException() {
        when(scriptDAO.getScriptByScriptId(anyString())).thenReturn(null);
        String scriptId = "dc65a20cd91811e993a2309c2357fc12";

        assertThatExceptionOfType(ServiceException.class).isThrownBy(() -> scriptService.deleteScript("admin", 2L,
            scriptId));
    }

    @Test
    public void whenScriptIsNotInAppWhenDeleteScriptThenThrowsException() {
        ScriptDTO scriptInAnotherApp = new ScriptDTO();
        scriptInAnotherApp.setAppId(1L);
        when(scriptDAO.getScriptByScriptId(anyString())).thenReturn(scriptInAnotherApp);
        String scriptId = "dc65a20cd91811e993a2309c2357fc12";

        assertThatExceptionOfType(ServiceException.class).isThrownBy(() -> scriptService.deleteScript("admin", 2L,
            scriptId));
    }

    @Test
    @DisplayName("批量获取线上脚本，调用DAO方法")
    public void whenBatchGetOnlineScriptVersionByScriptIdsThenInvokeDAO() {
        scriptService.batchGetOnlineScriptVersionByScriptIds(anyList());

        verify(scriptDAO).batchGetOnlineByScriptIds(anyList());
    }

    @Test
    @DisplayName("验证模糊搜索脚本名称")
    public void whenListScriptNamesThenInvokeDAO() {
        Long appId = 2L;
        String keyword = "test";
        scriptService.listScriptNames(appId, keyword);

        verify(scriptDAO).listScriptNames(appId, keyword);
    }

    @Nested
    @DisplayName("脚本保存测试")
    class SaveScriptTest {
        @Test
        @DisplayName("保存脚本，如果scriptId为空，需要创建脚本信息和版本信息")
        public void givenNewScriptWhenSaveScriptThenAddBothScriptAndVersion() {
            ScriptDTO newScript = new ScriptDTO();
            newScript.setAppId(2L);

            scriptService.saveScript("admin", 2L, newScript);
            verify(scriptDAO).saveScript(newScript);
            verify(scriptDAO).saveScriptVersion(newScript);
        }

        @Test
        @DisplayName("保存脚本，如果scriptId不为空，scriptVersionId为空，需要新增脚本版本并且更新脚本信息")
        public void givenNewScriptVersionWhenSaveScriptThenAddScriptVersionAndUpdateScript() {
            ScriptDTO newScriptVersion = new ScriptDTO();
            String scriptId = JobUUID.getUUID();
            newScriptVersion.setId(scriptId);
            newScriptVersion.setAppId(2L);

            scriptService.saveScript("admin", 2L, newScriptVersion);

            verify(scriptDAO).updateScript(newScriptVersion);
            verify(scriptDAO).saveScriptVersion(newScriptVersion);
        }

        @Test
        @DisplayName("新增脚本，如果脚本名称重复，抛出异常")
        public void givenDuplicateNameWhenSaveScriptThenThrowsException() {
            when(scriptDAO.isExistDuplicateName(anyLong(), anyString())).thenReturn(true);

            ScriptDTO newScript = new ScriptDTO();
            newScript.setAppId(2L);
            newScript.setName("test1");

            assertThatExceptionOfType(ServiceException.class).isThrownBy(() -> scriptService.saveScript("admin", 2L,
                newScript));
        }

        @Test
        @DisplayName("更新脚本版本，如果脚本版本的状态不是未上线，抛出异常")
        public void givenScriptVersionStatusIsNotDraftWhenSaveScriptThenThrowsException() {
            Long scriptVersionId = 1L;

            ScriptDTO scriptVersionToBeUpdate = new ScriptDTO();
            scriptVersionToBeUpdate.setStatus(JobResourceStatusEnum.OFFLINE.getValue());
            when(scriptDAO.getScriptVersionById(scriptVersionId)).thenReturn(scriptVersionToBeUpdate);

            ScriptDTO updateScriptVersion = new ScriptDTO();
            updateScriptVersion.setId("dc65a20cd91811e993a2309c2357fc12");
            updateScriptVersion.setScriptVersionId(1L);
            updateScriptVersion.setAppId(2L);
            updateScriptVersion.setName("test1");

            List<TagDTO> tags = new ArrayList<>();
            TagDTO tag1 = new TagDTO();
            tag1.setId(1L);
            tags.add(tag1);
            TagDTO tag2 = new TagDTO();
            tag1.setId(2L);
            tags.add(tag2);
            TagDTO tag3 = new TagDTO();
            tag3.setId(3L);
            tags.add(tag3);
            updateScriptVersion.setTags(tags);
            updateScriptVersion.setVersionDesc("new_desc");

            assertThatExceptionOfType(ServiceException.class).isThrownBy(() -> scriptService.saveScript("admin", 2L,
                updateScriptVersion));
        }

        @Test
        @DisplayName("更新脚本版本，如果脚本版本的状态是未上线，则更新")
        public void givenScriptVersionStatusIsDraftWhenSaveScriptThenUpdate() {
            Long scriptVersionId = 1L;
            String operator = "user1";

            ScriptDTO scriptVersionToBeUpdate = new ScriptDTO();
            scriptVersionToBeUpdate.setStatus(JobResourceStatusEnum.DRAFT.getValue());
            when(scriptDAO.getScriptVersionById(scriptVersionId)).thenReturn(scriptVersionToBeUpdate);

            ScriptDTO updateScriptVersion = new ScriptDTO();
            updateScriptVersion.setId("dc65a20cd91811e993a2309c2357fc12");
            updateScriptVersion.setScriptVersionId(1L);
            updateScriptVersion.setAppId(2L);
            updateScriptVersion.setName("test1");
            updateScriptVersion.setLastModifyUser(operator);

            List<TagDTO> tags = new ArrayList<>();
            TagDTO tag1 = new TagDTO();
            tag1.setId(1L);
            tags.add(tag1);
            TagDTO tag2 = new TagDTO();
            tag1.setId(2L);
            tags.add(tag2);
            TagDTO tag3 = new TagDTO();
            tag3.setId(3L);
            tags.add(tag3);
            updateScriptVersion.setTags(tags);
            updateScriptVersion.setVersionDesc("new_desc");

            scriptService.saveScript("admin", 2L, updateScriptVersion);

            verify(scriptDAO).updateScriptVersion(operator, scriptVersionId, updateScriptVersion);
        }


    }

    @Nested
    @DisplayName("删除脚本版本测试")
    class DeleteScriptVersionTest {
        @Test
        @DisplayName("删除脚本版本：脚本不在该业务下，抛出异常")
        public void givenScriptVersionNotInAppWhenDeleteScriptVersionThenThrowsException() {
            ScriptDTO scriptInAnotherApp = new ScriptDTO();
            scriptInAnotherApp.setAppId(1L);
            when(scriptDAO.getScriptVersionById(anyLong())).thenReturn(scriptInAnotherApp);

            assertThatExceptionOfType(ServiceException.class).isThrownBy(
                () -> scriptService.deleteScriptVersion("admin", 2L, 1L));
        }

        @Test
        @DisplayName("当脚本状态为已上线或者已下线时，不支持删除脚本")
        public void givenOnlineOrOfflineScriptWhenDeleteScriptVersionThenThrowException() {
            Long scriptVersionId = 1L;
            ScriptDTO scriptVersion = new ScriptDTO();
            scriptVersion.setScriptVersionId(scriptVersionId);
            scriptVersion.setStatus(JobResourceStatusEnum.ONLINE.getValue());
            scriptVersion.setAppId(2L);
            when(scriptDAO.getScriptVersionById(scriptVersionId)).thenReturn(scriptVersion);

            assertThatExceptionOfType(ServiceException.class).isThrownBy(
                () -> scriptService.deleteScriptVersion("admin", 2L, scriptVersionId));


            reset(scriptDAO);
            scriptVersion.setStatus(JobResourceStatusEnum.ONLINE.getValue());
            when(scriptDAO.getScriptVersionById(scriptVersionId)).thenReturn(scriptVersion);

            assertThatExceptionOfType(ServiceException.class).isThrownBy(
                () -> scriptService.deleteScriptVersion("admin", 2L, scriptVersionId));

        }

        @Test
        @DisplayName("当脚本多于一个版本，则只删除当前版本")
        public void whenScriptVersionIsMoreThanOnehenDeleteOnlyVersion() {
            List<ScriptDTO> scriptVersions = new ArrayList();
            ScriptDTO script1 = new ScriptDTO();
            script1.setId("dc65a20cd91811e993a2309c2357fc12");
            script1.setAppId(2L);
            script1.setScriptVersionId(1L);
            script1.setStatus(JobResourceStatusEnum.DRAFT.getValue());
            scriptVersions.add(script1);
            ScriptDTO script2 = new ScriptDTO();
            script1.setId("dc65a20cd91811e993a2309c2357fc12");
            script1.setAppId(2L);
            script1.setScriptVersionId(2L);
            scriptVersions.add(script2);
            when(scriptDAO.getScriptVersionById(1L)).thenReturn(script1);
            when(scriptDAO.listByScriptId("dc65a20cd91811e993a2309c2357fc12")).thenReturn(scriptVersions);

            scriptService.deleteScriptVersion("admin", 2L, 1L);

            verify(scriptDAO, never()).deleteScript("dc65a20cd91811e993a2309c2357fc12");
            verify(scriptDAO, times(1)).deleteScriptVersion(1L);
        }

        @Test
        @DisplayName("当脚本仅有一个版本，则删除脚本和脚本版本")
        public void whenScriptVersionIsOnlyThenDeleteBoth() {
            List<ScriptDTO> onlyScriptList = new ArrayList();
            ScriptDTO script = new ScriptDTO();
            script.setId("dc65a20cd91811e993a2309c2357fc12");
            script.setAppId(2L);
            script.setScriptVersionId(1L);
            script.setStatus(JobResourceStatusEnum.DRAFT.getValue());
            onlyScriptList.add(script);
            when(scriptDAO.getScriptVersionById(1L)).thenReturn(script);
            when(scriptDAO.listByScriptId("dc65a20cd91811e993a2309c2357fc12")).thenReturn(onlyScriptList);

            scriptService.deleteScriptVersion("admin", 2L, 1L);

            verify(scriptDAO, times(1)).deleteScript("dc65a20cd91811e993a2309c2357fc12");
            verify(scriptDAO, times(1)).deleteScriptVersion(1L);
        }

    }

    @Nested
    @DisplayName("测试上线脚本")
    class PublishScriptTest {
        @Test
        @DisplayName("上线脚本，脚本不存在，抛出异常")
        public void givenScriptNotExistWhenPublishScriptVersionThenThrowException() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long scriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(null);
            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.publishScript(appId, username, scriptId, scriptVersionId);
                });
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(Collections.emptyList());
            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.publishScript(appId, username, scriptId, scriptVersionId);
                });

        }

        @Test
        @DisplayName("上线脚本，脚本不在当前业务下，抛出异常")
        public void givenScriptNotInAppWhenPublishScriptVersionThenThrowException() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long scriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";

            List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
            ScriptDTO script = new ScriptDTO();
            Long scriptAppId = 1L;
            script.setId(scriptId);
            script.setAppId(scriptAppId);
            scriptsReturnByDAO.add(script);
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.publishScript(appId, username, scriptId, scriptVersionId);
                });

        }

        @Test
        @DisplayName("上线脚本，该脚本版本不存在，抛出异常")
        public void givenScriptVersionNotExistWhenPublishScriptVersionThenThrowException() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long scriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";

            List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
            ScriptDTO script = new ScriptDTO();
            Long scriptVersionIdInCurrentScript = 2L;
            script.setId(scriptId);
            script.setAppId(appId);
            script.setScriptVersionId(scriptVersionIdInCurrentScript);
            scriptsReturnByDAO.add(script);
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.publishScript(appId, username, scriptId, scriptVersionId);
                });
        }

        @Test
        @DisplayName("上线脚本：当前脚本状态非“未上线”，无法上线，抛出异常")
        public void givenNotUnpublishStatusScriptWhenPublishScriptThenThrowException() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long scriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";

            List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
            ScriptDTO script = new ScriptDTO();
            script.setId(scriptId);
            script.setAppId(appId);
            script.setScriptVersionId(scriptVersionId);
            script.setStatus(JobResourceStatusEnum.DISABLED.getValue());
            scriptsReturnByDAO.add(script);
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.publishScript(appId, username, scriptId, scriptVersionId);
                });
        }

        @Test
        @DisplayName("上线脚本：如果当前脚本存在已上线版本，首先需要下线该版本")
        public void givenPublishedScriptExistWhenPublishScriptThenUnpublishedFirst() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long publishedScriptVersionId = 1L;
            Long unpublishedScriptVersionId = 2L;
            Long appId = 2L;
            String username = "user1";

            List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
            ScriptDTO publishedScript = new ScriptDTO();
            publishedScript.setId(scriptId);
            publishedScript.setAppId(appId);
            publishedScript.setScriptVersionId(publishedScriptVersionId);
            publishedScript.setStatus(JobResourceStatusEnum.ONLINE.getValue());
            scriptsReturnByDAO.add(publishedScript);
            ScriptDTO unpublishedScript = new ScriptDTO();
            unpublishedScript.setId(scriptId);
            unpublishedScript.setAppId(appId);
            unpublishedScript.setScriptVersionId(unpublishedScriptVersionId);
            unpublishedScript.setStatus(JobResourceStatusEnum.DRAFT.getValue());
            scriptsReturnByDAO.add(unpublishedScript);

            when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

            scriptService.publishScript(appId, username, scriptId, unpublishedScriptVersionId);

            // 下线在线脚本操作被调用
            verify(scriptDAO).updateScriptVersionStatus(publishedScriptVersionId,
                JobResourceStatusEnum.OFFLINE.getValue());

            //上线脚本
            verify(scriptDAO).updateScriptVersionStatus(unpublishedScriptVersionId,
                JobResourceStatusEnum.ONLINE.getValue());
        }
    }

    @Nested
    @DisplayName("测试禁用脚本")
    class DisableScriptTest {
        @Test
        @DisplayName("禁用脚本，脚本不存在，抛出异常")
        public void givenScriptNotExistWhenDisableScriptVersionThenThrowException() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long scriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(null);
            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.disableScript(appId, username, scriptId, scriptVersionId);
                });
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(Collections.EMPTY_LIST);
            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.disableScript(appId, username, scriptId, scriptVersionId);
                });
        }

        @Test
        @DisplayName("禁用脚本，脚本不在当前业务下，抛出异常")
        public void givenScriptNotInAppWhenDisableScriptVersionThenThrowException() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long scriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";

            List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
            ScriptDTO script = new ScriptDTO();
            Long scriptAppId = 1L;
            script.setId(scriptId);
            script.setAppId(scriptAppId);
            scriptsReturnByDAO.add(script);
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.disableScript(appId, username, scriptId, scriptVersionId);
                });

        }

        @Test
        @DisplayName("禁用脚本，该脚本版本不存在，抛出异常")
        public void givenScriptVersionNotExistWhenDisableScriptVersionThenThrowException() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long scriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";

            List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
            ScriptDTO script = new ScriptDTO();
            Long scriptVersionIdInCurrentScript = 2L;
            script.setId(scriptId);
            script.setAppId(appId);
            script.setScriptVersionId(scriptVersionIdInCurrentScript);
            scriptsReturnByDAO.add(script);
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.disableScript(appId, username, scriptId, scriptVersionId);
                });
        }

        @Test
        @DisplayName("禁用脚本：当前脚本状态非“已上线”，无法禁用，抛出异常")
        public void givenNotUnpublishStatusScriptWhenDisableScriptThenThrowException() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long scriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";

            List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
            ScriptDTO script = new ScriptDTO();
            script.setId(scriptId);
            script.setAppId(appId);
            script.setScriptVersionId(scriptVersionId);
            script.setStatus(JobResourceStatusEnum.DRAFT.getValue());
            scriptsReturnByDAO.add(script);
            when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.disableScript(appId, username, scriptId, scriptVersionId);
                });
        }

        @Test
        @DisplayName("禁用脚本，该脚本若为已上线状态，成功")
        public void givenPublishedScriptExistWhenDisableScriptThenDisableIt() {
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            Long disableScriptVersionId = 1L;
            Long appId = 2L;
            String username = "user1";

            List<ScriptDTO> scriptsReturnByDAO = new ArrayList<>();
            ScriptDTO publishedScript = new ScriptDTO();
            publishedScript.setId(scriptId);
            publishedScript.setAppId(appId);
            publishedScript.setScriptVersionId(disableScriptVersionId);
            publishedScript.setStatus(JobResourceStatusEnum.ONLINE.getValue());
            scriptsReturnByDAO.add(publishedScript);

            when(scriptDAO.listByScriptId(scriptId)).thenReturn(scriptsReturnByDAO);

            scriptService.disableScript(appId, username, scriptId, disableScriptVersionId);

            // 禁用脚本操作被调用
            verify(scriptDAO).updateScriptVersionStatus(disableScriptVersionId,
                JobResourceStatusEnum.DISABLED.getValue());
        }
    }

    @Nested
    @DisplayName("更新脚本描述、标签、名称测试")
    class UpdateScriptInfoTest {
        @Test
        @DisplayName("更新脚本描述，脚本不存在，抛出异常")
        public void givenNotExistScriptWhenUpdateScriptDescThenThrowException() {
            Long appId = 2L;
            String username = "user1";
            String scriptId = "notexistscriptid";
            String desc = "desc1";
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(null);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.updateScriptDesc(appId, username, scriptId, desc);
                });
        }

        @Test
        @DisplayName("更新脚本描述，脚本不在当前业务下，抛出异常")
        public void givenScriptNotInAppWhenUpdateScriptDescThenThrowException() {
            Long appId = 2L;
            Long anotherAppId = 1L;
            String username = "user1";
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            String desc = "desc1";
            ScriptDTO script = new ScriptDTO();
            script.setAppId(anotherAppId);
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.updateScriptDesc(appId, username, scriptId, desc);
                });
        }

        @Test
        @DisplayName("更新脚本描述，调用DAO成功更新")
        public void whenUpdateScriptDescThenInvokeDAO() {
            Long appId = 2L;
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            String username = "user1";
            String desc = "desc1";
            ScriptDTO script = new ScriptDTO();
            script.setAppId(appId);
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);

            scriptService.updateScriptDesc(appId, username, scriptId, desc);

            verify(scriptDAO).updateScriptDesc(username, scriptId, desc);
        }

        @Test
        @DisplayName("更新脚本名称，脚本不存在，抛出异常")
        public void givenNotExistScriptWhenUpdateScriptNameThenThrowException() {
            Long appId = 2L;
            String username = "user1";
            String scriptId = "notexistscriptid";
            String scriptName = "newName";
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(null);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.updateScriptName(appId, username, scriptId, scriptName);
                });
        }

        @Test
        @DisplayName("更新脚本名称，脚本不在当前业务下，抛出异常")
        public void givenScriptNotInAppWhenUpdateScriptNameThenThrowException() {
            Long appId = 2L;
            Long anotherAppId = 1L;
            String username = "user1";
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            String scriptName = "newName";
            ScriptDTO script = new ScriptDTO();
            script.setAppId(anotherAppId);
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.updateScriptName(appId, username, scriptId, scriptName);
                });
        }

        @Test
        @DisplayName("更新脚本名称，脚本名称已存在，抛出异常")
        public void givenDuplicateNameWhenUpdateScriptNameThenThrowException() {
            Long appId = 2L;
            String username = "user1";
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            String newScriptName = "newName";
            ScriptDTO script = new ScriptDTO();
            script.setAppId(appId);
            script.setName("oldName");
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);
            when(scriptDAO.isExistDuplicateName(appId, newScriptName)).thenReturn(true);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.updateScriptName(appId, username, scriptId, newScriptName);
                });
        }

        @Test
        @DisplayName("更新脚本名称，脚本名称已存在，抛出异常")
        public void givenSameNameWhenUpdateScriptNameThenDoNothing() {
            Long appId = 2L;
            String username = "user1";
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            String newScriptName = "oldName";
            ScriptDTO script = new ScriptDTO();
            script.setAppId(appId);
            script.setName("oldName");
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);
            when(scriptDAO.isExistDuplicateName(appId, newScriptName)).thenReturn(true);

            scriptService.updateScriptName(appId, username, scriptId, newScriptName);

            verify(scriptDAO, never()).updateScriptName(username, scriptId, newScriptName);
        }

        @Test
        @DisplayName("更新脚本名称,调用DAO成功更新")
        public void whenUpdateScriptNameThenInvokeDAO() {
            Long appId = 2L;
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            String username = "user1";
            String newName = "newName";
            ScriptDTO script = new ScriptDTO();
            script.setAppId(appId);
            script.setName("oldName");
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);
            when(scriptDAO.isExistDuplicateName(appId, newName)).thenReturn(false);

            scriptService.updateScriptName(appId, username, scriptId, newName);

            verify(scriptDAO).updateScriptName(username, scriptId, newName);
        }

        @Test
        @DisplayName("更新脚本标签，脚本不存在，抛出异常")
        public void givenNotExistScriptWhenUpdateScriptTagsThenThrowException() {
            Long appId = 2L;
            String username = "user1";
            String scriptId = "notexistscriptid";
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(null);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.updateScriptTags(appId, username, scriptId, anyList());
                });
        }

        @Test
        @DisplayName("更新脚本标签，脚本不在当前业务下，抛出异常")
        public void givenScriptNotInAppWhenUpdateScriptTagsThenThrowException() {
            Long appId = 2L;
            Long anotherAppId = 1L;
            String username = "user1";
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            ScriptDTO script = new ScriptDTO();
            script.setAppId(anotherAppId);
            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);

            assertThatExceptionOfType(ServiceException.class)
                .isThrownBy(() -> {
                    scriptService.updateScriptTags(appId, username, scriptId, anyList());
                });
        }

        @Test
        @DisplayName("更新脚本标签，标签不存在，则新增标签")
        public void givenTagNotExistWhenUpdateScriptTagsThenCreate() {
            Long appId = 2L;
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            String username = "user1";
            List<TagDTO> tags = new ArrayList<>();
            TagDTO tag1 = new TagDTO();
            tag1.setId(1L);
            tag1.setName("tag1");
            tags.add(tag1);
            TagDTO tag2 = new TagDTO();
            tag2.setName("tag2");
            tags.add(tag2);
            ScriptDTO script = new ScriptDTO();
            script.setAppId(appId);

            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);

            scriptService.updateScriptTags(appId, username, scriptId, tags);
            // 调用tag创建服务
            verify(tagService).createNewTagIfNotExist(tags, appId, username);
        }

        @Test
        @DisplayName("更新脚本标签，调用DAO成功更新")
        public void whenUpdateScriptTagsThenInvokeDAO() {
            Long appId = 2L;
            String scriptId = "dc65a20cd91811e993a2309c2357fc12";
            String username = "user1";
            List<TagDTO> tags = new ArrayList<>();
            TagDTO tag1 = new TagDTO();
            tag1.setId(1L);
            tags.add(tag1);
            TagDTO tag2 = new TagDTO();
            tag1.setId(2L);
            tags.add(tag2);
            ScriptDTO script = new ScriptDTO();
            script.setAppId(appId);

            when(scriptDAO.getScriptByScriptId(scriptId)).thenReturn(script);

            scriptService.updateScriptTags(appId, username, scriptId, tags);

            verify(scriptDAO).updateScriptTags(username, scriptId, tags);
        }

    }


}
