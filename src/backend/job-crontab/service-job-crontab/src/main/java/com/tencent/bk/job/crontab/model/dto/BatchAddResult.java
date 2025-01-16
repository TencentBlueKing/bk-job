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

package com.tencent.bk.job.crontab.model.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务批量添加到Quartz的结果
 */
@Slf4j
@Data
public class BatchAddResult {
    /**
     * 添加结果Map，key为定时任务ID，value为添加结果
     */
    private Map<Long, AddJobToQuartzResult> resultMap;
    /**
     * 添加成功的任务数量
     */
    private int successNum = 0;
    /**
     * 添加失败的任务数量
     */
    private int failNum = 0;

    /**
     * 添加一个结果数据至批量结果
     *
     * @param result 单个结果数据
     */
    public void addResult(AddJobToQuartzResult result) {
        if (result == null) {
            return;
        }
        if (resultMap == null) {
            resultMap = new HashMap<>();
        }
        CronJobBasicInfoDTO cronJobBasicInfo = result.getCronJobBasicInfo();
        if (cronJobBasicInfo == null) {
            log.info("cronJobBasicInfo is null, ignore");
            return;
        }
        resultMap.put(cronJobBasicInfo.getId(), result);
        if (result.isSuccess()) {
            successNum++;
        } else {
            failNum++;
        }
    }

    /**
     * 合并批量结果
     *
     * @param batchAddResult 待合并的批量结果
     */
    public void merge(BatchAddResult batchAddResult) {
        if (batchAddResult == null) {
            return;
        }
        if (CollectionUtils.isEmpty(batchAddResult.resultMap)) {
            return;
        }
        if (resultMap == null) {
            resultMap = new HashMap<>(batchAddResult.resultMap);
        } else {
            resultMap.putAll(batchAddResult.resultMap);
        }
        successNum += batchAddResult.successNum;
        failNum += batchAddResult.failNum;
    }

    /**
     * 获取批量添加失败的添加结果列表
     *
     * @return 批量添加失败的添加结果列表
     */
    public List<AddJobToQuartzResult> getFailedResultList() {
        if (failNum == 0) {
            return Collections.emptyList();
        }
        List<AddJobToQuartzResult> failedResultList = new ArrayList<>();
        for (Map.Entry<Long, AddJobToQuartzResult> entry : resultMap.entrySet()) {
            if (!entry.getValue().isSuccess()) {
                failedResultList.add(entry.getValue());
            }
        }
        return failedResultList;
    }

    /**
     * 获取批量添加的总数
     *
     * @return 批量添加总数
     */
    public int getTotalNum() {
        return successNum + failNum;
    }

    /**
     * 获取批量添加失败的比率
     *
     * @return 批量添加失败的比率
     */
    public float getFailRate() {
        int totalNum = successNum + failNum;
        if (totalNum == 0) {
            return 0;
        }
        return 1.0f * failNum / totalNum;
    }
}
