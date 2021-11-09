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

package com.tencent.bk.job.file.worker.artifactory.impl;

import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.model.dto.ProjectDTO;
import com.tencent.bk.job.common.artifactory.model.dto.RepoDTO;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.CompareUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.file.FileSizeUtil;
import com.tencent.bk.job.file.worker.api.IFileResource;
import com.tencent.bk.job.file.worker.artifactory.consts.ArtifactoryActionCodeEnum;
import com.tencent.bk.job.file.worker.artifactory.consts.ArtifactoryNodeTypeEnum;
import com.tencent.bk.job.file.worker.artifactory.service.ArtifactoryBaseService;
import com.tencent.bk.job.file.worker.artifactory.service.ArtifactoryRemoteClient;
import com.tencent.bk.job.file.worker.cos.service.MetaDataService;
import com.tencent.bk.job.file.worker.cos.service.RemoteClient;
import com.tencent.bk.job.file.worker.model.req.BaseReq;
import com.tencent.bk.job.file.worker.model.req.ExecuteActionReq;
import com.tencent.bk.job.file.worker.model.req.ListFileNodeReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesDTO;
import com.tencent.bk.job.file_gateway.model.resp.common.FileTreeNodeDef;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("ArtifactoryFileResource")
public class ArtifactoryFileResourceImpl implements IFileResource {

    private final ArtifactoryBaseService baseService;
    private final MetaDataService metaDataService;

    @Autowired
    public ArtifactoryFileResourceImpl(ArtifactoryBaseService baseService, MetaDataService metaDataService) {
        this.baseService = baseService;
        this.metaDataService = metaDataService;
    }

    @Override
    public RemoteClient getRemoteClient(BaseReq req) {
        return baseService.getArtifactoryClientFromBaseReq(req);
    }

    @Override
    public InternalResponse<Boolean> isFileAvailable(BaseReq req) {
        ArtifactoryRemoteClient client = baseService.getArtifactoryClientFromBaseReq(req);
        return InternalResponse.buildSuccessResp(client.isAvailable());
    }

    private String parseParentNodeTypeByPath(String path) {
        if (StringUtils.isBlank(path)) return ArtifactoryNodeTypeEnum.FILE_SOURCE.name();
        path = StringUtil.removePrefix(path, "/");
        path = StringUtil.removeSuffix(path, "/");
        String[] pathArr = path.split("/");
        ArtifactoryNodeTypeEnum[] types = ArtifactoryNodeTypeEnum.values();
        int typeLen = types.length;
        if (pathArr.length < typeLen) {
            return types[pathArr.length].name();
        }
        return types[typeLen - 1].name();
    }

