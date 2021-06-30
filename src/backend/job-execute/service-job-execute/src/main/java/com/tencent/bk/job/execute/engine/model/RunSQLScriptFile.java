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

/**
 * 脚本文件
 */
public class RunSQLScriptFile {

    private String sqlScriptContent;
    private String sqlScriptFileName;
    private String publicScriptContent;
    private String publicScriptName;
    private String paramForDBInfo;
    private String downloadPath;
    private int timeout;

    public String getSqlScriptContent() {
        return sqlScriptContent;
    }

    public void setSqlScriptContent(String sqlScriptContent) {
        this.sqlScriptContent = sqlScriptContent;
    }

    public String getSqlScriptFileName() {
        return sqlScriptFileName;
    }

    public void setSqlScriptFileName(String sqlScriptFileName) {
        this.sqlScriptFileName = sqlScriptFileName;
    }

    public String getPublicScriptContent() {
        return publicScriptContent;
    }

    public void setPublicScriptContent(String publicScriptContent) {
        this.publicScriptContent = publicScriptContent;
    }

    public String getPublicScriptName() {
        return publicScriptName;
    }

    public void setPublicScriptName(String publicScriptName) {
        this.publicScriptName = publicScriptName;
    }

    public String getParamForDBInfo() {
        return paramForDBInfo;
    }

    public void setParamForDBInfo(String paramForDBInfo) {
        this.paramForDBInfo = paramForDBInfo;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}

