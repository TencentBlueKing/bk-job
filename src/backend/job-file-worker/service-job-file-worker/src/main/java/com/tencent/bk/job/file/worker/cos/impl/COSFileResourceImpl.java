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

package com.tencent.bk.job.file.worker.cos.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.file.worker.api.IFileResource;
import com.tencent.bk.job.file.worker.cos.JobTencentInnerCOSClient;
import com.tencent.bk.job.file.worker.cos.consts.COSActionCodeEnum;
import com.tencent.bk.job.file.worker.cos.consts.COSNodeTypeEnum;
import com.tencent.bk.job.file.worker.cos.service.COSBaseService;
import com.tencent.bk.job.file.worker.cos.service.COSRemoteClient;
import com.tencent.bk.job.file.worker.cos.service.MetaDataService;
import com.tencent.bk.job.file.worker.cos.service.RemoteClient;
import com.tencent.bk.job.file.worker.model.BucketDTO;
import com.tencent.bk.job.file.worker.model.FileDTO;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file.worker.model.req.ExecuteActionReq;
import com.tencent.bk.job.file.worker.model.req.ListFileNodeReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesDTO;
import com.tencent.bk.job.file_gateway.model.resp.common.FileTreeNodeDef;
import com.tencent.cos.model.Bucket;
import com.tencent.cos.model.COSObjectSummary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("COSFileResource")
public class COSFileResourceImpl implements IFileResource {

    private final COSBaseService cosBaseService;
    private final MetaDataService metaDataService;

    @Autowired
    public COSFileResourceImpl(COSBaseService cosBaseService, MetaDataService metaDataService) {
        this.cosBaseService = cosBaseService;
        this.metaDataService = metaDataService;
    }

    public List<BucketDTO> listBucket(ListFileNodeReq req) {
        try {
            JobTencentInnerCOSClient jobTencentInnerCOSClient = cosBaseService.getCOSClientFromBaseReq(req);
            List<Bucket> bucketList = jobTencentInnerCOSClient.listBuckets();
            // 根据name搜索
            bucketList = bucketList.parallelStream().filter(
                bucketDTO -> {
                    String name = req.getName();
                    String bucketName = bucketDTO.getName();
                    if (null == bucketName) {
                        bucketName = "";
                    }
                    if (null == name) {
                        name = "";
                    }
                    return bucketName.contains(name);
                }
            ).collect(Collectors.toList());
            List<BucketDTO> bucketDTOList = new ArrayList<>();
            bucketList.forEach(bucket -> {
                bucketDTOList.add(convertToBucketDTO(bucket));
            });
            return bucketDTOList;
        } catch (Throwable t) {
            log.error("Fail to listBucket", t);
            throw new InvalidParamException(ErrorCode.FAIL_TO_REQUEST_THIRD_FILE_SOURCE_LIST_BUCKET, t.getMessage());
        }
    }

    public List<FileDTO> listBucketFiles(String bucketName, String prefix, String delimiter, String marker,
                                         Integer maxKeys, ListFileNodeReq req) {
        JobTencentInnerCOSClient jobTencentInnerCOSClient = cosBaseService.getCOSClientFromBaseReq(req);
        List<COSObjectSummary> cosObjectSummaryList = null;
        try {
            cosObjectSummaryList = jobTencentInnerCOSClient.listAllObjects(bucketName, maxKeys, prefix, delimiter);
        } catch (Exception e) {
            String msg = "Fail to listAllObjects from " + cosBaseService.getEndPointDomain(req);
            log.error(msg, e);
            throw new InvalidParamException(ErrorCode.FAIL_TO_REQUEST_THIRD_FILE_SOURCE_LIST_OBJECTS, msg);
        }
        List<FileDTO> fileDTOList = new ArrayList<>();
        cosObjectSummaryList.forEach(cosObjectSummary -> {
            //TODO:性能优化
            String key = cosObjectSummary.getKey();
            // 搜索
            String name = req.getName();
            if (null == name) {
                name = "";
            }
            if (key != null && key.contains(name)) {
                String completeKey = getCompleteKey(prefix, key);
                String downloadUrl = jobTencentInnerCOSClient.genPresignedDownloadUrl(bucketName, completeKey);
                fileDTOList.add(convertToFileDTO(cosObjectSummary, downloadUrl));
            }
        });
        return fileDTOList;
    }

    public Boolean deleteBucket(String bucketName, BaseReq req) {
        checkBucketName(bucketName);
        JobTencentInnerCOSClient jobTencentInnerCOSClient = cosBaseService.getCOSClientFromBaseReq(req);
        try {
            jobTencentInnerCOSClient.deleteBucket(bucketName);
            return true;
        } catch (Exception e) {
            log.error("Fail to delete bucket {}", bucketName, e);
            throw new InvalidParamException(ErrorCode.FAIL_TO_REQUEST_THIRD_FILE_SOURCE_DELETE_BUCKET, e.getMessage());
        }
    }

