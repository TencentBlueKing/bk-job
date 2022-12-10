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

package com.tencent.bk.job.execute.engine.util;

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.common.util.function.LambdasUtil;
import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;
import com.tencent.bk.job.execute.engine.model.FileDest;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 源文件工具类
 */
public class JobSrcFileUtils {
    /**
     * 构造源文件路径与目标文件路径的映射关系
     *
     * @param srcFiles       源文件
     * @param targetDir      目标目录
     * @param targetFileName 文件分发到目标主机的对应名称
     * @return 源文件路径与目标文件路径的映射关系
     */
    public static Map<String, FileDest> buildSourceDestPathMapping(Set<JobFile> srcFiles,
                                                                   String targetDir,
                                                                   String targetFileName) {
        Map<String, FileDest> sourceDestPathMap = new HashMap<>();
        String standardTargetDir = FilePathUtils.standardizedDirPath(targetDir);
        long currentTime = System.currentTimeMillis();
        for (JobFile srcFile : srcFiles) {
            // 本地文件的源ip是本机ip，展开源文件IP地址宏采用"0.0.0.0"
            String destDirPath = MacroUtil.resolveFileSrcIpMacro(standardTargetDir,
                srcFile.getFileType() == TaskFileTypeEnum.LOCAL ? "0_0.0.0.0" : srcFile.getHost().toCloudIp());
            destDirPath = MacroUtil.resolveDate(destDirPath, currentTime);
            addSourceDestPathMapping(sourceDestPathMap, srcFile, destDirPath, targetFileName);
        }
        return sourceDestPathMap;
    }

    private static void addSourceDestPathMapping(Map<String, FileDest> sourceDestPathMap,
                                                 JobFile sourceFile,
                                                 String destDirPath,
                                                 String destName) {
        sourceDestPathMap.put(sourceFile.getUniqueKey(), buildFileDest(sourceFile, destDirPath, destName));
    }

    private static FileDest buildFileDest(JobFile sourceFile, String destDirPath, String destName) {
        if (sourceFile.isDir()) {
            String destPath = FilePathUtils.appendDirName(destDirPath, FilePathUtils.parseDirName(sourceFile.getDir()));
            return new FileDest(destPath, destDirPath, "");
        } else {
            String destFileName = StringUtils.isNotBlank(destName) ? destName : sourceFile.getFileName();
            String destPath = FilePathUtils.appendFileName(destDirPath, destFileName);
            return new FileDest(destPath, destDirPath, destFileName);
        }
    }

    private static boolean isServerOrThirdFileSource(FileSourceDTO fileSource) {
        // 兼容没有fileType字段的数据
        if (fileSource.getFileType() == null) {
            return !fileSource.isLocalUpload();
        }
        return fileSource.getFileType() == TaskFileTypeEnum.SERVER.getType()
            || fileSource.getFileType() == TaskFileTypeEnum.FILE_SOURCE.getType();
    }

