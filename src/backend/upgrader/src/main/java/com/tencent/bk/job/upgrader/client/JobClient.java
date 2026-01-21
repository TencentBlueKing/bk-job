/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.upgrader.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.http.BasicHttpReq;
import com.tencent.bk.job.manage.model.migration.BkPlatformInfo;
import com.tencent.bk.job.upgrader.model.AppInfo;
import com.tencent.bk.job.upgrader.model.BasicAppInfo;
import com.tencent.bk.job.upgrader.model.job.SetBizSetMigrationStatusReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.util.List;

/**
 * Job接口调用客户端
 */
@Slf4j
public class JobClient extends AbstractJobClient {

    private static final String URL_LIST_NORMAL_APPS = "/service/app/list/normal";

    private static final String URL_LIST_BIZ_SET_APPS = "/service/app/list/bizSet";

    private static final String URL_SET_BIZ_SET_MIGRATION_STATUS = "/manage/migration/action/setBizSetMigrationStatus";

    private static final String URL_GET_BK_PLATFORM_INFO = "/manage/migration/action/getBkPlatformInfo";


    public JobClient(String jobHostUrl, String jobAuthToken) {
        super(jobHostUrl, jobAuthToken);
    }

    public List<BasicAppInfo> listNormalApps() {
        Response<List<BasicAppInfo>> resp = getJobRespByReq(
            HttpGet.METHOD_NAME,
            URL_LIST_NORMAL_APPS,
            new BasicHttpReq(),
            new TypeReference<Response<List<BasicAppInfo>>>() {
            });
        return resp.getData();
    }

    public List<AppInfo> listBizSetApps() {
        Response<List<AppInfo>> resp = getJobRespByReq(
            HttpGet.METHOD_NAME,
            URL_LIST_BIZ_SET_APPS,
            new BasicHttpReq(),
            new TypeReference<Response<List<AppInfo>>>() {
            });
        return resp.getData();
    }

    /**
     * 设置业务集迁移完成状态
     *
     * @param isMigrated 是否已完成迁移
     * @return 当前迁移状态
     */
    public boolean setBizSetMigrationStatus(boolean isMigrated) {
        SetBizSetMigrationStatusReq req = new SetBizSetMigrationStatusReq(isMigrated);
        Response<Boolean> resp = getJobRespByReq(
            HttpPost.METHOD_NAME,
            URL_SET_BIZ_SET_MIGRATION_STATUS,
            req,
            new TypeReference<Response<Boolean>>() {
            });
        return resp.getData() != null && resp.getData();
    }

    public BkPlatformInfo getBkPlatformInfo() {
        Response<BkPlatformInfo> resp = getJobRespByReq(
            HttpGet.METHOD_NAME,
            URL_GET_BK_PLATFORM_INFO,
            new BasicHttpReq(),
            new TypeReference<Response<BkPlatformInfo>>() {
            });
        return resp.getData();
    }
}
