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

package com.tencent.bk.job.common.gse.sdk;

import com.tencent.bk.gse.cacheapi.CacheAPI;
import com.tencent.bk.job.common.gse.config.GseConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
public class GseCacheClientFactory {

    private static AtomicInteger currentIpIndex = new AtomicInteger(0);
    private final GseConfig gseConfig;

    public GseCacheClientFactory(GseConfig gseConfig) {
        this.gseConfig = gseConfig;
    }

    public GseCacheClient getClient() {
        if (gseConfig.getGseCacheApiServerHost().length == 1 && StringUtils.isBlank(gseConfig.getGseCacheApiServerHost()[0])) {
            return null;
        }

        int tryTimes = gseConfig.getGseCacheApiServerHost().length;
        while (true) {
            int ipIndex = currentIpIndex.incrementAndGet() % gseConfig.getGseCacheApiServerHost().length;
            if (tryTimes > 0) {
                try {
                    String ip = gseConfig.getGseCacheApiServerHost()[ipIndex];
                    if (StringUtils.isBlank(ip)) {
                        continue;
                    }
                    return getAgent(ip, gseConfig.getGseCacheApiServerPort());
                } catch (TException e) {
                    log.error("Get GseCacheClient fail| msg={}| cause={}", e.getMessage(),
                        e.getCause().getMessage());
                    if ((--tryTimes) == 0) {
                        return null;
                    }
                }
            }
        }
    }

    /**
     * 构建gse访问客户端, 并连接服务端
     */
    private GseCacheClient getAgent(String ip, int port) throws TException {
        log.info("enter getClient with ip=" + ip + ", port=" + port);
        TTransport tTransport;
        if (gseConfig.isEnableSsl()) {
            BKTSSLTransportFactory.TSSLTransportParameters params =
                new BKTSSLTransportFactory.TSSLTransportParameters();
            params.setTrustStore(gseConfig.getTrustStore(), gseConfig.getTrustStorePass(),
                gseConfig.getTrustManagerType(), gseConfig.getTrustStoreType());
            params.setKeyStore(gseConfig.getKeyStore(), gseConfig.getKeyStorePass());
            tTransport = new TFramedTransport(BKTSSLTransportFactory.getClientSocket(ip, port, 15000, params));
        } else {
            TSocket tSocket = new TSocket(ip, port);
            tSocket.setTimeout(15000);
            tTransport = new TFramedTransport(tSocket);
        }
        TProtocol tProtocal = new TBinaryProtocol(tTransport);
        CacheAPI.Client gseAgentclient = new CacheAPI.Client(tProtocal);
        return new GseCacheClient(gseAgentclient, tTransport);
    }

}