    public Boolean deleteBucketFile(String bucketName, String key, BaseReq req) {
        checkBucketName(bucketName);
        checkKey(key);
        JobTencentInnerCOSClient jobTencentInnerCOSClient = cosBaseService.getCOSClientFromBaseReq(req);
        try {
            jobTencentInnerCOSClient.deleteObject(bucketName, key);
            return true;
        } catch (Exception e) {
            log.error("Fail to delete bucket {} file:{}", bucketName, key, e);
            throw new InvalidParamException(ErrorCode.FAIL_TO_REQUEST_THIRD_FILE_SOURCE_DELETE_OBJECT, e.getMessage());
        }
    }

    private void checkBucketName(String bucketName) {
        if (StringUtils.isBlank(bucketName)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"bucketName"});
        }
    }

    private void checkKey(String key) {
        if (StringUtils.isBlank(key)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"key"});
        }
    }

    private String getCompleteKey(String prefix, String key) {
        if (key == null) {
            key = "";
        }
        if (prefix == null) {
            return key;
        }
        return prefix + key;
    }

    private FileDTO convertToFileDTO(COSObjectSummary cosObjectSummary, String downloadUrl) {
        FileDTO fileDTO = new FileDTO();
        fileDTO.setETag(cosObjectSummary.getETag());
        fileDTO.setKey(cosObjectSummary.getKey());
        fileDTO.setDownloadUrl(downloadUrl);
        fileDTO.setSize(cosObjectSummary.getSize());
        fileDTO.setLastModified(cosObjectSummary.getLastModified().getTime());
        return fileDTO;
    }

    private BucketDTO convertToBucketDTO(Bucket bucket) {
        BucketDTO bucketDTO = new BucketDTO();
        bucketDTO.setName(bucket.getName());
        bucketDTO.setCreateDate(bucket.getCreationDate().getTime());
        //TODO:SDK更新
        bucketDTO.setXCosAcl("private");
        return bucketDTO;
    }

    @Override
    public RemoteClient getRemoteClient(BaseReq req) {
        JobTencentInnerCOSClient jobTencentInnerCOSClient = cosBaseService.getCOSClientFromBaseReq(req);
        return new COSRemoteClient(jobTencentInnerCOSClient);
    }

    @Override
    public InternalResponse<Boolean> isFileAvailable(BaseReq req) {
        try {
            ListFileNodeReq listFileNodeReq = new ListFileNodeReq(req);
            listFileNodeReq.setPath("");
            listFileNodeReq.setName("");
            listFileNodeReq.setStart(0);
            listFileNodeReq.setPageSize(1);
            listBucket(listFileNodeReq);
            return InternalResponse.buildSuccessResp(true);
        } catch (Throwable t) {
            return InternalResponse.buildSuccessResp(false);
        }
    }

    private String getTypeFromFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "UNKNOWN";
        }
        fileName = fileName.toLowerCase();
        if (fileName.endsWith("/")) {
            return "文件夹";
        } else if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.endsWith(".txt")) {
            return "文本文件";
        } else if (fileName.endsWith(".zip")) {
            return "压缩文件";
        }
        return "未知类型";
    }

    private Boolean isDir(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return true;
        }
        return fileName.endsWith("/");
    }

    private int getSlashNum(String str) {
        if (StringUtils.isBlank(str)) return 0;
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '/') count += 1;
        }
        return count;
    }

    private String parseBucketNameFromPath(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.contains("/")) {
            return path.substring(0, path.indexOf("/"));
        } else {
            return path;
        }
    }

    private void fillBucketFileNodesDTO(FileNodesDTO fileNodesDTO, ListFileNodeReq req) {
        List<BucketDTO> bucketDTOList = listBucket(req);
        // 排序：创建时间降序
        bucketDTOList.sort((o1, o2) -> o2.getCreateDate().compareTo(o1.getCreateDate()));
        List<Map<String, Object>> mapData = bucketDTOList.parallelStream().map(bucketDTO -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", bucketDTO.getName());
            map.put("type", bucketDTO.getXCosAcl());
            map.put("createTime", DateUtils.formatUnixTimestamp(bucketDTO.getCreateDate(), ChronoUnit.MILLIS));
            map.put("completePath", PathUtil.joinFilePath(req.getPath(), bucketDTO.getName()));
            map.put("dir", true);
            return map;
        }).collect(Collectors.toList());
        fileNodesDTO.setPageData(PageUtil.pageInMem(mapData, req.getStart(), req.getPageSize()));
    }

    private void fillFileFileNodesDTO(FileNodesDTO fileNodesDTO, ListFileNodeReq req) {
        // 解析bucketName
        String bucketName = parseBucketNameFromPath(req.getPath());
        String prefix = req.getPath();
        prefix = StringUtil.removePrefix(prefix, "/");
        prefix = StringUtil.removePrefix(prefix, bucketName);
        prefix = StringUtil.removePrefixAndSuffix(prefix, "/");
        prefix += "/";
        // 搜索
        List<FileDTO> fileDTOList = listBucketFiles(bucketName, prefix, "/", null, 1000, req);
        // 排序：目录
        List<FileDTO> dirList = new ArrayList<>();
        List<FileDTO> fileList = new ArrayList<>();
        for (FileDTO fileVO : fileDTOList) {
            String fileName = fileVO.getKey();
            if (StringUtils.isBlank(fileName)
                || (fileName.contains("/") && !fileName.endsWith("/"))
                || getSlashNum(fileName) > 1) {
                // 传入文件夹路径作为前缀时，忽略文件夹本身这一个文件
                // 忽略子目录下的文件
                // 忽略多级子目录
                continue;
            }
            if (isDir(fileName)) {
                dirList.add(fileVO);
            } else {
                fileList.add(fileVO);
            }
        }
        // 排序：更新时间
        dirList.sort((o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));
        fileList.sort((o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));
        fileDTOList.clear();
        fileDTOList.addAll(dirList);
        fileDTOList.addAll(fileList);
        // 分页
        List<Map<String, Object>> mapData = fileDTOList.parallelStream().map(fileDTO -> {
            Map<String, Object> map = new HashMap<>();
            String fileName = fileDTO.getKey();
            map.put("name", fileName);
            map.put("type", getTypeFromFileName(fileName));
            map.put("updateTime", DateUtils.formatUnixTimestamp(fileDTO.getLastModified(), ChronoUnit.MILLIS));
            map.put("completePath", PathUtil.joinFilePath(req.getPath(), fileName));
            map.put("dir", isDir(fileName));
            return map;
        }).collect(Collectors.toList());
        Pair<Integer, Integer> pageParam = PageUtil.normalizePageParam(req.getStart(), req.getPageSize());
        fileNodesDTO.setPageData(PageUtil.pageInMem(mapData, pageParam.getLeft(), pageParam.getRight()));
    }

    private String parseParentNodeTypeByPath(String path) {
        if (StringUtils.isBlank(path)) return COSNodeTypeEnum.FILE_SOURCE.name();
        path = StringUtil.removePrefix(path, "/");
        path = StringUtil.removeSuffix(path, "/");
        String[] pathArr = path.split("/");
        COSNodeTypeEnum[] types = COSNodeTypeEnum.values();
        int typeLen = types.length;
        if (pathArr.length < typeLen) {
            return types[pathArr.length].name();
        }
        return types[typeLen - 1].name();
    }

    @Override
    public InternalResponse<FileNodesDTO> listFileNode(ListFileNodeReq req) {
        FileNodesDTO fileNodesDTO = new FileNodesDTO();
        String parentNodeType = parseParentNodeTypeByPath(req.getPath());
        FileTreeNodeDef metaData = metaDataService.getChildFileNodeMetaDataByParent(req.getFileSourceTypeCode(),
            parentNodeType);
        fileNodesDTO.setMetaData(metaData);
        if (COSNodeTypeEnum.FILE_SOURCE.name().equals(parentNodeType)) {
            // 父节点类型为文件源，则子节点为Bucket，listBucket
            fillBucketFileNodesDTO(fileNodesDTO, req);
            return InternalResponse.buildSuccessResp(fileNodesDTO);
        } else if (COSNodeTypeEnum.BUCKET.name().equals(parentNodeType)) {
            // 父节点类型为Bucket，则子节点为File，listBucketFile
            fillFileFileNodesDTO(fileNodesDTO, req);
            return InternalResponse.buildSuccessResp(fileNodesDTO);
        } else if (COSNodeTypeEnum.FILE.name().equals(parentNodeType)) {
            // 父节点类型为File，则子节点仍为File，listBucketFile
            fillFileFileNodesDTO(fileNodesDTO, req);
            return InternalResponse.buildSuccessResp(fileNodesDTO);
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"nodeType"});
        }
    }

    @Override
    public InternalResponse<Boolean> executeAction(ExecuteActionReq req) {
        String actionCode = req.getActionCode();
        if (COSActionCodeEnum.DELETE_BUCKET.name().equals(actionCode)) {
            // deleteBucket
            String bucketName = null;
            Map<String, Object> params = req.getParams();
            if (params != null) {
                bucketName = (String) params.get("bucketName");
            }
            if (StringUtils.isNotBlank(bucketName)) {
                deleteBucket(bucketName, req);
            } else {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"bucketName"});
            }
        } else if (COSActionCodeEnum.DELETE_FILE.name().equals(actionCode)) {
            // deleteBucketFile
            String path = null;
            Map<String, Object> params = req.getParams();
            if (params != null) {
                path = (String) params.get("path");
            }
            if (StringUtils.isNotBlank(path)) {
                String bucketName = parseBucketNameFromPath(path);
                path = StringUtil.removePrefix(path, "/");
                path = StringUtil.removePrefix(path, bucketName);
                path = StringUtil.removePrefix(path, "/");
                log.debug("deleteBucketFile:bucketName={},path={}", bucketName, path);
                deleteBucketFile(bucketName, path, req);
            } else {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"path"});
            }
        }
        return InternalResponse.buildSuccessResp(true);
    }
}
