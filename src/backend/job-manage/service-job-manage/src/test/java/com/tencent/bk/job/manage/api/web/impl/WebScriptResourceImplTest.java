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

package com.tencent.bk.job.manage.api.web.impl;

//@DisplayName("脚本管理API测试")
public class WebScriptResourceImplTest {
//    private WebScriptResourceImpl scriptResourceImpl;
//    private ScriptService scriptService;
//    private MessageI18nService i18nService;
//    private ScriptCheckService scriptCheckService;
//    private ScriptDTOBuilder scriptDTOBuilder;
//
//    @BeforeEach
//    public void init() {
//        scriptService = mock(ScriptService.class);
//        i18nService = mock(MessageI18nService.class);
//        scriptCheckService = mock(ScriptCheckService.class);
//        scriptDTOBuilder = new ScriptDTOBuilder();
//        scriptResourceImpl = new WebScriptResourceImpl(scriptService, i18nService, scriptCheckService, 
//       scriptDTOBuilder);
//    }
//
//    @AfterEach
//    public void destroy() {
//        scriptService = null;
//        i18nService = null;
//        scriptResourceImpl = null;
//    }
//
//    @Test
//    @DisplayName("获取脚本版本详情，成功响应")
//    public void whenGetScriptVersionDetailThenReturnSuccResp() {
//        when(scriptService.getScriptVersion(anyString(), anyLong(), eq(1L))).thenReturn(buildScriptDTO());
//        when(i18nService.getI18n(any())).thenReturn("");
//
//        ServiceResponse<ScriptVO> resp = scriptResourceImpl.getScriptVersionDetail("admin", 2L, 1L);
//
//        assertThat(resp.isSuccess()).isEqualTo(true);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.RESULT_OK);
//        assertThat(resp.getData()).isNotNull();
//
//        ScriptVO actualScriptVO = resp.getData();
//        ScriptVO expectedScriptVO = buildScriptVO();
//        assertThat(actualScriptVO).isNotNull();
//        String[] compareFields = {"id", "scriptVersionId", "category", "type", "typeName", "name", "creator", 
//       "content", "publicScript", "status",
//            "description", "versionDesc", "version", "createTime", "lastModifyTime", "tags"};
//        assertThat(actualScriptVO).isEqualToComparingOnlyGivenFields(expectedScriptVO, compareFields);
//    }
//
//    @Test
//    @DisplayName("获取脚本版本详情，appId或者scriptId为空，返回错误响应")
//    public void givenEmptyAppIdOrScriptIdWhenGetScriptVersionThenReturnFailResp() {
//        ServiceResponse<ScriptVO> resp = scriptResourceImpl.getScriptVersionDetail(null, null, null);
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getData()).isNull();
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.MISSING_PARAM);
//
//        ServiceResponse<ScriptVO> resp1 = scriptResourceImpl.getScriptVersionDetail(null, 2L, null);
//        assertThat(resp1.isSuccess()).isEqualTo(false);
//        assertThat(resp1.getData()).isNull();
//        assertThat(resp1.getCode()).isEqualTo(ErrorCode.MISSING_PARAM);
//    }
//
//    @Test
//    @DisplayName("获取脚本版本详情，脚本不存在，返回错误响应")
//    public void givenScriptNotExistWhenGetScriptVersionThenReturnFailResponse() {
//        when(scriptService.getScriptVersionByScriptVersionId(1L)).thenReturn(null);
//        when(i18nService.getI18n(any())).thenReturn("");
//
//        ServiceResponse<ScriptVO> resp = scriptResourceImpl.getScriptVersionDetail(null, 2L, 2L);
//        assertThat(resp).isNotNull();
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.SCRIPT_NOT_EXIST);
//    }
//
//    @Test
//    @DisplayName("获取脚本详情，脚本不存在，返回失败响应")
//    public void givenNotExistScriptwhenGetScriptThenReturnFailResp() {
//        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
//        when(scriptService.getScriptByScriptId(scriptId)).thenReturn(null);
//
//        ServiceResponse<ScriptVO> resp = scriptResourceImpl.getScript("admin", 2L, scriptId);
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.SCRIPT_NOT_EXIST);
//
//        reset(scriptService);
//        when(scriptService.getScriptByScriptId(scriptId)).thenReturn(buildScriptDTO());
//        when(scriptService.listScriptVersion(anyString(), anyLong(), eq(scriptId))).thenReturn(null);
//
//        resp = scriptResourceImpl.getScript("admin", 2L, scriptId);
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.SCRIPT_NOT_EXIST);
//    }
//
//    @Test
//    @DisplayName("获取脚本详情，脚本存在，返回成功响应")
//    public void whenGetScriptThenReturnSuccResp() {
//        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
//
//        when(scriptService.getScript(anyString(), anyLong(), eq(scriptId))).thenReturn(buildScriptDTO());
//
//        List<ScriptDTO> scriptVersions = new ArrayList<>();
//        scriptVersions.add(buildScriptDTO());
//        when(scriptService.listScriptVersion(anyString(), anyLong(), eq(scriptId))).thenReturn(scriptVersions);
//
//        ServiceResponse<ScriptVO> resp = scriptResourceImpl.getScript("admin", 2L, scriptId);
//
//        assertThat(resp.isSuccess()).isEqualTo(true);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.RESULT_OK);
//        assertThat(resp.getData().getScriptVersions()).hasSize(1);
//    }
//
//
//    @Test
//    @DisplayName("分页查询脚本列表，成功响应")
//    public void whenListScriptThenReturnSuccessResp() {
//        PageData<ScriptDTO> pageData = new PageData<>();
//        ScriptDTO expectedScript = buildScriptDTO();
//        List<ScriptDTO> scripts = new ArrayList<>();
//        scripts.add(expectedScript);
//        pageData.setData(scripts);
//        pageData.setStart(0);
//        pageData.setPageSize(10);
//        when(scriptService.listPageScript(any(ScriptQueryDTO.class), any(BaseSearchCondition.class))).thenReturn
//       (pageData);
//
//        ScriptRelatedTaskPlanDTO relatedTaskPlanDTO = new ScriptRelatedTaskPlanDTO();
//        relatedTaskPlanDTO.setTaskName("test");
//        relatedTaskPlanDTO.setTaskId(1L);
//        relatedTaskPlanDTO.setAppId(expectedScript.getAppId());
//        relatedTaskPlanDTO.setScriptVersion(expectedScript.getVersion());
//        relatedTaskPlanDTO.setScriptId(expectedScript.getId());
//        relatedTaskPlanDTO.setScriptVersionId(expectedScript.getScriptVersionId());
//        relatedTaskPlanDTO.setScriptStatus(JobResourceStatusEnum.OFFLINE);
//        List<ScriptRelatedTaskPlanDTO> relatedTaskPlanDTOS = new ArrayList<>();
//        relatedTaskPlanDTOS.add(relatedTaskPlanDTO);
//        when(scriptService.listScriptRelatedTasks(anyString())).thenReturn(relatedTaskPlanDTOS);
//
//        ServiceResponse<PageData<ScriptVO>> actualResp = scriptResourceImpl.listPageScript("user1", 2L, false, 
//       "test", 1,
//            "<2>", "user1", 0, 10, null, null);
//
//        assertThat(actualResp.isSuccess()).isEqualTo(true);
//        assertThat(actualResp.getCode()).isEqualTo(ErrorCode.RESULT_OK);
//        assertThat(actualResp.getData().getStart()).isEqualTo(0);
//        assertThat(actualResp.getData().getPageSize()).isEqualTo(10);
//        assertThat(actualResp.getData().getData()).isNotEmpty();
//
//        ScriptVO actualScript = actualResp.getData().getData().get(0);
//        ScriptVO expectedScriptVO = buildScriptVO();
//
//        String[] compareScriptFields = {"id", "appId", "category", "type", "typeName", "tags", "name", 
//       "publicScript", "creator"};
//        assertThat(actualScript).isEqualToComparingOnlyGivenFields(expectedScriptVO, compareScriptFields);
//
//        assertThat(actualScript.getRelatedTaskNum()).isEqualTo(1);
//        assertThat(actualScript.getRelatedTaskPlans()).hasSize(1);
//        ScriptRelatedTaskPlanVO actualRelatedPlanVO = actualScript.getRelatedTaskPlans().get(0);
//        String[] compareFields = {"scriptId", "scriptVersionId", "scriptVersion", "scriptName", "taskId", "appId", 
//       "taskName"};
//        assertThat(actualRelatedPlanVO).isEqualToComparingOnlyGivenFields(relatedTaskPlanDTO, compareFields);
//    }
//
//
//    @Test
//    @DisplayName("获取脚本版本列表，成功响应")
//    public void whenListScriptVersionThenReturnSuccResp() {
//        when(i18nService.getI18n(anyString())).thenReturn("statusDesc");
//
//        List<ScriptDTO> scriptVersions = new ArrayList<>();
//        ScriptDTO returnScriptDTO = buildScriptDTO();
//        scriptVersions.add(returnScriptDTO);
//        when(scriptService.listScriptVersion(anyString(), anyLong(), eq(returnScriptDTO.getId()))).thenReturn
//       (scriptVersions);
//
//        ServiceResponse<List<ScriptVO>> actualResp = scriptResourceImpl.listScriptVersion("admin", returnScriptDTO
//       .getAppId(), returnScriptDTO.getId());
//
//        assertThat(actualResp.isSuccess()).isEqualTo(true);
//        assertThat(actualResp.getData()).isNotEmpty();
//
//        ScriptVO actualScriptVersion = actualResp.getData().get(0);
//        ScriptVO expectedScriptVO = buildScriptVO();
//
//        assertThat(actualScriptVersion).isNotNull();
//        String[] compareFields = {"id", "scriptVersionId", "category", "type", "typeName", "name", "creator", 
//       "content", "publicScript", "status",
//            "description", "versionDesc", "version", "createTime", "lastModifyTime", "tags"};
//        assertThat(actualScriptVersion).isEqualToComparingOnlyGivenFields(expectedScriptVO, compareFields);
//    }
//
//    @Test
//    @DisplayName("获取版本列表：当脚本版本列表被执行方案引用时，返回执行方案列表")
//    public void whenScriptVersionIsRefWhenListScriptVersionThenReturnTaskPlanList() {
//        Long scriptVersionId = 1L;
//        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
//
//        List<ScriptDTO> scriptVersions = new ArrayList<>();
//        ScriptDTO scriptVersion = buildScriptDTO();
//        scriptVersions.add(scriptVersion);
//        when(scriptService.listScriptVersion(anyString(), anyLong(), eq(scriptVersion.getId()))).thenReturn
//       (scriptVersions);
//
//        List<ScriptRelatedTaskPlanDTO> relatedTaskPlanList = new ArrayList<>();
//        ScriptRelatedTaskPlanDTO relatedTaskPlanDTO = new ScriptRelatedTaskPlanDTO();
//        relatedTaskPlanDTO.setTaskName("test");
//        relatedTaskPlanDTO.setTaskId(1L);
//        relatedTaskPlanDTO.setAppId(scriptVersion.getAppId());
//        relatedTaskPlanDTO.setScriptVersion(scriptVersion.getVersion());
//        relatedTaskPlanDTO.setScriptId(scriptVersion.getId());
//        relatedTaskPlanDTO.setScriptVersionId(scriptVersion.getScriptVersionId());
//        relatedTaskPlanDTO.setScriptStatus(JobResourceStatusEnum.ONLINE);
//        relatedTaskPlanList.add(relatedTaskPlanDTO);
//        when(scriptService.listScriptVersionRelatedTasks(scriptId, scriptVersionId)).thenReturn(relatedTaskPlanList);
//
//        ServiceResponse<List<ScriptVO>> actualResp = scriptResourceImpl.listScriptVersion("user", 2L, 
//       "dc65a20cd91811e993a2309c2357fc12");
//        ScriptVO actualScript = actualResp.getData().get(0);
//
//        assertThat(actualScript.getRelatedTaskNum()).isEqualTo(1);
//        assertThat(actualScript.getRelatedTaskPlans()).hasSize(1);
//        ScriptRelatedTaskPlanVO actualRelatedPlanVO = actualScript.getRelatedTaskPlans().get(0);
//        String[] compareFields = {"scriptId", "scriptVersionId", "scriptVersion", "scriptName", "taskId", "appId", 
//       "taskName"};
//        assertThat(actualRelatedPlanVO).isEqualToComparingOnlyGivenFields(relatedTaskPlanDTO, compareFields);
//    }
//
//    private ScriptDTO buildScriptDTO() {
//        ScriptDTO scriptVersion = new ScriptDTO();
//        scriptVersion.setId("dc65a20cd91811e993a2309c2357fc12");
//        scriptVersion.setScriptVersionId(1L);
//        scriptVersion.setAppId(2L);
//        scriptVersion.setCategory(1);
//        scriptVersion.setType(1);
//        scriptVersion.setName("test");
//        scriptVersion.setCreator("admin");
//        scriptVersion.setContent("#!/bin/bash\necho '123'");
//        scriptVersion.setPublicScript(false);
//        scriptVersion.setStatus(JobResourceStatusEnum.ONLINE.getValue());
//        scriptVersion.setVersionDesc("desc1");
//        scriptVersion.setDescription("desc");
//        scriptVersion.setVersion("admin.20190927101000");
//        scriptVersion.setCreateTime(1569574800000L);
//        scriptVersion.setLastModifyTime(1569574800000L);
//        List<TagDTO> tags = new ArrayList<>();
//        TagDTO tag1 = new TagDTO();
//        tag1.setId(1L);
//        tag1.setName("tag1");
//        tags.add(tag1);
//        TagDTO tag2 = new TagDTO();
//        tag2.setId(2L);
//        tag2.setName("tag2");
//        tags.add(tag2);
//        scriptVersion.setTags(tags);
//        return scriptVersion;
//    }
//
//    private ScriptVO buildScriptVO() {
//        ScriptVO scriptVersion = new ScriptVO();
//        scriptVersion.setId("dc65a20cd91811e993a2309c2357fc12");
//        scriptVersion.setScriptVersionId(1L);
//        scriptVersion.setAppId(2L);
//        scriptVersion.setCategory(1);
//        scriptVersion.setType(1);
//        scriptVersion.setTypeName("shell");
//        scriptVersion.setName("test");
//        scriptVersion.setCreator("admin");
//        scriptVersion.setContent("IyEvYmluL2Jhc2gKZWNobyAnMTIzJw==");
//        scriptVersion.setPublicScript(false);
//        scriptVersion.setStatus(JobResourceStatusEnum.ONLINE.getValue());
//        scriptVersion.setVersionDesc("desc1");
//        scriptVersion.setDescription("desc");
//        scriptVersion.setVersion("admin.20190927101000");
//        scriptVersion.setCreateTime(1569574800000L);
//        scriptVersion.setLastModifyTime(1569574800000L);
//        List<TagVO> tags = new ArrayList<>();
//        TagVO tag1 = new TagVO();
//        tag1.setId(1L);
//        tag1.setName("tag1");
//        tags.add(tag1);
//        TagVO tag2 = new TagVO();
//        tag2.setId(2L);
//        tag2.setName("tag2");
//        tags.add(tag2);
//        scriptVersion.setTags(tags);
//        return scriptVersion;
//    }
//
//    private ScriptCreateUpdateReq buildScriptCreateUpdateReq() {
//        ScriptCreateUpdateReq scriptVersion = new ScriptCreateUpdateReq();
//        scriptVersion.setId("dc65a20cd91811e993a2309c2357fc12");
//        scriptVersion.setScriptVersionId(1L);
//        scriptVersion.setAppId(2L);
//        scriptVersion.setType(1);
//        scriptVersion.setName("test");
//        scriptVersion.setCreator("admin");
//        scriptVersion.setContent("IyEvYmluL2Jhc2gKZWNobyAnMTIzJwo=");
//        scriptVersion.setDescription("desc1");
//        scriptVersion.setVersion("admin.20190927101000");
//        List<TagVO> tags = new ArrayList<>();
//        TagVO tag1 = new TagVO();
//        tag1.setId(1L);
//        tags.add(tag1);
//        TagVO tag2 = new TagVO();
//        tag1.setId(2L);
//        tags.add(tag2);
//        scriptVersion.setTags(tags);
//        return scriptVersion;
//    }
//
//    @Test
//    @DisplayName("保存脚本，返回成功响应")
//    public void whenSaveNewScriptThenReturnSuccResp() {
//        ScriptCreateUpdateReq req = buildScriptCreateUpdateReq();
//        ServiceResponse actualResp = scriptResourceImpl.saveScript("admin", req.getAppId(), req);
//
//        assertThat(actualResp.isSuccess()).isEqualTo(true);
//    }
//
//    @Test
//    @DisplayName("保存脚本，服务异常，返回错误响应")
//    public void whenSaveScritpExceptionThenReturnFailResp() {
//        when(i18nService.getI18n(anyString())).thenReturn("error msg");
//        doThrow(ServiceException.class).when(scriptService).saveScript(anyString(), anyLong(), any(ScriptDTO.class));
//
//        ScriptCreateUpdateReq req = buildScriptCreateUpdateReq();
//        ServiceResponse actualResp = scriptResourceImpl.saveScript("admin", req.getAppId(), req);
//
//        assertThat(actualResp.isSuccess()).isEqualTo(false);
//        assertThat(actualResp.getErrorMsg()).isNotBlank();
//    }
//
//    @Test
//    @DisplayName("保存脚本，名称不合法，返回错误响应")
//    public void givenInvalidNameWhenSaveScriptThenReturnFailResp() {
//        when(i18nService.getI18n(anyString())).thenReturn("error msg");
//
//        ScriptCreateUpdateReq req = buildScriptCreateUpdateReq();
//        //超过100字符
//        req.setName
//       ("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaA");
//        ServiceResponse actualResp = scriptResourceImpl.saveScript("admin", req.getAppId(), req);
//        assertThat(actualResp.isSuccess()).isEqualTo(false);
//        assertThat(actualResp.getCode()).isEqualTo(ErrorCode.SCRIPT_NAME_INVALID);
//    }
//
//    @Test
//    @DisplayName("删除脚本，返回成功响应")
//    public void whenDeleteScriptThenReturnSuccResp() {
//        ScriptDTO script = new ScriptDTO();
//        script.setAppId(2L);
//        script.setId("dc65a20cd91811e993a2309c2357fc12");
//        when(scriptService.getScriptByScriptId(anyString())).thenReturn(script);
//        ServiceResponse resp = scriptResourceImpl.deleteScriptByScriptId("admin", 2L, 
//       "dc65a20cd91811e993a2309c2357fc12");
//        assertThat(resp.isSuccess()).isEqualTo(true);
//    }
//
//    @Test
//    @DisplayName("删除脚本，服务异常，返回错误响应")
//    public void WhenDeleteScritpExceptionThenReturnFailResp() {
//        ServiceException exception = new ServiceException(ErrorCode.SCRIPT_NOT_EXIST);
//        doThrow(exception).when(scriptService).deleteScript(anyString(), anyLong(), anyString());
//        ServiceResponse resp = scriptResourceImpl.deleteScriptByScriptId("user1", 2L, 
//       "dc65a20cd91811e993a2309c2357fc12");
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.SCRIPT_NOT_EXIST);
//    }
//
//    @Test
//    @DisplayName("删除脚本版本，服务异常，返回错误响应")
//    public void whenDeleteScriptVersionExceptionThenReturnFailResp() {
//        ServiceException exception = new ServiceException(ErrorCode.SCRIPT_NOT_IN_APP);
//        doThrow(exception).when(scriptService).deleteScriptVersion(anyString(), eq(2L), eq(1L));
//
//        ServiceResponse resp = scriptResourceImpl.deleteScriptByScriptVersionId("user1", 2L, 1L);
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.SCRIPT_NOT_IN_APP);
//    }
//
//    @Test
//    @DisplayName("脚本上线:调用service异常，返回失败响应")
//    public void givenThrowServiceExceptionWhenPublishScriptThenReturnFailResp() {
//        ServiceException exception = new ServiceException(ErrorCode.SCRIPT_NOT_EXIST);
//        doThrow(exception).when(scriptService).publishScript(anyLong(), anyString(), anyString(), anyLong());
//
//        ServiceResponse resp = scriptResourceImpl.publishScriptVersion("user1", 2L, 
//       "dc65a20cd91811e993a2309c2357fc12", 1L);
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.SCRIPT_NOT_EXIST);
//    }
//
//    @DisplayName("脚本上线:参数不合法，返回失败响应")
//    @ParameterizedTest
//    @MethodSource("publishScriptParamProvider")
//    public void givenInvalidParamWhenPublishScriptThenReturnFailResp(Long appId, String scriptId, Long 
//   scriptVersionId) {
//        ServiceResponse resp = scriptResourceImpl.publishScriptVersion("user1", appId, scriptId, scriptVersionId);
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM);
//    }
//
//    static Stream<Arguments> publishScriptParamProvider() {
//        return Stream.of(
//            Arguments.of(null, "dc65a20cd91811e993a2309c2357fc12", 1L),
//            Arguments.of(1L, null, 1L),
//            Arguments.of(1L, "dc65a20cd91811e993a2309c2357fc12", null));
//    }
//
//    @Test
//    @DisplayName("禁用脚本:调用service异常，返回失败响应")
//    public void givenThrowServiceExceptionWhenDisableScriptThenReturnFailResp() {
//        ServiceException exception = new ServiceException(ErrorCode.SCRIPT_NOT_EXIST);
//        doThrow(exception).when(scriptService).disableScript(anyLong(), anyString(), anyString(), anyLong());
//
//        ServiceResponse resp = scriptResourceImpl.disableScriptVersion("user1", 2L, 
//       "dc65a20cd91811e993a2309c2357fc12", 1L);
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.SCRIPT_NOT_EXIST);
//    }
//
//    @Test
//    @DisplayName("禁用脚本:调用service异常，返回失败响应")
//    public void whenDisableScriptThenReturnSuccResp() {
//        ServiceResponse resp = scriptResourceImpl.disableScriptVersion("user1", 2L, 
//       "dc65a20cd91811e993a2309c2357fc12", 1L);
//
//        assertThat(resp.isSuccess()).isEqualTo(true);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.RESULT_OK);
//    }
//
//    @Test
//    @DisplayName("更新脚本描述，service抛出异常，需要返回错误响应")
//    public void givenExceptionWhenUpdateScriptDescThenReturnFailResp() {
//        Long appId = 2L;
//        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
//        String username = "user1";
//        ScriptInfoUpdateReq updateReq = new ScriptInfoUpdateReq();
//        updateReq.setUpdateField("scriptDesc");
//        updateReq.setScriptDesc("newDesc");
//
//        ServiceException ex = new ServiceException(ErrorCode.SCRIPT_NOT_EXIST);
//        doThrow(ex).when(scriptService).updateScriptDesc(appId, username, scriptId, "newDesc");
//
//        ServiceResponse resp = scriptResourceImpl.updateScriptInfo(username, appId, scriptId, updateReq);
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isNotEqualTo(ErrorCode.RESULT_OK);
//    }
//
//    @Test
//    @DisplayName("更新脚本名称，service抛出异常，需要返回错误响应")
//    public void givenExceptionWhenUpdateScriptNameThenReturnFailResp() {
//        Long appId = 2L;
//        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
//        String username = "user1";
//        ScriptInfoUpdateReq updateReq = new ScriptInfoUpdateReq();
//        updateReq.setUpdateField("scriptName");
//        updateReq.setScriptName("newName");
//
//        ServiceException ex = new ServiceException(ErrorCode.SCRIPT_NOT_EXIST);
//        doThrow(ex).when(scriptService).updateScriptName(appId, username, scriptId, updateReq.getScriptName());
//
//        ServiceResponse resp = scriptResourceImpl.updateScriptInfo(username, appId, scriptId, updateReq);
//
//        assertThat(resp.isSuccess()).isEqualTo(false);
//        assertThat(resp.getCode()).isNotEqualTo(ErrorCode.RESULT_OK);
//    }
//
//    @Test
//    @DisplayName("更新脚本元数据，根据不同的参数更新不同的字段")
//    public void givenUpdateFieldWhenUpdateScriptInfoThenUpdateField() {
//        Long appId = 2L;
//        String scriptId = "dc65a20cd91811e993a2309c2357fc12";
//        String username = "user1";
//
//        // 验证更新脚本名称
//        ScriptInfoUpdateReq updateScriptNameReq = new ScriptInfoUpdateReq();
//        updateScriptNameReq.setUpdateField("scriptName");
//        updateScriptNameReq.setScriptName("newName");
//
//        scriptResourceImpl.updateScriptInfo(username, appId, scriptId, updateScriptNameReq);
//
//        verify(scriptService).updateScriptName(appId, username, scriptId, updateScriptNameReq.getScriptName());
//
//        // 验证更新脚本描述
//        ScriptInfoUpdateReq updateScriptDescReq = new ScriptInfoUpdateReq();
//        updateScriptDescReq.setUpdateField("scriptDesc");
//        updateScriptDescReq.setScriptName("newDesc");
//
//        scriptResourceImpl.updateScriptInfo(username, appId, scriptId, updateScriptDescReq);
//
//        verify(scriptService).updateScriptDesc(appId, username, scriptId, updateScriptDescReq.getScriptDesc());
//
//        // 验证更新脚本标签
//        ScriptInfoUpdateReq updateScriptTagsReq = new ScriptInfoUpdateReq();
//        updateScriptTagsReq.setUpdateField("scriptTags");
//        List<TagVO> tags = new ArrayList<>();
//        TagVO tag1 = new TagVO();
//        tag1.setName("tag1");
//        tag1.setId(1L);
//        tags.add(tag1);
//        updateScriptTagsReq.setScriptTags(tags);
//
//        scriptResourceImpl.updateScriptInfo(username, appId, scriptId, updateScriptTagsReq);
//
//        List<TagDTO> tagDTOS = new ArrayList<>();
//        TagDTO tagDTO1 = new TagDTO();
//        tagDTO1.setName("tag1");
//        tagDTO1.setId(1L);
//        tagDTOS.add(tagDTO1);
//        verify(scriptService).updateScriptTags(appId, username, scriptId, tagDTOS);
//    }
//
//    @Test
//    @DisplayName("验证根据脚本名称模糊查询脚本名称列表")
//    public void whenListScriptNames() {
//        Long appId = 2L;
//        String keyword = "test";
//
//        List<String> scriptNames = new ArrayList<>();
//        scriptNames.add("test1");
//        scriptNames.add("test2");
//        when(scriptService.listScriptNames(appId, keyword)).thenReturn(scriptNames);
//
//        ServiceResponse<List<String>> resp = scriptResourceImpl.listAppScriptNames("user", appId, keyword);
//
//        assertThat(resp.isSuccess()).isEqualTo(true);
//        assertThat(resp.getCode()).isEqualTo(ErrorCode.RESULT_OK);
//        assertThat(resp.getData()).containsSequence("test1", "test2");
//    }


}
