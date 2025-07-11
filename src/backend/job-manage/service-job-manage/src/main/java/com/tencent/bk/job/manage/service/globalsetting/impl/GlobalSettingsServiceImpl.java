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

package com.tencent.bk.job.manage.service.globalsetting.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.notice.config.BkNoticeProperties;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.common.constants.OSTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.globalsetting.GlobalSettingKeys;
import com.tencent.bk.job.manage.api.common.constants.globalsetting.RelatedUrlKeys;
import com.tencent.bk.job.manage.api.common.constants.globalsetting.RestrictModeEnum;
import com.tencent.bk.job.manage.api.common.constants.globalsetting.StorageUnitEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.NotifyConsts;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.config.LocalFileConfigForManage;
import com.tencent.bk.job.manage.dao.globalsetting.GlobalSettingDAO;
import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTemplateDAO;
import com.tencent.bk.job.manage.model.dto.GlobalSettingDTO;
import com.tencent.bk.job.manage.model.dto.converter.NotifyTemplateConverter;
import com.tencent.bk.job.manage.model.dto.globalsetting.HelperInfo;
import com.tencent.bk.job.manage.model.dto.globalsetting.TitleFooter;
import com.tencent.bk.job.manage.model.dto.globalsetting.TitleFooterDTO;
import com.tencent.bk.job.manage.model.dto.globalsetting.UploadFileRestrictDTO;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTemplateDTO;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AccountNameRule;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AccountNameRulesReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.FileUploadSettingReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.HistoryExpireReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplatePreviewReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplateReq;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.request.notify.SetAvailableNotifyChannelReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRuleVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.FileUploadSettingVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.PlatformInfoVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.PlatformInfoWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.TitleFooterVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateDetailVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateDetailWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateStatusVO;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;
import com.tencent.bk.job.manage.model.web.vo.notify.TemplateBasicInfo;
import com.tencent.bk.job.manage.model.web.vo.notify.UserVO;
import com.tencent.bk.job.manage.service.NotifyService;
import com.tencent.bk.job.manage.service.globalsetting.GlobalSettingsService;
import com.tencent.bk.job.manage.service.impl.notify.NotifySendService;
import com.tencent.bk.job.manage.service.impl.notify.NotifyUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GlobalSettingsServiceImpl implements GlobalSettingsService {

    private static final Pattern PATTERN = Pattern.compile("^([.0-9]+)([a-zA-Z]{0,2})$");
    private static final String STRING_TPL_KEY_CURRENT_VERSION = "current_ver";
    private static final String STRING_TPL_KEY_CURRENT_YEAR = "current_year";
    private final NotifyEsbChannelDAO notifyEsbChannelDAO;
    private final AvailableEsbChannelDAO availableEsbChannelDAO;
    private final NotifyService notifyService;
    private final NotifySendService notifySendService;
    private final NotifyUserService notifyUserService;
    private final GlobalSettingDAO globalSettingDAO;
    private final NotifyTemplateDAO notifyTemplateDAO;
    private final MessageI18nService i18nService;
    private final JobManageConfig jobManageConfig;
    private final LocalFileConfigForManage localFileConfigForManage;
    private final BkNoticeProperties bkNoticeProperties;
    private final NotifyTemplateConverter notifyTemplateConverter;
    private final BuildProperties buildProperties;
    @Value("${job.manage.upload.filesize.max:5GB}")
    private String configedMaxFileSize;


    public GlobalSettingsServiceImpl(NotifyEsbChannelDAO notifyEsbChannelDAO,
                                     AvailableEsbChannelDAO availableEsbChannelDAO,
                                     NotifyService notifyService,
                                     NotifySendService notifySendService,
                                     NotifyUserService notifyUserService,
                                     GlobalSettingDAO globalSettingDAO,
                                     NotifyTemplateDAO notifyTemplateDAO,
                                     MessageI18nService i18nService,
                                     JobManageConfig jobManageConfig,
                                     LocalFileConfigForManage localFileConfigForManage,
                                     BkNoticeProperties bkNoticeProperties,
                                     NotifyTemplateConverter notifyTemplateConverter,
                                     BuildProperties buildProperties) {
        this.notifyEsbChannelDAO = notifyEsbChannelDAO;
        this.availableEsbChannelDAO = availableEsbChannelDAO;
        this.notifyService = notifyService;
        this.notifySendService = notifySendService;
        this.notifyUserService = notifyUserService;
        this.globalSettingDAO = globalSettingDAO;
        this.notifyTemplateDAO = notifyTemplateDAO;
        this.i18nService = i18nService;
        this.jobManageConfig = jobManageConfig;
        this.localFileConfigForManage = localFileConfigForManage;
        this.bkNoticeProperties = bkNoticeProperties;
        this.notifyTemplateConverter = notifyTemplateConverter;
        this.buildProperties = buildProperties;
    }

    private static String removeSuffixBackSlash(String rawStr) {
        if (rawStr == null) {
            return null;
        }
        while (rawStr.endsWith("/")) {
            rawStr = rawStr.substring(0, rawStr.length() - 1);
        }
        return rawStr;
    }

    @Override
    public Boolean isNotifyChannelConfiged() {
        GlobalSettingDTO globalSettingDTO = globalSettingDAO.getGlobalSetting(
            GlobalSettingKeys.KEY_NOTIFY_CHANNEL_CONFIGED);
        return globalSettingDTO != null && globalSettingDTO.getValue().toLowerCase().equals("true");
    }

    @Override
    public Boolean setNotifyChannelConfiged() {
        GlobalSettingDTO globalSettingDTO = globalSettingDAO.getGlobalSetting(
            GlobalSettingKeys.KEY_NOTIFY_CHANNEL_CONFIGED);
        if (globalSettingDTO == null) {
            globalSettingDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_NOTIFY_CHANNEL_CONFIGED,
                "true", "whether available notify channels are configed");
            return 1 == globalSettingDAO.insertGlobalSetting(globalSettingDTO);
        } else if (!globalSettingDTO.getValue().toLowerCase().equals("true")) {
            globalSettingDTO.setValue("true");
            return 1 == globalSettingDAO.updateGlobalSetting(globalSettingDTO);
        } else {
            return true;
        }
    }

    @Override
    public List<NotifyChannelWithIconVO> listNotifyChannel(String username) {
        List<NotifyEsbChannelDTO> allNotifyChannelList =
            notifyEsbChannelDAO.listNotifyEsbChannel().stream()
                .filter(NotifyEsbChannelDTO::isActive).collect(Collectors.toList());
        List<AvailableEsbChannelDTO> availableNotifyChannelList =
            availableEsbChannelDAO.listAvailableEsbChannel();
        Set<String> availableNotifyChannelTypeSet =
            availableNotifyChannelList.stream().map(AvailableEsbChannelDTO::getType).collect(Collectors.toSet());
        return allNotifyChannelList.stream().map(it -> {
            String icon = it.getIcon();
            String prefix = "data:image/png;base64,";
            if (!icon.startsWith("data:image")) {
                icon = prefix + icon;
            }
            return new NotifyChannelWithIconVO(
                it.getType(),
                it.getLabel(),
                icon,
                availableNotifyChannelTypeSet.contains(it.getType())
            );
        }).collect(Collectors.toList());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.EDIT_GLOBAL_SETTINGS
    )
    public Integer setAvailableNotifyChannel(String username, SetAvailableNotifyChannelReq req) {
        return notifyService.setAvailableNotifyChannel(username, req);
    }

    @Override
    public List<UserVO> listUsers(String username, String prefixStr, Long offset, Long limit) {
        //这里就是要选择人来添加黑名单，故不排除已在黑名单内的人
        return notifyUserService.listUsers(prefixStr, offset, limit, false);
    }

    @Override
    public List<NotifyBlackUserInfoVO> listNotifyBlackUsers(String username, Integer start, Integer pageSize) {
        return notifyUserService.listNotifyBlackUsers(start, pageSize);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.EDIT_GLOBAL_SETTINGS
    )
    public List<String> saveNotifyBlackUsers(String username, NotifyBlackUsersReq req) {
        return notifyUserService.saveNotifyBlackUsers(username, req);
    }

    @Override
    public Long getHistoryExpireTime(String username) {
        GlobalSettingDTO globalSettingDTO = globalSettingDAO.getGlobalSetting(
            GlobalSettingKeys.KEY_HISTORY_EXPIRE_DAYS);
        if (globalSettingDTO == null) {
            globalSettingDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_HISTORY_EXPIRE_DAYS,
                "60", "执行记录默认保存天数" +
                "(default history expire days)");
            globalSettingDAO.insertGlobalSetting(globalSettingDTO);
        }
        return Long.parseLong(globalSettingDTO.getValue());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.EDIT_GLOBAL_SETTINGS
    )
    public Integer setHistoryExpireTime(String username, HistoryExpireReq req) {
        Long days = req.getDays();
        if (days == null || days <= 0) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"days", "days must be positive"});
        }
        GlobalSettingDTO globalSettingDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_HISTORY_EXPIRE_DAYS,
            days.toString(), String.format("执行记录保存天数(history expire days):%s,%s", username,
            DateUtils.defaultLocalDateTime(LocalDateTime.now())));
        return globalSettingDAO.updateGlobalSetting(globalSettingDTO);
    }

    @Override
    public AccountNameRule getCurrentAccountNameRule(OSTypeEnum osType) {
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        GlobalSettingDTO currentNameRulesDTO;
        String globalSettingKey;
        if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
            //英文环境
            globalSettingKey = GlobalSettingKeys.KEY_CURRENT_NAME_RULES_EN;
        } else {
            //中文环境
            globalSettingKey = GlobalSettingKeys.KEY_CURRENT_NAME_RULES;
        }
        List<AccountNameRule> currentNameRules;
        currentNameRulesDTO = globalSettingDAO.getGlobalSetting(globalSettingKey);
        if (currentNameRulesDTO != null) {
            currentNameRules = JsonUtils.fromJson(currentNameRulesDTO.getValue(),
                new TypeReference<List<AccountNameRule>>() {
                });
            for (AccountNameRule rule : currentNameRules) {
                if (rule.getOsType() == osType) {
                    return rule;
                }
            }
        } else {
            log.warn("Cannot find currentNameRules in language:{}, please check key:{} in database table job_manage" +
                ".global_setting", normalLang, globalSettingKey);
        }
        return null;
    }

    // 默认账号命名规则：Linux
    private static final String DEFAULT_ACCOUNT_NAME_RULE_LINUX = "^[a-z_][a-z0-9_-]{1,31}$";
    // 默认账号命名规则：Windows
    private static final String DEFAULT_ACCOUNT_NAME_RULE_WINDOWS = "^[a-æA-Æ0-9-]{1,32}$";
    // 默认账号命名规则：Database
    private static final String DEFAULT_ACCOUNT_NAME_RULE_DATABASE = "^[a-zA-Z0-9\\.\\-\\_]{1,16}$";

    private List<AccountNameRule> getDefaultNameRules(Locale locale) {
        List<AccountNameRule> defaultNameRules = new ArrayList<>();
        defaultNameRules.add(new AccountNameRule(OSTypeEnum.LINUX, DEFAULT_ACCOUNT_NAME_RULE_LINUX,
            i18nService.getI18n(locale, "job.manage.globalsettings.defaultNameRules.description.linux")));
        defaultNameRules.add(new AccountNameRule(OSTypeEnum.WINDOWS, DEFAULT_ACCOUNT_NAME_RULE_WINDOWS,
            i18nService.getI18n(locale, "job.manage.globalsettings.defaultNameRules.description.windows")));
        defaultNameRules.add(new AccountNameRule(OSTypeEnum.DATABASE, DEFAULT_ACCOUNT_NAME_RULE_DATABASE,
            i18nService.getI18n(locale, "job.manage.globalsettings.defaultNameRules.description.database")));
        return defaultNameRules;
    }

    @Override
    public AccountNameRulesWithDefaultVO getAccountNameRules() {
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        List<AccountNameRule> defaultNameRules;
        GlobalSettingDTO currentNameRulesDTO;
        if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
            //英文环境
            defaultNameRules = getDefaultNameRules(Locale.ENGLISH);
            currentNameRulesDTO = globalSettingDAO.getGlobalSetting(
                GlobalSettingKeys.KEY_CURRENT_NAME_RULES_EN);
        } else {
            //中文环境
            defaultNameRules = getDefaultNameRules(Locale.CHINA);
            currentNameRulesDTO = globalSettingDAO.getGlobalSetting(
                GlobalSettingKeys.KEY_CURRENT_NAME_RULES);
        }
        List<AccountNameRule> currentNameRules;
        if (currentNameRulesDTO != null) {
            currentNameRules = JsonUtils.fromJson(currentNameRulesDTO.getValue(),
                new TypeReference<List<AccountNameRule>>() {
                });
        } else {
            currentNameRules = defaultNameRules;
        }
        return new AccountNameRulesWithDefaultVO(currentNameRules.stream()
            .map(it -> new AccountNameRuleVO(it.getOsType(), it.getExpression(), it.getDescription()))
            .collect(Collectors.toList()),
            defaultNameRules.stream()
                .map(it -> new AccountNameRuleVO(it.getOsType(), it.getExpression(), it.getDescription()))
                .collect(Collectors.toList())
        );
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.EDIT_GLOBAL_SETTINGS
    )
    public Boolean setAccountNameRules(String username, AccountNameRulesReq req) {
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        String currentNameRulesKey = GlobalSettingKeys.KEY_CURRENT_NAME_RULES;
        if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
            //英文环境
            currentNameRulesKey = GlobalSettingKeys.KEY_CURRENT_NAME_RULES_EN;
        }
        GlobalSettingDTO currentNameRulesDTO = globalSettingDAO.getGlobalSetting(currentNameRulesKey);
        GlobalSettingDTO inputNameRulesDTO = new GlobalSettingDTO(currentNameRulesKey,
            JsonUtils.toJson(req.getRules()), String.format("Updated by %s at %s", username,
            DateUtils.defaultLocalDateTime(LocalDateTime.now())));
        if (currentNameRulesDTO == null) {
            return globalSettingDAO.insertGlobalSetting(inputNameRulesDTO) == 1;
        } else {
            return globalSettingDAO.updateGlobalSetting(inputNameRulesDTO) == 1;
        }
    }

    private Pair<Float, String> parseFileSize(String str) {
        Matcher matcher = PATTERN.matcher(str);
        if (!matcher.matches()) {
            return null;
        }
        float amount = Float.parseFloat(matcher.group(1));
        String unit = matcher.group(2);
        return Pair.of(amount, unit);
    }

    private FileUploadSettingVO getFileUploadSettingsFromStr(String str) {
        Pair<Float, String> configedValue = parseFileSize(str);
        if (configedValue == null) return null;
        return new FileUploadSettingVO(configedValue.getLeft(), configedValue.getRight(), null, null);
    }

    private FileUploadSettingVO getConfigedFileUploadSettings() {
        log.debug("configedMaxFileSize=" + configedMaxFileSize);
        return getFileUploadSettingsFromStr(configedMaxFileSize);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.EDIT_GLOBAL_SETTINGS
    )
    public Boolean saveFileUploadSettings(String username, FileUploadSettingReq req) {
        Float uploadMaxSize = req.getAmount();
        StorageUnitEnum unit = req.getUnit();
        Integer restrictMode = req.getRestrictMode();
        List<String> suffixList = req.getSuffixList();
        if (unit == null) {
            unit = StorageUnitEnum.B;
        }
        if (uploadMaxSize <= 0) {
            uploadMaxSize = 5.0f;
            unit = StorageUnitEnum.GB;
        }
        if (restrictMode == null) {
            restrictMode = RestrictModeEnum.UNLIMITED.getType();
        }

        if (CollectionUtils.isNotEmpty(suffixList)) {
            // 去重
            suffixList = suffixList.stream().distinct().collect(Collectors.toList());
        }
        GlobalSettingDTO fileUploadSettingDTO = globalSettingDAO.getGlobalSetting(
            GlobalSettingKeys.KEY_FILE_UPLOAD_SETTING);
        if (fileUploadSettingDTO == null) {
            fileUploadSettingDTO = new GlobalSettingDTO();
            fileUploadSettingDTO.setDescription("setting of upload file");
        }
        fileUploadSettingDTO.setKey(GlobalSettingKeys.KEY_FILE_UPLOAD_SETTING);
        fileUploadSettingDTO.setValue(JsonUtils.toJson(
            new UploadFileRestrictDTO(
                uploadMaxSize.toString() + unit.name()
                , restrictMode
                , suffixList)));
        int affectedRows = globalSettingDAO.upsertGlobalSetting(fileUploadSettingDTO);
        return affectedRows > 0;
    }

    @Override
    public FileUploadSettingVO getFileUploadSettings() {
        GlobalSettingDTO fileUploadSettingDTO = globalSettingDAO.getGlobalSetting(
            GlobalSettingKeys.KEY_FILE_UPLOAD_SETTING);
        FileUploadSettingVO fileUploadSettingVO;
        if (fileUploadSettingDTO == null) {
            fileUploadSettingVO = getConfigedFileUploadSettings();
        } else {
            String settingStr = fileUploadSettingDTO.getValue();
            UploadFileRestrictDTO uploadFileRestrictDTO = JsonUtils.fromJson(settingStr,
                new TypeReference<UploadFileRestrictDTO>() {
                });
            fileUploadSettingVO = getFileUploadSettingsFromStr(uploadFileRestrictDTO.getMaxSize());
            if (fileUploadSettingVO == null) {
                fileUploadSettingVO = new FileUploadSettingVO();
            }
            fileUploadSettingVO.setRestrictMode(uploadFileRestrictDTO.getRestrictMode());
            fileUploadSettingVO.setSuffixList(uploadFileRestrictDTO.getSuffixList());
        }
        return fileUploadSettingVO;
    }


    @Override
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.EDIT_GLOBAL_SETTINGS
    )
    public Integer saveChannelTemplate(String username, ChannelTemplateReq req) {
        if (StringUtils.isBlank(req.getChannelCode()) || StringUtils.isBlank(req.getMessageTypeCode())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"channelCode", "channelCode cannot be blank"});
        }
        NotifyTemplateDTO notifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(
            req.getChannelCode(),
            req.getMessageTypeCode(),
            false
        );
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        if (notifyTemplateDTO == null) {
            notifyTemplateDTO = new NotifyTemplateDTO(null, req.getMessageTypeCode(), req.getMessageTypeCode(),
                req.getChannelCode(), req.getTitle(), req.getContent(), req.getTitle(), req.getContent(), false,
                username, System.currentTimeMillis(), username, System.currentTimeMillis());
            return notifyTemplateDAO.insertNotifyTemplate(notifyTemplateDTO);
        } else {
            if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
                notifyTemplateDTO.setTitleEn(req.getTitle());
                notifyTemplateDTO.setContentEn(req.getContent());
            } else {
                notifyTemplateDTO.setTitle(req.getTitle());
                notifyTemplateDTO.setContent(req.getContent());
            }
            notifyTemplateDTO.setLastModifyUser(username);
            notifyTemplateDTO.setLastModifyTime(System.currentTimeMillis());
            return notifyTemplateDAO.updateNotifyTemplateById(notifyTemplateDTO);
        }
    }

    @Override
    public Integer sendChannelTemplate(String username, ChannelTemplatePreviewReq req) {
        List<String> receiverList = StringUtil.strToList(req.getReceiverStr(), String.class,
            NotifyConsts.SEPERATOR_COMMA);
        Set<String> receiverSet = new HashSet<>(receiverList);
        notifySendService.sendUserChannelNotify(
            null,
            receiverSet,
            req.getChannelCode(),
            req.getTitle(),
            req.getContent()
        );
        return receiverSet.size();
    }

    @Override
    public ChannelTemplateDetailWithDefaultVO getChannelTemplateDetail(String username, String channelCode,
                                                                       String messageTypeCode) {
        NotifyTemplateDTO currentNotifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(
            channelCode,
            messageTypeCode,
            false
        );
        NotifyTemplateDTO defaultNotifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(
            channelCode,
            messageTypeCode,
            true
        );
        // 渠道下无默认模板，则为新增渠道，使用通用模板
        if (defaultNotifyTemplateDTO == null) {
            defaultNotifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(
                NotifyConsts.NOTIFY_CHANNEL_CODE_COMMON,
                messageTypeCode,
                false
            );
            if (defaultNotifyTemplateDTO == null) {
                defaultNotifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(
                    NotifyConsts.NOTIFY_CHANNEL_CODE_COMMON,
                    messageTypeCode,
                    true
                );
            }
            if (defaultNotifyTemplateDTO != null) {
                defaultNotifyTemplateDTO.setChannel(channelCode);
            } else {
                log.warn("common template of messageType={} not configed", messageTypeCode);
            }
        }
        ChannelTemplateDetailVO currentChannelTemplateVO =
            notifyTemplateConverter.convertToChannelTemplateDetailVO(currentNotifyTemplateDTO);
        ChannelTemplateDetailVO defaultChannelTemplateVO =
            notifyTemplateConverter.convertToChannelTemplateDetailVO(defaultNotifyTemplateDTO);
        // 渠道下未配置模板则使用该渠道默认模板
        if (currentChannelTemplateVO == null) {
            currentChannelTemplateVO = defaultChannelTemplateVO;
        }
        // 渠道下已配置模板信息不完整则使用默认模板内容填充
        if (currentChannelTemplateVO != null && currentChannelTemplateVO.getTitle() == null) {
            if (defaultChannelTemplateVO != null) {
                currentChannelTemplateVO.setTitle(defaultChannelTemplateVO.getTitle());
            }
        }
        if (currentChannelTemplateVO != null && currentChannelTemplateVO.getContent() == null) {
            if (defaultChannelTemplateVO != null) {
                currentChannelTemplateVO.setContent(defaultChannelTemplateVO.getContent());
            }
        }
        return new ChannelTemplateDetailWithDefaultVO(currentChannelTemplateVO, defaultChannelTemplateVO);
    }

    @Override
    public List<ChannelTemplateStatusVO> listChannelTemplateStatus(String username) {
        List<ChannelTemplateStatusVO> resultList = new ArrayList<>();
        List<String> messageCodeList = Arrays.asList(
            NotifyConsts.NOTIFY_TEMPLATE_CODE_CONFIRMATION,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_EXECUTE_SUCCESS,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_EXECUTE_FAILURE,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_EXECUTE,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_END,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_CRON_EXECUTE_FAILED
        );
        List<NotifyChannelWithIconVO> notifyChannelWithIconVOList = listNotifyChannel(username);
        notifyChannelWithIconVOList.forEach(notifyChannelWithIconVO -> {
            String channelCode = notifyChannelWithIconVO.getCode();
            List<TemplateBasicInfo> configStatusList = new ArrayList<>();
            messageCodeList.forEach(messageCode -> {
                Boolean configStatus = notifyTemplateDAO.existsNotifyTemplate(channelCode, messageCode,
                    false);
                configStatusList.add(new TemplateBasicInfo(messageCode,
                    i18nService.getI18n(NotifyConsts.NOTIFY_TEMPLATE_NAME_PREFIX + messageCode), configStatus));
            });
            resultList.add(new ChannelTemplateStatusVO(
                channelCode,
                notifyChannelWithIconVO.getName(),
                notifyChannelWithIconVO.getIcon(),
                notifyChannelWithIconVO.getIsActive(),
                configStatusList
            ));
        });
        return resultList;
    }

    @Override
    public String getDocJobRootUrl() {
        String docCenterBaseUrl = getDocCenterBaseUrl();
        StringBuilder sb = new StringBuilder(docCenterBaseUrl);
        sb.append("/markdown/");
        if (JobContextUtil.isEnglishLocale()) {
            sb.append("EN");
        } else {
            sb.append("ZH");
        }
        sb.append("/JOB/");
        sb.append(getTwoDigitVersion());
        return sb.toString();
    }

    private String getTwoDigitVersion() {
        String completeVersion = buildProperties.getVersion();
        String[] versionParts = completeVersion.split("\\.");
        return versionParts[0] + "." + versionParts[1];
    }

    @Override
    public String getDocCenterBaseUrl() {
        String url;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(jobManageConfig.getBkDocRoot())) {
            url = removeSuffixBackSlash(jobManageConfig.getBkDocRoot());
        } else {
            String jobEdition = jobManageConfig.getJobEdition();
            if (jobEdition.toLowerCase().equals("ee")) {
                // 企业版
                url = removeSuffixBackSlash(jobManageConfig.getPaasServerUrl()) + "/o/bk_docs_center";
            } else {
                // 社区版
                url = removeSuffixBackSlash(jobManageConfig.getBkCERoot()) + "/docs";
            }
        }
        return url;
    }

    private String getFeedBackRootUrl() {
        String url;
        if (StringUtils.isNotBlank(jobManageConfig.getBkFeedBackRoot())) {
            url = removeSuffixBackSlash(jobManageConfig.getBkFeedBackRoot());
        } else {
            // 企业版&社区版
            url = jobManageConfig.getBkCERoot() + "/s-mart/community";
        }
        return url;
    }

    private String getBkSharedResBaseJsUrl() {
        String bkSharedResUrl = jobManageConfig.getBkSharedResUrl();
        if (StringUtils.isNotBlank(bkSharedResUrl)) {
            // bkSharedResUrl配置了有效值才生效
            return jobManageConfig.getBkSharedResUrl() + jobManageConfig.getBkSharedBaseJsPath();
        }
        return null;
    }

    private String getNodemanRootUrl() {
        String url = jobManageConfig.getNodemanServerUrl();
        if (StringUtils.isBlank(url)) {
            url = removeSuffixBackSlash(jobManageConfig.getPaasServerUrl()) + jobManageConfig.getPaasNodemanPath();
        }
        return url;
    }

    @Override
    public Map<String, String> getRelatedSystemUrls(String username) {
        Map<String, String> urlMap = new HashMap<>();
        urlMap.put(RelatedUrlKeys.KEY_BK_CMDB_ROOT_URL, jobManageConfig.getCmdbServerUrl());
        urlMap.put(RelatedUrlKeys.KEY_BK_CMDB_APP_INDEX_URL,
            removeSuffixBackSlash(jobManageConfig.getCmdbServerUrl()) + jobManageConfig.getCmdbAppIndexPath());
        urlMap.put(RelatedUrlKeys.KEY_BK_NODEMAN_ROOT_URL, getNodemanRootUrl());
        urlMap.put(RelatedUrlKeys.KEY_BK_DOC_CENTER_ROOT_URL, getDocCenterBaseUrl());
        urlMap.put(RelatedUrlKeys.KEY_BK_DOC_JOB_ROOT_URL, getDocJobRootUrl());
        urlMap.put(RelatedUrlKeys.KEY_BK_FEED_BACK_ROOT_URL, getFeedBackRootUrl());
        urlMap.put(RelatedUrlKeys.KEY_BK_SHARED_RES_BASE_JS_URL, getBkSharedResBaseJsUrl());
        return urlMap;
    }

    private void addFileUploadConfig(Map<String, Object> configMap) {
        FileUploadSettingVO fileUploadSettingVO = getFileUploadSettings();
        if (fileUploadSettingVO != null) {
            configMap.put(GlobalSettingKeys.KEY_FILE_UPLOAD_SETTING, fileUploadSettingVO);
        }
    }

    private void addEnableFeatureFileManageConfig(Map<String, Object> configMap) {
        configMap.put(GlobalSettingKeys.KEY_ENABLE_FEATURE_FILE_MANAGE,
            FeatureToggle.checkFeature(FeatureIdConstants.FEATURE_FILE_MANAGE,
                FeatureExecutionContext.EMPTY));
    }

    private void addEnableUploadToArtifactoryConfig(Map<String, Object> configMap) {
        configMap.put(
            GlobalSettingKeys.KEY_ENABLE_UPLOAD_TO_ARTIFACTORY,
            JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(localFileConfigForManage.getStorageBackend())
        );
    }

    private void addEnableBkNoticeConfig(Map<String, Object> configMap) {
        configMap.put(
            GlobalSettingKeys.KEY_ENABLE_BK_NOTICE,
            bkNoticeProperties.isEnabled() && bkNoticeRegisteredSuccess()
        );
    }

    private boolean bkNoticeRegisteredSuccess() {
        GlobalSettingDTO globalSettingDTO =
            globalSettingDAO.getGlobalSetting(GlobalSettingKeys.KEY_BK_NOTICE_REGISTERED_SUCCESS);
        return globalSettingDTO != null && "true".equals(globalSettingDTO.getValue().toLowerCase());
    }

    @Override
    public Map<String, Object> getJobConfig(String username) {
        Map<String, Object> configMap = new HashMap<>();
        addFileUploadConfig(configMap);
        addEnableFeatureFileManageConfig(configMap);
        addEnableUploadToArtifactoryConfig(configMap);
        addEnableBkNoticeConfig(configMap);
        return configMap;
    }

    @Override
    public PlatformInfoVO getRenderedPlatformInfo() {
        TitleFooterVO titleFooterVO = getRenderedTitleFooter();
        PlatformInfoVO platformInfoVO = new PlatformInfoVO(titleFooterVO);
        HelperInfo helperInfo = getHelperInfo();
        if (helperInfo != null) {
            platformInfoVO.setHelperContactLink(helperInfo.getContactLink());
        }
        return platformInfoVO;
    }

    public PlatformInfoVO getPlatformInfoVO() {
        TitleFooterVO titleFooterVO = getTitleFooter();
        HelperInfo helperInfo = getHelperInfo();
        return new PlatformInfoVO(titleFooterVO, helperInfo == null ? "" : helperInfo.getContactLink());
    }

    public TitleFooterVO getRenderedTitleFooter() {
        TitleFooterVO titleFooterVO = getTitleFooter();
        // 渲染版本号
        Map<String, String> valuesMap = new HashMap<>();
        String pattern = "(\\{\\{(.*?)\\}\\})";
        if (buildProperties != null) {
            valuesMap.put(STRING_TPL_KEY_CURRENT_VERSION, "V" + buildProperties.getVersion());
        }
        valuesMap.put(STRING_TPL_KEY_CURRENT_YEAR, DateUtils.getCurrentDateStr("yyyy"));
        titleFooterVO.setFooterCopyRight(
            StringUtil.replaceByRegex(titleFooterVO.getFooterCopyRight(), pattern, valuesMap)
        );
        titleFooterVO.setFooterLink(
            StringUtil.replaceByRegex(titleFooterVO.getFooterLink(), pattern, valuesMap)
        );
        return titleFooterVO;
    }

    private TitleFooterVO getTitleFooter() {
        GlobalSettingDTO titleFooterSettingDTO = globalSettingDAO.getGlobalSetting(
            GlobalSettingKeys.KEY_TITLE_FOOTER);
        if (titleFooterSettingDTO == null) {
            log.warn("Default titleFooter not configured, use system default, plz contact admin to set");
            return getDefaultTitleFooterVO();
        }
        TitleFooterDTO titleFooterDTO = JsonUtils.fromJson(titleFooterSettingDTO.getValue(),
            new TypeReference<TitleFooterDTO>() {
            });
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        TitleFooter titleFooter = titleFooterDTO.getTitleFooterLanguageMap().get(normalLang);
        return titleFooter == null ? getDefaultTitleFooterVO() :
            new TitleFooterVO(titleFooter.getTitleHead(), titleFooter.getTitleSeparator(),
                titleFooter.getFooterLink(), titleFooter.getFooterCopyRight());
    }

    private PlatformInfoVO getDefaultPlatformInfoVO() {
        return new PlatformInfoVO(getDefaultTitleFooterVO());
    }

    private TitleFooterVO getDefaultTitleFooterVO() {
        String jobEdition = jobManageConfig.getJobEdition();
        if (jobEdition.toLowerCase().equals("ee")) {
            return getEEDefaultTitleFooterVO();
        } else {
            return getCEDefaultTitleFooterVO();
        }
    }

    private TitleFooterVO getCEDefaultTitleFooterVO() {
        return getEEDefaultTitleFooterVO();
    }

    private TitleFooterVO getEEDefaultTitleFooterVO() {
        return new TitleFooterVO(
            i18nService.getI18n("job.manage.globalsettings.defaultTitleHead"),
            "|",
            i18nService.getI18n("job.manage.globalsettings.ee.footerLink").replace("{PAAS_SERVER_URL}",
                jobManageConfig.getPaasServerUrl()),
            i18nService.getI18n("job.manage.globalsettings.ee.footerCopyRight")
        );
    }


    @Override
    public PlatformInfoWithDefaultVO getPlatformInfoWithDefault(String username) {
        return new PlatformInfoWithDefaultVO(getPlatformInfoVO(), getDefaultPlatformInfoVO());
    }

    private HelperInfo getHelperInfo() {
        GlobalSettingDTO globalSetting = globalSettingDAO.getGlobalSetting(
            GlobalSettingKeys.KEY_BK_HELPER);
        if (globalSetting == null || StringUtils.isEmpty(globalSetting.getValue())) {
            return null;
        }
        return JsonUtils.fromJson(globalSetting.getValue(), HelperInfo.class);
    }
}
