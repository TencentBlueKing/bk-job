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

import com.tencent.bk.gse.cacheapi.CacheAPI;
import com.tencent.bk.job.common.gse.config.GseV1Properties;
import com.tencent.bk.job.common.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class GseCacheClientFactory {

    private static final AtomicInteger currentHostIndex = new AtomicInteger(0);
    private final GseV1Properties gseV1Properties;
    private final GseV1Properties.CacheApiServer.ApiServer cacheApiServerProperties;
    private final String[] gseCacheApiServerHosts;

    @Autowired
    public GseCacheClientFactory(GseV1Properties gseV1Properties) {
        this.gseV1Properties = gseV1Properties;
        this.cacheApiServerProperties = gseV1Properties.getCache().getApiServer();
        this.gseCacheApiServerHosts = this.cacheApiServerProperties.getHost().split(",");
    }

    public GseCacheClient getClient() {
        int tryTimes = gseCacheApiServerHosts.length;
        while (true) {
            int hostIndex = currentHostIndex.incrementAndGet() % gseCacheApiServerHosts.length;
            String ip = "";
            if (tryTimes > 0) {
                try {
                    ip = gseCacheApiServerHosts[hostIndex];
                    if (StringUtils.isBlank(ip)) {
                        continue;
                    }
                    return getAgent(ip, cacheApiServerProperties.getPort());
                } catch (TException e) {
                    String msg = MessageFormatter.format(
                        "Get GseCacheClient fail|{}:{}|msg={}",
                        ArrayUtil.toArray(ip, cacheApiServerProperties.getPort(), e.getMessage()))
                        .getMessage();
                    log.error(msg, e);
                    if ((--tryTimes) == 0) {
                        return null;
                    }
                }
            }
        }
    }

    private GseCacheClient getAgent(String ip, int port) throws TException {
        log.info("Enter GetGseCacheClient with ip=" + ip + ", port=" + port);
        TTransport tTransport;
        BKTSSLTransportFactory.TSSLTransportParameters params =
            new BKTSSLTransportFactory.TSSLTransportParameters();
        params.setTrustStore(gseV1Properties.getSsl().getTrustStore().getPath(),
            gseV1Properties.getSsl().getTrustStore().getPassword(),
            gseV1Properties.getSsl().getTrustStore().getManagerType(),
            gseV1Properties.getSsl().getTrustStore().getStoreType());
        params.setKeyStore(gseV1Properties.getSsl().getKeyStore().getPath(),
            gseV1Properties.getSsl().getKeyStore().getPassword());
        StopWatch watch = new StopWatch("getAgent");

        watch.start("BKTSSLTransportFactory.getClientSocket");
        TSocket tSocket = BKTSSLTransportFactory.getClientSocket(ip, port, 15000, params);
        watch.stop();

        watch.start("new TFramedTransport");
        tTransport = new TFramedTransport(tSocket);
        watch.stop();

        watch.start("new TBinaryProtocol");
        TProtocol tProtocol = new TBinaryProtocol(tTransport);
        watch.stop();

        watch.start("new CacheAPI.Client");
        CacheAPI.Client gseAgentClient = new CacheAPI.Client(tProtocol);
        watch.stop();

        watch.start("new GseCacheClient");
        GseCacheClient gseCacheClient = new GseCacheClient(gseAgentClient, tTransport);
        watch.stop();

        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("getAgent slow, statistics: " + watch.prettyPrint());
        }
        return gseCacheClient;
    }

}
