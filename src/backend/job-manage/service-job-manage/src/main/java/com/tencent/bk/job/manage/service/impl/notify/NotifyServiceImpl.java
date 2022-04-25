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

package com.tencent.bk.job.manage.service.impl.notify;

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.model.vo.NotifyChannelVO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.Counter;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PrefConsts;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TypeUtil;
import com.tencent.bk.job.manage.common.consts.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.common.consts.notify.JobRoleEnum;
import com.tencent.bk.job.manage.common.consts.notify.NotifyConsts;
import com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.common.consts.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskPlanTypeEnum;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.EsbAppRoleDAO;
import com.tencent.bk.job.manage.dao.notify.EsbUserInfoDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyBlackUserInfoDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyConfigStatusDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyPolicyRoleTargetDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyRoleTargetChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTemplateDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTriggerPolicyDAO;
import com.tencent.bk.job.manage.dao.plan.TaskPlanDAO;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.EsbUserInfoDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyBlackUserInfoDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyPolicyRoleTargetDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyRoleTargetChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTemplateDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTriggerPolicyDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationTriggerDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTriggerTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceUserNotificationDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyPoliciesCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.notify.SetAvailableNotifyChannelReq;
import com.tencent.bk.job.manage.model.web.vo.notify.ExecuteStatusVO;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;
import com.tencent.bk.job.manage.model.web.vo.notify.PageTemplateVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ResourceTypeVO;
import com.tencent.bk.job.manage.model.web.vo.notify.RoleVO;
import com.tencent.bk.job.manage.model.web.vo.notify.TriggerPolicyVO;
import com.tencent.bk.job.manage.model.web.vo.notify.TriggerTypeVO;
import com.tencent.bk.job.manage.model.web.vo.notify.UserVO;
import com.tencent.bk.job.manage.service.AppRoleService;
import com.tencent.bk.job.manage.service.LocalPermissionService;
import com.tencent.bk.job.manage.service.NotifyService;
import com.tencent.bk.job.manage.service.PaaSService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    private static final String REDIS_KEY_SAVE_APP_DEFAULT_NOTIFY_POLICIES = "NotifyServiceImpl" +
        ".saveAppDefaultNotifyPolicies";
    private static final Logger logger = LoggerFactory.getLogger(NotifyServiceImpl.class);
    //发通知专用线程池
    private final ThreadPoolExecutor notificationThreadPoolExecutor = new ThreadPoolExecutor(
        5, 30, 60L,
        TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));

    private DSLContext dslContext;
    private NotifyTriggerPolicyDAO notifyTriggerPolicyDAO;
    private NotifyRoleTargetChannelDAO notifyRoleTargetChannelDAO;
    private NotifyPolicyRoleTargetDAO notifyPolicyRoleTargetDAO;
    private EsbUserInfoDAO esbUserInfoDAO;
    private EsbAppRoleDAO esbAppRoleDAO;
    private AvailableEsbChannelDAO availableEsbChannelDAO;
    private NotifyEsbChannelDAO notifyEsbChannelDAO;
    private NotifyBlackUserInfoDAO notifyBlackUserInfoDAO;
    private LocalPermissionService localPermissionService;
    private NotifyConfigStatusDAO notifyConfigStatusDAO;
    private NotifyTemplateDAO notifyTemplateDAO;
    private ScriptDAO scriptDAO;
    private TaskPlanDAO taskPlanDAO;
    private ApplicationDAO applicationDAO;
    private JobManageConfig jobManageConfig;
    private PaaSService paaSService;
    private AppRoleService roleService;

    @Autowired
    public NotifyServiceImpl(
        @Qualifier("job-manage-dsl-context")
            DSLContext dslContext,
        NotifyTriggerPolicyDAO notifyTriggerPolicyDAO,
        NotifyPolicyRoleTargetDAO notifyPolicyRoleTargetDAO,
        NotifyRoleTargetChannelDAO notifyRoleTargetChannelDAO,
        EsbUserInfoDAO esbUserInfoDAO,
        EsbAppRoleDAO esbAppRoleDAO,
        AvailableEsbChannelDAO availableEsbChannelDAO,
        NotifyEsbChannelDAO notifyEsbChannelDAO,
        NotifyBlackUserInfoDAO notifyBlackUserInfoDAO,
        LocalPermissionService localPermissionService,
        PaaSService paaSService,
        AppRoleService roleService,
        NotifyConfigStatusDAO notifyConfigStatusDAO,
        NotifyTemplateDAO notifyTemplateDAO,
        ScriptDAO scriptDAO,
        TaskPlanDAO taskPlanDAO,
        ApplicationDAO applicationDAO,
        JobManageConfig jobManageConfig,
        MeterRegistry meterRegistry
    ) {
        this.dslContext = dslContext;
        this.notifyTriggerPolicyDAO = notifyTriggerPolicyDAO;
        this.notifyPolicyRoleTargetDAO = notifyPolicyRoleTargetDAO;
        this.notifyRoleTargetChannelDAO = notifyRoleTargetChannelDAO;
        this.esbUserInfoDAO = esbUserInfoDAO;
        this.esbAppRoleDAO = esbAppRoleDAO;
        this.availableEsbChannelDAO = availableEsbChannelDAO;
        this.notifyEsbChannelDAO = notifyEsbChannelDAO;
        this.notifyBlackUserInfoDAO = notifyBlackUserInfoDAO;
        this.localPermissionService = localPermissionService;
        this.paaSService = paaSService;
        this.roleService = roleService;
        this.notifyConfigStatusDAO = notifyConfigStatusDAO;
        this.notifyTemplateDAO = notifyTemplateDAO;
        this.scriptDAO = scriptDAO;
        this.taskPlanDAO = taskPlanDAO;
        this.applicationDAO = applicationDAO;
        this.jobManageConfig = jobManageConfig;
        meterRegistry.gauge(
            MetricsConstants.NAME_NOTIFY_POOL_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_MODULE, MetricsConstants.VALUE_MODULE_NOTIFY)),
            notificationThreadPoolExecutor,
            ThreadPoolExecutor::getPoolSize
        );
        meterRegistry.gauge(
            MetricsConstants.NAME_NOTIFY_QUEUE_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_MODULE, MetricsConstants.VALUE_MODULE_NOTIFY)),
            notificationThreadPoolExecutor,
            threadPoolExecutor -> threadPoolExecutor.getQueue().size()
        );
    }

    @Override
    public List<TriggerPolicyVO> listAppDefaultNotifyPolicies(String username, Long appId) {
        return notifyTriggerPolicyDAO.list(dslContext, getTriggerUser(username), appId,
            NotifyConsts.DEFAULT_RESOURCE_ID);
    }

    @Override
    public Long saveAppDefaultNotifyPolicies(
        String username,
        Long appId,
        NotifyPoliciesCreateUpdateReq createUpdateReq
    ) {
        // 外部调用默认鉴权
        return saveAppDefaultNotifyPolicies(username, appId, createUpdateReq, true);
    }

    @Override
    public Long saveAppDefaultNotifyPoliciesToLocal(String operator, Long appId, String triggerUser,
                                                    NotifyPoliciesCreateUpdateReq createUpdateReq) {
        val policyList = createUpdateReq.getTriggerPoliciesList();
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            //1.删除当前用户定义的已有个人策略
            notifyTriggerPolicyDAO.deleteAppNotifyPolicies(context, appId, operator);
            //1.删除当前用户定义的已有业务策略
            notifyTriggerPolicyDAO.deleteAppNotifyPolicies(context, appId, triggerUser);
            //2.保存notify_trigger_policy
            policyList.forEach(triggerPolicy -> {
                new HashSet<>(triggerPolicy.getResourceTypeList()).forEach(resourceTypeCode -> {
                    new HashSet<>(triggerPolicy.getResourceStatusChannelList()).forEach(resourceStatusChannel -> {
                        //保存触发策略
                        val policyId = notifyTriggerPolicyDAO.insertNotifyTriggerPolicy(context,
                            new NotifyTriggerPolicyDTO(
                                null,
                                appId,
                                NotifyConsts.DEFAULT_RESOURCE_ID,
                                resourceTypeCode,
                                triggerUser,
                                triggerPolicy.getTriggerType(),
                                resourceStatusChannel.getExecuteStatus(),
                                operator,
                                System.currentTimeMillis(),
                                operator,
                                System.currentTimeMillis()
                            ));
                        //保存所有通知对象
                        new HashSet<>(triggerPolicy.getRoleList()).forEach(appRole -> {
                            Long roleTargetId = -1L;
                            if (null != appRole && appRole.equals(JobRoleEnum.JOB_EXTRA_OBSERVER.name())) {
                                String extraObserverStr = null;
                                if (triggerPolicy.getExtraObserverList() != null) {
                                    Set<String> extraObserverSet =
                                        Sets.newHashSet(triggerPolicy.getExtraObserverList());
                                    triggerPolicy.getExtraObserverList().forEach(extraObserver -> {
                                        if (StringUtils.isBlank(extraObserver)) {
                                            extraObserverSet.remove(extraObserver);
                                        }
                                    });
                                    extraObserverStr = String.join(NotifyConsts.SEPERATOR_COMMA, extraObserverSet);
                                }
                                //额外通知对象添加额外通知者字段
                                roleTargetId = notifyPolicyRoleTargetDAO.insert(context,
                                    new NotifyPolicyRoleTargetDTO(
                                        null,
                                        policyId,
                                        appRole,
                                        true,
                                        extraObserverStr,
                                        operator,
                                        System.currentTimeMillis(),
                                        operator,
                                        System.currentTimeMillis()
                                    ));
                            } else {
                                roleTargetId = notifyPolicyRoleTargetDAO.insert(context,
                                    new NotifyPolicyRoleTargetDTO(
                                        null,
                                        policyId,
                                        appRole,
                                        true,
                                        null,
                                        operator,
                                        System.currentTimeMillis(),
                                        operator,
                                        System.currentTimeMillis()
                                    ));
                            }
                            final Long finalRoleTargetId = roleTargetId;
                            //为每一个通知对象保存所有channel
                            new HashSet<>(resourceStatusChannel.getChannelList()).forEach(channel -> {
                                val roleTargetChannelId =
                                    notifyRoleTargetChannelDAO.insert(context,
                                        new NotifyRoleTargetChannelDTO(
                                            null,
                                            finalRoleTargetId,
                                            channel,
                                            operator,
                                            System.currentTimeMillis(),
                                            operator,
                                            System.currentTimeMillis()
                                        ));
                            });
                        });
                    });
                });
            });
        });
        return (long) policyList.size();
    }

    private String getTriggerUser(String operator) {
        // 按业务保存、触发策略，采用公共触发者
        return NotifyConsts.DEFAULT_TRIGGER_USER;
    }

    @Override
    public Long saveAppDefaultNotifyPolicies(
        String username,
        Long appId,
        NotifyPoliciesCreateUpdateReq createUpdateReq,
        boolean checkAuth
    ) {
        String requestId = JobContextUtil.getRequestId();
        //0.1配置记录
        if (!notifyConfigStatusDAO.exist(dslContext, getTriggerUser(username), appId)) {
            notifyConfigStatusDAO.insertNotifyConfigStatus(dslContext, getTriggerUser(username), appId);
        }
        try {
            if (!LockUtils.tryGetDistributedLock(
                REDIS_KEY_SAVE_APP_DEFAULT_NOTIFY_POLICIES,
                requestId,
                5000)
            ) {
                return 0L;
            }
            return saveAppDefaultNotifyPoliciesToLocal(username, appId, getTriggerUser(username), createUpdateReq);
        } catch (Throwable t) {
            log.error("Fail to saveAppDefaultNotifyPolicies", t);
            throw t;
        } finally {
            LockUtils.releaseDistributedLock(REDIS_KEY_SAVE_APP_DEFAULT_NOTIFY_POLICIES, requestId);
        }
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
        List<AppRoleDTO> appRoles = esbAppRoleDAO.listEsbAppRole(dslContext);
        if (CollectionUtils.isEmpty(appRoles)) {
            return Collections.emptyList();
        }
        List<RoleVO> resultList = new ArrayList<>();
        resultList.addAll(JobRoleEnum.getVOList());
        resultList.addAll(appRoles.stream().map(it ->
            new RoleVO(it.getId(), it.getName())).collect(Collectors.toList()));
        return resultList;
    }

    @Override
    public List<AppRoleDTO> listRoles() {
        //Job系统角色+CMDB业务角色
        List<AppRoleDTO> resultList = new ArrayList<>();
        List<AppRoleDTO> appRoleDTOList = esbAppRoleDAO.listEsbAppRole(dslContext);
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
    public List<NotifyEsbChannelDTO> listAllNotifyChannel() {
        return notifyEsbChannelDAO.listNotifyEsbChannel(dslContext).stream()
            .filter(NotifyEsbChannelDTO::isActive).map(it -> {
                    NotifyEsbChannelDTO channel = new NotifyEsbChannelDTO();
                    channel.setType(it.getType());
                    channel.setLabel(it.getLabel());
                    return channel;
                }
            ).collect(Collectors.toList());
    }

    private List<String> getAvailableChannelTypeList() {
        List<AvailableEsbChannelDTO> availableEsbChannelDTOList =
            availableEsbChannelDAO.listAvailableEsbChannel(dslContext);
        return availableEsbChannelDTOList.stream().map(AvailableEsbChannelDTO::getType).collect(Collectors.toList());
    }

    @Override
    public List<NotifyChannelVO> listAvailableNotifyChannel(String username) {
        List<String> availableChannelTypeList = getAvailableChannelTypeList();
        return notifyEsbChannelDAO.listNotifyEsbChannel(dslContext).stream().map(it -> new NotifyChannelVO(
            it.getType(),
            it.getLabel()
        )).filter(it -> availableChannelTypeList.contains(it.getCode())).collect(Collectors.toList());
    }

    @Override
    public Integer setAvailableNotifyChannel(String username, SetAvailableNotifyChannelReq req) {
        List<String> channelCodeList =
            Arrays.asList(req.getChannelCodeStr().trim().split(NotifyConsts.SEPERATOR_COMMA));
        dslContext.transaction(configuration -> {
            availableEsbChannelDAO.deleteAll(dslContext);
            channelCodeList.forEach(it -> availableEsbChannelDAO.insertAvailableEsbChannel(dslContext,
                new AvailableEsbChannelDTO(it, true, username, LocalDateTime.now())));

        });
        return channelCodeList.size();
    }

    @Override
    public List<UserVO> listUsers(
        String username,
        String prefixStr,
        Long offset,
        Long limit,
        Boolean excludeBlackUsers
    ) {
        if (null == prefixStr) {
            prefixStr = "";
        }
        if (null == offset || offset < 0) {
            offset = 0L;
        }
        if (null == limit || limit <= 0) {
            limit = -1L;
        }
        List<EsbUserInfoDTO> esbUserInfoDTOList;
        // 从数据库查
        if (prefixStr.contains(NotifyConsts.SEPERATOR_COMMA)) {
            // 前端回显，传全量
            List<String> userNames = Arrays.asList(prefixStr.split(NotifyConsts.SEPERATOR_COMMA));
            while (userNames.contains("")) {
                userNames.remove("");
            }
            esbUserInfoDTOList = esbUserInfoDAO.listEsbUserInfo(userNames, -1L);
        } else {
            esbUserInfoDTOList = esbUserInfoDAO.listEsbUserInfo(prefixStr, -1L);
        }
        if (esbUserInfoDTOList == null) {
            return new ArrayList<>();
        }
        List<UserVO> userVOList = esbUserInfoDTOList.stream().map(it -> new UserVO(it.getUsername(),
            it.getDisplayName(), it.getLogo(), true)).collect(Collectors.toList());
        if (excludeBlackUsers) {
            //过滤黑名单内用户
            Set<String> blackUserSet =
                notifyBlackUserInfoDAO.listNotifyBlackUserInfo(dslContext).stream()
                    .map(NotifyBlackUserInfoDTO::getUsername).collect(Collectors.toSet());
            logger.debug(String.format("listUsers:blackUserSet:%s", String.join(",", blackUserSet)));
            userVOList = userVOList.stream().map(it -> {
                if (blackUserSet.contains(it.getEnglishName())) {
                    it.setEnable(false);
                }
                return it;
            }).collect(Collectors.toList());
        }
        if (offset >= userVOList.size()) {
            return new ArrayList<>();
        }
        if (limit != -1L) {
            Long stopIndex = offset + limit;
            if (stopIndex >= userVOList.size()) {
                stopIndex = (long) userVOList.size();
            }
            return userVOList.subList(offset.intValue(), stopIndex.intValue());
        } else {
            return userVOList.subList(offset.intValue(), userVOList.size());
        }
    }

    @Override
    public List<NotifyBlackUserInfoVO> listNotifyBlackUsers(String username, Integer start, Integer pageSize) {
        return notifyBlackUserInfoDAO.listNotifyBlackUserInfo(dslContext, start, pageSize);
    }

    @Override
    public List<String> saveNotifyBlackUsers(String username, NotifyBlackUsersReq req) {
        val resultList = new ArrayList<String>();
        String[] users = req.getUsersStr().split(NotifyConsts.SEPERATOR_COMMA);
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            notifyBlackUserInfoDAO.deleteAllNotifyBlackUser(context);
            for (String user : users) {
                if ("".equals(user.replace(" ", ""))) {
                    continue;
                }
                notifyBlackUserInfoDAO.insertNotifyBlackUserInfo(context, new NotifyBlackUserInfoDTO(
                    null,
                    user,
                    username,
                    System.currentTimeMillis()
                ));
                resultList.add(user);
            }
        });
        return resultList;
    }

    @Override
    public Integer sendSimpleNotification(ServiceNotificationDTO notification) {
        logger.debug("Input:" + notification.toString());
        // 1.获取需要通知的渠道与人员
        Map<String, Set<String>> channelUsersMap = getChannelUsersMap(notification.getTriggerDTO());
        channelUsersMap.forEach((channel, users) ->
            logger.debug(String.format("[%s]-->[%s]", channel, String.join(","
                , users))));
        // 2.调ESB接口发送通知
        val notifyMessageMap = notification.getNotificationMessageMap();
        Set<String> channelSet = notifyMessageMap.keySet();
        if (channelSet.size() == 0) {
            return 0;
        }
        ServiceNotificationMessage notificationMessage =
            notifyMessageMap.get(new ArrayList<>(notifyMessageMap.keySet()).get(0));
        sendNotifyMessages(channelUsersMap, notificationMessage.getTitle(), notificationMessage.getContent());
        return channelSet.size();
    }

    @Override
    public Set<String> findUserByResourceRoles(Long appId, String triggerUser, Integer resourceType,
                                               String resourceIdStr, Set<String> roleSet) {
        logger.debug("Input:{},{},{},{},{}", appId, triggerUser, resourceType, resourceIdStr, roleSet);
        Set<String> userSet = new HashSet<>();
        for (String role : roleSet) {
            if (role.equals(JobRoleEnum.JOB_RESOURCE_OWNER.name())) {
                if (resourceType == ResourceTypeEnum.SCRIPT.getType()) {
                    userSet.add(scriptDAO.getScriptByScriptId(resourceIdStr).getLastModifyUser());
                } else if (resourceType == ResourceTypeEnum.JOB.getType()) {
                    long resourceId = -1L;
                    try {
                        resourceId = Long.parseLong(resourceIdStr);
                    } catch (Exception e) {
                        logger.warn("fail to parse resourceId to long", e);
                    }
                    if (resourceId > 0) {
                        userSet.add(
                            taskPlanDAO.getTaskPlanById(appId, -1L, resourceId, TaskPlanTypeEnum.NORMAL)
                                .getLastModifyUser()
                        );
                    }
                } else {
                    logger.warn("Unknown resourceType:{}", resourceType);
                }
            } else if (role.equals(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())) {
                if (!StringUtils.isBlank(triggerUser)) {
                    userSet.add(triggerUser);
                }
            } else if (role.equals(JobRoleEnum.JOB_EXTRA_OBSERVER.name())) {
                //忽略
            } else {
                // CMDB中的业务角色，获取角色对应人员
                try {
                    userSet.addAll(roleService.listAppUsersByRole(appId, role));
                } catch (Exception e) {
                    logger.error(String.format("Fail to fetch role users:(%d,%s)", appId, role), e);
                }
            }
        }
        return userSet;
    }

    private Map<String, Set<String>> getChannelUsersMap(ServiceNotificationTriggerDTO triggerDTO) {
        Long appId = triggerDTO.getAppId();
        if (null == appId) {
            appId = NotifyConsts.DEFAULT_APP_ID;
        }
        final Long finalAppId = appId;
        String triggerUser = triggerDTO.getTriggerUser();
        Integer triggerType = triggerDTO.getTriggerType();
        Integer resourceType = triggerDTO.getResourceType();
        Integer resourceExecuteStatus = triggerDTO.getResourceExecuteStatus();
        // 1.业务公共触发策略
        List<NotifyTriggerPolicyDTO> notifyTriggerPolicyDTOList = notifyTriggerPolicyDAO.list(dslContext,
            NotifyConsts.DEFAULT_TRIGGER_USER, appId, NotifyConsts.DEFAULT_RESOURCE_ID, resourceType, triggerType,
            resourceExecuteStatus);
        logger.debug(String.format("search application policies of (triggerUser=%s,appId=%d,resourceId=%s," +
                "resourceExecuteStatus=%d)", NotifyConsts.DEFAULT_TRIGGER_USER, appId, NotifyConsts.DEFAULT_RESOURCE_ID,
            resourceExecuteStatus));
        // 2.查找当前触发者自己制定的触发策略
        if (null == notifyTriggerPolicyDTOList || notifyTriggerPolicyDTOList.isEmpty()) {
            notifyTriggerPolicyDTOList = notifyTriggerPolicyDAO.list(dslContext, triggerUser, appId,
                NotifyConsts.DEFAULT_RESOURCE_ID, resourceType, triggerType, resourceExecuteStatus);
            logger.debug(String.format("search default policies of (triggerUser=%s,appId=%d,resourceId=%s," +
                    "resourceExecuteStatus=%d)", triggerUser, appId, NotifyConsts.DEFAULT_RESOURCE_ID,
                resourceExecuteStatus));
        }
        // 3.默认业务
        // 业务未配置消息通知策略才使用默认策略
        if (!notifyConfigStatusDAO.exist(dslContext, getTriggerUser(triggerUser), appId)) {
            if (null == notifyTriggerPolicyDTOList || notifyTriggerPolicyDTOList.isEmpty()) {
                notifyTriggerPolicyDTOList = notifyTriggerPolicyDAO.list(dslContext,
                    NotifyConsts.DEFAULT_TRIGGER_USER, NotifyConsts.DEFAULT_APP_ID, NotifyConsts.DEFAULT_RESOURCE_ID,
                    resourceType, triggerType, resourceExecuteStatus);
                logger.debug(String.format("search default policies of (triggerUser=%s,appId=%d,resourceId=%s," +
                        "resourceExecuteStatus=%d)", NotifyConsts.DEFAULT_TRIGGER_USER, NotifyConsts.DEFAULT_APP_ID,
                    NotifyConsts.DEFAULT_RESOURCE_ID, resourceExecuteStatus));
            }
            // 4.默认策略未配置
            if (null == notifyTriggerPolicyDTOList || notifyTriggerPolicyDTOList.isEmpty()) {
                logger.debug("Do not sendNotification because default notify policies not triggered");
            }
        }
        if (notifyTriggerPolicyDTOList == null) {
            notifyTriggerPolicyDTOList = Collections.emptyList();
        }
        // 5.根据策略查找通知角色，找出对象通知渠道、去重
        Map<String, Set<String>> channelUsersMap = new HashMap<>();
        notifyTriggerPolicyDTOList.forEach(policy -> {
            List<NotifyPolicyRoleTargetDTO> notifyPolicyRoleTargetDTOList =
                notifyPolicyRoleTargetDAO.listByPolicyId(dslContext, policy.getId());
            notifyPolicyRoleTargetDTOList.forEach(roleTargetDTO -> {
                String role = roleTargetDTO.getRole();
                List<NotifyRoleTargetChannelDTO> notifyRoleTargetChannelDTOList =
                    notifyRoleTargetChannelDAO.listByRoleTargetId(dslContext,
                        roleTargetDTO.getId());
                Set<String> channels =
                    notifyRoleTargetChannelDTOList.stream()
                        .map(NotifyRoleTargetChannelDTO::getChannel).collect(Collectors.toSet());
                // Job内部角色
                if (role.equals(JobRoleEnum.JOB_RESOURCE_OWNER.name())) {
                    Set<String> userSet = new HashSet<>();
                    if (resourceType == ResourceTypeEnum.JOB.getType()) {
                        // 只有执行计划支持资源所属者角色
                        long resourceIdLongValue = -1L;
                        try {
                            resourceIdLongValue = Long.parseLong(triggerDTO.getResourceId());
                        } catch (Exception e) {
                            logger.warn("fail to parse resourceId to long", e);
                        }
                        if (resourceIdLongValue > 0) {
                            userSet.add(
                                taskPlanDAO.getTaskPlanById(
                                    triggerDTO.getAppId(),
                                    -1L,
                                    resourceIdLongValue,
                                    TaskPlanTypeEnum.NORMAL
                                ).getLastModifyUser()
                            );
                        }
                    }
                    channels.forEach(channel -> addChannelUsersToMap(channelUsersMap, channel, userSet));
                } else if (role.equals(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())) {
                    channels.forEach(channel -> addChannelUsersToMap(channelUsersMap, channel,
                        Sets.newHashSet(triggerUser)));
                } else if (role.equals(JobRoleEnum.JOB_EXTRA_OBSERVER.name())) {
                    Set<String> extraObservers =
                        Sets.newHashSet(roleTargetDTO.getExtraObservers().split(NotifyConsts.SEPERATOR_COMMA));
                    channels.forEach(channel -> addChannelUsersToMap(channelUsersMap, channel, extraObservers));
                } else {
                    // CMDB中的业务角色，获取角色对应人员
                    Set<String> userSet = Sets.newHashSet();
                    try {
                        userSet = roleService.listAppUsersByRole(finalAppId, role);
                    } catch (Exception e) {
                        logger.error(String.format("Fail to fetch role users:(%d,%s)", finalAppId, role), e);
                    }
                    final Set<String> finalUserSet = userSet;
                    channels.forEach(channel -> addChannelUsersToMap(channelUsersMap, channel, finalUserSet));
                }
            });
        });
        // 6.过滤通知黑名单
        channelUsersMap.keySet().forEach(key -> {
            channelUsersMap.put(key, filterBlackUser(channelUsersMap.get(key)));
        });
        return channelUsersMap;
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

    @Override
    public Integer sendNotificationsToUsers(ServiceUserNotificationDTO serviceUserNotificationDTO) {
        // 1.获取所有可用渠道
        List<String> availableChannelTypeList = getAvailableChannelTypeList();
        return sendNotificationsToUsersByChannel(serviceUserNotificationDTO, availableChannelTypeList);
    }

    @Override
    public Integer sendNotificationsToUsersByChannel(ServiceUserNotificationDTO serviceUserNotificationDTO,
                                                     List<String> channelTypeList) {
        // 2.组装通知map
        Map<String, Set<String>> channelUsersMap = new HashMap<>();
        for (String channelType : channelTypeList) {
            channelUsersMap.put(channelType, serviceUserNotificationDTO.getReceivers());
        }
        ServiceNotificationMessage notificationMessage = serviceUserNotificationDTO.getNotificationMessage();
        sendNotifyMessages(channelUsersMap, notificationMessage.getTitle(), notificationMessage.getContent());
        return serviceUserNotificationDTO.getReceivers().size();
    }

    @Override
    public Integer sendNotificationsToAdministrators(ServiceNotificationMessage serviceNotificationMessage) {
        List<String> administrators = localPermissionService.getAdministrators();
        return sendNotificationsToUsers(new ServiceUserNotificationDTO(new HashSet<>(administrators),
            serviceNotificationMessage));
    }

    private Set<String> findUserByRole(Long appId, String triggerUser, Integer resourceType, String resourceId,
                                       Collection<String> roleCodes) {
        Set<String> userSet = new HashSet<>();
        if (CollectionUtils.isEmpty(roleCodes)) {
            return userSet;
        }
        for (String role : roleCodes) {
            // Job内部角色
            if (role.equals(JobRoleEnum.JOB_RESOURCE_OWNER.name())) {
                if (resourceType != null) {
                    ResourceTypeEnum resourceTypeEnum = ResourceTypeEnum.get(resourceType);
                    if (resourceTypeEnum != null) {
                        switch (resourceTypeEnum) {
                            case JOB:
                                long resourceIdLongValue = -1L;
                                try {
                                    resourceIdLongValue = Long.parseLong(resourceId);
                                } catch (Exception e) {
                                    logger.warn("fail to parse resourceId to long", e);
                                }
                                if (resourceIdLongValue > 0) {
                                    userSet.add(
                                        taskPlanDAO.getTaskPlanById(
                                            appId,
                                            -1L,
                                            resourceIdLongValue,
                                            TaskPlanTypeEnum.NORMAL
                                        ).getLastModifyUser());
                                }
                                break;
                            case SCRIPT:
                                userSet.add(scriptDAO.getScriptByScriptId(resourceId).getLastModifyUser());
                                break;
                        }
                    } else {
                        logger.warn("Unknown resourceType:{}", resourceType);
                    }
                } else {
                    logger.warn("No resourceType in request");
                }
            } else if (role.equals(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())) {
                userSet.add(triggerUser);
            } else {
                // CMDB中的业务角色，获取角色对应人员
                try {
                    ApplicationDTO appInfo = applicationDAO.getAppById(appId);
                    if (appInfo.getAppType() == AppTypeEnum.NORMAL) {
                        userSet.addAll(roleService.listAppUsersByRole(appId, role));
                    } else {
                        log.info("Ignore role {} of not normal appId {}", role, appId);
                    }
                } catch (Exception e) {
                    logger.error(String.format("Fail to fetch role users:(%d,%s)", appId, role), e);
                }
            }
        }
        return userSet;
    }

    @Override
    public Integer sendTemplateNotification(ServiceTemplateNotificationDTO templateNotificationDTO) {
        String templateCode = templateNotificationDTO.getTemplateCode();
        List<NotifyTemplateDTO> notifyTemplateDTOList = notifyTemplateDAO.listNotifyTemplateByCode(dslContext,
            templateCode);
        // 优先使用自定义模板
        notifyTemplateDTOList.sort(Comparator.comparingInt(o -> TypeUtil.booleanToInt(o.isDefault())));
        Map<String, NotifyTemplateDTO> channelTemplateMap = new HashMap<>();
        notifyTemplateDTOList.forEach(notifyTemplateDTO -> {
            channelTemplateMap.putIfAbsent(notifyTemplateDTO.getChannel(), notifyTemplateDTO);
        });
        if (notifyTemplateDTOList.isEmpty()) {
            log.warn("no valid templates of code:{}", templateNotificationDTO.getTemplateCode());
            return 0;
        }
        //获取通知用户
        Set<String> userSet = new HashSet<>();
        UserRoleInfoDTO receiverInfo = templateNotificationDTO.getReceiverInfo();
        userSet.addAll(receiverInfo.getUserList());
        userSet.addAll(findUserByRole(templateNotificationDTO.getAppId(), templateNotificationDTO.getTriggerUser(),
            templateNotificationDTO.getResourceType(), templateNotificationDTO.getResourceId(),
            receiverInfo.getRoleList()));
        //过滤黑名单用户
        userSet = filterBlackUser(userSet);
        //获取可用通知渠道
        Set<String> availableChannelSet = new HashSet<>(getAvailableChannelTypeList());
        //与激活通知渠道取交集
        Set<String> validChannelSet = Sets.intersection(new HashSet<>(templateNotificationDTO.getActiveChannels()),
            availableChannelSet);
        Long appId = templateNotificationDTO.getAppId();
        for (String channel : validChannelSet) {
            //取得Title与Content模板
            if (channelTemplateMap.keySet().contains(channel)) {
                NotifyTemplateDTO templateDTO = channelTemplateMap.get(channel);
                //变量替换
                Map<String, String> variablesMap = templateNotificationDTO.getVariablesMap();
                ServiceNotificationMessage notificationMessage = getNotificationMessageFromTemplate(appId,
                    templateDTO, variablesMap);
                //发送消息通知
                if (notificationMessage != null) {
                    sendUserChannelNotify(userSet, channel, notificationMessage.getTitle(),
                        notificationMessage.getContent());
                } else {
                    log.warn("Fail to get notificationMessage from template of templateCode:{},channel:{}, ignore",
                        templateCode, channel);
                }
            } else {
                log.warn("No templates found for channel:{}", channel);
            }
        }
        return userSet.size();
    }

    private String getDisplayIdStr(ResourceScope scope) {
        return scope.getType().getValue() + ":" + scope.getId();
    }

    private ServiceNotificationMessage getNotificationMessageFromTemplate(
        Long appId,
        NotifyTemplateDTO templateDTO,
        Map<String, String> variablesMap
    ) {
        ApplicationDTO applicationDTO = applicationDAO.getCacheAppById(appId);
        if (applicationDTO == null) {
            log.error("cannot find applicationInfo of appId:{}", appId);
            return null;
        }
        String appName = applicationDTO.getName();
        //国际化
        String title;
        String content;
        String userLang = JobContextUtil.getUserLang();
        if (userLang == null) {
            String appLang = applicationDTO.getLanguage();
            if ("1".equals(appLang)) {
                userLang = LocaleUtils.LANG_ZH_CN;
            } else if ("2".equals(appLang)) {
                userLang = LocaleUtils.LANG_EN_US;
            } else {
                log.warn("appLang=null, use zh_CN, appId={}", appId);
                userLang = LocaleUtils.LANG_ZH_CN;
            }
        }
        String normalLang = LocaleUtils.getNormalLang(userLang);
        if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
            title = templateDTO.getTitleEn();
            if (title == null || StringUtils.isEmpty(title)) {
                title = templateDTO.getTitle();
            }
            content = templateDTO.getContentEn();
            if (content == null || StringUtils.isEmpty(content)) {
                content = templateDTO.getContent();
            }
        } else {
            title = templateDTO.getTitle();
            content = templateDTO.getContent();
        }
        //添加默认变量
        ResourceScope scope = applicationDTO.getScope();
        variablesMap.putIfAbsent("BASE_HOST", jobManageConfig.getJobWebUrl());
        variablesMap.putIfAbsent("APP_ID", getDisplayIdStr(scope));
        variablesMap.putIfAbsent("task.bk_biz_id", scope.getId());
        variablesMap.putIfAbsent("APP_NAME", appName);
        variablesMap.putIfAbsent("task.bk_biz_name", appName);
        String pattern = "(\\{\\{(.*?)\\}\\})";
        StopWatch watch = new StopWatch();
        watch.start("replace title and content");
        title = StringUtil.replaceByRegex(title, pattern, variablesMap);
        content = StringUtil.replaceByRegex(content, pattern, variablesMap);
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("{},{},{},{},{}", PrefConsts.TAG_PREF_SLOW + watch.prettyPrint(), title, content, pattern,
                variablesMap);
        }
        return new ServiceNotificationMessage(title, content);
    }

    private ServiceNotificationMessage getNotificationMessage(Long appId, String templateCode, String channel,
                                                              Map<String, String> variablesMap) {
        //1.查出自定义模板信息
        NotifyTemplateDTO notifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(dslContext, channel, templateCode,
            false);
        if (notifyTemplateDTO == null) {
            //2.未配置自定义模板则使用默认模板
            notifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(dslContext, channel, templateCode, true);
        }
        if (notifyTemplateDTO == null) {
            log.warn("Cannot find template of templateCode:{},channel:{}, plz config a default template",
                templateCode, channel);
            return null;
        }
        return getNotificationMessageFromTemplate(appId, notifyTemplateDTO, variablesMap);
    }

    @Override
    public Integer triggerTemplateNotification(ServiceTriggerTemplateNotificationDTO triggerTemplateNotification) {
        Long appId = triggerTemplateNotification.getTriggerDTO().getAppId();
        // 1.获取所有可用渠道
        StopWatch watch = new StopWatch();
        watch.start("getAvailableChannelTypeList");
        List<String> availableChannelTypeList = getAvailableChannelTypeList();
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
                log.error(String.format("channel %s is not available, not notified, please contack admin", channel));
            } else {
                String templateCode = triggerTemplateNotification.getTemplateCode();
                Map<String, String> variablesMap = triggerTemplateNotification.getVariablesMap();
                watch.start("getNotificationMessage_" + channel);
                ServiceNotificationMessage notificationMessage = getNotificationMessage(appId, templateCode, channel,
                    variablesMap);
                watch.stop();
                if (watch.getLastTaskTimeMillis() > 500) {
                    log.warn(PrefConsts.TAG_PREF_SLOW + watch.prettyPrint());
                }
                if (notificationMessage != null) {
                    sendUserChannelNotify(userSet, channel, notificationMessage.getTitle(),
                        notificationMessage.getContent());
                    counter.addOne();
                } else {
                    log.warn("Cannot find template of templateCode:{},channel:{}, ignore", templateCode, channel);
                }
            }
        });
        return counter.getValue().intValue();
    }

    private Set<String> filterBlackUser(Set<String> userSet) {
        // 过滤黑名单内用户
        Set<String> blackUserSet =
            notifyBlackUserInfoDAO.listNotifyBlackUserInfo(dslContext).stream()
                .map(NotifyBlackUserInfoDTO::getUsername).collect(Collectors.toSet());
        logger.debug(String.format("sendUserChannelNotify:blackUserSet:%s", String.join(",", blackUserSet)));
        val removedBlackUserSet = userSet.stream().filter(blackUserSet::contains).collect(Collectors.toSet());
        userSet = userSet.stream().filter(it -> !blackUserSet.contains(it)).collect(Collectors.toSet());
        logger.debug(String.format("sendUserChannelNotify:%d black users are removed, removed users=[%s]",
            removedBlackUserSet.size(), String.join(",", removedBlackUserSet)));
        return userSet;
    }

    private void sendUserChannelNotify(Set<String> userSet, String channel, String title, String content) {
        if (null == userSet) {
            logger.warn(String.format("userSet is null of channel [%s], do not send notification", channel));
            return;
        }
        if (userSet.isEmpty()) {
            logger.warn(String.format("userSet is empty of channel [%s], do not send notification", channel));
            return;
        }
        logger.debug(String.format("Begin to send %s notify to %s, title:%s, content:%s",
            channel, String.join(",", userSet), title, content));
        notificationThreadPoolExecutor.submit(new SendNotificationTask(paaSService, esbUserInfoDAO,
            JobContextUtil.getRequestId(), channel, null,
            userSet, title, content));
    }

    private void sendNotifyMessages(Map<String, Set<String>> channelUsersMap, String title, String content) {
        channelUsersMap.forEach((channel, userSet) -> sendUserChannelNotify(userSet, channel, title, content));
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
