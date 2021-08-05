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

package com.tencent.bk.job.file.worker.artifactory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.http.AbstractHttpHelper;
import com.tencent.bk.job.common.util.http.DefaultHttpHelper;
import com.tencent.bk.job.common.util.http.LongRetryableHttpHelper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file.worker.artifactory.consts.ArtifactoryInterfaceConsts;
import com.tencent.bk.job.file.worker.artifactory.model.dto.ArtifactoryResp;
import com.tencent.bk.job.file.worker.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.file.worker.artifactory.model.dto.PageData;
import com.tencent.bk.job.file.worker.artifactory.model.dto.ProjectDTO;
import com.tencent.bk.job.file.worker.artifactory.model.dto.RepoDTO;
import com.tencent.bk.job.file.worker.artifactory.model.req.ArtifactoryReq;
import com.tencent.bk.job.file.worker.artifactory.model.req.DeleteNodeReq;
import com.tencent.bk.job.file.worker.artifactory.model.req.DeleteRepoReq;
import com.tencent.bk.job.file.worker.artifactory.model.req.DownloadGenericFileReq;
import com.tencent.bk.job.file.worker.artifactory.model.req.ListNodePageReq;
import com.tencent.bk.job.file.worker.artifactory.model.req.ListProjectReq;
import com.tencent.bk.job.file.worker.artifactory.model.req.ListRepoPageReq;
import com.tencent.bk.job.file.worker.artifactory.model.req.QueryNodeDetailReq;
import com.tencent.bk.job.file.worker.cos.service.RemoteClient;
import com.tencent.bk.job.file.worker.model.FileMetaData;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ArtifactoryRemoteClient implements RemoteClient {

    public static final String URL_ACTUATOR_INFO = "/repository/actuator/info";
    public static final String URL_LIST_PROJECT = "/repository/api/project/list";
    public static final String URL_LIST_REPO_PAGE = "/repository/api/repo/page/{projectId}/{pageNumber}/{pageSize}";
    public static final String URL_LIST_NODE_PAGE = "/repository/api/node/page/{projectId}/{repoName}/{fullPath}";
    public static final String URL_DELETE_REPO = "/repository/api/repo/delete/{projectId}/{repoName}";
    public static final String URL_DELETE_NODE = "/repository/api/node/delete/{projectId}/{repoName}/{fullPath}";
    public static final String URL_DOWNLOAD_GENERIC_FILE = "/generic/{project}/{repo}/{path}";
    public static final String URL_QUERY_NODE_DETAIL = "/repository/api/node/detail/{projectId}/{repoName}/{fullPath}";
    AbstractHttpHelper httpHelper = new DefaultHttpHelper();
    AbstractHttpHelper longHttpHelper = new LongRetryableHttpHelper();
    private String baseUrl;
    private String username;
    private String password;
    private MeterRegistry meterRegistry;

    public ArtifactoryRemoteClient(String baseUrl, String username, String password, MeterRegistry meterRegistry) {
        this.baseUrl = StringUtil.removeSuffix(baseUrl, "/");
        this.username = username;
        this.password = password;
        this.meterRegistry = meterRegistry;
    }

    private String getCompleteUrl(String url) {
        if (url.startsWith(baseUrl)) return url;
        return baseUrl + "/" + StringUtil.removePrefix(url, "/");
    }

    private List<Header> getBaseHeaderList() {
        List<Header> headerList = new ArrayList<>();
        headerList.add(new BasicHeader("accept", "*/*"));
        headerList.add(new BasicHeader("Content-Type", "application/json"));
        headerList.add(new BasicHeader(ArtifactoryInterfaceConsts.AUTH_HEADER_KEY,
            "Basic " + Base64Util.encodeContentToStr(username + ":" + password)));
        return headerList;
    }

    private Header[] getBaseHeaders() {
        List<Header> headerList = getBaseHeaderList();
        Header[] headers = new Header[headerList.size()];
        return headerList.toArray(headers);
    }

    private String doHttpGet(String url, ArtifactoryReq reqBody, AbstractHttpHelper httpHelper) throws IOException {
        if (null == reqBody) {
            return httpHelper.get(url, getBaseHeaders());
        } else {
            return httpHelper.get(url + reqBody.toUrlParams(), getBaseHeaders());
        }
    }

    private String doHttpPost(String url, ArtifactoryReq reqBody, AbstractHttpHelper httpHelper) throws Exception {
        if (null == reqBody) {
            return httpHelper.post(url, "{}", getBaseHeaders());
        } else {
            return httpHelper.post(url, JsonUtils.toJson(reqBody), getBaseHeaders());
        }
    }

    private String doHttpDelete(String url, ArtifactoryReq reqBody, AbstractHttpHelper httpHelper) throws Exception {
        if (null == reqBody) {
            return httpHelper.delete(url, "{}", getBaseHeaders());
        } else {
            return httpHelper.delete(url + reqBody.toUrlParams(), JsonUtils.toJson(reqBody), getBaseHeaders());
        }
    }

    private <T, R> R getArtifactoryRespByReq(
        String method,
        String url,
        ArtifactoryReq reqBody,
        TypeReference<R> typeReference,
        AbstractHttpHelper httpHelper
    ) throws RuntimeException {
        // URL模板变量替换
        url = StringUtil.replacePathVariables(url, reqBody);
        url = getCompleteUrl(url);
        String reqStr = "{}";
        if (reqBody != null) {
            reqStr = JsonUtils.toJsonWithoutSkippedFields(reqBody);
        }
        String respStr = null;
        long start = System.nanoTime();
        String status = "none";
        try {
            if (method.equals(HttpGet.METHOD_NAME)) {
                respStr = doHttpGet(url, reqBody, httpHelper);
            } else if (method.equals(HttpPost.METHOD_NAME)) {
                respStr = doHttpPost(url, reqBody, httpHelper);
            } else if (method.equals(HttpDelete.METHOD_NAME)) {
                respStr = doHttpDelete(url, reqBody, httpHelper);
            }
            if (StringUtils.isBlank(respStr)) {
                log.error("fail:response is blank|method={}|url={}|reqStr={}", method, url, reqStr);
                throw new ServiceException(ErrorCode.ARTIFACTORY_API_DATA_ERROR, "response is blank");
            } else {
                log.debug("success|method={}|url={}|reqStr={}|respStr={}", method, url, reqStr, respStr);
            }
            R result = JsonUtils.fromJson(respStr, typeReference);
            if (result instanceof ArtifactoryResp) {
                ArtifactoryResp artifactoryResp = (ArtifactoryResp) result;
                if (artifactoryResp == null) {
                    log.error("fail:artifactoryResp is null after parse|method={}|url={}|reqStr={}|respStr={}", method,
                        url, reqStr, respStr);
                    status = "error";
                    throw new ServiceException(ErrorCode.ARTIFACTORY_API_DATA_ERROR, "artifactoryResp is null after parse");
                } else if (artifactoryResp.getCode() != ArtifactoryInterfaceConsts.RESULT_CODE_OK) {
                    log.error(
                        "fail:artifactoryResp code!={}|artifactoryResp.requestId={}|artifactoryResp" +
                            ".code={}|artifactoryResp.message={}|method={}|url={}|reqStr={}|respStr={}"
                        , ArtifactoryInterfaceConsts.RESULT_CODE_OK
                        , artifactoryResp.getTraceId()
                        , artifactoryResp.getCode()
                        , artifactoryResp.getMessage()
                        , method, url, reqStr, respStr
                    );
                    status = "error";
                    throw new ServiceException(ErrorCode.ARTIFACTORY_API_DATA_ERROR, "artifactoryResp code!=0");
                }
                if (artifactoryResp.getData() == null) {
                    log.warn(
                        "warn:artifactoryResp.getData() == null|artifactoryResp.requestId={}|artifactoryResp" +
                            ".code={}|artifactoryResp.message={}|method={}|url={}|reqStr={}|respStr={}"
                        , artifactoryResp.getTraceId()
                        , artifactoryResp.getCode()
                        , artifactoryResp.getMessage()
                        , method, url, reqStr, respStr
                    );
                }
            }
            status = "ok";
            return result;
        } catch (Exception e) {
            log.error("Fail to request ARTIFACTORY data|method={}|url={}|reqStr={}", method, url, reqStr, e);
            status = "error";
            throw new ServiceException(ErrorCode.ARTIFACTORY_API_DATA_ERROR, "Fail to request ARTIFACTORY data");
        } finally {
            long end = System.nanoTime();
            if (null != meterRegistry) {
                meterRegistry.timer("artifactory.api", "api_name", url, "status", status)
                    .record(end - start, TimeUnit.NANOSECONDS);
            }
        }
    }

    public boolean isAvailable() {
        try {
            getArtifactoryRespByReq(HttpGet.METHOD_NAME, URL_ACTUATOR_INFO,
                new ArtifactoryReq(), new TypeReference<Map<Object, Object>>() {
                }, httpHelper);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public List<ProjectDTO> listProject() {
        ArtifactoryResp<List<ProjectDTO>> resp = getArtifactoryRespByReq(HttpGet.METHOD_NAME, URL_LIST_PROJECT,
            new ListProjectReq(), new TypeReference<ArtifactoryResp<List<ProjectDTO>>>() {
            }, httpHelper);
        return resp.getData();
    }

    public PageData<RepoDTO> listRepo(String projectId, String name, int pageNumber, int pageSize) {
        ListRepoPageReq req = new ListRepoPageReq();
        req.setProjectId(projectId);
        req.setName(name);
        req.setPageNumber(pageNumber);
        req.setPageSize(pageSize);
        ArtifactoryResp<PageData<RepoDTO>> resp = getArtifactoryRespByReq(HttpGet.METHOD_NAME, URL_LIST_REPO_PAGE,
            req, new TypeReference<ArtifactoryResp<PageData<RepoDTO>>>() {
            }, httpHelper);
        return resp.getData();
    }

    public PageData<NodeDTO> listNode(String projectId, String repoName, String fullPath, int pageNumber,
                                      int pageSize) {
        ListNodePageReq req = new ListNodePageReq();
        req.setProjectId(projectId);
        req.setRepoName(repoName);
        req.setFullPath(fullPath);
        req.setPageNumber(pageNumber);
        req.setPageSize(pageSize);
        ArtifactoryResp<PageData<NodeDTO>> resp = getArtifactoryRespByReq(HttpGet.METHOD_NAME, URL_LIST_NODE_PAGE,
            req, new TypeReference<ArtifactoryResp<PageData<NodeDTO>>>() {
            }, httpHelper);
        return resp.getData();
    }

    public NodeDTO queryNodeDetail(String projectId, String repoName, String fullPath) {
        QueryNodeDetailReq req = new QueryNodeDetailReq();
        req.setProjectId(projectId);
        req.setRepoName(repoName);
        req.setFullPath(fullPath);
        ArtifactoryResp<NodeDTO> resp = getArtifactoryRespByReq(HttpGet.METHOD_NAME, URL_QUERY_NODE_DETAIL, req,
            new TypeReference<ArtifactoryResp<NodeDTO>>() {
            }, httpHelper);
        return resp.getData();
    }

    public Boolean deleteProject(String projectId) {
        throw new ServiceException(ErrorCode.NOT_SUPPORT_FEATURE);
    }

    public Boolean deleteRepo(String projectId, String repoName, Boolean forced) {
        DeleteRepoReq req = new DeleteRepoReq();
        req.setProjectId(projectId);
        req.setRepoName(repoName);
        req.setForced(forced);
        ArtifactoryResp<Object> resp = getArtifactoryRespByReq(HttpDelete.METHOD_NAME, URL_DELETE_REPO, req,
            new TypeReference<ArtifactoryResp<Object>>() {
            }, httpHelper);
        return resp.getCode() == 0;
    }

    public Boolean deleteNode(String projectId, String repoName, String fullPath) {
        DeleteNodeReq req = new DeleteNodeReq();
        req.setProjectId(projectId);
        req.setRepoName(repoName);
        req.setFullPath(fullPath);
        ArtifactoryResp<Object> resp = getArtifactoryRespByReq(HttpDelete.METHOD_NAME, URL_DELETE_NODE, req,
            new TypeReference<ArtifactoryResp<Object>>() {
            }, httpHelper);
        return resp.getCode() == 0;
    }

    private List<String> parsePath(String filePath) {
        // 解析projectId,repoName,fullPath
        filePath = StringUtil.removePrefixAndSuffix(filePath, "/");
        String[] pathArr = filePath.split("/");
        if (pathArr.length < 2) {
            throw new ServiceException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{"path", "path" +
                " must contain projectId and repoName"});
        }
        String projectId = pathArr[0];
        String repoName = pathArr[1];
        String fullPath = filePath.substring(projectId.length() + repoName.length() + 1);
        List<String> pathList = new ArrayList<>();
        pathList.add(projectId);
        pathList.add(repoName);
        pathList.add(fullPath);
        return pathList;
    }

    @Override
    public FileMetaData getFileMetaData(String filePath) {
        List<String> pathList = parsePath(filePath);
        NodeDTO nodeDTO = queryNodeDetail(pathList.get(0), pathList.get(1), pathList.get(2));
        if (null == nodeDTO) {
            throw new ServiceException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{"path", "can " +
                "not find node by filePath"});
        }
        return new FileMetaData(nodeDTO.getSize(), nodeDTO.getMd5());
    }

    @Override
    public InputStream getFileInputStream(String filePath) throws ServiceException {
        List<String> pathList = parsePath(filePath);
        DownloadGenericFileReq req = new DownloadGenericFileReq();
        req.setProject(pathList.get(0));
        req.setRepo(pathList.get(1));
        req.setPath(pathList.get(2));
        String url = StringUtil.replacePathVariables(URL_DOWNLOAD_GENERIC_FILE, req);
        url = getCompleteUrl(url);
        CloseableHttpResponse resp = null;
        try {
            resp = longHttpHelper.getRawResp(true, url, getBaseHeaders());
            return resp.getEntity().getContent();
        } catch (IOException e) {
            log.error("Fail to getFileInputStream", e);
            throw new ServiceException(e, ErrorCode.FAIL_TO_REQUEST_THIRD_FILE_SOURCE_DOWNLOAD_GENERIC_FILE,
                new String[]{e.getMessage()});
        }
    }

    @Override
    public void shutdown() {
    }
}
