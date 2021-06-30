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

package com.tencent.bk.job.common.gse.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @since 12/11/2019 11:10
 */
@Getter
@Setter
public class GseConfig {
    /**
     * GSE task server host
     */
    private String[] gseTaskServerHost;

    /**
     * GSE task server port
     */
    private int gseTaskServerPort = 48673;

    /**
     * GSE cache api service host
     */
    private String[] gseCacheApiServerHost;

    /**
     * GSE cache api service port
     */
    private int gseCacheApiServerPort = 59313;

    /**
     * 是否使用ssl
     */
    private boolean enableSsl = true;


    private String keyStore;

    private String keyStorePass;


    private String trustStore;

    private String trustStorePass;


    private String trustManagerType = "SunX509";

    private String trustStoreType = "JKS";

    private int queryThreadsNum = 5;

    private int queryBatchSize = 5000;

}
