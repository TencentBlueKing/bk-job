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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.model.dto.ArchiveSummary;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ArchiveSummaryHolder {
    private Map<String, ArchiveSummary> summaryMap = new ConcurrentHashMap<>();
    private Long endTimeInMills;

    private ArchiveSummaryHolder() {
    }

    public static ArchiveSummaryHolder getInstance() {
        return Inner.instance;
    }

    public void init(Long endTimeInMills) {
        this.summaryMap.clear();
        this.endTimeInMills = endTimeInMills;
    }

    public void addArchiveSummary(ArchiveSummary summary) {
        if (summary == null) {
            return;
        }
        summary.setArchiveEndDate(DateUtils.formatUnixTimestamp(endTimeInMills, ChronoUnit.MILLIS));
        summaryMap.put(summary.getTableName(), summary);
    }

    public void print() {
        log.info("Archive summary : {}", JsonUtils.toJson(summaryMap.values()));
    }

    private static class Inner {
        private static final ArchiveSummaryHolder instance = new ArchiveSummaryHolder();
    }
}
