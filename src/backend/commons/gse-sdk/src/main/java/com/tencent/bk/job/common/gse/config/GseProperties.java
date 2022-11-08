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
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GSE 配置
 */
@ConfigurationProperties(prefix = "gse")
@Getter
@Setter
public class GseProperties {

    private TaskServer taskServer = new TaskServer();

    private CacheApiServer cache = new CacheApiServer();

    private Ssl ssl = new Ssl();

    private Server server = new Server();

    /**
     * 对接的GSE 版本(v1/v2)
     */
    private String version = "v2";

    /**
     * GSE Task Server properties
     */
    @Getter
    @Setter
    @ToString
    public static class TaskServer {
        private String host;
        private Integer port = 48673;
    }

    /**
     * GSE Cache API Server properties
     */
    @Getter
    @Setter
    @ToString
    public static class CacheApiServer {
        private ApiServer apiServer = new ApiServer();

        @Getter
        @Setter
        @ToString
        public static class ApiServer {
            private String host;
            private Integer port = 59313;
        }
    }

    /**
     * GSE SSL properties
     */
    @Getter
    @Setter
    @ToString
    public static class Ssl {
        private KeyStore keyStore = new KeyStore();
        private TrustStore trustStore = new TrustStore();

        @Getter
        @Setter
        @ToString
        public static class KeyStore {
            private String path;
            private String password;
        }

        @Getter
        @Setter
        @ToString
        public static class TrustStore {
            private String path;
            private String password;
            private String managerType = "SunX509";
            private String storeType = "JKS";
        }
    }

    /**
     * Gse Server connection properties
     */
    @Getter
    @Setter
    @ToString
    public static class Server {
        private ZooKeeper zooKeeper;
        private Discovery discovery;

        @Getter
        @Setter
        @ToString
        public static class ZooKeeper {
            private ZooKeeperConnect connect = new ZooKeeperConnect();
            private String path = "/gse/config/server/task/accessv3";
            private ZooKeeperTimeout timeout = new ZooKeeperTimeout();

            /**
             * ZooKeeper timeout
             */
            @Getter
            @Setter
            @ToString
            public static class ZooKeeperTimeout {
                private Integer session = 60000;
                private Integer connect = 60000;
            }

            /**
             * ZooKeeper connect
             */
            @Getter
            @Setter
            @ToString
            public static class ZooKeeperConnect {
                private String string;
            }
        }

        @Getter
        @Setter
        @ToString
        public static class Discovery {
            private String type;
        }
    }
}
