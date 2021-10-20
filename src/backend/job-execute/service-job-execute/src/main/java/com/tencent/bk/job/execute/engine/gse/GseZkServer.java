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

package com.tencent.bk.job.execute.engine.gse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.gse.model.AccessServerInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.shaded.com.google.common.base.Charsets;
import org.apache.curator.utils.PathUtils;
import org.apache.curator.utils.ZKPaths;

import java.util.List;
import java.util.Map;

@Slf4j
public class GseZkServer extends GseServer {

    private static final String DEFAULT_GSE_SERVER_PATH = "/gse/config/server/task/accessv3";
    private final Map<String, Integer> serverMap = Maps.newConcurrentMap();
    private PathChildrenCache cache;
    private List<Map.Entry<String, Integer>> servers;

    public GseZkServer(CuratorFramework client, String gseServerPath) {
        super();
        if (client != null) {
            try {
                gseServerPath = PathUtils.validatePath(gseServerPath);
                if (gseServerPath.length() == 1) {
                    gseServerPath = DEFAULT_GSE_SERVER_PATH;
                }
            } catch (Exception e) {
                gseServerPath = DEFAULT_GSE_SERVER_PATH;
            }

            try {
                cache = new PathChildrenCache(client, gseServerPath, true);
                cache.start();
                addListener(cache);
            } catch (Exception e) {
                log.error("Gse zk listener start failed!", e);
            }
        }
    }

    /**
     * 构建gse访问客户端, 并连接服务端
     */
    @Override
    public GseClient getClient() {
        List<Map.Entry<String, Integer>> servers = getGseServer();
        GseClient client = getGseClient(servers);

        if (client == null) {
            client = super.getClient();
        }
        return client;
    }

    /**
     * 获取 GSE 服务端列表
     *
     * @return GSE 服务端列表
     */
    @Override
    List<Map.Entry<String, Integer>> getGseServer() {
        if (servers == null || servers.isEmpty() || cache == null) {
            // No data in zk cache, fallback to domain/ip list
            return super.getGseServer();
        }
        return servers;
    }

    /**
     * 初始化 ZK 事件监听器
     *
     * @param cache 待监听的 zk 节点
     */
    private void addListener(PathChildrenCache cache) {
        PathChildrenCacheListener listener = (client, event) -> {
            switch (event.getType()) {
                case CHILD_ADDED: {
                    String server = ZKPaths.getNodeFromPath(event.getData().getPath());
                    AccessServerInfoDTO accessServerInfo = parseAccessInfo(new String(event.getData().getData(),
                        Charsets.UTF_8));
                    if (accessServerInfo != null) {
                        if (accessServerInfo.getPort() > 0) {
                            int port = accessServerInfo.getPort();
                            log.info("GSE Server added| {}:{}", server, port);
                            serverMap.put(server, port);
                            servers = Lists.newArrayList(serverMap.entrySet());
                        }
                    }
                    break;
                }

                case CHILD_UPDATED: {
                    String server = ZKPaths.getNodeFromPath(event.getData().getPath());
                    AccessServerInfoDTO accessServerInfo = parseAccessInfo(new String(event.getData().getData(),
                        Charsets.UTF_8));
                    if (accessServerInfo != null) {
                        if (accessServerInfo.getPort() > 0) {
                            int port = accessServerInfo.getPort();
                            log.info("GSE Server changed| {}:{}", server, port);
                            serverMap.put(server, port);
                            servers = Lists.newArrayList(serverMap.entrySet());
                        }
                    }
                    break;
                }

                case CHILD_REMOVED: {
                    String server = ZKPaths.getNodeFromPath(event.getData().getPath());
                    log.info("GSE Server removed| {}", server);
                    serverMap.remove(server);
                    servers = Lists.newArrayList(serverMap.entrySet());
                    break;
                }

                default:
                    log.debug("Skip {} event", event.getType());
                    break;
            }
        };
        cache.getListenable().addListener(listener);
    }

    /**
     * 解析 ZK 中存储的服务端信息
     *
     * @param data 服务端信息字符串
     * @return 服务端信息
     */
    private AccessServerInfoDTO parseAccessInfo(String data) {
        return JsonUtils.fromJson(data, AccessServerInfoDTO.class);
    }
}
