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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.file.worker.model.FileMetaData;
import com.tencent.cos.COSClient;
import com.tencent.cos.ClientConfig;
import com.tencent.cos.auth.BasicCOSCredentials;
import com.tencent.cos.auth.COSCredentials;
import com.tencent.cos.http.HttpMethodName;
import com.tencent.cos.model.Bucket;
import com.tencent.cos.model.COSObject;
import com.tencent.cos.model.COSObjectSummary;
import com.tencent.cos.model.GeneratePresignedUrlRequest;
import com.tencent.cos.model.ListObjectsRequest;
import com.tencent.cos.model.ObjectListing;
import com.tencent.cos.model.ObjectMetadata;
import com.tencent.cos.model.ResponseHeaderOverrides;
import com.tencent.cos.region.Region;
import com.tencent.cos.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class TencentInnerCOSUtil {

    public static COSClient getCOSClient(String accessKey, String secretKey, String regionName) {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(accessKey, secretKey);
        // 2 设置bucket的区域
        ClientConfig clientConfig = new ClientConfig(new Region(regionName));
        // 3 生成cos客户端
        return new COSClient(cred, clientConfig);
    }

    public static List<Bucket> listBuckets(String accessKey, String secretKey, String regionName) {
        log.info(String.format("Input=(%s,%s,%s)", accessKey, secretKey, regionName));
        // 3 生成cos客户端
        COSClient cosclient = getCOSClient(accessKey, secretKey, regionName);
        try {
            List<Bucket> resultList = cosclient.listBuckets();
            return resultList;
        } finally {
            cosclient.shutdown();
        }
    }

    /**
     * 查出Bucket中存储的对象概要信息
     *
     * @param accessKey
     * @param secretKey
     * @param regionName
     * @param bucketName
     */
    public static List<COSObjectSummary> listAllObjects(
        String accessKey,
        String secretKey,
        String regionName,
        String bucketName,
        Integer maxKeys,
        String prefix,
        String delimiter
    ) throws Exception {
        log.info(String.format("Input=(%s,%s,%s,%s,%s,%s,%s)", accessKey, secretKey, regionName, bucketName, maxKeys,
            prefix, delimiter));
        // 如果要获取超过maxkey数量的object或者获取所有的object,
        // 则需要循环调用listobject, 用上一次返回的next marker作为下一次调用的marker,
        // 直到返回的truncated为false
        // bucket名需包含appid
        if (maxKeys == null) {
            maxKeys = 100;
        }
        // 3 生成cos客户端
        COSClient cosclient = getCOSClient(accessKey, secretKey, regionName);
        try {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
            // 设置bucket名称
            listObjectsRequest.setBucketName(bucketName);
            // prefix表示列出的object的key以prefix开始
            listObjectsRequest.setPrefix(prefix);
            // deliter表示分隔符, 设置为/表示列出当前目录下的object, 设置为空表示列出所有的object
            listObjectsRequest.setDelimiter(delimiter);
            // 设置最大遍历出多少个对象, 一次listobject最大支持1000
            listObjectsRequest.setMaxKeys(maxKeys);
            ObjectListing objectListing = null;
            List<COSObjectSummary> cosObjectSummaries = new ArrayList<>();
            do {
                objectListing = cosclient.listObjects(listObjectsRequest);
                // common prefix表示表示被delimiter截断的路径, 如delimter设置为/, common prefix则表示所有子目录的路径
                List<String> commonPrefixs = objectListing.getCommonPrefixes();
                // object summary表示所有列出的object列表
                List<COSObjectSummary> partCosObjectSummaries = objectListing.getObjectSummaries();
                cosObjectSummaries.addAll(partCosObjectSummaries);
                String nextMarker = objectListing.getNextMarker();
                listObjectsRequest.setMarker(nextMarker);
            } while (objectListing.isTruncated());
            return cosObjectSummaries;
        } finally {
            cosclient.shutdown();
        }
    }

    public static InputStream getFileInputStream(String accessKey, String secretKey, String regionName,
                                                 String bucketName, String key) {
        COSClient cosClient = getCOSClient(accessKey, secretKey, regionName);
        COSObject cosObject = cosClient.getObject(bucketName, key);
        if (cosObject == null) {
            throw new InternalException(ErrorCode.FAIL_TO_REQUEST_THIRD_FILE_SOURCE_DOWNLOAD_GENERIC_FILE,
                new String[]{String.format("Fail to getObject by bucketName %s key %s", bucketName, key)});
        }
        return cosObject.getObjectContent();
    }

    public static FileMetaData getFileMetaData(String accessKey, String secretKey, String regionName,
                                               String bucketName, String key) {
        COSClient cosClient = getCOSClient(accessKey, secretKey, regionName);
        COSObject cosObject = cosClient.getObject(bucketName, key);
        if (cosObject == null) {
            throw new InternalException(ErrorCode.FAIL_TO_REQUEST_THIRD_FILE_SOURCE_GET_OBJECT,
                new String[]{String.format("Fail to getObject by bucketName %s key %s", bucketName, key)});
        }
        ObjectMetadata objectMetadata = cosObject.getObjectMetadata();
        if (objectMetadata == null) {
            throw new InternalException(ErrorCode.FAIL_TO_REQUEST_THIRD_FILE_SOURCE_GET_OBJECT,
                new String[]{String.format("Fail to getObjectMetaData by bucketName %s key %s", bucketName, key)});
        }
        long fileSize = objectMetadata.getContentLength();
        String fileMd5 = objectMetadata.getContentMD5();
        return new FileMetaData(fileSize, fileMd5);
    }

    public static void deleteBucket(String accessKey, String secretKey, String regionName, String bucketName) {
        log.debug("Input=({},{},{},{})", accessKey, secretKey, regionName, bucketName);
        // 1.生成cos客户端
        COSClient cosClient = getCOSClient(accessKey, secretKey, regionName);
        try {
            // 2.删除文件
            cosClient.deleteBucket(bucketName);
        } finally {
            cosClient.shutdown();
        }
    }

    public static void deleteObject(String accessKey, String secretKey, String regionName, String bucketName,
                                    String key) {
        log.debug("Input=({},{},{},{},{})", accessKey, secretKey, regionName, bucketName, key);
        // 1.生成cos客户端
        COSClient cosClient = getCOSClient(accessKey, secretKey, regionName);
        try {
            // 2.删除文件
            cosClient.deleteObject(bucketName, key);
        } finally {
            cosClient.shutdown();
        }
    }

    // 获取预签名的下载链接, 并设置返回的content-type, cache-control等http头
    public static String genPresignedDownloadUrlWithOverrideResponseHeader(
        String accessKey,
        String secretKey,
        String regionName,
        String bucketName,
        String key
    ) {
        // 3 生成cos客户端
        COSClient cosClient = getCOSClient(accessKey, secretKey, regionName);
        try {
            GeneratePresignedUrlRequest req =
                new GeneratePresignedUrlRequest(bucketName, key, HttpMethodName.GET);
            // 设置下载时返回的http头
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            String responseContentType = "text/plain";
            String responseContentLanguage = "zh-CN";
            String fileName = PathUtil.getFileNameByPath(key);
            String responseContentDispositon = "attachment; filename*=\"UTF-8''" + fileName + "\"";
            String responseCacheControl = "no-cache";
            String cacheExpireStr =
                DateUtils.formatRFC822Date(new Date(System.currentTimeMillis() + 24 * 3600 * 1000));
            responseHeaders.setContentType(responseContentType);
            responseHeaders.setContentLanguage(responseContentLanguage);
            responseHeaders.setContentDisposition(responseContentDispositon);
            responseHeaders.setCacheControl(responseCacheControl);
            responseHeaders.setExpires(cacheExpireStr);
            req.setResponseHeaders(responseHeaders);
            // 设置签名过期时间(可选), 若未进行设置则默认使用ClientConfig中的签名过期时间(1小时)
            // 这里设置签名在2个小时后过期
            Date expirationDate = new Date(System.currentTimeMillis() + 120 * 60 * 1000);
            req.setExpiration(expirationDate);
            URL url = cosClient.generatePresignedUrl(req);
            return url.toString();
        } finally {
            cosClient.shutdown();
        }
    }

    // TODO:上传文件
    public static void uploadToCOS(String accessKey, String secretKey, String regionName, String bucketName,
                                   File file, String targetKey) {
        log.info(String.format("Input=(%s,%s,%s,%s,%s,%s)", accessKey, secretKey, regionName, bucketName,
            file.getAbsolutePath(), targetKey));
        // 3 生成cos客户端
        COSClient cosclient = getCOSClient(accessKey, secretKey, regionName);
        FileInputStream fins = null;
        BufferedOutputStream out = null;
        try {
            // 默认两小时
            Date expirationTime = new Date(System.currentTimeMillis() + 120 * 60 * 1000);
            URL url = cosclient.generatePresignedUrl(bucketName, targetKey, expirationTime, HttpMethodName.PUT);
            fins = new FileInputStream(file);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 打开文件输入流
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            out = new BufferedOutputStream(connection.getOutputStream());
            // 写入要上传的数据
            int batchSize = 1024;
            int l = 0;
            byte[] bytes = new byte[batchSize];
            do {
                l = fins.read(bytes, 0, batchSize);
                out.write(bytes, 0, l);
            } while (l == batchSize);
            int responseCode = connection.getResponseCode();
            log.info("Service returned response code " + responseCode);
        } catch (IOException e) {
            log.error("Exception occured when uploadToCOS", e);
        } finally {
            try {
                if (fins != null) {
                    fins.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                log.error("Exception occured when close stream", e);
            }
            cosclient.shutdown();
        }
    }
}
