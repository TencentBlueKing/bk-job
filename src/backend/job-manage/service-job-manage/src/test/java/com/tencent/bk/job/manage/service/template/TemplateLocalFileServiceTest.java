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

package com.tencent.bk.job.manage.service.template;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryHelper;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.manage.config.LocalFileConfigForManage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TemplateLocalFileService} 单元测试。
 *
 * <p>路径归属校验是安全边界：调用方只提供一个字符串路径，若不校验就能引用其他业务上传的文件。
 */
class TemplateLocalFileServiceTest {

    private static final Long APP_ID = 2L;
    private static final String PROJECT = "bkjob";
    private static final String REPO = "localupload";

    private ArtifactoryClient artifactoryClient;
    private TemplateLocalFileService service;

    @BeforeEach
    void setUp() {
        ArtifactoryHelper artifactoryHelper = mock(ArtifactoryHelper.class);
        when(artifactoryHelper.getJobRealProject()).thenReturn(PROJECT);
        LocalFileConfigForManage localFileConfig = new LocalFileConfigForManage();
        localFileConfig.setLocalUploadRepo(REPO);
        localFileConfig.setExpireDays(7);
        artifactoryClient = mock(ArtifactoryClient.class);
        service = new TemplateLocalFileService(artifactoryHelper, localFileConfig, artifactoryClient);
    }

    @Test
    @DisplayName("正常路径返回制品库中的 hash 与大小")
    void returns_hash_and_size_of_file_node() {
        String filePath = "2/8f1c/admin/app.tar.gz";
        mockFileNode(filePath, "d41d8cd98f00b204e9800998ecf8427e", 2048L);

        TemplateLocalFileService.LocalFileDetail detail = service.getFileDetail(APP_ID, filePath);

        assertThat(detail.getFileHash()).isEqualTo("d41d8cd98f00b204e9800998ecf8427e");
        assertThat(detail.getFileSize()).isEqualTo(2048L);
    }

    @Test
    @DisplayName("查询制品库时按 项目/仓库 前缀拼出完整路径")
    void queries_artifactory_with_project_and_repo_prefix() {
        String filePath = "2/8f1c/admin/app.tar.gz";
        mockFileNode(filePath, "hash", 1L);

        service.getFileDetail(APP_ID, filePath);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(artifactoryClient).getFileNode(pathCaptor.capture());
        assertThat(pathCaptor.getValue()).isEqualTo(PROJECT + "/" + REPO + "/" + filePath);
    }

    @Test
    @DisplayName("路径首段不是当前业务时拒绝，且不触达制品库")
    void rejects_path_of_another_app() {
        assertThatThrownBy(() -> service.getFileDetail(APP_ID, "3/8f1c/admin/app.tar.gz"))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(errorReason(e)).contains("does not belong to current scope"));
        verify(artifactoryClient, never()).getFileNode(anyString());
    }

    @Test
    @DisplayName("路径含 .. 时拒绝，防止跨目录穿越")
    void rejects_path_traversal() {
        assertThatThrownBy(() -> service.getFileDetail(APP_ID, "2/../3/8f1c/admin/app.tar.gz"))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(errorReason(e)).contains(".."));
        verify(artifactoryClient, never()).getFileNode(anyString());
    }

    @Test
    @DisplayName("路径为空白时拒绝")
    void rejects_blank_path() {
        assertThatThrownBy(() -> service.getFileDetail(APP_ID, "   "))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(errorReason(e)).contains("cannot be blank"));
        verify(artifactoryClient, never()).getFileNode(anyString());
    }

    @Test
    @DisplayName("路径带首尾斜杠与反斜杠时仍能正确识别归属业务")
    void normalizes_slashes_before_checking_owner() {
        mockFileNode("/2\\8f1c\\admin\\app.tar.gz", "hash", 1L);

        assertThat(service.getFileDetail(APP_ID, "/2\\8f1c\\admin\\app.tar.gz")).isNotNull();
    }

    @Test
    @DisplayName("制品库中找不到节点时转为本地文件不存在，而非内部错误")
    void maps_missing_node_to_local_file_not_exist() {
        when(artifactoryClient.getFileNode(anyString()))
            .thenThrow(new InternalException(ErrorCode.CAN_NOT_FIND_NODE_IN_ARTIFACTORY));

        assertThatThrownBy(() -> service.getFileDetail(APP_ID, "2/8f1c/admin/app.tar.gz"))
            .isInstanceOfSatisfying(NotFoundException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOCAL_FILE_NOT_EXIST_IN_BACKEND));
    }

    @Test
    @DisplayName("节点返回 null 时按本地文件不存在处理")
    void treats_null_node_as_local_file_not_exist() {
        when(artifactoryClient.getFileNode(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.getFileDetail(APP_ID, "2/8f1c/admin/app.tar.gz"))
            .isInstanceOfSatisfying(NotFoundException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOCAL_FILE_NOT_EXIST_IN_BACKEND));
    }

    @Test
    @DisplayName("路径指向目录时按本地文件不存在处理")
    void treats_folder_node_as_local_file_not_exist() {
        NodeDTO folder = new NodeDTO();
        folder.setFolder(true);
        when(artifactoryClient.getFileNode(anyString())).thenReturn(folder);

        assertThatThrownBy(() -> service.getFileDetail(APP_ID, "2/8f1c/admin"))
            .isInstanceOfSatisfying(NotFoundException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOCAL_FILE_NOT_EXIST_IN_BACKEND));
    }

    @Test
    @DisplayName("制品库的其它异常原样抛出，不被吞成文件不存在")
    void propagates_other_artifactory_errors() {
        when(artifactoryClient.getFileNode(anyString()))
            .thenThrow(new InternalException(ErrorCode.ARTIFACTORY_API_DATA_ERROR));

        assertThatThrownBy(() -> service.getFileDetail(APP_ID, "2/8f1c/admin/app.tar.gz"))
            .isInstanceOfSatisfying(InternalException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ARTIFACTORY_API_DATA_ERROR));
    }

    private void mockFileNode(String filePath, String md5, Long size) {
        NodeDTO node = new NodeDTO();
        node.setFolder(false);
        node.setMd5(md5);
        node.setSize(size);
        node.setFullPath(filePath);
        when(artifactoryClient.getFileNode(anyString())).thenReturn(node);
    }

    /**
     * 参数错误的原因文案在 errorParams 末位，异常自身的 message 为空。
     */
    private String errorReason(ServiceException e) {
        Object[] errorParams = e.getErrorParams();
        return errorParams == null ? "" : String.valueOf(errorParams[errorParams.length - 1]);
    }
}
