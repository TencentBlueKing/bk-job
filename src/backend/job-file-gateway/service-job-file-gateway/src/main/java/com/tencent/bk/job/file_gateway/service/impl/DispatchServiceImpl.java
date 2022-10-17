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

package com.tencent.bk.job.file_gateway.service.impl;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.consts.WorkerSelectModeEnum;
import com.tencent.bk.job.file_gateway.consts.WorkerSelectScopeEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.metrics.MetricsConstants;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.service.AbilityTagService;
import com.tencent.bk.job.file_gateway.service.DispatchService;
import com.tencent.bk.job.file_gateway.service.FileWorkerService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DispatchServiceImpl implements DispatchService {

    private final AbilityTagService abilityTagService;
    private final MeterRegistry meterRegistry;
    private final FileWorkerDAO fileWorkerDAO;
    private final FileWorkerService fileWorkerService;

    @Autowired
    public DispatchServiceImpl(FileWorkerDAO fileWorkerDAO,
                               AbilityTagService abilityTagService,
                               MeterRegistry meterRegistry,
                               FileWorkerService fileWorkerService) {
        this.fileWorkerDAO = fileWorkerDAO;
        this.abilityTagService = abilityTagService;
        this.meterRegistry = meterRegistry;
        this.fileWorkerService = fileWorkerService;
    }

    private List<FileWorkerDTO> getFileWorkerByScope(Long appId, String workerSelectScope) {
        WorkerIdsCondition workerIdsCondition = fileWorkerService.getIncludedAndExcludedWorkerIds();
        if (WorkerSelectScopeEnum.APP.name().equals(workerSelectScope)) {
            return fileWorkerDAO.listFileWorkers(
                appId,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
        } else if (WorkerSelectScopeEnum.PUBLIC.name().equals(workerSelectScope)) {
            return fileWorkerDAO.listPublicFileWorkers(
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
        } else if (WorkerSelectScopeEnum.ALL.name().equals(workerSelectScope)) {
            List<FileWorkerDTO> finalWorkerList = new ArrayList<>();
            finalWorkerList.addAll(fileWorkerDAO.listPublicFileWorkers(
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            ));
            finalWorkerList.addAll(fileWorkerDAO.listFileWorkers(
                appId,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            ));
            return finalWorkerList;
        } else {
            log.error("not supported workerSelectScope:{}, use public workers", workerSelectScope);
            return fileWorkerDAO.listPublicFileWorkers(
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
        }
    }

    private List<FileWorkerDTO> getFileWorkerByScopeAndAbilityTag(Long appId,
                                                                  String workerSelectScope,
                                                                  String abilityTag) {
        WorkerIdsCondition workerIdsCondition = fileWorkerService.getIncludedAndExcludedWorkerIds();
        if (WorkerSelectScopeEnum.APP.name().equals(workerSelectScope)) {
            return fileWorkerDAO.listFileWorkersByAbilityTag(
                appId,
                abilityTag,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
        } else if (WorkerSelectScopeEnum.PUBLIC.name().equals(workerSelectScope)) {
            return fileWorkerDAO.listPublicFileWorkersByAbilityTag(
                abilityTag,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
        } else if (WorkerSelectScopeEnum.ALL.name().equals(workerSelectScope)) {
            List<FileWorkerDTO> finalWorkerList = new ArrayList<>();
            finalWorkerList.addAll(fileWorkerDAO.listFileWorkersByAbilityTag(
                appId,
                abilityTag,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            ));
            finalWorkerList.addAll(fileWorkerDAO.listPublicFileWorkersByAbilityTag(
                abilityTag,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            ));
            return finalWorkerList;
        } else {
            log.error("not supported workerSelectScope:{}, use public workers", workerSelectScope);
            return fileWorkerDAO.listPublicFileWorkersByAbilityTag(
                abilityTag,
                workerIdsCondition.getIncludedWorkerIds(),
                workerIdsCondition.getExcludedWorkerIds()
            );
        }
    }

    @Override
    public FileWorkerDTO findBestFileWorker(FileSourceDTO fileSourceDTO) {
        Timer.Sample sample = Timer.start(meterRegistry);
        FileWorkerDTO fileWorkerDTO = findBestFileWorkerIndeed(fileSourceDTO);
        long nanoSeconds = sample.stop(meterRegistry.timer(MetricsConstants.NAME_FILE_GATEWAY_DISPATCH_TIME,
            MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_FILE_GATEWAY));
        long millis = TimeUnit.NANOSECONDS.toMillis(nanoSeconds);
        if (millis > 2000) {
            log.warn("Dispatch time over 2000ms, fileSourceDTO={}", JsonUtils.toJson(fileSourceDTO));
        }
        return fileWorkerDTO;
    }

    private FileWorkerDTO findWorkerByAuto(FileSourceDTO fileSourceDTO) {
        FileWorkerDTO fileWorkerDTO = null;
        String workerSelectScope = fileSourceDTO.getWorkerSelectScope();
        List<String> abilityTagList = abilityTagService.getAbilityTagList(fileSourceDTO);
        List<FileWorkerDTO> fileWorkerDTOList;
        if (abilityTagList == null || abilityTagList.size() == 0) {
            // 无能力标签要求，任选一个FileWorker
            fileWorkerDTOList = getFileWorkerByScope(fileSourceDTO.getAppId(), workerSelectScope);
            if (fileWorkerDTOList.isEmpty()) {
                log.error("cannot find any file worker!");
                return null;
            }
        } else {
            String abilityTag = abilityTagList.get(0);
            fileWorkerDTOList = getFileWorkerByScopeAndAbilityTag(
                fileSourceDTO.getAppId(),
                workerSelectScope,
                abilityTag
            );
            log.debug("abilityTag:{}, workerNum:{}", abilityTag, fileWorkerDTOList.size());
            // 能力交集
            for (int i = 1; i < abilityTagList.size(); i++) {
                List<FileWorkerDTO> tmpFileWorkerDTOList = getFileWorkerByScopeAndAbilityTag(
                    fileSourceDTO.getAppId(),
                    workerSelectScope,
                    abilityTag
                );
                log.debug("abilityTag:{}, workerNum:{}", abilityTag, tmpFileWorkerDTOList.size());
                abilityTag = abilityTagList.get(i);
                // 取交集
                fileWorkerDTOList.retainAll(tmpFileWorkerDTOList);
                log.debug("after intersection of {}, retained workNum={}", abilityTag, fileWorkerDTOList.size());
            }
        }
        if (fileWorkerDTOList.isEmpty()) {
            log.warn("cannot find any file worker after ability intersection");
            return null;
        }
        // 在线状态过滤
        fileWorkerDTOList = fileWorkerDTOList.parallelStream().filter(tmpFileWorkerDTO -> {
            Byte onlineStatus = tmpFileWorkerDTO.getOnlineStatus();
            return onlineStatus != null && onlineStatus.intValue() == 1;
        }).collect(Collectors.toList());
        if (!fileWorkerDTOList.isEmpty()) {
            // 按策略调度：内存占用最小
            fileWorkerDTOList.sort(Comparator.comparing(FileWorkerDTO::getMemRate));
            log.debug("ordered fileWorkerDTOList:{}", fileWorkerDTOList);
            fileWorkerDTO = fileWorkerDTOList.get(0);
        } else {
            log.error("Cannot find available file worker, abilityTagList={}", abilityTagList);
        }
        return fileWorkerDTO;
    }

    private FileWorkerDTO findBestFileWorkerIndeed(FileSourceDTO fileSourceDTO) {
        String mode = fileSourceDTO.getWorkerSelectMode();
        FileWorkerDTO fileWorkerDTO;
        log.info("select worker with mode={},fileSourceDTO={}", mode, JsonUtils.toJson(fileSourceDTO));
        if (WorkerSelectModeEnum.MANUAL.name().equals(mode)) {
            Long workerId = fileSourceDTO.getWorkerId();
            fileWorkerDTO = fileWorkerDAO.getFileWorkerById(workerId);
            if (!fileWorkerDTO.isOnline()) {
                log.info("Worker selected manually is not online");
                return null;
            }
        } else if (WorkerSelectModeEnum.AUTO.name().equals(mode)) {
            fileWorkerDTO = findWorkerByAuto(fileSourceDTO);
        } else {
            throw new RuntimeException(String.format("workerSelectMode %s not supported yet", mode));
        }
        if (fileWorkerDTO != null) {
            log.info("FileSource ({},{}) choose worker:{}", fileSourceDTO.getCode(), fileSourceDTO.getAlias(),
                JsonUtils.toJson(fileWorkerDTO.toBaseVO()));
        } else {
            log.info("FileSource ({},{}) can not find worker", fileSourceDTO.getCode(), fileSourceDTO.getAlias());
        }
        return fileWorkerDTO;
    }
}