    private void fillProjectFileNodesDTO(FileNodesDTO fileNodesDTO, ListFileNodeReq req) {
        ArtifactoryRemoteClient client = baseService.getArtifactoryClientFromBaseReq(req);
        List<ProjectDTO> projectDTOList = client.listProject();
        // 按名称搜索
        projectDTOList = projectDTOList.parallelStream().filter(projectDTO -> {
            String displayName = projectDTO.getDisplayName();
            String name = req.getName();
            if (null == displayName) {
                displayName = "";
            }
            if (null == name) {
                name = "";
            }
            return displayName.contains(name);
        }).collect(Collectors.toList());
        // 排序
        projectDTOList.sort((o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else if (o2 == null) {
                return -1;
            } else {
                return CompareUtil.safeCompareNullBack(o2.getLastModifiedDate(), o1.getLastModifiedDate());
            }
        });
        PageData<ProjectDTO> pageData = PageUtil.pageInMem(projectDTOList, req.getStart(), req.getPageSize());
        PageData<Map<String, Object>> mappedPageData = new PageData<>();
        mappedPageData.setStart(pageData.getStart());
        mappedPageData.setPageSize(pageData.getPageSize());
        mappedPageData.setTotal(pageData.getTotal());
        mappedPageData.setData(pageData.getData().parallelStream().map(projectDTO -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", projectDTO.getName());
            map.put("displayName", projectDTO.getDisplayName());
            map.put("description", projectDTO.getDescription());
            map.put("createdBy", projectDTO.getCreatedBy());
            // 补充字段
            map.put("completePath", projectDTO.getName());
            return map;
        }).collect(Collectors.toList()));
        fileNodesDTO.setPageData(mappedPageData);
    }

    private void fillRepoFileNodesDTO(FileNodesDTO fileNodesDTO, ListFileNodeReq req) {
        ArtifactoryRemoteClient client = baseService.getArtifactoryClientFromBaseReq(req);
        String path = req.getPath();
        path = StringUtil.removePrefixAndSuffix(path, "/");
        if (path.contains("/")) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{"path",
                "Parent path of repo must only contains projectName"});
        }
        if (StringUtils.isBlank(path)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{"path", "path" +
                " cannot be blank"});
        }
        String projectId = path;
        // TODO:搜索优化，当前只支持原生前缀搜索
        com.tencent.bk.job.common.artifactory.model.dto.PageData<RepoDTO> pageData = null;
        Integer start = req.getStart();
        Integer pageSize = req.getPageSize();
        if (pageSize <= 0) {
            // 不分页，全量拉取
            pageData = client.listRepo(projectId, req.getName(), 1, Integer.MAX_VALUE);
        } else {
            pageData = client.listRepo(projectId, req.getName(), start / pageSize + 1, pageSize);
        }
        // 排序
        pageData.getRecords().sort((o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else if (o2 == null) {
                return -1;
            } else {
                return CompareUtil.safeCompareNullBack(o2.getLastModifiedDate(), o1.getLastModifiedDate());
            }
        });
        PageData<Map<String, Object>> mappedPageData = new PageData<>();
        mappedPageData.setStart((pageData.getPageNumber() - 1) * pageData.getPageSize());
        mappedPageData.setPageSize(pageData.getPageSize());
        mappedPageData.setTotal(pageData.getTotalRecords());
        mappedPageData.setData(pageData.getRecords().parallelStream().map(repoDTO -> {
            Map<String, Object> map = new HashMap<>();
            map.put("projectId", repoDTO.getProjectId());
            map.put("name", repoDTO.getName());
            map.put("category", repoDTO.getCategory());
            map.put("type", repoDTO.getType());
            map.put("public", repoDTO.getIsPublic() ? "是" : "否");
            map.put("description", repoDTO.getDescription());
            map.put("createdBy", repoDTO.getCreatedBy());
            // 补充字段
            map.put("completePath", repoDTO.getProjectId() + "/" + repoDTO.getName());
            return map;
        }).collect(Collectors.toList()));
        fileNodesDTO.setPageData(mappedPageData);
    }

    private void fillNodeFileNodesDTO(FileNodesDTO fileNodesDTO, ListFileNodeReq req) {
        ArtifactoryRemoteClient client = baseService.getArtifactoryClientFromBaseReq(req);
        String path = req.getPath();
        path = StringUtil.removePrefixAndSuffix(path, "/");
        String[] pathArr = path.split("/");
        if (pathArr.length < 2) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{"path", "path" +
                " must contain projectId and repoName"});
        }
        String projectId = pathArr[0];
        String repoName = pathArr[1];
        String fullPath = path.substring(projectId.length() + repoName.length() + 1);
        // TODO:搜索优化
        com.tencent.bk.job.common.artifactory.model.dto.PageData<NodeDTO> pageData = null;
        Integer start = req.getStart();
        Integer pageSize = req.getPageSize();
        if (pageSize <= 0) {
            // 不分页，全量拉取
            pageData = client.listNode(projectId, repoName, fullPath, 1, Integer.MAX_VALUE);
        } else {
            pageData = client.listNode(projectId, repoName, fullPath, start / pageSize + 1, pageSize);
        }
        // 排序
        pageData.getRecords().sort((o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else if (o2 == null) {
                return -1;
            } else {
                // 目录靠前
                int result = CompareUtil.safeCompareNullBack(o1.getFolder(), o2.getFolder());
                if (result != 0) {
                    return result;
                } else {
                    // 最近更新靠前
                    return CompareUtil.safeCompareNullBack(o2.getLastModifiedDate(), o1.getLastModifiedDate());
                }
            }
        });
        PageData<Map<String, Object>> mappedPageData = new PageData<>();
        mappedPageData.setStart((pageData.getPageNumber() - 1) * pageData.getPageSize());
        mappedPageData.setPageSize(pageData.getPageSize());
        mappedPageData.setTotal(pageData.getTotalRecords());
        mappedPageData.setData(pageData.getRecords().parallelStream().map(nodeDTO -> {
            Map<String, Object> map = new HashMap<>();
            map.put("projectId", nodeDTO.getProjectId());
            map.put("repoName", nodeDTO.getRepoName());
            map.put("path", nodeDTO.getPath());
            map.put("name", nodeDTO.getName());
            map.put("fullPath", nodeDTO.getFullPath());
            map.put("folder", nodeDTO.getFolder());
            map.put("size", FileSizeUtil.getFileSizeStr(nodeDTO.getSize()));
            map.put("lastModifiedBy", nodeDTO.getLastModifiedBy());
            map.put("lastModifiedDate", nodeDTO.getLastModifiedDate());
            // 补充字段
            map.put("completePath",
                nodeDTO.getProjectId() + "/" + nodeDTO.getRepoName()
                    + "/" + StringUtil.removePrefix(nodeDTO.getFullPath(), "/"));
            return map;
        }).collect(Collectors.toList()));
        fileNodesDTO.setPageData(mappedPageData);
    }

    @Override
    public InternalResponse<FileNodesDTO> listFileNode(ListFileNodeReq req) {
        FileNodesDTO fileNodesDTO = new FileNodesDTO();
        String parentNodeType = parseParentNodeTypeByPath(req.getPath());
        FileTreeNodeDef metaData = metaDataService.getChildFileNodeMetaDataByParent(req.getFileSourceTypeCode(),
            parentNodeType);
        fileNodesDTO.setMetaData(metaData);
        if (ArtifactoryNodeTypeEnum.FILE_SOURCE.name().equals(parentNodeType)) {
            // 父节点类型为文件源，则子节点为Project，listProject
            fillProjectFileNodesDTO(fileNodesDTO, req);
            return InternalResponse.buildSuccessResp(fileNodesDTO);
        } else if (ArtifactoryNodeTypeEnum.PROJECT.name().equals(parentNodeType)) {
            // 父节点类型为Project，则子节点为Repo，listRepo
            fillRepoFileNodesDTO(fileNodesDTO, req);
            return InternalResponse.buildSuccessResp(fileNodesDTO);
        } else if (ArtifactoryNodeTypeEnum.REPO.name().equals(parentNodeType)) {
            // 父节点类型为Repo，则子节点为Node，listNode
            fillNodeFileNodesDTO(fileNodesDTO, req);
            return InternalResponse.buildSuccessResp(fileNodesDTO);
        } else if (ArtifactoryNodeTypeEnum.NODE.name().equals(parentNodeType)) {
            // 父节点类型为Node，则子节点仍为Node，listNode
            fillNodeFileNodesDTO(fileNodesDTO, req);
            return InternalResponse.buildSuccessResp(fileNodesDTO);
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"nodeType"});
        }
    }

    private boolean deleteProject(String projectId, BaseReq req) {
        ArtifactoryRemoteClient client = baseService.getArtifactoryClientFromBaseReq(req);
        return client.deleteProject(projectId);
    }

    private boolean deleteRepo(String projectId, String repoName, BaseReq req) {
        ArtifactoryRemoteClient client = baseService.getArtifactoryClientFromBaseReq(req);
        return client.deleteRepo(projectId, repoName, false);
    }

    private boolean deleteNode(String projectId, String repoName, String fullPath, BaseReq req) {
        ArtifactoryRemoteClient client = baseService.getArtifactoryClientFromBaseReq(req);
        return client.deleteNode(projectId, repoName, fullPath);
    }

    @Override
    public InternalResponse<Boolean> executeAction(ExecuteActionReq req) {
        String actionCode = req.getActionCode();
        if (ArtifactoryActionCodeEnum.DELETE_PROJECT.name().equals(actionCode)) {
            // deleteProject
            String projectId = null;
            Map<String, Object> params = req.getParams();
            if (params != null) {
                projectId = (String) params.get("projectId");
            }
            if (StringUtils.isNotBlank(projectId)) {
                deleteProject(projectId, req);
            } else {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"projectId"});
            }
        } else if (ArtifactoryActionCodeEnum.DELETE_REPO.name().equals(actionCode)) {
            String projectId = null;
            String repoName = null;
            Map<String, Object> params = req.getParams();
            if (params != null) {
                projectId = (String) params.get("projectId");
                repoName = (String) params.get("repoName");
            }
            if (StringUtils.isBlank(projectId)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"projectId"});
            } else if (StringUtils.isBlank(repoName)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"repoName"});
            } else {
                deleteRepo(projectId, repoName, req);
            }
        } else if (ArtifactoryActionCodeEnum.DELETE_NODE.name().equals(actionCode)) {
            String projectId = null;
            String repoName = null;
            String fullPath = null;
            Map<String, Object> params = req.getParams();
            if (params != null) {
                projectId = (String) params.get("projectId");
                repoName = (String) params.get("repoName");
                fullPath = (String) params.get("fullPath");
            }
            if (StringUtils.isBlank(projectId)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"projectId"});
            } else if (StringUtils.isBlank(repoName)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"repoName"});
            } else if (StringUtils.isBlank(fullPath)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"fullPath"});
            } else {
                deleteNode(projectId, repoName, fullPath, req);
            }
        }
        return InternalResponse.buildSuccessResp(true);
    }
}
