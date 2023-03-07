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

package com.tencent.bk.job.common.gse.v1;

import com.tencent.bk.gse.taskapi.doSomeCmdV3;
import com.tencent.bk.job.common.gse.config.GseProperties;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.BeansException;

import java.io.Closeable;

/**
 * GSE Task Server Client
 */
@Slf4j
public class GseTaskClient implements Closeable {

    private static final String KEY_STORE;
    private static final String TRUST_STORE;
    private static final String KEY_STORE_PASS;
    private static final String TRUST_STORE_PASS;
    private static final String TRUST_MANAGER_TYPE;
    private static final String TRUST_STORE_TYPE;

    static {
        GseProperties gseProperties = ApplicationContextRegister.getBean(GseProperties.class);
        KEY_STORE = gseProperties.getSsl().getKeyStore().getPath();
        KEY_STORE_PASS = gseProperties.getSsl().getKeyStore().getPassword();
        TRUST_STORE = gseProperties.getSsl().getTrustStore().getPath();
        TRUST_STORE_PASS = gseProperties.getSsl().getTrustStore().getPassword();
        TRUST_MANAGER_TYPE = gseProperties.getSsl().getTrustStore().getManagerType();
        TRUST_STORE_TYPE = gseProperties.getSsl().getTrustStore().getStoreType();
    }

    /**
     * gse服务接口
     */
    private final doSomeCmdV3.Client gseAgentClient;
    private final TTransport transport;

    private GseTaskClient(String ip, int port) throws TException {
        BKTSSLTransportFactory.TSSLTransportParameters params =
            new BKTSSLTransportFactory.TSSLTransportParameters();
        params.setTrustStore(TRUST_STORE, TRUST_STORE_PASS, TRUST_MANAGER_TYPE, TRUST_STORE_TYPE);
        params.setKeyStore(KEY_STORE, KEY_STORE_PASS);
        transport = BKTSSLTransportFactory.getClientSocket(ip, port, 60000, params);
        TBinaryProtocol tProtocol = new TBinaryProtocol(transport);
        this.gseAgentClient = new doSomeCmdV3.Client(tProtocol);
        if (!transport.isOpen())
            transport.open();
    }

    public static GseTaskClient getClient() {
        try {
            GseServer gseServer = ApplicationContextRegister.getBean("gseServer");
            return gseServer.getClient();
        } catch (BeansException ignored) {
            log.error("load gseSever failed!", ignored);
        }
        return null;
    }

    /**
     * 构建gse访问客户端, 并连接服务端
     *
     * @param ip   GSE IP
     * @param port GSE Port
     * @return GseClient
     */
    static GseTaskClient getClient(String ip, int port) throws TException {
        log.info("Get gseTaskClient, ip: {}, port: {}", ip, port);
        return new GseTaskClient(ip, port);
    }

    public doSomeCmdV3.Client getGseAgentClient() {
        return gseAgentClient;
    }

    /**
     * 关闭连接
     */
    public void tearDown() {
        try {
            transport.close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void close() {
        tearDown();
    }
}