    /**
     * 从步骤解析源文件，处理服务器文件、本地文件、第三方源文件的差异，统一为IP+Path信息
     *
     * @param stepInstance      步骤
     * @param localHost         job local host
     * @param jobStorageRootDir job共享存储根目录
     * @return 多个要分发的源文件信息集合
     */
    public static Set<JobFile> parseSendFileList(StepInstanceDTO stepInstance, HostDTO localHost,
                                                 String jobStorageRootDir) {
        Set<JobFile> sendFiles = Sets.newHashSet();
        for (FileSourceDTO fileSource : stepInstance.getFileSourceList()) {
            List<FileDetailDTO> files = fileSource.getFiles();
            if (isServerOrThirdFileSource(fileSource)) {
                boolean isThirdFile = false;
                Integer fileSourceId = fileSource.getFileSourceId();
                if (fileSourceId != null && fileSourceId > 0) {
                    // 第三方文件源文件
                    isThirdFile = true;
                }
                // 远程服务器文件分发
                Long accountId = fileSource.getAccountId();
                String accountAlias = fileSource.getAccountAlias();
                // 远程文件
                List<HostDTO> sourceHosts = fileSource.getServers().getIpList();
                Set<HostDTO> invalidHosts = stepInstance.getInvalidHosts();
                for (FileDetailDTO file : files) {
                    String filePath = StringUtils.isNotEmpty(file.getResolvedFilePath()) ? file.getResolvedFilePath()
                        : file.getFilePath();
                    Pair<String, String> fileNameAndPath = FilePathUtils.parseDirAndFileName(filePath);
                    String dir = fileNameAndPath.getLeft();
                    String fileName = fileNameAndPath.getRight();
                    Predicate<HostDTO> predicate = LambdasUtil.not(invalidHosts::contains);
                    for (HostDTO sourceHost : sourceHosts) {
                        if (predicate.test(sourceHost)) {
                            // 第三方源文件的displayName不同
                            if (isThirdFile) {
                                sendFiles.add(new JobFile(
                                    TaskFileTypeEnum.FILE_SOURCE,
                                    sourceHost,
                                    file.getThirdFilePathWithFileSourceName(),
                                    file.getThirdFilePathWithFileSourceName(),
                                    dir,
                                    fileName,
                                    stepInstance.getAppId(),
                                    accountId,
                                    accountAlias
                                ));
                            } else {
                                sendFiles.add(new JobFile(
                                    TaskFileTypeEnum.SERVER,
                                    sourceHost,
                                    filePath,
                                    filePath,
                                    dir,
                                    fileName,
                                    stepInstance.getAppId(),
                                    accountId,
                                    accountAlias
                                ));
                            }
                        }
                    }

                }
            } else if (fileSource.getFileType() == TaskFileTypeEnum.LOCAL.getType()) {
                // 本地文件
                for (FileDetailDTO file : files) {
                    Pair<String, String> fileNameAndPath = FilePathUtils.parseDirAndFileName(file.getFilePath());
                    String dir = NFSUtils.getFileDir(jobStorageRootDir, FileDirTypeConf.UPLOAD_FILE_DIR)
                        + fileNameAndPath.getLeft();
                    String fileName = fileNameAndPath.getRight();
                    ServersDTO servers = fileSource.getServers();
                    if (servers != null && servers.getIpList() != null && !servers.getIpList().isEmpty()) {
                        List<HostDTO> ipList = servers.getIpList();
                        for (HostDTO hostDTO : ipList) {
                            sendFiles.add(new JobFile(TaskFileTypeEnum.LOCAL, hostDTO, file.getFilePath(), dir,
                                fileName, "root", null,
                                FilePathUtils.parseDirAndFileName(file.getFilePath()).getRight()));
                        }
                    } else {
                        sendFiles.add(new JobFile(TaskFileTypeEnum.LOCAL, localHost, file.getFilePath(), dir,
                            fileName, "root", null,
                            FilePathUtils.parseDirAndFileName(file.getFilePath()).getRight()));
                    }
                }
            } else if (fileSource.getFileType() == TaskFileTypeEnum.BASE64_FILE.getType()) {
                // 配置文件
                for (FileDetailDTO file : files) {
                    Pair<String, String> fileNameAndPath = FilePathUtils.parseDirAndFileName(file.getFilePath());
                    String dir = NFSUtils.getFileDir(jobStorageRootDir, FileDirTypeConf.UPLOAD_FILE_DIR)
                        + fileNameAndPath.getLeft();
                    String fileName = fileNameAndPath.getRight();
                    List<HostDTO> ipList = fileSource.getServers().getIpList();
                    for (HostDTO hostDTO : ipList) {
                        sendFiles.add(new JobFile(TaskFileTypeEnum.BASE64_FILE, hostDTO, file.getFilePath(), dir,
                            fileName, "root", null,
                            FilePathUtils.parseDirAndFileName(file.getFilePath()).getRight()));
                    }
                }
            }
        }
        return sendFiles;
    }

    /**
     * 构造源文件原始路径与显示路径的映射关系
     *
     * @param sourceFiles    源文件
     * @param localUploadDir 本地上传文件根目录
     * @return 源文件原始路径与显示路径的映射关系
     */
    public static Map<String, String> buildSourceFileDisplayMapping(Set<JobFile> sourceFiles, String localUploadDir) {
        Map<String, String> sourceFileDisplayMap = new HashMap<>();
        sourceFiles.forEach(sourceFile -> {
            String standardPath = sourceFile.getStandardFilePath();
            if (sourceFile.getFileType() == TaskFileTypeEnum.LOCAL && !standardPath.startsWith(localUploadDir)) {
                sourceFileDisplayMap.put(PathUtil.joinFilePath(localUploadDir, standardPath),
                    sourceFile.getDisplayFilePath());
            } else {
                sourceFileDisplayMap.put(standardPath, sourceFile.getDisplayFilePath());
            }
        });
        return sourceFileDisplayMap;
    }
}
