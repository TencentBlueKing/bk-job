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

package com.tencent.bk.job.file.worker.cos;

import com.tencent.bk.job.file.worker.model.FileMetaData;
import com.tencent.cos.model.Bucket;
import com.tencent.cos.model.COSObjectSummary;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.List;

@Slf4j
public class JobTencentInnerCOSClient {
    private String accessKey;
    private String secretKey;
    // 文件源EndPoint域名
    private String endPointDomain;
    // 文件源Bucket域名模板
    private String appId;
    private String regionName;

    public JobTencentInnerCOSClient(
        String accessKey,
        String secretKey,
        String endPointDomain,
        String appId
    ) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endPointDomain = endPointDomain;
        this.appId = appId;
        this.regionName = getRegionName(endPointDomain);
    }

    private String getRegionName(String endPointDomain) {
        // 内部COS特有
        String regionName = endPointDomain.replace("service.", "");
        regionName = regionName.replace(".tencent-cloud.com", "");
        return regionName;
    }

    private String getRealBucketName(String bucketName) {
        return bucketName + "-" + appId;
    }

    public List<Bucket> listBuckets() {
        return TencentInnerCOSUtil.listBuckets(accessKey, secretKey, regionName);
    }

    public List<COSObjectSummary> listAllObjects(
        String bucketName,
        Integer maxKeys,
        String prefix,
        String delimiter
    ) throws Exception {
        return TencentInnerCOSUtil.listAllObjects(accessKey, secretKey, regionName, getRealBucketName(bucketName),
            maxKeys, prefix, delimiter);
    }

    public void deleteBucket(String bucketName) {
        TencentInnerCOSUtil.deleteBucket(accessKey, secretKey, regionName, getRealBucketName(bucketName));
    }

    public void deleteObject(String bucketName, String key) {
        TencentInnerCOSUtil.deleteObject(accessKey, secretKey, regionName, getRealBucketName(bucketName), key);
    }

    public String genPresignedDownloadUrl(String bucketName, String key) {
        return TencentInnerCOSUtil.genPresignedDownloadUrlWithOverrideResponseHeader(
            accessKey, secretKey, regionName
            , getRealBucketName(bucketName), key);
    }

    public void uploadToCOS(String bucketName, File file, String targetKey) {
        TencentInnerCOSUtil.uploadToCOS(accessKey, secretKey, regionName, getRealBucketName(bucketName), file,
            targetKey);
    }

    public FileMetaData getFileMetaData(String bucketName, String key) {
        return TencentInnerCOSUtil.getFileMetaData(accessKey, secretKey, regionName, getRealBucketName(bucketName),
            key);
    }

    public InputStream getFileInputStream(String bucketName, String key) {
        return TencentInnerCOSUtil.getFileInputStream(accessKey, secretKey, regionName, getRealBucketName(bucketName)
            , key);
    }

    public void shutdown() {
    }
}
