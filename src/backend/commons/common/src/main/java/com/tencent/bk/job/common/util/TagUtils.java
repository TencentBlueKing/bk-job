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

package com.tencent.bk.job.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since 27/9/2019 17:29
 */
@Slf4j
public class TagUtils {
    private static final String PREFIX = "<";
    private static final String SUFFIX = ">";
    private static final String SEPARATOR = ",";

    public static String buildDbTag(Long tagId) {
        return PREFIX + tagId + SUFFIX;
    }

    public static String buildDbTagList(List<Long> tagIdList) {
        if (CollectionUtils.isEmpty(tagIdList)) {
            return null;
        }
        StringBuilder tagListStringBuilder = new StringBuilder();
        for (Long tagId : tagIdList) {
            if (tagId != null && tagId > 0) {
                tagListStringBuilder.append(buildDbTag(tagId));
                tagListStringBuilder.append(SEPARATOR);
            } else {
                log.warn("Empty tag id!");
            }
        }
        if (tagListStringBuilder.length() == 0) {
            return null;
        }
        tagListStringBuilder.deleteCharAt(tagListStringBuilder.length() - 1);
        return tagListStringBuilder.toString();
    }

    public static List<Long> decodeDbTag(String tags) {
        if (StringUtils.isBlank(tags)) {
            return Collections.emptyList();
        }
        String[] tagStringList = tags.split(SEPARATOR);
        if (tagStringList.length <= 0) {
            return Collections.emptyList();
        }
        List<Long> tagList = new ArrayList<>();
        for (String tagString : tagStringList) {
            if (tagString.startsWith(PREFIX) && tagString.endsWith(SUFFIX)) {
                tagString = tagString.substring(1, tagString.length() - 1);
                try {
                    Long tag = Long.valueOf(tagString);
                    if (tag > 0) {
                        tagList.add(tag);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Tag must be number {}!", tagString);
                } catch (Exception e) {
                    log.error("Exception while processing tag {}!", tagString, e);
                }
            }
        }
        return tagList;
    }
}
