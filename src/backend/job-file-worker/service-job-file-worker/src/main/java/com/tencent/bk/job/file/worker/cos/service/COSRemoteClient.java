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

package com.tencent.bk.job.file.worker.cos.service;

import com.tencent.bk.job.file.worker.cos.JobTencentInnerCOSClient;
import com.tencent.bk.job.file.worker.model.FileMetaData;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class COSRemoteClient implements RemoteClient {

    JobTencentInnerCOSClient jobTencentInnerCOSClient;

    public COSRemoteClient(JobTencentInnerCOSClient jobTencentInnerCOSClient) {
        this.jobTencentInnerCOSClient = jobTencentInnerCOSClient;
    }

    private List<String> parsePath(String rawPath) {
        int i = rawPath.indexOf("/");
        String bucketName = rawPath.substring(0, i);
        String key = rawPath.substring(i + 1);
        List<String> list = new ArrayList<>();
        list.add(bucketName);
        list.add(key);
        return list;
    }

    @Override
    public FileMetaData getFileMetaData(String filePath) {
        List<String> pathList = parsePath(filePath);
        return jobTencentInnerCOSClient.getFileMetaData(pathList.get(0), pathList.get(1));
    }

    @Override
    public Pair<InputStream, Long> getFileInputStream(String filePath) {
        List<String> pathList = parsePath(filePath);
        return jobTencentInnerCOSClient.getFileInputStream(pathList.get(0), pathList.get(1));
    }

    @Override
    public void shutdown() {
        jobTencentInnerCOSClient.shutdown();
    }
}
