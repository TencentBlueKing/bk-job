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

package com.tencent.bk.job.execute.engine.model;

import com.tencent.bk.job.execute.engine.util.FilePathUtils;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 分发的单个文件信息（一个IP+一个Path），含用户输入的原始路径、解析后的真实路径、用于展示的路径等信息
 */
@Data
@ToString(exclude = {"password"})
public class JobFile {
    /**
     * 是否本地文件
     */
    private boolean localUploadFile;
    /**
     * 是否包含敏感信息
     */
    private boolean sensitive;
    /**
     * 文件源主机ip
     */
    private String cloudAreaIdAndIp;
    /**
     * 文件路径(用户输入)
     */
    private String filePath;
    /**
     * 标准化之后的文件路径
     */
    private String standardFilePath;
    /**
     * 文件路径(用于显示，隐藏本地文件路径信息)
     */
    private String displayFilePath;
    /**
     * 解析filePath得到的文件目录，例如：/data、C:。不能以/\结束
     */
    private String dir;

    /**
     * 文件名
     */
    private String fileName;
    /**
     * 执行账号ID
     */
    private Long accountId;
    /**
     * 执行账号别名
     */
    private String accountAlias;
    /**
     * 业务ID
     */
    private Long appId;
    /**
     * 执行账户
     */
    private String account;
    /**
     * 执行账户密码
     */
    private String password;

    /**
     * 文件Key
     */
    private String uniqueKey;

    /**
     * @param cloudAreaIdAndIp 云区域:IP
     * @param dir              目录名称
     * @param fileName         文件名
     */
    public JobFile(String cloudAreaIdAndIp, String dir, String fileName) {
        this.cloudAreaIdAndIp = cloudAreaIdAndIp;
        this.dir = dir;
        this.fileName = fileName;
    }

    /**
     * @param isLocalUploadFile 是否本地文件
     * @param cloudAreaIdAndIp  云区域:IP
     * @param filePath          文件路径
     * @param dir               目录名称
     * @param fileName          文件名
     * @param account           源文件账号
     * @param password          源文件密码
     * @param displayFilePath   要展示的文件路径
     */
    public JobFile(boolean isLocalUploadFile, String cloudAreaIdAndIp, String filePath, String dir,
                   String fileName, String account, String password, String displayFilePath) {
        this.localUploadFile = isLocalUploadFile;
        this.cloudAreaIdAndIp = cloudAreaIdAndIp;
        this.filePath = filePath;
        this.dir = dir;
        this.fileName = fileName;
        this.account = account;
        this.password = password;
        this.displayFilePath = displayFilePath;
        this.sensitive = this.localUploadFile;
    }

    public JobFile(boolean isLocalUploadFile, String cloudAreaIdAndIp, String filePath, String displayFilePath,
                   String dir, String fileName, Long appId, Long accountId, String accountAlias) {
        this.localUploadFile = isLocalUploadFile;
        this.cloudAreaIdAndIp = cloudAreaIdAndIp;
        this.filePath = filePath;
        this.displayFilePath = displayFilePath;
        this.dir = dir;
        this.fileName = fileName;
        this.appId = appId;
        this.accountId = accountId;
        this.accountAlias = accountAlias;
        this.sensitive = this.localUploadFile;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JobFile)) {
            return false;
        }
        JobFile target = (JobFile) obj;

        if (this.cloudAreaIdAndIp == null || target.cloudAreaIdAndIp == null
            || !this.cloudAreaIdAndIp.equals(target.cloudAreaIdAndIp)) {
            return false;
        }
        if (this.dir == null || target.dir == null || !this.dir.equals(target.dir)) {
            return false;
        }
        if (this.fileName == null || target.fileName == null) {
            return false;
        }
        return this.fileName.equals(target.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cloudAreaIdAndIp, dir, fileName, account);
    }

    public String getDisplayFilePath() {
        if (!StringUtils.isEmpty(this.displayFilePath)) {
            return this.displayFilePath;
        }
        if (localUploadFile) {
            this.displayFilePath = this.fileName;
        } else {
            this.displayFilePath = this.filePath;
        }
        return this.displayFilePath;
    }

    public String getFileUniqueKey() {
        if (!StringUtils.isEmpty(this.uniqueKey)) {
            return this.uniqueKey;
        }
        if (localUploadFile) {
            this.uniqueKey = fileName;
        } else {
            this.uniqueKey = getCloudAreaIdAndIp() + ":" + getStandardFilePath();
        }
        return this.uniqueKey;
    }

    public String getStandardFilePath() {
        if (standardFilePath != null) {
            return standardFilePath;
        } else {
            standardFilePath = FilePathUtils.appendFileName(dir, fileName);
        }
        return standardFilePath;
    }

    public boolean isDir() {
        return this.filePath.endsWith("/") || this.filePath.endsWith("\\");
    }
}
