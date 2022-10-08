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

package com.tencent.bk.job.manage.migration;

import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPIPDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对IP白名单数据中不存在hostId的数据使用CMDB中的完整数据补全hostId
 */
@Slf4j
@Service
public class AddHostIdForWhiteIpMigrationTask {

    private final WhiteIPIPDAO whiteIPIPDAO;
    private final BizCmdbClient bizCmdbClient;

    @Autowired
    public AddHostIdForWhiteIpMigrationTask(WhiteIPIPDAO whiteIPIPDAO, BizCmdbClient bizCmdbClient) {
        this.whiteIPIPDAO = whiteIPIPDAO;
        this.bizCmdbClient = bizCmdbClient;
    }

    private String getTaskName() {
        return "migrate_white_ip_ip";
    }

    private AddHostIdResult getInitAddHostIdResult() {
        AddHostIdResult result = new AddHostIdResult(getTaskName());
        result.setTotalRecords(0);
        result.setSuccessRecords(0);
        result.setSuccess(true);
        return result;
    }

    public AddHostIdResult execute(boolean isDryRun) {
        StopWatch watch = new StopWatch(getTaskName());
        try {
            return makeUpWhiteIps(isDryRun, watch);
        } catch (Throwable t) {
            log.warn("Fail to makeUpWhiteIps", t);
            AddHostIdResult result = getInitAddHostIdResult();
            result.setSuccess(false);
            return result;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            log.info("whiteIp makeUp task finished, time consuming:{}", watch.prettyPrint());
        }
    }

    private AddHostIdResult makeUpWhiteIps(boolean isDryRun, StopWatch watch) {
        AddHostIdResult result = getInitAddHostIdResult();
        int start = 0;
        int batchSize = 100;
        List<WhiteIPIPDTO> whiteIpList;
        int totalNullHostIdRecordsCount = 0;
        int totalUpdatedCount = 0;
        do {
            watch.start("listWhiteIPIPWithNullHostId_" + start + "_" + (start + batchSize));
            whiteIpList = whiteIPIPDAO.listWhiteIPIPWithNullHostId(start, batchSize);
            watch.stop();
            if (CollectionUtils.isEmpty(whiteIpList)) {
                continue;
            }
            totalNullHostIdRecordsCount += whiteIpList.size();
            int count = 0;

            watch.start("addHostIdForWhiteIps_" + start + "_" + (start + batchSize));
            int foundHostIdCount = addHostIdForWhiteIps(whiteIpList);
            watch.stop();

            watch.start("updateHostId_" + start + "_" + (start + batchSize));
            log.debug("{} hostIds found for {} whiteIps", foundHostIdCount, whiteIpList.size());
            for (WhiteIPIPDTO whiteIPIPDTO : whiteIpList) {
                if (whiteIPIPDTO.getHostId() != null) {
                    if (isDryRun) {
                        log.info(
                            "[DryRun]set hostId={} for whiteIpIp(id={})",
                            whiteIPIPDTO.getHostId(),
                            whiteIPIPDTO.getId()
                        );
                        count += 1;
                    } else {
                        count += whiteIPIPDAO.updateHostIdById(whiteIPIPDTO.getId(), whiteIPIPDTO.getHostId());
                    }
                }
            }
            watch.stop();

            start += batchSize;
            totalUpdatedCount += count;
            log.info((isDryRun ? "[DryRun]" : "") + "{}/{} whiteIps have been made up", count, whiteIpList.size());
        } while (!CollectionUtils.isEmpty(whiteIpList));
        log.info((isDryRun ? "[DryRun]" : "") + "{} whiteIps have been made up in total", totalUpdatedCount);
        result.setTotalRecords(totalNullHostIdRecordsCount);
        result.setSuccessRecords(totalUpdatedCount);
        result.setSuccess(true);
        return result;
    }

    private int addHostIdForWhiteIps(List<WhiteIPIPDTO> whiteIpList) {
        List<String> cloudIpList = new ArrayList<>();
        List<String> cloudIpv6List = new ArrayList<>();
        for (WhiteIPIPDTO whiteIPIPDTO : whiteIpList) {
            String ip = whiteIPIPDTO.getIp();
            String ipv6 = whiteIPIPDTO.getIpv6();
            if (StringUtils.isNotBlank(ip)) {
                cloudIpList.add(whiteIPIPDTO.getCloudIp());
            } else if (StringUtils.isNotBlank(ipv6)) {
                cloudIpv6List.add(whiteIPIPDTO.getCloudIpv6());
            }
        }
        List<ApplicationHostDTO> hostList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(cloudIpList)) {
            hostList.addAll(bizCmdbClient.listHostsByCloudIps(cloudIpList));
        }
        if (CollectionUtils.isNotEmpty(cloudIpv6List)) {
            hostList.addAll(bizCmdbClient.listHostsByCloudIpv6s(cloudIpv6List));
        }
        Map<String, ApplicationHostDTO> hostMap = new HashMap<>();
        for (ApplicationHostDTO hostDTO : hostList) {
            if (StringUtils.isNotBlank(hostDTO.getIp())) {
                hostMap.put(hostDTO.getCloudAreaId() + ":" + hostDTO.getIp(), hostDTO);
            }
            if (StringUtils.isNotBlank(hostDTO.getIpv6())) {
                hostMap.put(hostDTO.getCloudAreaId() + ":" + hostDTO.getIpv6(), hostDTO);
            }
        }
        int foundHostIdCount = 0;
        for (WhiteIPIPDTO whiteIPIPDTO : whiteIpList) {
            String ip = whiteIPIPDTO.getIp();
            String ipv6 = whiteIPIPDTO.getIpv6();
            if (StringUtils.isNotBlank(ip) && hostMap.containsKey(whiteIPIPDTO.getCloudIp())) {
                whiteIPIPDTO.setHostId(hostMap.get(whiteIPIPDTO.getCloudIp()).getHostId());
                foundHostIdCount += 1;
                log.info(
                    "Makeup whiteIp {}(cloudIp={}) with hostId={}",
                    whiteIPIPDTO.getId(),
                    whiteIPIPDTO.getCloudIp(),
                    whiteIPIPDTO.getHostId()
                );
            } else if (StringUtils.isNotBlank(ipv6) && hostMap.containsKey(whiteIPIPDTO.getCloudIpv6())) {
                whiteIPIPDTO.setHostId(hostMap.get(whiteIPIPDTO.getCloudIpv6()).getHostId());
                foundHostIdCount += 1;
                log.info(
                    "Makeup whiteIp {}(cloudIpv6={}) with hostId={}",
                    whiteIPIPDTO.getId(),
                    whiteIPIPDTO.getCloudIpv6(),
                    whiteIPIPDTO.getHostId()
                );
            }
        }
        return foundHostIdCount;
    }
}
