
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

import com.tencent.bk.job.common.gse.config.GseProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.utils.CloseableUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.SmartLifecycle;

@Slf4j
public class CuratorFrameworkFactoryBean implements FactoryBean<CuratorFramework>, SmartLifecycle {

    private final Object lifecycleLock = new Object();

    private final CuratorFramework client;

    /**
     * @see SmartLifecycle
     */
    private volatile boolean autoStartup = true;

    /**
     * @see SmartLifecycle
     */
    private volatile boolean running;

    /**
     * @see SmartLifecycle
     */
    private volatile int phase;

    public CuratorFrameworkFactoryBean(GseProperties.Server.ZooKeeper zooKeeperConfig,
                                       RetryPolicy retryPolicy) {
        String connectionString = zooKeeperConfig.getConnect().getString();
        int sessionTimeOutMills = zooKeeperConfig.getTimeout().getSession();
        int connectTimeOutMills = zooKeeperConfig.getTimeout().getConnect();
        if (connectionString == null || connectionString.trim().length() == 0) {
            this.client = null;
        } else {
            this.client = CuratorFrameworkFactory.newClient(connectionString, sessionTimeOutMills,
                connectTimeOutMills, retryPolicy);
        }
    }

    @Override
    public int getPhase() {
        return this.phase;
    }

    /**
     * @param phase the phase
     * @see SmartLifecycle
     */
    public void setPhase(int phase) {
        this.phase = phase;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public boolean isAutoStartup() {
        return this.autoStartup;
    }

    /**
     * @param autoStartup true to automatically start
     * @see SmartLifecycle
     */
    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    @Override
    public void start() {
        synchronized (this.lifecycleLock) {
            if (!this.running) {
                if (this.client != null) {
                    this.client.start();
                }
                this.running = true;
            }
        }
    }

    @Override
    public void stop() {
        synchronized (this.lifecycleLock) {
            if (this.running) {
                if (this.client != null) {
                    CloseableUtils.closeQuietly(this.client);
                }
                this.running = false;
            }
        }
    }

    @Override
    public void stop(Runnable runnable) {
        stop();
        runnable.run();
    }

    @Override
    public CuratorFramework getObject() {
        return this.client;
    }

    @Override
    public Class<?> getObjectType() {
        return CuratorFramework.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
