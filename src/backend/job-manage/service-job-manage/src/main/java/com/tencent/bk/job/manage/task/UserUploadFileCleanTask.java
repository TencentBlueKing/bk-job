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

package com.tencent.bk.job.manage.task;

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.manage.config.StorageSystemConfig;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @since 25/1/2021 21:19
 */
@Slf4j
@Component
public class UserUploadFileCleanTask {

    private static final String FILE_CLEAN_TASK_LOCK_KEY = "user:upload:file:clean";
    private final String uploadPath;
    private final File uploadDirectory;
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanService taskPlanService;

    public UserUploadFileCleanTask(StorageSystemConfig storageSystemConfig, TaskTemplateService taskTemplateService,
                                   TaskPlanService taskPlanService) {
        this.uploadPath = storageSystemConfig.getJobStorageRootPath() + "/localupload/";
        this.taskTemplateService = taskTemplateService;
        this.taskPlanService = taskPlanService;
        this.uploadDirectory = new File(uploadPath);
    }

    public void execute() {
        String executeId = UUID.randomUUID().toString();

        try {
            if (LockUtils.tryGetDistributedLock(FILE_CLEAN_TASK_LOCK_KEY, executeId, 3600_000L)) {
                Set<String> skipFile = loadFileListFromDb();
                processFile(skipFile);
            } else {
                log.info("Some one else is running this task! Skip!");
            }
        } catch (Exception e) {
            log.error("Error while running user upload file clean task!", e);
        } finally {
            LockUtils.releaseDistributedLock(FILE_CLEAN_TASK_LOCK_KEY, executeId);
        }
    }

    private Set<String> loadFileListFromDb() {
        Set<String> templateFile = taskTemplateService.listLocalFiles();
        Set<String> planFile = taskPlanService.listLocalFiles();
        return Sets.union(templateFile, planFile);
    }

    private void processFile(Set<String> skipFile) {
        Date thresholdDate = new Date(System.currentTimeMillis() - 7 * 3600 * 24 * 1000);
        Iterator<File> fileIterator = FileUtils.iterateFiles(uploadDirectory, new AgeFileFilter(thresholdDate),
            TrueFileFilter.TRUE);
        while (fileIterator.hasNext()) {
            File aFile = fileIterator.next();
            if (skipFile.contains(aFile.getPath().replaceAll(uploadPath, ""))) {
                if (log.isDebugEnabled()) {
                    log.debug("Skip file {}", aFile.getPath());
                }
                continue;
            } else {
                log.info("Delete file {}", aFile.getPath());
                FileUtils.deleteQuietly(aFile);
            }

            try {
                deleteEmptyDirectory(aFile.getParentFile());
                deleteEmptyDirectory(aFile.getParentFile().getParentFile());
            } catch (Exception e) {
                log.warn("Error while delete empty parent of file {}", aFile.getPath());
            }
        }
    }

    private void deleteEmptyDirectory(File directory) {
        if (directory == null) {
            log.warn("Directory is null!");
            return;
        }

        if (isEmpty(directory.toPath())) {
            boolean delete = directory.delete();
            if (delete) {
                log.info("Delete empty directory {}", directory.getPath());
            } else {
                log.warn("Delete directory {} failed!", directory.getPath());
            }
        } else {
            log.debug("Directory {} not empty!", directory.getPath());
        }
    }

    private boolean isEmpty(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            } catch (IOException e) {
                return false;
            }
        }

        return false;
    }
}
