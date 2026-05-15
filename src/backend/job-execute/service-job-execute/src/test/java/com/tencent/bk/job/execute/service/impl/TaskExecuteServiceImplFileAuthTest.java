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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.cache.CustomPasswordCache;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.quota.limit.RunningJobResourceQuotaManager;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.KubeContainerFilter;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.DangerousScriptCheckService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.api.inner.ServiceUserResource;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 文件分发鉴权（authFileTransfer）单元测试。
 * <p>
 * 覆盖白名单豁免账号鉴权的核心场景以及 {@link
 * TaskExecuteServiceImpl#areAllHostsWhitelistedFor} 工具方法，
 * 同时包含 {@code extractNeedAuthHostsAndAccounts}（作业方案路径）的回归断言：
 * 在白名单场景下账号集合保持改造前完全一致，防止误改作业方案路径。
 */
@ExtendWith(MockitoExtension.class)
public class TaskExecuteServiceImplFileAuthTest {

    private static final long APP_ID = 100L;
    private static final long TARGET_ACCOUNT_ID = 9001L;
    private static final long SOURCE_ACCOUNT_ID_1 = 9101L;
    private static final long SOURCE_ACCOUNT_ID_2 = 9102L;
    private static final long TARGET_HOST_1 = 1001L;
    private static final long TARGET_HOST_2 = 1002L;
    private static final long SOURCE_HOST_1 = 2001L;
    private static final long SOURCE_HOST_2 = 2002L;
    private static final String FILE_DIST_ACTION = ActionScopeEnum.FILE_DISTRIBUTION.name();

    @Mock
    private AccountService accountService;
    @Mock
    private TaskInstanceService taskInstanceService;
    @Mock
    private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    @Mock
    private TaskPlanService taskPlanService;
    @Mock
    private TaskInstanceVariableService taskInstanceVariableService;
    @Mock
    private TaskOperationLogService taskOperationLogService;
    @Mock
    private ScriptService scriptService;
    @Mock
    private StepInstanceService stepInstanceService;
    @Mock
    private ServiceUserResource userResource;
    @Mock
    private ExecuteAuthService executeAuthService;
    @Mock
    private DangerousScriptCheckService dangerousScriptCheckService;
    @Mock
    private JobExecuteConfig jobExecuteConfig;
    @Mock
    private TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    @Mock
    private RollingConfigService rollingConfigService;
    @Mock
    private ServiceTaskTemplateResource taskTemplateResource;
    @Mock
    private TaskInstanceExecuteObjectProcessor taskInstanceExecuteObjectProcessor;
    @Mock
    private RunningJobResourceQuotaManager runningJobResourceQuotaManager;
    @Mock
    private HostService hostService;
    @Mock
    private CustomPasswordCache customPasswordCache;
    @Mock
    private TenantService tenantService;

    private TaskExecuteServiceImpl service;
    private User operator;

    @BeforeEach
    void setUp() {
        service = new TaskExecuteServiceImpl(
            accountService,
            taskInstanceService,
            taskExecuteMQEventDispatcher,
            taskPlanService,
            taskInstanceVariableService,
            taskOperationLogService,
            scriptService,
            stepInstanceService,
            userResource,
            executeAuthService,
            dangerousScriptCheckService,
            jobExecuteConfig,
            taskEvictPolicyExecutor,
            rollingConfigService,
            taskTemplateResource,
            taskInstanceExecuteObjectProcessor,
            runningJobResourceQuotaManager,
            hostService,
            customPasswordCache,
            tenantService
        );
        operator = new User("tenant-1", "admin", "admin");
    }

    // ========================================================================
    // areAllHostsWhitelistedFor 工具方法单测
    // ========================================================================

    @Nested
    @DisplayName("areAllHostsWhitelistedFor 工具方法")
    class AreAllHostsWhitelistedForTest {

        @Test
        @DisplayName("target 为 null → false")
        void nullTarget_returnsFalse() {
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                null, ActionScopeEnum.FILE_DISTRIBUTION, allowMap(TARGET_HOST_1)))
                .isFalse();
        }

        @Test
        @DisplayName("白名单 map 为 null → false")
        void nullWhitelistMap_returnsFalse() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1);
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, null))
                .isFalse();
        }

        @Test
        @DisplayName("白名单 map 为空 → false（与现有 filterHostsDoNotRequireAuth 行为一致）")
        void emptyWhitelistMap_returnsFalse() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1);
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, Collections.emptyMap()))
                .isFalse();
        }

        @Test
        @DisplayName("staticIpList 为空 → false（保守策略，避免 corner case 越权）")
        void emptyStaticIpList_returnsFalse() {
            ExecuteTargetDTO target = new ExecuteTargetDTO();
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, allowMap(TARGET_HOST_1)))
                .isFalse();
        }

        @Test
        @DisplayName("staticIpList 全部命中 + action 允许 → true")
        void allStaticHostsAllowed_returnsTrue() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1, TARGET_HOST_2);
            Map<Long, List<String>> whitelist = allowMap(TARGET_HOST_1, TARGET_HOST_2);
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, whitelist))
                .isTrue();
        }

        @Test
        @DisplayName("一台主机不在白名单 → false")
        void oneHostMissing_returnsFalse() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1, TARGET_HOST_2);
            Map<Long, List<String>> whitelist = allowMap(TARGET_HOST_1);
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, whitelist))
                .isFalse();
        }

        @Test
        @DisplayName("主机在白名单但 action 列表不包含目标动作 → false")
        void hostWhitelistedButActionMismatch_returnsFalse() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1);
            Map<Long, List<String>> whitelist = new HashMap<>();
            whitelist.put(TARGET_HOST_1,
                Collections.singletonList(ActionScopeEnum.SCRIPT_EXECUTE.name()));
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, whitelist))
                .isFalse();
        }

        @Test
        @DisplayName("含动态分组 → false（不在主机白名单机制内）")
        void hasDynamicServerGroups_returnsFalse() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1);
            DynamicServerGroupDTO group = new DynamicServerGroupDTO();
            group.setGroupId("group-1");
            target.setDynamicServerGroups(Collections.singletonList(group));
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, allowMap(TARGET_HOST_1)))
                .isFalse();
        }

        @Test
        @DisplayName("含拓扑节点 → false")
        void hasTopoNodes_returnsFalse() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1);
            DynamicServerTopoNodeDTO node = new DynamicServerTopoNodeDTO();
            node.setNodeType("module");
            node.setTopoNodeId(1L);
            target.setTopoNodes(Collections.singletonList(node));
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, allowMap(TARGET_HOST_1)))
                .isFalse();
        }

        @Test
        @DisplayName("含静态容器 → false")
        void hasStaticContainerList_returnsFalse() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1);
            Container container = new Container();
            container.setId(1L);
            target.setStaticContainerList(Collections.singletonList(container));
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, allowMap(TARGET_HOST_1)))
                .isFalse();
        }

        @Test
        @DisplayName("含容器过滤器 → false")
        void hasContainerFilters_returnsFalse() {
            ExecuteTargetDTO target = staticHosts(TARGET_HOST_1);
            target.setContainerFilters(Collections.singletonList(new KubeContainerFilter()));
            assertThat(TaskExecuteServiceImpl.areAllHostsWhitelistedFor(
                target, ActionScopeEnum.FILE_DISTRIBUTION, allowMap(TARGET_HOST_1)))
                .isFalse();
        }
    }

    // ========================================================================
    // authFileTransfer 集成单测
    // ========================================================================

    @Nested
    @DisplayName("authFileTransfer 文件分发鉴权")
    class AuthFileTransferTest {

        @Test
        @DisplayName("5.1.1 目标主机 + 源主机全部在白名单 → 不调用 batchAuthAccountExecutable，不调用 authFastPushFile")
        void allHostsWhitelisted_skipAllAuth() {
            TaskInstanceDTO task = newTask();
            FileSourceDTO source = serverFileSource(SOURCE_ACCOUNT_ID_1, SOURCE_HOST_1);
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, staticHosts(TARGET_HOST_1, TARGET_HOST_2),
                source);
            Map<Long, List<String>> whitelist = allowMap(
                TARGET_HOST_1, TARGET_HOST_2, SOURCE_HOST_1);

            AuthResult result = service.authFileTransfer(operator, task, step, whitelist);

            assertThat(result.isPass()).isTrue();
            verify(executeAuthService, never())
                .batchAuthAccountExecutable(any(), any(), anyCollection());
            verify(executeAuthService, never())
                .authFastPushFile(any(), any(), any());
        }

        @Test
        @DisplayName("5.1.2 目标主机全在白名单，源主机部分在白名单 → 仅鉴源账号 + 仅鉴未豁免源主机")
        void targetAllWhitelisted_sourcePartial_authSourceAccountOnly() {
            TaskInstanceDTO task = newTask();
            FileSourceDTO source = serverFileSource(SOURCE_ACCOUNT_ID_1, SOURCE_HOST_1, SOURCE_HOST_2);
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, staticHosts(TARGET_HOST_1), source);
            Map<Long, List<String>> whitelist = allowMap(TARGET_HOST_1, SOURCE_HOST_1);
            stubAccountAuthPass();
            stubFastPushFilePass();

            AuthResult result = service.authFileTransfer(operator, task, step, whitelist);

            assertThat(result.isPass()).isTrue();
            ArgumentCaptor<Collection<Long>> accountIdsCaptor = collectionCaptor();
            verify(executeAuthService, times(1)).batchAuthAccountExecutable(
                any(User.class), any(AppResourceScope.class), accountIdsCaptor.capture());
            assertThat(accountIdsCaptor.getValue()).containsExactlyInAnyOrder(SOURCE_ACCOUNT_ID_1);

            ArgumentCaptor<ExecuteTargetDTO> targetCaptor = ArgumentCaptor.forClass(ExecuteTargetDTO.class);
            verify(executeAuthService, times(1)).authFastPushFile(
                any(User.class), any(AppResourceScope.class), targetCaptor.capture());
            assertThat(hostIds(targetCaptor.getValue())).containsExactlyInAnyOrder(SOURCE_HOST_2);
        }

        @Test
        @DisplayName("5.1.3 源主机全在白名单，目标主机部分在白名单 → 仅鉴目标账号 + 仅鉴未豁免目标主机")
        void sourceAllWhitelisted_targetPartial_authTargetAccountOnly() {
            TaskInstanceDTO task = newTask();
            FileSourceDTO source = serverFileSource(SOURCE_ACCOUNT_ID_1, SOURCE_HOST_1);
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, staticHosts(TARGET_HOST_1, TARGET_HOST_2),
                source);
            Map<Long, List<String>> whitelist = allowMap(TARGET_HOST_1, SOURCE_HOST_1);
            stubAccountAuthPass();
            stubFastPushFilePass();

            AuthResult result = service.authFileTransfer(operator, task, step, whitelist);

            assertThat(result.isPass()).isTrue();
            ArgumentCaptor<Collection<Long>> accountIdsCaptor = collectionCaptor();
            verify(executeAuthService, times(1)).batchAuthAccountExecutable(
                any(User.class), any(AppResourceScope.class), accountIdsCaptor.capture());
            assertThat(accountIdsCaptor.getValue()).containsExactlyInAnyOrder(TARGET_ACCOUNT_ID);

            ArgumentCaptor<ExecuteTargetDTO> targetCaptor = ArgumentCaptor.forClass(ExecuteTargetDTO.class);
            verify(executeAuthService, times(1)).authFastPushFile(
                any(User.class), any(AppResourceScope.class), targetCaptor.capture());
            assertThat(hostIds(targetCaptor.getValue())).containsExactlyInAnyOrder(TARGET_HOST_2);
        }

        @Test
        @DisplayName("5.1.4 多个 FileSource 不同源账号、混合白名单 → 各源账号独立判定豁免")
        void multipleFileSources_independentExemption() {
            TaskInstanceDTO task = newTask();
            FileSourceDTO source1 = serverFileSource(SOURCE_ACCOUNT_ID_1, SOURCE_HOST_1);
            FileSourceDTO source2 = serverFileSource(SOURCE_ACCOUNT_ID_2, SOURCE_HOST_2);
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, staticHosts(TARGET_HOST_1),
                source1, source2);
            // 目标 + 源1 在白名单；源2 不在
            Map<Long, List<String>> whitelist = allowMap(TARGET_HOST_1, SOURCE_HOST_1);
            stubAccountAuthPass();
            stubFastPushFilePass();

            AuthResult result = service.authFileTransfer(operator, task, step, whitelist);

            assertThat(result.isPass()).isTrue();
            ArgumentCaptor<Collection<Long>> accountIdsCaptor = collectionCaptor();
            verify(executeAuthService, times(1)).batchAuthAccountExecutable(
                any(User.class), any(AppResourceScope.class), accountIdsCaptor.capture());
            // 目标账号豁免、源1豁免、源2 保留
            assertThat(accountIdsCaptor.getValue()).containsExactlyInAnyOrder(SOURCE_ACCOUNT_ID_2);
        }

        @Test
        @DisplayName("5.1.5 目标含动态分组 → 即使 staticIpList 全部在白名单，仍需鉴目标账号")
        void targetWithDynamicGroup_doesNotExemptAccount() {
            TaskInstanceDTO task = newTask();
            FileSourceDTO source = serverFileSource(SOURCE_ACCOUNT_ID_1, SOURCE_HOST_1);
            ExecuteTargetDTO targetWithGroup = staticHosts(TARGET_HOST_1);
            DynamicServerGroupDTO group = new DynamicServerGroupDTO();
            group.setGroupId("group-1");
            targetWithGroup.setDynamicServerGroups(Collections.singletonList(group));
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, targetWithGroup, source);
            Map<Long, List<String>> whitelist = allowMap(TARGET_HOST_1, SOURCE_HOST_1);
            stubAccountAuthPass();
            stubFastPushFilePass();

            AuthResult result = service.authFileTransfer(operator, task, step, whitelist);

            assertThat(result.isPass()).isTrue();
            ArgumentCaptor<Collection<Long>> accountIdsCaptor = collectionCaptor();
            verify(executeAuthService, times(1)).batchAuthAccountExecutable(
                any(User.class), any(AppResourceScope.class), accountIdsCaptor.capture());
            // 目标账号不豁免（含动态分组）；源账号豁免
            assertThat(accountIdsCaptor.getValue()).containsExactlyInAnyOrder(TARGET_ACCOUNT_ID);
        }

        @Test
        @DisplayName("5.1.6 本地上传 FileSource → 不参与豁免判定（无源账号、无源主机）")
        void localUploadFileSource_ignoredInExemption() {
            TaskInstanceDTO task = newTask();
            FileSourceDTO localSource = new FileSourceDTO();
            localSource.setLocalUpload(true);
            localSource.setFileType(TaskFileTypeEnum.LOCAL.getType());
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, staticHosts(TARGET_HOST_1), localSource);
            Map<Long, List<String>> whitelist = allowMap(TARGET_HOST_1);

            AuthResult result = service.authFileTransfer(operator, task, step, whitelist);

            assertThat(result.isPass()).isTrue();
            // 目标账号豁免，本地上传 FileSource 不参与，账号 set 为空
            verify(executeAuthService, never())
                .batchAuthAccountExecutable(any(), any(), anyCollection());
            verify(executeAuthService, never())
                .authFastPushFile(any(), any(), any());
        }

        @Test
        @DisplayName("5.1.7 BASE64 文件源（push_config_file 真实生产构造：accountId=null, servers=null）→ 完全不参与账号鉴权")
        void base64FileSource_doesNotParticipateInAccountAuth() {
            // 重要：生产代码中 BASE64 源仅由 EsbPushConfigFileResourceImpl/V3Impl 构造，
            // 始终是 setAccount("root") 占位字符串、setLocalUpload(false)、setFileType(BASE64_FILE)，
            // 不调 setAccountId、不调 setServers。此处复刻该真实状态。
            // 产品语义：BASE64 源是从 job-execute 机器分发的，没有源主机也就没有源账号鉴权概念。
            TaskInstanceDTO task = newTask();
            FileSourceDTO base64 = new FileSourceDTO();
            base64.setAccount("root");
            base64.setLocalUpload(false);
            base64.setFileType(TaskFileTypeEnum.BASE64_FILE.getType());
            // accountId 与 servers 故意保持 null，与生产构造一致
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, staticHosts(TARGET_HOST_1), base64);
            Map<Long, List<String>> whitelist = allowMap(TARGET_HOST_1);

            AuthResult result = service.authFileTransfer(operator, task, step, whitelist);

            assertThat(result.isPass()).isTrue();
            // 目标账号被白名单豁免；BASE64 源因 accountId=null 直接被跳过
            // 账号集合为空 → 完全不调 batchAuthAccountExecutable
            verify(executeAuthService, never())
                .batchAuthAccountExecutable(any(), any(), anyCollection());
            // 目标主机被白名单过滤、BASE64 源不贡献任何主机 → executeTarget 为空
            verify(executeAuthService, never())
                .authFastPushFile(any(), any(), any());
        }

        @Test
        @DisplayName("5.1.8 白名单 map 为空 → 行为与改造前完全一致：所有账号都鉴权、所有主机都鉴权")
        void emptyWhitelist_behavesAsBeforeChange() {
            TaskInstanceDTO task = newTask();
            FileSourceDTO source = serverFileSource(SOURCE_ACCOUNT_ID_1, SOURCE_HOST_1);
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, staticHosts(TARGET_HOST_1), source);
            stubAccountAuthPass();
            stubFastPushFilePass();

            AuthResult result = service.authFileTransfer(operator, task, step, Collections.emptyMap());

            assertThat(result.isPass()).isTrue();
            ArgumentCaptor<Collection<Long>> accountIdsCaptor = collectionCaptor();
            verify(executeAuthService, times(1)).batchAuthAccountExecutable(
                any(User.class), any(AppResourceScope.class), accountIdsCaptor.capture());
            assertThat(accountIdsCaptor.getValue())
                .containsExactlyInAnyOrder(TARGET_ACCOUNT_ID, SOURCE_ACCOUNT_ID_1);

            ArgumentCaptor<ExecuteTargetDTO> targetCaptor = ArgumentCaptor.forClass(ExecuteTargetDTO.class);
            verify(executeAuthService, times(1)).authFastPushFile(
                any(User.class), any(AppResourceScope.class), targetCaptor.capture());
            assertThat(hostIds(targetCaptor.getValue()))
                .containsExactlyInAnyOrder(TARGET_HOST_1, SOURCE_HOST_1);
        }

        @Test
        @DisplayName("5.1.9 executeTarget.isEmpty() 边界：合并后无主机 → 仍按账号集合鉴权（保留旧行为）")
        void executeTargetEmptyAfterMerge_keepsAccountAuth() {
            TaskInstanceDTO task = newTask();
            // 复刻 push_config_file 真实生产构造：BASE64 源 accountId=null, servers=null
            // 配合目标也是空 ExecuteTargetDTO —— 合并后 executeTarget.isEmpty() 为 true
            FileSourceDTO base64 = new FileSourceDTO();
            base64.setAccount("root");
            base64.setLocalUpload(false);
            base64.setFileType(TaskFileTypeEnum.BASE64_FILE.getType());
            // accountId 与 servers 与生产一致地保持 null
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID, new ExecuteTargetDTO(), base64);
            stubAccountAuthPass();

            AuthResult result = service.authFileTransfer(operator, task, step, Collections.emptyMap());

            assertThat(result.isPass()).isTrue();
            // 目标账号未被白名单豁免（whitelist 为空 → areAllHostsWhitelistedFor 直接返回 false）
            // 仍鉴目标账号（保留改造前行为）
            ArgumentCaptor<Collection<Long>> accountIdsCaptor = collectionCaptor();
            verify(executeAuthService, times(1))
                .batchAuthAccountExecutable(any(), any(), accountIdsCaptor.capture());
            assertThat(accountIdsCaptor.getValue()).containsExactlyInAnyOrder(TARGET_ACCOUNT_ID);
            // 主机为空，跳过主机鉴权
            verify(executeAuthService, never())
                .authFastPushFile(any(), any(), any());
        }
    }

    // ========================================================================
    // 作业方案路径回归测试
    // 防止误改 extractNeedAuthHostsAndAccounts —— 在白名单场景下账号集合应保持改造前完全一致
    // ========================================================================

    @Nested
    @DisplayName("extractNeedAuthHostsAndAccounts 作业方案路径回归（本期不应被修改）")
    class ExtractNeedAuthHostsAndAccountsRegressionTest {

        @Test
        @DisplayName("即使所有主机都在白名单，账号集合仍应包含目标账号 + 所有源账号（行为保持改造前一致）")
        void planPath_accountsUnchangedEvenIfAllHostsWhitelisted() {
            FileSourceDTO source1 = serverFileSource(SOURCE_ACCOUNT_ID_1, SOURCE_HOST_1);
            FileSourceDTO source2 = serverFileSource(SOURCE_ACCOUNT_ID_2, SOURCE_HOST_2);
            StepInstanceDTO step = fileStep(TARGET_ACCOUNT_ID,
                staticHosts(TARGET_HOST_1, TARGET_HOST_2), source1, source2);
            Map<Long, List<String>> whitelist = allowMap(
                TARGET_HOST_1, TARGET_HOST_2, SOURCE_HOST_1, SOURCE_HOST_2);

            Pair<ExecuteTargetDTO, Set<Long>> result =
                service.extractNeedAuthHostsAndAccounts(step, whitelist);

            // 关键断言：账号集合完整保留（与改造前完全一致），不受白名单影响
            assertThat(result.getRight()).containsExactlyInAnyOrder(
                TARGET_ACCOUNT_ID, SOURCE_ACCOUNT_ID_1, SOURCE_ACCOUNT_ID_2);
            // 主机维度仍应被白名单过滤
            assertThat(result.getLeft().getStaticIpList()).isNullOrEmpty();
            // executeAuthService 不应被调用（extractNeedAuthHostsAndAccounts 不触发鉴权）
            verifyNoInteractions(executeAuthService);
        }
    }

    // ========================================================================
    // 测试辅助方法
    // ========================================================================

    private void stubAccountAuthPass() {
        when(executeAuthService.batchAuthAccountExecutable(any(), any(), anyCollection()))
            .thenReturn(AuthResult.pass(operator));
    }

    private void stubFastPushFilePass() {
        when(executeAuthService.authFastPushFile(any(), any(), any()))
            .thenReturn(AuthResult.pass(operator));
    }

    private static ExecuteTargetDTO staticHosts(long... hostIds) {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        List<HostDTO> hosts = new ArrayList<>(hostIds.length);
        for (long hostId : hostIds) {
            hosts.add(new HostDTO(hostId));
        }
        target.setStaticIpList(hosts);
        return target;
    }

    private static Map<Long, List<String>> allowMap(long... hostIds) {
        Map<Long, List<String>> map = new HashMap<>();
        for (long hostId : hostIds) {
            map.put(hostId, Collections.singletonList(FILE_DIST_ACTION));
        }
        return map;
    }

    private static List<Long> hostIds(ExecuteTargetDTO target) {
        if (target == null || target.getStaticIpList() == null) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>(target.getStaticIpList().size());
        target.getStaticIpList().forEach(host -> ids.add(host.getHostId()));
        return ids;
    }

    private static FileSourceDTO serverFileSource(Long accountId, long... sourceHostIds) {
        FileSourceDTO source = new FileSourceDTO();
        source.setLocalUpload(false);
        source.setFileType(TaskFileTypeEnum.SERVER.getType());
        source.setAccountId(accountId);
        source.setServers(staticHosts(sourceHostIds));
        return source;
    }

    private static TaskInstanceDTO newTask() {
        TaskInstanceDTO task = new TaskInstanceDTO();
        task.setAppId(APP_ID);
        return task;
    }

    private static StepInstanceDTO fileStep(Long targetAccountId,
                                            ExecuteTargetDTO targetExecuteObjects,
                                            FileSourceDTO... fileSources) {
        StepInstanceDTO step = new StepInstanceDTO();
        step.setAppId(APP_ID);
        step.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        step.setAccountId(targetAccountId);
        step.setTargetExecuteObjects(targetExecuteObjects);
        step.setFileSourceList(Arrays.asList(fileSources));
        return step;
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Collection<Long>> collectionCaptor() {
        return ArgumentCaptor.forClass(Collection.class);
    }
}
