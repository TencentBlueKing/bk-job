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

package com.tencent.bk.job.manage.service.globalsetting.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpRequest;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.common.constants.globalsetting.GlobalSettingKeys;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.globalsetting.GlobalSettingDAO;
import com.tencent.bk.job.manage.model.dto.GlobalSettingDTO;
import com.tencent.bk.job.manage.model.dto.globalsetting.HelperInfo;
import com.tencent.bk.job.manage.model.dto.globalsetting.TitleFooter;
import com.tencent.bk.job.manage.model.dto.globalsetting.TitleFooterDTO;
import com.tencent.bk.job.manage.model.migration.BkPlatformInfo;
import com.tencent.bk.job.manage.service.globalsetting.BkPlatformInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 将全局配置中的平台信息导出为蓝鲸统一规范的平台信息数据
 */
@Slf4j
@Service
public class BkPlatformInfoServiceImpl implements BkPlatformInfoService {

    private final GlobalSettingDAO globalSettingDAO;
    private final JobManageConfig jobManageConfig;

    @Autowired
    public BkPlatformInfoServiceImpl(GlobalSettingDAO globalSettingDAO, JobManageConfig jobManageConfig) {
        this.globalSettingDAO = globalSettingDAO;
        this.jobManageConfig = jobManageConfig;
    }

    @Override
    public BkPlatformInfo getCurrentBkPlatformInfo() {
        GlobalSettingDTO titleFooterSettingDTO = globalSettingDAO.getGlobalSetting(GlobalSettingKeys.KEY_TITLE_FOOTER);
        TitleFooterDTO titleFooterDTO = null;
        if (titleFooterSettingDTO != null) {
            titleFooterDTO = JsonUtils.fromJson(titleFooterSettingDTO.getValue(), new TypeReference<TitleFooterDTO>() {
            });
        }
        GlobalSettingDTO bkHelperSettingDTO = globalSettingDAO.getGlobalSetting(GlobalSettingKeys.KEY_BK_HELPER);
        log.info("titleFooterSettingDTO={}, bkHelperSettingDTO={}", titleFooterSettingDTO, bkHelperSettingDTO);
        HelperInfo helperInfo = null;
        if (bkHelperSettingDTO != null && StringUtils.isNotEmpty(bkHelperSettingDTO.getValue())) {
            helperInfo = JsonUtils.fromJson(bkHelperSettingDTO.getValue(), HelperInfo.class);
        }
        BkPlatformInfo defaultBkPlatformInfo = getDefaultBkPlatformInfo();
        BkPlatformInfo bkPlatformInfo = buildBkPlatformInfo(defaultBkPlatformInfo, titleFooterDTO, helperInfo);
        log.info("bkPlatformInfo={}", JsonUtils.toJson(bkPlatformInfo));
        return bkPlatformInfo;
    }

    private BkPlatformInfo getDefaultBkPlatformInfo() {
        try {
            return getBkPlatformInfoFromArtifactory();
        } catch (Exception e) {
            log.warn("Fail to getBkPlatformInfoFromArtifactory, use local default", e);
            return new BkPlatformInfo();
        }
    }

    private BkPlatformInfo getBkPlatformInfoFromArtifactory() {
        String bkSharedResUrl = jobManageConfig.getBkSharedResUrl();
        String bkSharedBaseJsPath = jobManageConfig.getBkSharedBaseJsPath();
        String baseJsonFileUrl = buildBaseJsFileUrl(bkSharedResUrl, bkSharedBaseJsPath);
        HttpHelper httpHelper = HttpHelperFactory.getDefaultHttpHelper();
        HttpRequest request = HttpRequest.builder(HttpMethodEnum.GET, baseJsonFileUrl).build();
        HttpResponse resp = httpHelper.requestForSuccessResp(request);
        String baseJsStr = resp.getEntity();
        log.info("Got base.js content from bkSharedRes: " + baseJsStr);
        String platformJsonStr = parsePlatformJsonStr(baseJsStr);
        return JsonUtils.fromJson(platformJsonStr, new TypeReference<BkPlatformInfo>() {
        });
    }

    private String parsePlatformJsonStr(String baseJsStr) {
        int startIndex = baseJsStr.indexOf("{");
        int endIndex = baseJsStr.lastIndexOf("}");
        return baseJsStr.substring(startIndex, endIndex + 1);
    }

    /**
     * 构建获取base.js文件的URL
     *
     * @param bkSharedResUrl     共享资源基础路径
     * @param bkSharedBaseJsPath base.js文件路径
     * @return base.js文件对应的URL
     */
    private String buildBaseJsFileUrl(String bkSharedResUrl, String bkSharedBaseJsPath) {
        if (bkSharedResUrl.endsWith("/")) {
            bkSharedResUrl = StringUtil.removeSuffix(bkSharedResUrl, "/");
        }
        if (bkSharedBaseJsPath.startsWith("/")) {
            return bkSharedResUrl + bkSharedBaseJsPath;
        }
        return bkSharedResUrl + "/" + bkSharedBaseJsPath;
    }

    private BkPlatformInfo buildBkPlatformInfo(BkPlatformInfo defaultBkPlatformInfo,
                                               TitleFooterDTO titleFooterDTO,
                                               HelperInfo helperInfo) {
        if (titleFooterDTO != null) {
            Map<String, TitleFooter> titleFooterLanguageMap = titleFooterDTO.getTitleFooterLanguageMap();
            if (titleFooterLanguageMap != null) {
                TitleFooter englishTitleFooter = getEnglishTitleFooter(titleFooterLanguageMap);
                TitleFooter chineseTitleFooter = getChineseTitleFooter(titleFooterLanguageMap);
                if (englishTitleFooter != null) {
                    defaultBkPlatformInfo.setNameEn(englishTitleFooter.getTitleHead());
                    defaultBkPlatformInfo.setFooterInfoEn(englishTitleFooter.getFooterLink());
                    defaultBkPlatformInfo.setFooterCopyright(englishTitleFooter.getFooterCopyRight());
                }
                if (chineseTitleFooter != null) {
                    defaultBkPlatformInfo.setName(chineseTitleFooter.getTitleHead());
                    defaultBkPlatformInfo.setFooterInfo(chineseTitleFooter.getFooterLink());
                    defaultBkPlatformInfo.setFooterCopyright(chineseTitleFooter.getFooterCopyRight());
                }
            }
        }
        if (helperInfo != null) {
            String contactLink = helperInfo.getContactLink();
            defaultBkPlatformInfo.setHelperLink(contactLink);
        }
        return defaultBkPlatformInfo;
    }

    private TitleFooter getEnglishTitleFooter(Map<String, TitleFooter> titleFooterLanguageMap) {
        TitleFooter titleFooter = titleFooterLanguageMap.get(LocaleUtils.LANG_EN);
        if (titleFooter == null) {
            titleFooter = titleFooterLanguageMap.get(LocaleUtils.LANG_EN_US);
        }
        return titleFooter;
    }

    private TitleFooter getChineseTitleFooter(Map<String, TitleFooter> titleFooterLanguageMap) {
        TitleFooter titleFooter = titleFooterLanguageMap.get(LocaleUtils.LANG_ZH_CN);
        if (titleFooter == null) {
            titleFooter = titleFooterLanguageMap.get(LocaleUtils.LANG_ZH);
        }
        return titleFooter;
    }
}
