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

package com.tencent.bk.job.manage.service.impl.notify;

import com.google.common.collect.Sets;
import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.model.dto.notify.CustomNotifyDTO;
import com.tencent.bk.job.common.model.vo.NotifyChannelVO;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.Counter;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PrefConsts;
import com.tencent.bk.job.manage.api.common.constants.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.JobRoleEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.NotifyConsts;
import com.tencent.bk.job.manage.api.common.constants.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.EsbAppRoleDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyConfigStatusDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyPolicyRoleTargetDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyRoleTargetChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTriggerPolicyDAO;
import com.tencent.bk.job.manage.dao.plan.TaskPlanDAO;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyPolicyRoleTargetDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyRoleTargetChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTemplateDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTriggerPolicyDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationTriggerDTO;
import com.tencent.bk.job.manage.model.inner.ServiceSpecificResourceNotifyPolicyDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTriggerTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyPoliciesCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.notify.ResourceStatusChannel;
import com.tencent.bk.job.manage.model.web.request.notify.SetAvailableNotifyChannelReq;
import com.tencent.bk.job.manage.model.web.request.notify.TriggerPolicy;
import com.tencent.bk.job.manage.model.web.vo.notify.ExecuteStatusVO;
import com.tencent.bk.job.manage.model.web.vo.notify.PageTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ResourceTypeVO;
import com.tencent.bk.job.manage.model.web.vo.notify.RoleVO;
import com.tencent.bk.job.manage.model.web.vo.notify.TriggerPolicyVO;
import com.tencent.bk.job.manage.model.web.vo.notify.TriggerTypeVO;
import com.tencent.bk.job.manage.service.AppRoleService;
import com.tencent.bk.job.manage.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    private static final String REDIS_KEY_SAVE_APP_DEFAULT_NOTIFY_POLICIES = "NotifyServiceImpl" +
        ".saveAppDefaultNotifyPolicies";

    private final NotifyTriggerPolicyDAO notifyTriggerPolicyDAO;
    private final NotifyRoleTargetChannelDAO notifyRoleTargetChannelDAO;
    private final NotifyPolicyRoleTargetDAO notifyPolicyRoleTargetDAO;
    private final EsbAppRoleDAO esbAppRoleDAO;
    private final AvailableEsbChannelDAO availableEsbChannelDAO;
    private final NotifyEsbChannelDAO notifyEsbChannelDAO;
    private final NotifyConfigStatusDAO notifyConfigStatusDAO;
    private final NotifyTemplateService notifyTemplateService;
    private final TaskPlanDAO taskPlanDAO;
    private final NotifyUserService notifyUserService;
    private final NotifySendService notifySendService;
    private final AppRoleService roleService;
    private final TenantService tenantService;
    private final NotifyChannelInitService notifyChannelInitService;

    @Autowired
    public NotifyServiceImpl(
        NotifyTriggerPolicyDAO notifyTriggerPolicyDAO,
        NotifyPolicyRoleTargetDAO notifyPolicyRoleTargetDAO,
        NotifyRoleTargetChannelDAO notifyRoleTargetChannelDAO,
        EsbAppRoleDAO esbAppRoleDAO,
        AvailableEsbChannelDAO availableEsbChannelDAO,
        NotifyEsbChannelDAO notifyEsbChannelDAO,
        NotifySendService notifySendService,
        AppRoleService roleService,
        NotifyConfigStatusDAO notifyConfigStatusDAO,
        NotifyTemplateService notifyTemplateService,
        TaskPlanDAO taskPlanDAO,
        NotifyUserService notifyUserService,
        TenantService tenantService,
        NotifyChannelInitService notifyChannelInitService) {
        this.notifyTriggerPolicyDAO = notifyTriggerPolicyDAO;
        this.notifyPolicyRoleTargetDAO = notifyPolicyRoleTargetDAO;
        this.notifyRoleTargetChannelDAO = notifyRoleTargetChannelDAO;
        this.esbAppRoleDAO = esbAppRoleDAO;
        this.availableEsbChannelDAO = availableEsbChannelDAO;
        this.notifyEsbChannelDAO = notifyEsbChannelDAO;
        this.notifyUserService = notifyUserService;
        this.notifySendService = notifySendService;
        this.roleService = roleService;
        this.notifyConfigStatusDAO = notifyConfigStatusDAO;
        this.notifyTemplateService = notifyTemplateService;
        this.taskPlanDAO = taskPlanDAO;
        this.tenantService = tenantService;
        this.notifyChannelInitService = notifyChannelInitService;
    }

    @Override
    public List<TriggerPolicyVO> listAppDefaultNotifyPolicies(String username, Long appId) {
        return notifyTriggerPolicyDAO.listAppDefault(getDefaultTriggerUser(), appId, NotifyConsts.DEFAULT_RESOURCE_ID);
    }

    @Override
    public CustomNotifyDTO getSpecificResourceNotifyPolicy(Long appId,
                                                           Integer resourceType,
                                                           String resourceId,
                                                           Integer triggerType) {
        return notifyTriggerPolicyDAO.getSpecificResourceNotifyPolicy(appId, resourceType, resourceId, triggerType);
    }


    @Override
    @ActionAuditRecord(
        actionId = ActionId.NOTIFICATION_SETTING,
        content = EventContentConstants.EDIT_BUSINESS_NOTIFY_SETTINGS
    )
    public Long saveAppDefaultNotifyPolicies(
        String username,
        Long appId,
        NotifyPoliciesCreateUpdateReq createUpdateReq
    ) {
        // 外部调用默认鉴权
        return saveAppDefaultNotifyPolicies(username, appId, createUpdateReq, true);
    }

    private Long saveTriggerPolicy(Long appId,
                                   ResourceTypeEnum resourceType,
                                   String resourceId,
                                   String triggerUser,
                                   TriggerPolicy triggerPolicy,
                                   ResourceStatusChannel resourceStatusChannel,
                                   String operator) {
        return notifyTriggerPolicyDAO.insertNotifyTriggerPolicy(
            new NotifyTriggerPolicyDTO(
                null,
                appId,
                resourceId,
                resourceType,
                triggerUser,
                triggerPolicy.getTriggerType(),
                resourceStatusChannel.getExecuteStatus(),
                operator,
                System.currentTimeMillis(),
                operator,
                System.currentTimeMillis()
            ));
    }

    private void setExtraObserversForRoleTarget(NotifyPolicyRoleTargetDTO roleTargetDTO,
                                                String role,
                                                TriggerPolicy triggerPolicy) {
        if (StringUtils.isBlank(role) || !JobRoleEnum.JOB_EXTRA_OBSERVER.name().equals(role)) {
            return;
        }
        if (CollectionUtils.isEmpty(triggerPolicy.getExtraObserverList())) {
            return;
        }
        Set<String> extraObserverSet = Sets.newHashSet(triggerPolicy.getExtraObserverList());
        triggerPolicy.getExtraObserverList().forEach(extraObserver -> {
            if (StringUtils.isBlank(extraObserver)) {
                extraObserverSet.remove(extraObserver);
            }
        });
        String extraObserverStr = String.join(NotifyConsts.SEPERATOR_COMMA, extraObserverSet);
        //额外通知对象添加额外通知者字段
        roleTargetDTO.setExtraObservers(extraObserverStr);
    }

    private NotifyPolicyRoleTargetDTO buildBasicRoleTarget(Long policyId, String role, String operator) {
        return NotifyPolicyRoleTargetDTO.builder()
            .policyId(policyId)
            .role(role)
            .enable(true)
            .creator(operator)
            .createTime(System.currentTimeMillis())
            .lastModifier(operator)
            .lastModifyTime(System.currentTimeMillis())
            .build();
    }

    private void saveNotifyRoleTargets(Long policyId,
                                       TriggerPolicy triggerPolicy,
                                       ResourceStatusChannel resourceStatusChannel,
                                       String operator) {
        Set<String> roleSet = new HashSet<>(triggerPolicy.getRoleList());
        roleSet.forEach(role -> {
            NotifyPolicyRoleTargetDTO roleTargetDTO = buildBasicRoleTarget(policyId, role, operator);
            setExtraObserversForRoleTarget(roleTargetDTO, role, triggerPolicy);
            Long roleTargetId = notifyPolicyRoleTargetDAO.insert(roleTargetDTO);
            //为每一个通知对象保存所有channel
            Set<String> channelSet = new HashSet<>(resourceStatusChannel.getChannelList());
            for (String channel : channelSet) {
                notifyRoleTargetChannelDAO.insert(
                    new NotifyRoleTargetChannelDTO(roleTargetId, channel, operator)
                );
            }
        });
    }

    private void saveTriggerPolicy(String operator,
                                   Long appId,
                                   String triggerUser,
                                   TriggerPolicy triggerPolicy) {
        Set<ResourceTypeEnum> resourceTypeSet = new HashSet<>(triggerPolicy.getResourceTypeList());
        resourceTypeSet.forEach(resourceType -> {
            Set<ResourceStatusChannel> channelSet = new HashSet<>(triggerPolicy.getResourceStatusChannelList());
            channelSet.forEach(channel -> {
                cascadeSaveTriggerPolicyAndTarget(
                    appId,
                    operator,
                    resourceType,
                    NotifyConsts.DEFAULT_RESOURCE_ID,
                    triggerUser,
                    triggerPolicy,
                    channel);
            });
        });
    }

    private void cascadeSaveTriggerPolicyAndTarget(Long appId,
                                                   String operator,
                                                   ResourceTypeEnum resourceType,
                                                   String resourceId,
                                                   String triggerUser,
                                                   TriggerPolicy triggerPolicy,
                                                   ResourceStatusChannel channel) {
        //保存触发策略
        val policyId = saveTriggerPolicy(
            appId, resourceType, resourceId,
            triggerUser, triggerPolicy, channel, operator
        );
        //保存所有通知对象
        saveNotifyRoleTargets(policyId, triggerPolicy, channel, operator);
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public Long saveAppDefaultNotifyPoliciesToLocal(String operator, Long appId, String triggerUser,
                                                    NotifyPoliciesCreateUpdateReq createUpdateReq) {
        val policyList = createUpdateReq.getTriggerPoliciesList();
        //1.删除当前用户定义的已有个人策略
        notifyTriggerPolicyDAO.deleteAppNotifyPolicies(appId, operator);
        //2.删除当前用户定义的已有业务策略
        notifyTriggerPolicyDAO.deleteAppNotifyPolicies(appId, triggerUser);
        //3.保存notify_trigger_policy
        policyList.forEach(policy -> saveTriggerPolicy(operator, appId, triggerUser, policy));
        return (long) policyList.size();
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public Boolean saveSpecificResourceNotifyPolicies(
        Long appId,
        String operator,
        ServiceSpecificResourceNotifyPolicyDTO specificResourceNotifyPolicyDTO
    ) {

        if (specificResourceNotifyPolicyDTO.getResourceId() == -1) {
            log.error("[saveSpecificResourceNotifyPolicies] not allow to save app default resource notify policies "
                + "with resourceId=-1");
            throw new InvalidParameterException("cannot save custom notify policy with resourceId=-1");
        }

        notifyTriggerPolicyDAO.deleteAppResourceNotifyPolicies(
            appId,
            specificResourceNotifyPolicyDTO.getResourceType(),
            specificResourceNotifyPolicyDTO.getResourceId().toString()
        );

        TriggerPolicy triggerPolicy = buildTriggerPolicy(specificResourceNotifyPolicyDTO);

        List<ResourceStatusChannel> resourceStatusChannelList = ResourceStatusChannel.fromMap(
            specificResourceNotifyPolicyDTO.getResourceStatusChannelMap()
        );
        resourceStatusChannelList.forEach(resourceStatusChannel -> {
            cascadeSaveTriggerPolicyAndTarget(
                appId,
                operator,
                ResourceTypeEnum.get(specificResourceNotifyPolicyDTO.getResourceType()),
                specificResourceNotifyPolicyDTO.getResourceId().toString(),
                getDefaultTriggerUser(),
                triggerPolicy,
                resourceStatusChannel
            );
        });

        return true;
    }

    private TriggerPolicy buildTriggerPolicy(ServiceSpecificResourceNotifyPolicyDTO specificResourceNotifyPolicyDTO) {
        TriggerPolicy triggerPolicy = new TriggerPolicy();
        triggerPolicy.setTriggerType(TriggerTypeEnum.get(specificResourceNotifyPolicyDTO.getTriggerType()));
        triggerPolicy.setResourceTypeList(Collections.singletonList(ResourceTypeEnum.get(
            specificResourceNotifyPolicyDTO.getResourceType()
        )));
        triggerPolicy.setRoleList(specificResourceNotifyPolicyDTO.getRoleList());
        triggerPolicy.setExtraObserverList(specificResourceNotifyPolicyDTO.getExtraObserverList());
        triggerPolicy.setResourceStatusChannelList(
            ResourceStatusChannel.fromMap(specificResourceNotifyPolicyDTO.getResourceStatusChannelMap())
        );
        return triggerPolicy;
    }

    private String getDefaultTriggerUser() {
        // 按业务保存、触发策略，采用公共触发者
        return NotifyConsts.DEFAULT_TRIGGER_USER;
    }

    private Long tryToSavePoliciesToLocal(String username, Long appId, NotifyPoliciesCreateUpdateReq createUpdateReq) {
        String requestId = JobContextUtil.getRequestId();
        try {
            if (!LockUtils.tryGetDistributedLock(
                REDIS_KEY_SAVE_APP_DEFAULT_NOTIFY_POLICIES,
                requestId,
                5000)
            ) {
                return 0L;
            }
            return saveAppDefaultNotifyPoliciesToLocal(username, appId, getDefaultTriggerUser(), createUpdateReq);
        } catch (Throwable t) {
            log.error("Fail to saveAppDefaultNotifyPoliciesToLocal", t);
            throw t;
        } finally {
            LockUtils.releaseDistributedLock(REDIS_KEY_SAVE_APP_DEFAULT_NOTIFY_POLICIES, requestId);
        }
    }

    @Override
    public Long saveAppDefaultNotifyPolicies(
        String username,
        Long appId,
        NotifyPoliciesCreateUpdateReq createUpdateReq,
        boolean checkAuth
    ) {
        //0.1配置记录
        if (!notifyConfigStatusDAO.exist(getDefaultTriggerUser(), appId)) {
            notifyConfigStatusDAO.insertNotifyConfigStatus(getDefaultTriggerUser(), appId);
        }
        return tryToSavePoliciesToLocal(username, appId, createUpdateReq);
    }

    @Override
    public int deleteAppResourceNotifyPolicies(Long appId, Integer resourceType, String resourceId) {
        // 业务全局消息通知方式不允许删除
        if (NotifyConsts.DEFAULT_RESOURCE_ID.equals(resourceId)) {
            log.error("not allow to delete app default resource notify policies with resourceId=-1");
            throw new InvalidParameterException("cannot delete notify policy with resourceId=-1");
        }
        return notifyTriggerPolicyDAO.deleteAppResourceNotifyPolicies(appId, resourceType, resourceId);
    }

    @Override
    public List<TriggerTypeVO> listTriggerType(String username) {
        return TriggerTypeEnum.getVOList();
    }

    @Override
    public List<ResourceTypeVO> listResourceType(String username) {
        return ResourceTypeEnum.getVOList();
    }

    @Override
    public List<RoleVO> listRole(String username) {
        //Job系统角色+CMDB业务角色
        List<RoleVO> resultList = new ArrayList<>(JobRoleEnum.getVOList());
        List<AppRoleDTO> appRoles = esbAppRoleDAO.listEsbAppRole(JobContextUtil.getTenantId());
        if (CollectionUtils.isNotEmpty(appRoles)) {
            resultList.addAll(appRoles.stream().map(it ->
                new RoleVO(it.getId(), it.getName())).collect(Collectors.toList()));
        }
        return resultList;
    }

    @Override
    public List<AppRoleDTO> listRoles(String tenantId) {
        //Job系统角色+CMDB业务角色
        List<AppRoleDTO> resultList = new ArrayList<>();
        List<AppRoleDTO> appRoleDTOList = esbAppRoleDAO.listEsbAppRole(tenantId);
        if (appRoleDTOList != null) {
            resultList.addAll(appRoleDTOList);
        }
        resultList.add(new AppRoleDTO(JobRoleEnum.JOB_RESOURCE_OWNER.name(),
            I18nUtil.getI18nMessage(JobRoleEnum.JOB_RESOURCE_OWNER.getDefaultName()), null));
        resultList.add(new AppRoleDTO(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name(),
            I18nUtil.getI18nMessage(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.getDefaultName()), null));
        return resultList;
    }

    @Override
    public List<ExecuteStatusVO> listExecuteStatus(String username) {
        List<ExecuteStatusVO> executeStatusVOList = ExecuteStatusEnum.getVOList();
        executeStatusVOList =
            executeStatusVOList.stream().filter(it ->
                !(it.getCode().equals(ExecuteStatusEnum.READY.name()))).collect(Collectors.toList());
        return executeStatusVOList;
    }

    @Override
    public List<NotifyEsbChannelDTO> listAllNotifyChannel(String tenantId) {
        return notifyEsbChannelDAO.listNotifyEsbChannel(tenantId).stream()
            .filter(NotifyEsbChannelDTO::isActive).map(it -> {
                    NotifyEsbChannelDTO channel = new NotifyEsbChannelDTO();
                    channel.setType(it.getType());
                    channel.setLabel(it.getLabel());
                    return channel;
                }
            ).collect(Collectors.toList());
    }

    private List<String> getAvailableChannelTypeList(String tenantId) {

        // 懒加载当前租户下的消息通知渠道
        notifyChannelInitService.tryToInitDefaultNotifyChannelsWithSingleTenant(tenantId);

        List<AvailableEsbChannelDTO> availableEsbChannelDTOList =
            availableEsbChannelDAO.listAvailableEsbChannel(tenantId);
        return availableEsbChannelDTOList.stream().map(AvailableEsbChannelDTO::getType).collect(Collectors.toList());
    }

    @Override
    public List<NotifyChannelVO> listAvailableNotifyChannel(String username) {
        List<String> availableChannelTypeList = getAvailableChannelTypeList(JobContextUtil.getTenantId());
        String tenantId = JobContextUtil.getTenantId();
        return notifyEsbChannelDAO.listNotifyEsbChannel(tenantId).stream().map(it -> new NotifyChannelVO(
            it.getType(),
            it.getLabel()
        )).filter(it -> availableChannelTypeList.contains(it.getCode())).collect(Collectors.toList());
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public Integer setAvailableNotifyChannel(String username, SetAvailableNotifyChannelReq req) {
        String tenantId = JobContextUtil.getTenantId();
        List<String> channelCodeList =
            Arrays.asList(req.getChannelCodeStr().trim().split(NotifyConsts.SEPERATOR_COMMA));
        availableEsbChannelDAO.deleteAllChannelsByTenantId(tenantId);
        channelCodeList.forEach(it -> availableEsbChannelDAO.insertAvailableEsbChannel(
            new AvailableEsbChannelDTO(
                it,
                true,
                username,
                LocalDateTime.now(),
                tenantId)));
        return channelCodeList.size();
    }

    @Override
    public Integer sendSimpleNotification(ServiceNotificationDTO notification) {
        log.debug("Input:" + notification.toString());
        // 1.获取需要通知的渠道与人员
        Map<String, Set<String>> channelUsersMap = getChannelUsersMap(notification.getTriggerDTO());
        channelUsersMap.forEach((channel, users) ->
            log.debug(String.format("[%s]-->[%s]", channel, String.join(","
                , users))));
        // 2.调ESB接口发送通知
        val notifyMessageMap = notification.getNotificationMessageMap();
        Set<String> channelSet = notifyMessageMap.keySet();
        if (channelSet.isEmpty()) {
            return 0;
        }
        ServiceNotificationMessage notificationMessage =
            notifyMessageMap.get(new ArrayList<>(notifyMessageMap.keySet()).get(0));
        notifySendService.asyncSendNotifyMessages(
            notification.getTriggerDTO().getAppId(),
            channelUsersMap,
            notificationMessage.getTitle(),
            notificationMessage.getContent()
        );
        return channelSet.size();
    }

    private Set<String> findJobResourceOwners(Integer resourceType, String resourceIdStr) {
        Set<String> userSet = new HashSet<>();
        if (resourceType == ResourceTypeEnum.JOB.getType()) {
            long resourceId = Long.parseLong(resourceIdStr);
            userSet.add(taskPlanDAO.getTaskPlanById(resourceId).getLastModifyUser());
        } else {
            // 没有资源的任务不通知"资源所属者"
        }
        return userSet;
    }

    private Set<String> findCmdbRoleUsers(Long appId, String role) {
        // CMDB中的业务角色，获取角色对应人员
        try {
            return roleService.listAppUsersByRole(appId, role);
        } catch (Exception e) {
            log.error(String.format("Fail to fetch role users:(%d,%s)", appId, role), e);
        }
        return Collections.emptySet();
    }

    @Override
    public Set<String> findUserByResourceRoles(Long appId, String triggerUser, Integer resourceType,
                                               String resourceIdStr, Set<String> roleSet) {
        log.debug("Input:{},{},{},{},{}", appId, triggerUser, resourceType, resourceIdStr, roleSet);
        Set<String> userSet = new HashSet<>();
        for (String role : roleSet) {
            if (role.equals(JobRoleEnum.JOB_RESOURCE_OWNER.name())) {
                userSet.addAll(findJobResourceOwners(resourceType, resourceIdStr));
            } else if (role.equals(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())) {
                userSet.add(triggerUser);
            } else if (role.equals(JobRoleEnum.JOB_EXTRA_OBSERVER.name())) {
                log.debug("ignore extra observers role");
            } else {
                userSet.addAll(findCmdbRoleUsers(appId, role));
            }
        }
        return userSet;
    }

    private List<NotifyTriggerPolicyDTO> searchNotifyPolices(Long appId,
                                                             String triggerUser,
                                                             Integer triggerType,
                                                             Integer resourceType,
                                                             Integer resourceExecuteStatus) {
        FormattingTuple msg = MessageFormatter.format(
            "search policies of (triggerUser={},appId={},resourceId={}, resourceExecuteStatus={})",
            new String[]{
                NotifyConsts.DEFAULT_TRIGGER_USER,
                appId.toString(),
                NotifyConsts.DEFAULT_RESOURCE_ID,
                resourceExecuteStatus.toString()
            }
        );
        log.debug(msg.getMessage());
        return notifyTriggerPolicyDAO.list(
            triggerUser,
            appId,
            NotifyConsts.DEFAULT_RESOURCE_ID,
            resourceType,
            triggerType,
            resourceExecuteStatus
        );
    }

    private List<NotifyTriggerPolicyDTO> searchResourceNotifyPolices(Long appId,
                                                                     Integer triggerType,
                                                                     Integer resourceType,
                                                                     String resourceIdStr,
                                                                     Integer resourceExecuteStatus) {
        log.debug(
            "try to get resource custom trigger policy:|appId={}|triggerType={}"
                + "|resourceType={}|resourceIdStr={}|resourceExecuteStatus={}",
            appId,
            triggerType,
            resourceType,
            resourceIdStr,
            resourceExecuteStatus
        );
        return notifyTriggerPolicyDAO.list(
            NotifyConsts.DEFAULT_TRIGGER_USER,
            appId,
            resourceIdStr,
            resourceType,
            triggerType,
            resourceExecuteStatus
        );
    }

    private List<NotifyTriggerPolicyDTO> searchAppNotifyPolices(Long appId,
                                                                Integer triggerType,
                                                                Integer resourceType,
                                                                Integer resourceExecuteStatus) {
        return searchNotifyPolices(
            appId,
            NotifyConsts.DEFAULT_TRIGGER_USER,
            triggerType,
            resourceType,
            resourceExecuteStatus
        );
    }

    private List<NotifyTriggerPolicyDTO> systemDefaultNotifyPolices(Integer triggerType,
                                                                    Integer resourceType,
                                                                    Integer resourceExecuteStatus) {
        List<NotifyTriggerPolicyDTO> policyList = searchNotifyPolices(
            NotifyConsts.DEFAULT_APP_ID,
            NotifyConsts.DEFAULT_TRIGGER_USER,
            triggerType,
            resourceType,
            resourceExecuteStatus
        );
        if (CollectionUtils.isEmpty(policyList)) {
            log.warn("system default notify policies not configured, please check sql migration");
        }
        return policyList;
    }

    private boolean appNotifyPolicyConfigured(Long appId) {
        return notifyConfigStatusDAO.exist(getDefaultTriggerUser(), appId);
    }

    private List<NotifyTriggerPolicyDTO> getTriggerPolicys(ServiceNotificationTriggerDTO triggerDTO) {
        Long appId = triggerDTO.getAppId() == null ? NotifyConsts.DEFAULT_APP_ID : triggerDTO.getAppId();
        String triggerUser = triggerDTO.getTriggerUser();
        Integer triggerType = triggerDTO.getTriggerType();
        Integer resourceType = triggerDTO.getResourceType();
        String resourceIdStr = triggerDTO.getResourceId();
        Integer resourceExecuteStatus = triggerDTO.getResourceExecuteStatus();
        List<NotifyTriggerPolicyDTO> triggerPolicyList = Collections.emptyList();
        // 1.查找资源级别的触发策略
        if (canHaveCustomTriggerPolicy(triggerDTO)) {
            Pair<Integer, String> resource = getResourceIndeed(triggerDTO);
            Integer triggerPolicyResourceType = resource.getLeft();
            String triggerPolicyResourceId = resource.getRight();
            triggerPolicyList = searchResourceNotifyPolices(
                appId,
                triggerType,
                triggerPolicyResourceType,
                triggerPolicyResourceId,
                resourceExecuteStatus
            );
        }
        // 2.业务公共触发策略
        if (CollectionUtils.isEmpty(triggerPolicyList)) {
            log.debug(
                "not found custom trigger policy|appId={}|triggerUser={}"
                    + "|triggerType={}|resourceType={}|{}|resourceIdStr={}",
                appId,
                triggerUser,
                triggerType,
                resourceType,
                resourceIdStr,
                resourceExecuteStatus
            );
            triggerPolicyList = searchAppNotifyPolices(
                appId,
                triggerType,
                resourceType,
                resourceExecuteStatus
            );
        }
        // 3.查找当前触发者自己制定的触发策略
        if (CollectionUtils.isEmpty(triggerPolicyList)) {
            triggerPolicyList = searchNotifyPolices(
                appId,
                triggerUser,
                triggerType,
                resourceType,
                resourceExecuteStatus
            );
        }
        // 4.默认业务
        // 业务未配置消息通知策略才使用默认策略
        if (!appNotifyPolicyConfigured(appId) && CollectionUtils.isEmpty(triggerPolicyList)) {
            triggerPolicyList = systemDefaultNotifyPolices(triggerType, resourceType, resourceExecuteStatus);
        }
        return triggerPolicyList;
    }

    private boolean canHaveCustomTriggerPolicy(ServiceNotificationTriggerDTO triggerDTO) {
        Pair<Integer, String> resource = getResourceIndeed(triggerDTO);
        // 当前只有定时任务支持资源级别的自定义通知
        if (resource.getLeft() == ResourceTypeEnum.CRON.getType()) {
            return true;
        }

        return false;
    }

    private Set<String> findUserByRole(Long appId,
                                       String role,
                                       String triggerUser,
                                       Integer resourceType,
                                       String resourceId,
                                       String extraObservers) {
        if (role.equals(JobRoleEnum.JOB_RESOURCE_OWNER.name())) {
            return findJobResourceOwners(resourceType, resourceId);
        } else if (role.equals(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())) {
            return Sets.newHashSet(triggerUser);
        } else if (role.equals(JobRoleEnum.JOB_EXTRA_OBSERVER.name())) {
            if (StringUtils.isNotBlank(extraObservers)) {
                return Sets.newHashSet(extraObservers.split(NotifyConsts.SEPERATOR_COMMA));
            }
        } else {
            return findCmdbRoleUsers(appId, role);
        }
        return Collections.emptySet();
    }

    private Map<String, Set<String>> getChannelUsersMap(ServiceNotificationTriggerDTO triggerDTO) {
        Long appId = triggerDTO.getAppId() == null ? NotifyConsts.DEFAULT_APP_ID : triggerDTO.getAppId();
        String tenantId = tenantService.getTenantIdByAppId(appId);
        String triggerUser = triggerDTO.getTriggerUser();
        Integer resourceType = triggerDTO.getResourceType();
        List<NotifyTriggerPolicyDTO> triggerPolicyList = getTriggerPolicys(triggerDTO);
        if (CollectionUtils.isEmpty(triggerPolicyList)) {
            return Collections.emptyMap();
        }
        // 根据策略查找通知角色，找出对象通知渠道、去重
        Map<String, Set<String>> channelUsersMap = new HashMap<>();
        for (NotifyTriggerPolicyDTO policy : triggerPolicyList) {
            List<NotifyPolicyRoleTargetDTO> roleTargetList =
                notifyPolicyRoleTargetDAO.listByPolicyId(policy.getId());
            for (NotifyPolicyRoleTargetDTO roleTarget : roleTargetList) {
                String role = roleTarget.getRole();
                List<NotifyRoleTargetChannelDTO> roleTargetChannelList =
                    notifyRoleTargetChannelDAO.listByRoleTargetId(roleTarget.getId());
                Set<String> channels =
                    roleTargetChannelList.stream()
                        .map(NotifyRoleTargetChannelDTO::getChannel).collect(Collectors.toSet());
                Set<String> userSet = findUserByRole(
                    appId,
                    role,
                    triggerUser,
                    resourceType,
                    triggerDTO.getResourceId(),
                    roleTarget.getExtraObservers()
                );
                channels.forEach(channel -> addChannelUsersToMap(channelUsersMap, channel, userSet));
            }
        }
        // 过滤通知黑名单
        channelUsersMap.keySet().forEach(key ->
            channelUsersMap.put(key, notifyUserService.filterBlackUser(channelUsersMap.get(key), tenantId))
        );
        return channelUsersMap;
    }

    /**
     * 是定时任务的话，返回定时任务ID而非执行方案ID
     */
    private Pair<Integer, String> getResourceIndeed(ServiceNotificationTriggerDTO triggerDTO) {
        if (triggerDTO.getCronTaskId() != null && triggerDTO.getCronTaskId() != -1L) {
            return Pair.of(ResourceTypeEnum.CRON.getType(), String.valueOf(triggerDTO.getCronTaskId()));
        }

        return Pair.of(triggerDTO.getResourceType(), triggerDTO.getResourceId());
    }

    @Override
    public PageTemplateVO getPageTemplate(String username) {
        return new PageTemplateVO(
            listTriggerType(username),
            listResourceType(username),
            listRole(username),
            listExecuteStatus(username),
            listAvailableNotifyChannel(username)
        );
    }

    private Set<String> findUserByRole(Long appId,
                                       String triggerUser,
                                       Integer resourceType,
                                       String resourceId,
                                       Collection<String> roleCodes) {
        Set<String> userSet = new HashSet<>();
        if (CollectionUtils.isEmpty(roleCodes)) {
            return userSet;
        }
        for (String role : roleCodes) {
            userSet.addAll(findUserByRole(appId, role, triggerUser, resourceType, resourceId, null));
        }
        return userSet;
    }

    @Override
    public Integer sendTemplateNotification(ServiceTemplateNotificationDTO templateNotificationDTO) {
        Long appId = templateNotificationDTO.getAppId();
        String tenantId = tenantService.getTenantIdByAppId(appId);
        Map<String, NotifyTemplateDTO> channelTemplateMap = notifyTemplateService.getChannelTemplateMap(
            templateNotificationDTO.getTemplateCode(),
            tenantId
        );
        //获取通知用户
        Set<String> userSet = new HashSet<>();
        UserRoleInfoDTO receiverInfo = templateNotificationDTO.getReceiverInfo();
        userSet.addAll(receiverInfo.getUserList());
        userSet.addAll(findUserByRole(templateNotificationDTO.getAppId(), templateNotificationDTO.getTriggerUser(),
            templateNotificationDTO.getResourceType(), templateNotificationDTO.getResourceId(),
            receiverInfo.getRoleList()));
        //过滤黑名单用户
        userSet = notifyUserService.filterBlackUser(userSet, tenantId);
        //获取可用通知渠道
        Set<String> availableChannelSet = new HashSet<>(getAvailableChannelTypeList(tenantId));
        //与激活通知渠道取交集
        Set<String> validChannelSet = Sets.intersection(new HashSet<>(templateNotificationDTO.getActiveChannels()),
            availableChannelSet);
        for (String channel : validChannelSet) {
            //取得Title与Content模板
            if (channelTemplateMap.containsKey(channel)) {
                NotifyTemplateDTO templateDTO = channelTemplateMap.get(channel);
                //变量替换
                Map<String, String> variablesMap = templateNotificationDTO.getVariablesMap();
                ServiceNotificationMessage notifyMsg = notifyTemplateService.getNotificationMessageFromTemplate(
                    appId,
                    templateDTO,
                    variablesMap
                );
                //发送消息通知
                if (notifyMsg != null) {
                    notifySendService.asyncSendUserChannelNotify(
                        appId,
                        userSet,
                        channel,
                        notifyMsg.getTitle(),
                        notifyMsg.getContent()
                    );
                } else {
                    log.warn(
                        "Fail to get notifyMsg from template of templateCode:{},channel:{}, ignore",
                        templateNotificationDTO.getTemplateCode(),
                        channel
                    );
                }
            } else {
                log.warn("No templates found for channel:{}", channel);
            }
        }
        return userSet.size();
    }

    @Override
    public Integer triggerTemplateNotification(ServiceTriggerTemplateNotificationDTO triggerTemplateNotification) {
        Long appId = triggerTemplateNotification.getTriggerDTO().getAppId();
        String tenantId = tenantService.getTenantIdByAppId(appId);
        // 1.获取所有可用渠道
        StopWatch watch = new StopWatch();
        watch.start("getAvailableChannelTypeList");
        List<String> availableChannelTypeList = getAvailableChannelTypeList(tenantId);
        watch.stop();
        if (watch.getLastTaskTimeMillis() > 500) {
            log.warn(PrefConsts.TAG_PREF_SLOW + watch.prettyPrint());
        }
        watch.start("getChannelUsersMap");
        Map<String, Set<String>> channelUsersMap = getChannelUsersMap(triggerTemplateNotification.getTriggerDTO());
        watch.stop();
        if (watch.getLastTaskTimeMillis() > 500) {
            log.warn(PrefConsts.TAG_PREF_SLOW + watch.prettyPrint());
        }
        Counter counter = new Counter();
        channelUsersMap.forEach((channel, userSet) -> {
            if (!availableChannelTypeList.contains(channel)) {
                log.error(
                    String.format("channel %s of tenant %s is not available, not notified, please contact admin",
                        channel,
                        tenantId
                    )
                );
            } else {
                String templateCode = triggerTemplateNotification.getTemplateCode();
                Map<String, String> variablesMap = triggerTemplateNotification.getVariablesMap();
                watch.start("getNotificationMessage_" + channel);
                ServiceNotificationMessage notificationMessage = notifyTemplateService.getNotificationMessage(
                    appId,
                    templateCode,
                    channel,
                    variablesMap
                );
                watch.stop();
                if (watch.getLastTaskTimeMillis() > 500) {
                    log.warn(PrefConsts.TAG_PREF_SLOW + watch.prettyPrint());
                }
                if (notificationMessage != null) {
                    notifySendService.asyncSendUserChannelNotify(
                        appId,
                        userSet,
                        channel,
                        notificationMessage.getTitle(),
                        notificationMessage.getContent()
                    );
                    counter.addOne();
                } else {
                    log.warn("Cannot find template of templateCode:{},channel:{}, ignore", templateCode, channel);
                }
            }
        });
        return counter.getValue().intValue();
    }

    private void addChannelUsersToMap(Map<String, Set<String>> channelUsersMap, String channel, Set<String> userSet) {
        if (null == userSet || userSet.isEmpty()) {
            return;
        }
        if (channelUsersMap.containsKey(channel)) {
            channelUsersMap.get(channel).addAll(userSet);
        } else {
            channelUsersMap.put(channel, userSet);
        }
    }
}
