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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.consts.globalsetting.GlobalSettingKeys;
import com.tencent.bk.job.manage.common.consts.globalsetting.OSTypeEnum;
import com.tencent.bk.job.manage.common.consts.globalsetting.RelatedUrlKeys;
import com.tencent.bk.job.manage.common.consts.globalsetting.StorageUnitEnum;
import com.tencent.bk.job.manage.common.consts.notify.NotifyConsts;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.globalsetting.GlobalSettingDAO;
import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTemplateDAO;
import com.tencent.bk.job.manage.model.dto.GlobalSettingDTO;
import com.tencent.bk.job.manage.model.dto.converter.NotifyTemplateConverter;
import com.tencent.bk.job.manage.model.dto.globalsetting.TitleFooter;
import com.tencent.bk.job.manage.model.dto.globalsetting.TitleFooterDTO;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTemplateDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import com.tencent.bk.job.manage.model.inner.ServiceUserNotificationDTO;
import com.tencent.bk.job.manage.model.web.request.globalsetting.*;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplatePreviewReq;
import com.tencent.bk.job.manage.model.web.request.notify.ChannelTemplateReq;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyBlackUsersReq;
import com.tencent.bk.job.manage.model.web.request.notify.SetAvailableNotifyChannelReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.*;
import com.tencent.bk.job.manage.model.web.vo.notify.*;
import com.tencent.bk.job.manage.service.GlobalSettingsService;
import com.tencent.bk.job.manage.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
@Slf4j
@Service
public class GlobalSettingsServiceImpl implements GlobalSettingsService {

    private static final Pattern PATTERN = Pattern.compile("^([+\\-]?\\d+)([a-zA-Z]{0,2})$");
    private DSLContext dslContext;
    private NotifyEsbChannelDAO notifyEsbChannelDAO;
    private AvailableEsbChannelDAO availableEsbChannelDAO;
    private NotifyService notifyService;
    private GlobalSettingDAO globalSettingDAO;
    private NotifyTemplateDAO notifyTemplateDAO;
    private MessageI18nService i18nService;
    private JobManageConfig jobManageConfig;
    private NotifyTemplateConverter notifyTemplateConverter;
    @Value("${job.manage.upload.filesize.max:5GB}")
    private String configedMaxFileSize;
    @Value("${job.feature.file-manage.enable:false}")
    private Boolean enableFeatureFileManage;

    @Autowired
    public GlobalSettingsServiceImpl(
        DSLContext dslContext
        , NotifyEsbChannelDAO notifyEsbChannelDAO
        , AvailableEsbChannelDAO availableEsbChannelDAO
        , NotifyService notifyService
        , GlobalSettingDAO globalSettingDAO
        , NotifyTemplateDAO notifyTemplateDAO, MessageI18nService i18nService, JobManageConfig jobManageConfig,
        NotifyTemplateConverter notifyTemplateConverter) {
        this.dslContext = dslContext;
        this.notifyEsbChannelDAO = notifyEsbChannelDAO;
        this.availableEsbChannelDAO = availableEsbChannelDAO;
        this.notifyService = notifyService;
        this.globalSettingDAO = globalSettingDAO;
        this.notifyTemplateDAO = notifyTemplateDAO;
        this.i18nService = i18nService;
        this.jobManageConfig = jobManageConfig;
        this.notifyTemplateConverter = notifyTemplateConverter;
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
    public Boolean isNotifyChannelConfiged(DSLContext dslContext) {
        GlobalSettingDTO globalSettingDTO = globalSettingDAO.getGlobalSetting(dslContext,
            GlobalSettingKeys.KEY_NOTIFY_CHANNEL_CONFIGED);
        if (globalSettingDTO == null || !globalSettingDTO.getValue().toLowerCase().equals("true")) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean setNotifyChannelConfiged(DSLContext dslContext) {
        GlobalSettingDTO globalSettingDTO = globalSettingDAO.getGlobalSetting(dslContext,
            GlobalSettingKeys.KEY_NOTIFY_CHANNEL_CONFIGED);
        if (globalSettingDTO == null) {
            globalSettingDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_NOTIFY_CHANNEL_CONFIGED,
                "true", "whether " +
                "avaliable notify channels are configed");
            return 1 == globalSettingDAO.insertGlobalSetting(dslContext, globalSettingDTO);
        } else if (!globalSettingDTO.getValue().toLowerCase().equals("true")) {
            globalSettingDTO.setValue("true");
            return 1 == globalSettingDAO.updateGlobalSetting(dslContext, globalSettingDTO);
        } else {
            return true;
        }
    }

    @Override
    public List<NotifyChannelWithIconVO> listNotifyChannel(String username) {
        List<NotifyEsbChannelDTO> allNotifyChannelList =
            notifyEsbChannelDAO.listNotifyEsbChannel(dslContext).stream()
                .filter(NotifyEsbChannelDTO::isActive).collect(Collectors.toList());
        List<AvailableEsbChannelDTO> availableNotifyChannelList =
            availableEsbChannelDAO.listAvailableEsbChannel(dslContext);
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
    public Integer setAvailableNotifyChannel(String username, SetAvailableNotifyChannelReq req) {
        return notifyService.setAvailableNotifyChannel(username, req);
    }

    @Override
    public List<UserVO> listUsers(String username, String prefixStr, Long offset, Long limit) {
        //这里就是要选择人来添加黑名单，故不排除已在黑名单内的人
        return notifyService.listUsers(username, prefixStr, offset, limit, false);
    }

    @Override
    public List<NotifyBlackUserInfoVO> listNotifyBlackUsers(String username, Integer start, Integer pageSize) {
        return notifyService.listNotifyBlackUsers(username, start, pageSize);
    }

    @Override
    public List<String> saveNotifyBlackUsers(String username, NotifyBlackUsersReq req) {
        return notifyService.saveNotifyBlackUsers(username, req);
    }

    @Override
    public Long getHistoryExpireTime(String username) {
        GlobalSettingDTO globalSettingDTO = globalSettingDAO.getGlobalSetting(dslContext,
            GlobalSettingKeys.KEY_HISTORY_EXPIRE_DAYS);
        if (globalSettingDTO == null) {
            globalSettingDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_HISTORY_EXPIRE_DAYS,
                "60", "执行记录默认保存天数" +
                "(default history expire days)");
            globalSettingDAO.insertGlobalSetting(dslContext, globalSettingDTO);
        }
        return Long.parseLong(globalSettingDTO.getValue());
    }

    @Override
    public Integer setHistoryExpireTime(String username, HistoryExpireReq req) {
        Long days = req.getDays();
        if (days == null || days <= 0) {
            throw new InvalidParamException("days", "days must be positive");
        }
        GlobalSettingDTO globalSettingDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_HISTORY_EXPIRE_DAYS,
            days.toString(), String.format("执行记录保存天数(history expire days):%s,%s", username,
            DateUtils.defaultLocalDateTime(LocalDateTime.now())));
        return globalSettingDAO.updateGlobalSetting(dslContext, globalSettingDTO);
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
        currentNameRulesDTO = globalSettingDAO.getGlobalSetting(dslContext, globalSettingKey);
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

    @Override
    public AccountNameRulesWithDefaultVO getAccountNameRules() {
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        List<AccountNameRule> defaultNameRules = new ArrayList<>();
        GlobalSettingDTO currentNameRulesDTO;
        if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
            //英文环境
            GlobalSettingDTO defaultNameRulesDTO = globalSettingDAO.getGlobalSetting(dslContext,
                GlobalSettingKeys.KEY_DEFAULT_NAME_RULES_EN);
            if (defaultNameRulesDTO == null) {
                defaultNameRules.add(new AccountNameRule(OSTypeEnum.LINUX, "^[a-z_][a-z0-9_-]{2,31}$",
                    i18nService.getI18n("job.manage.globalsettings.defaultNameRules.description.linux",
                        Locale.ENGLISH)));
                defaultNameRules.add(new AccountNameRule(OSTypeEnum.WINDOWS, "^[a-æA-Æ0-9-]{1,32}$",
                    i18nService.getI18n("job.manage.globalsettings.defaultNameRules.description.windows",
                        Locale.ENGLISH)));
                defaultNameRules.add(new AccountNameRule(OSTypeEnum.DATABASE, "^[a-zA-Z0-9\\.\\-\\_]{1,16}$",
                    i18nService.getI18n("job.manage.globalsettings.defaultNameRules.description.database",
                        Locale.ENGLISH)));
                defaultNameRulesDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_DEFAULT_NAME_RULES_EN,
                    JsonUtils.toJson(defaultNameRules),
                    "默认命名规则JSON串(default json-serialized name rules)");
                currentNameRulesDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_CURRENT_NAME_RULES_EN,
                    JsonUtils.toJson(defaultNameRules),
                    "默认命名规则JSON串(default json-serialized name rules)");
                globalSettingDAO.insertGlobalSetting(dslContext, defaultNameRulesDTO);
                globalSettingDAO.insertGlobalSetting(dslContext, currentNameRulesDTO);
            } else {
                defaultNameRules = JsonUtils.fromJson(defaultNameRulesDTO.getValue(),
                    new TypeReference<List<AccountNameRule>>() {
                });
                currentNameRulesDTO = globalSettingDAO.getGlobalSetting(dslContext,
                    GlobalSettingKeys.KEY_CURRENT_NAME_RULES_EN);
            }
        } else {
            //中文环境
            GlobalSettingDTO defaultNameRulesDTO = globalSettingDAO.getGlobalSetting(dslContext,
                GlobalSettingKeys.KEY_DEFAULT_NAME_RULES);
            if (defaultNameRulesDTO == null) {
                defaultNameRules.add(new AccountNameRule(OSTypeEnum.LINUX, "^[a-z_][a-z0-9_-]{2,31}$",
                    i18nService.getI18n("job.manage.globalsettings.defaultNameRules.description.linux",
                        Locale.CHINA)));
                defaultNameRules.add(new AccountNameRule(OSTypeEnum.WINDOWS, "^[a-æA-Æ0-9-]{1,32}$",
                    i18nService.getI18n("job.manage.globalsettings.defaultNameRules.description.windows",
                        Locale.CHINA)));
                defaultNameRules.add(new AccountNameRule(OSTypeEnum.DATABASE, "^[a-zA-Z0-9\\.\\-\\_]{1,16}$",
                    i18nService.getI18n("job.manage.globalsettings.defaultNameRules.description.database",
                        Locale.CHINA)));
                defaultNameRulesDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_DEFAULT_NAME_RULES,
                    JsonUtils.toJson(defaultNameRules),
                    "默认命名规则JSON串(default json-serialized name rules)");
                currentNameRulesDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_CURRENT_NAME_RULES,
                    JsonUtils.toJson(defaultNameRules),
                    "默认命名规则JSON串(default json-serialized name rules)");
                globalSettingDAO.insertGlobalSetting(dslContext, defaultNameRulesDTO);
                globalSettingDAO.insertGlobalSetting(dslContext, currentNameRulesDTO);
            } else {
                defaultNameRules = JsonUtils.fromJson(defaultNameRulesDTO.getValue(),
                    new TypeReference<List<AccountNameRule>>() {
                });
                currentNameRulesDTO = globalSettingDAO.getGlobalSetting(dslContext,
                    GlobalSettingKeys.KEY_CURRENT_NAME_RULES);
            }
        }
        List<AccountNameRule> currentNameRules = new ArrayList<>();
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
    public Boolean setAccountNameRules(String username, AccountNameRulesReq req) {
        GlobalSettingDTO currentNameRulesDTO = globalSettingDAO.getGlobalSetting(dslContext,
            GlobalSettingKeys.KEY_CURRENT_NAME_RULES);
        GlobalSettingDTO inputNameRulesDTO = new GlobalSettingDTO(GlobalSettingKeys.KEY_CURRENT_NAME_RULES,
            JsonUtils.toJson(req.getRules()), String.format("Updated by %s at %s", username,
            DateUtils.defaultLocalDateTime(LocalDateTime.now())));
        if (currentNameRulesDTO == null) {
            return globalSettingDAO.insertGlobalSetting(dslContext, inputNameRulesDTO) == 1;
        } else {
            return globalSettingDAO.updateGlobalSetting(dslContext, inputNameRulesDTO) == 1;
        }
    }

    private Pair<Long, String> parseFileSize(String str) {
        Matcher matcher = PATTERN.matcher(str);
        if (!matcher.matches()) {
            return null;
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        return Pair.of(amount, unit);
    }

    private FileUploadSettingVO getFileUploadSettingsFromStr(String str) {
        Pair<Long, String> configedValue = parseFileSize(str);
        if (configedValue == null) return null;
        return new FileUploadSettingVO(configedValue.getLeft(), configedValue.getRight());
    }

    private FileUploadSettingVO getConfigedFileUploadSettings() {
        log.debug("configedMaxFileSize=" + configedMaxFileSize);
        return getFileUploadSettingsFromStr(configedMaxFileSize);
    }

    private FileUploadSettingVO getDBFileUploadSettings() {
        GlobalSettingDTO fileUploadSettingDTO = globalSettingDAO.getGlobalSetting(dslContext,
            GlobalSettingKeys.KEY_FILE_UPLOAD_MAX_SIZE);
        if (fileUploadSettingDTO == null) {
            return null;
        }
        String maxFileSizeStr = fileUploadSettingDTO.getValue();
        return getFileUploadSettingsFromStr(maxFileSizeStr);
    }

    @Override
    public Boolean saveFileUploadSettings(String username, FileUploadSettingReq req) {
        Long uploadMaxSize = req.getAmount();
        StorageUnitEnum unit = req.getUnit();
        if (unit == null) {
            unit = StorageUnitEnum.B;
        }
        if (uploadMaxSize <= 0) {
            uploadMaxSize = 5L;
            unit = StorageUnitEnum.GB;
        }
        GlobalSettingDTO fileUploadSettingDTO = globalSettingDAO.getGlobalSetting(dslContext,
            GlobalSettingKeys.KEY_FILE_UPLOAD_MAX_SIZE);
        if (fileUploadSettingDTO == null) {
            fileUploadSettingDTO = new GlobalSettingDTO();
            fileUploadSettingDTO.setDescription("max size of upload file");
        }
        fileUploadSettingDTO.setKey(GlobalSettingKeys.KEY_FILE_UPLOAD_MAX_SIZE);
        fileUploadSettingDTO.setValue(uploadMaxSize.toString() + unit.name());
        int affectedRows = globalSettingDAO.upsertGlobalSetting(dslContext, fileUploadSettingDTO);
        return affectedRows > 0;
    }

    @Override
    public FileUploadSettingVO getFileUploadSettings(String username) {
        FileUploadSettingVO fileUploadSettingVO = getDBFileUploadSettings();
        if (fileUploadSettingVO == null) {
            fileUploadSettingVO = getConfigedFileUploadSettings();
        }
        return fileUploadSettingVO;
    }

    @Override
    public Boolean setTitleFooter(String username, SetTitleFooterReq req) {
        //参数校验
        String lang = JobContextUtil.getUserLang();
        if (req.getTitleHead() == null) {
            req.setTitleHead("");
        }
        if (req.getTitleSeparator() == null) {
            req.setTitleSeparator("");
        }
        if (req.getFooterLink() == null) {
            req.setFooterLink("");
        }
        if (req.getFooterCopyRight() == null) {
            req.setFooterCopyRight("");
        }
        GlobalSettingDTO titleFooterDTO = globalSettingDAO.getGlobalSetting(dslContext,
            GlobalSettingKeys.KEY_TITLE_FOOTER);
        if (titleFooterDTO == null) {
            Map<String, TitleFooter> titleFooterLanguageMap = new HashMap<>();
            titleFooterLanguageMap.put(
                LocaleUtils.getNormalLang(lang), new TitleFooter(
                    req.getTitleHead()
                    , req.getTitleSeparator()
                    , req.getFooterLink()
                    , req.getFooterCopyRight()
                ));
            titleFooterDTO = new GlobalSettingDTO(
                GlobalSettingKeys.KEY_TITLE_FOOTER
                , JsonUtils.toJson(
                new TitleFooterDTO(
                    titleFooterLanguageMap
                    , username
                    , TimeUtil.getCurrentTimeStr()))
                , String.format("Updated by %s at %s", username, DateUtils.defaultLocalDateTime(LocalDateTime.now())));
            return globalSettingDAO.insertGlobalSetting(dslContext, titleFooterDTO) == 1;
        } else {
            Map<String, TitleFooter> titleFooterLanguageMap = JsonUtils.fromJson(titleFooterDTO.getValue(),
                new TypeReference<TitleFooterDTO>() {
            }).getTitleFooterLanguageMap();
            titleFooterLanguageMap.put(
                LocaleUtils.getNormalLang(lang), new TitleFooter(
                    req.getTitleHead()
                    , req.getTitleSeparator()
                    , req.getFooterLink()
                    , req.getFooterCopyRight()
                ));
            titleFooterDTO.setValue(JsonUtils.toJson(new TitleFooterDTO(titleFooterLanguageMap
                , username
                , TimeUtil.getCurrentTimeStr())));
            return globalSettingDAO.updateGlobalSetting(dslContext, titleFooterDTO) == 1;
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

    private TitleFooterVO getDefaultTitleFooterVO() {
        String jobEdition = jobManageConfig.getJobEdition();
        if (jobEdition.toLowerCase().equals("ee")) {
            return getEEDefaultTitleFooterVO();
        } else {
            return getCEDefaultTitleFooterVO();
        }
    }

    private TitleFooterVO getInnerDefaultTitleFooterVO() {
        String currentYear = TimeUtil.getCurrentTimeStr("yyyy");
        return new TitleFooterVO(
            i18nService.getI18n("job.manage.globalsettings.defaultTitleHead")
            , "|"
            , String.format(
            "[" + i18nService.getI18n("job.manage.globalsettings.contactBKHelper") + "](%s) | ["
                + i18nService.getI18n("job.manage.globalsettings.BKDesktop") + "](%s)"
            , jobManageConfig.getBkHelperUrl()
            , jobManageConfig.getPaasServerUrl())
            , String.format("Copyright © 2012-%s Tencent BlueKing. All Rights Reserved.", currentYear));
    }

    @Override
    public TitleFooterVO getTitleFooter() {
        GlobalSettingDTO titleFooterSettingDTO = globalSettingDAO.getGlobalSetting(dslContext,
            GlobalSettingKeys.KEY_TITLE_FOOTER);
        if (titleFooterSettingDTO == null) {
            log.warn("Default titleFooter not configured, use system default, plz contact admin to set");
            return getDefaultTitleFooterVO();
        }
        TitleFooterDTO titleFooterDTO = JsonUtils.fromJson(titleFooterSettingDTO.getValue(),
            new TypeReference<TitleFooterDTO>() {
        });
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        log.info("normalLang={}", normalLang);
        TitleFooter titleFooter = titleFooterDTO.getTitleFooterLanguageMap().get(normalLang);
        String currentYear = TimeUtil.getCurrentTimeStr("yyyy");
        if (titleFooter != null) {
            String copyRight = titleFooter.getFooterCopyRight();
            //替换变量
            copyRight = copyRight.replace("${currentYear}", currentYear);
            return new TitleFooterVO(titleFooter.getTitleHead(), titleFooter.getTitleSeparator(),
                titleFooter.getFooterLink(), copyRight);
        } else {
            log.warn("TitleFooter of language:{} not configured, use default:zh_CN", normalLang);
            return getDefaultTitleFooterVO();
        }
    }

    @Override
    public TitleFooterWithDefaultVO getTitleFooterWithDefault(String username) {
        return new TitleFooterWithDefaultVO(getTitleFooter(), getDefaultTitleFooterVO());
    }

    @Override
    public Integer saveChannelTemplate(String username, ChannelTemplateReq req) {
        if (StringUtils.isBlank(req.getChannelCode()) || StringUtils.isBlank(req.getMessageTypeCode())) {
            throw new InvalidParamException("channelCode", "channelCode cannot be blank");
        }
        NotifyTemplateDTO notifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(dslContext, req.getChannelCode(),
            req.getMessageTypeCode(), false);
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        if (notifyTemplateDTO == null) {
            notifyTemplateDTO = new NotifyTemplateDTO(null, req.getMessageTypeCode(), req.getMessageTypeCode(),
                req.getChannelCode(), req.getTitle(), req.getContent(), req.getTitle(), req.getContent(), false,
                username, System.currentTimeMillis(), username, System.currentTimeMillis());
            return notifyTemplateDAO.insertNotifyTemplate(dslContext, notifyTemplateDTO);
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
            return notifyTemplateDAO.updateNotifyTemplateById(dslContext, notifyTemplateDTO);
        }
    }

    @Override
    public Integer sendChannelTemplate(String username, ChannelTemplatePreviewReq req) {
        List<String> receiverList = StringUtil.strToList(req.getReceiverStr(), String.class,
            NotifyConsts.SEPERATOR_COMMA);
        return notifyService.sendNotificationsToUsersByChannel(
            new ServiceUserNotificationDTO(
                new HashSet<>(receiverList),
                new ServiceNotificationMessage(
                    req.getTitle(), req.getContent())),
            Collections.singletonList(req.getChannelCode()));
    }

    @Override
    public ChannelTemplateDetailWithDefaultVO getChannelTemplateDetail(String username, String channelCode,
                                                                       String messageTypeCode) {
        NotifyTemplateDTO currentNotifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(dslContext, channelCode,
            messageTypeCode, false);
        NotifyTemplateDTO defaultNotifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(dslContext, channelCode,
            messageTypeCode, true);
        // 渠道下无默认模板，则为新增渠道，使用通用模板
        if (defaultNotifyTemplateDTO == null) {
            defaultNotifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(dslContext,
                NotifyConsts.NOTIFY_CHANNEL_CODE_COMMON, messageTypeCode, false);
            if (defaultNotifyTemplateDTO == null) {
                defaultNotifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(dslContext,
                    NotifyConsts.NOTIFY_CHANNEL_CODE_COMMON, messageTypeCode, true);
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
                Boolean configStatus = notifyTemplateDAO.existsNotifyTemplate(dslContext, channelCode, messageCode,
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
    public String getDocCenterBaseUrl() {
        String url = "";
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
        String url = "";
        if (StringUtils.isNotBlank(jobManageConfig.getBkFeedBackRoot())) {
            url = removeSuffixBackSlash(jobManageConfig.getBkFeedBackRoot());
        } else {
            // 企业版&社区版
            url = jobManageConfig.getBkCERoot() + "/s-mart/community";
        }
        return url;
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
        urlMap.put(RelatedUrlKeys.KEY_BK_DOC_JOB_ROOT_URL, removeSuffixBackSlash(getDocCenterBaseUrl()) + "/markdown" +
            "/产品白皮书/Introduction/What-is-Job.md");
        urlMap.put(RelatedUrlKeys.KEY_BK_FEED_BACK_ROOT_URL, getFeedBackRootUrl());
        return urlMap;
    }

    private void addFileUploadMaxSizeConfig(String username, Map<String, Object> configMap) {
        FileUploadSettingVO fileUploadSettingVO = getFileUploadSettings(username);
        if (fileUploadSettingVO != null) {
            configMap.put(GlobalSettingKeys.KEY_FILE_UPLOAD_MAX_SIZE, fileUploadSettingVO);
        }
    }

    private void addEnableFeatureFileManageConfig(String username, Map<String, Object> configMap) {
        configMap.put(GlobalSettingKeys.KEY_ENABLE_FEATURE_FILE_MANAGE, enableFeatureFileManage);
    }

    @Override
    public Map<String, Object> getJobConfig(String username) {
        Map<String, Object> configMap = new HashMap<>();
        addFileUploadMaxSizeConfig(username, configMap);
        addEnableFeatureFileManageConfig(username, configMap);
        return configMap;
    }
}
