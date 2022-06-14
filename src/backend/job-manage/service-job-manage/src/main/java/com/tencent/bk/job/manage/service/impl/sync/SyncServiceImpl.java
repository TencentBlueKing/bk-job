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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.manager.app.ApplicationCache;
import com.tencent.bk.job.manage.manager.host.HostCache;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@DependsOn("redisLockConfig")
public class SyncServiceImpl implements SyncService {

    private static final String REDIS_KEY_SYNC_APP_JOB_LOCK = "sync-app-job-lock";
    private static final String REDIS_KEY_SYNC_HOST_JOB_LOCK = "sync-host-job-lock";
    private static final String REDIS_KEY_SYNC_AGENT_STATUS_JOB_LOCK = "sync-agent-status-job-lock";
    private static final String REDIS_KEY_LAST_FINISH_TIME_SYNC_APP = "last-finish-time-sync-app";
    private static final String REDIS_KEY_LAST_FINISH_TIME_SYNC_HOST = "last-finish-time-sync-host";
    private static final String REDIS_KEY_LAST_FINISH_TIME_SYNC_AGENT_STATUS = "last-finish-time-sync-agent-status";
    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final int MAX_RETRY_COUNT = 3;

    static {
        List<String> keyList = Arrays.asList(REDIS_KEY_SYNC_APP_JOB_LOCK, REDIS_KEY_SYNC_HOST_JOB_LOCK,
            REDIS_KEY_SYNC_AGENT_STATUS_JOB_LOCK);
        keyList.forEach(key -> {
            try {
                //进程重启首先尝试释放上次加上的锁避免死锁
                LockUtils.releaseDistributedLock(key, machineIp);
            } catch (Throwable t) {
                log.info("Redis key:" + key + " does not need to be released, ignore");
            }
        });
    }

    private final DSLContext dslContext;
    private final ApplicationDAO applicationDAO;
    private final ApplicationHostDAO applicationHostDAO;
    private final HostTopoDAO hostTopoDAO;
    private final ApplicationService applicationService;
    private final QueryAgentStatusClient queryAgentStatusClient;
    private final ThreadPoolExecutor syncAppExecutor;
    private final ThreadPoolExecutor syncHostExecutor;
    private final ThreadPoolExecutor syncAgentStatusExecutor;
    private final JobManageConfig jobManageConfig;
    private final RedisTemplate<String, String> redisTemplate;
    private final String REDIS_KEY_SYNC_APP_JOB_RUNNING_MACHINE = "sync-app-job-running-machine";
    private final String REDIS_KEY_SYNC_HOST_JOB_RUNNING_MACHINE = "sync-host-job-running-machine";
    private final String REDIS_KEY_SYNC_AGENT_STATUS_JOB_RUNNING_MACHINE = "sync-agent-status-job-running-machine";
    private final BlockingQueue<Pair<ApplicationDTO, Integer>> appHostFailQueue = new LinkedBlockingDeque<>();
    private volatile LinkedBlockingQueue<Long> extraSyncAppQueue;
    private volatile boolean enableSyncApp;
    private volatile boolean enableSyncHost;
    private volatile boolean enableSyncAgentStatus;
    private BizWatchThread bizWatchThread = null;
    private HostWatchThread hostWatchThread = null;
    private HostRelationWatchThread hostRelationWatchThread = null;
    private final ApplicationCache applicationCache;
    private final BizSyncService bizSyncService;
    private final BizSetSyncService bizSetSyncService;
    private final HostSyncService hostSyncService;
    private final AgentStatusSyncService agentStatusSyncService;
    private final HostCache hostCache;
    private final BizSetEventWatcher bizSetEventWatcher;
    private final BizSetRelationEventWatcher bizSetRelationEventWatcher;

    @Autowired
    public SyncServiceImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext,
                           BizSyncService bizSyncService,
                           BizSetSyncService bizSetSyncService,
                           HostSyncService hostSyncService,
                           AgentStatusSyncService agentStatusSyncService,
                           ApplicationDAO applicationDAO,
                           ApplicationHostDAO applicationHostDAO,
                           HostTopoDAO hostTopoDAO,
                           ApplicationService applicationService,
                           QueryAgentStatusClient queryAgentStatusClient,
                           JobManageConfig jobManageConfig,
                           RedisTemplate<String, String> redisTemplate,
                           ApplicationCache applicationCache,
                           HostCache hostCache,
                           BizSetEventWatcher bizSetEventWatcher,
                           BizSetRelationEventWatcher bizSetRelationEventWatcher) {
        this.dslContext = dslContext;
        this.applicationDAO = applicationDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.applicationService = applicationService;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.jobManageConfig = jobManageConfig;
        this.redisTemplate = redisTemplate;
        this.enableSyncApp = jobManageConfig.isEnableSyncApp();
        this.enableSyncHost = jobManageConfig.isEnableSyncHost();
        this.enableSyncAgentStatus = jobManageConfig.isEnableSyncAgentStatus();
        this.applicationCache = applicationCache;
        this.bizSyncService = bizSyncService;
        this.bizSetSyncService = bizSetSyncService;
        this.hostSyncService = hostSyncService;
        this.agentStatusSyncService = agentStatusSyncService;
        this.hostCache = hostCache;
        this.bizSetEventWatcher = bizSetEventWatcher;
        this.bizSetRelationEventWatcher = bizSetRelationEventWatcher;
        // 同步业务的线程池配置
        syncAppExecutor = new ThreadPoolExecutor(5, 5, 1L,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(20), (r, executor) ->
            log.error(
                "syncAppExecutor Runnable rejected! executor.poolSize={}, executor.queueSize={}",
                executor.getPoolSize(), executor.getQueue().size()));
        syncAppExecutor.setThreadFactory(getThreadFactoryByNameAndSeq("syncAppExecutor-",
            new AtomicInteger(1)));
        // 同步主机的线程池配置
        syncHostExecutor = new ThreadPoolExecutor(5, 5, 1L,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(5000), (r, executor) ->
            log.error("syncHostExecutor Runnable rejected! executor.poolSize={}, executor.queueSize={}",
                executor.getPoolSize(), executor.getQueue().size()));
        syncHostExecutor.setThreadFactory(getThreadFactoryByNameAndSeq("syncHostExecutor-",
            new AtomicInteger(1)));
        // 同步主机Agent状态的线程池配置
        syncAgentStatusExecutor = new ThreadPoolExecutor(5, 5, 1L,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(5000),
            (r, executor) -> log.error("syncAgentStatusExecutor Runnable rejected! executor.poolSize={}, executor"
                + ".queueSize={}", executor.getPoolSize(), executor.getQueue().size()));
        syncAgentStatusExecutor.setThreadFactory(getThreadFactoryByNameAndSeq("syncAgentStatusExecutor-",
            new AtomicInteger(1)));
    }

    @Override
    public void init() {
        // 额外同步业务主机的队列与线程配置
        extraSyncAppQueue = new LinkedBlockingQueue<>(500);
        for (int i = 0; i < 3; i++) {
            AppHostsSyncer extraAppHostsSyncer = new AppHostsSyncer(applicationService, hostSyncService,
                extraSyncAppQueue);
            extraAppHostsSyncer.setName("[" + extraAppHostsSyncer.getId() + "]-extraAppHostsSyncer-" + (i + 1));
            extraAppHostsSyncer.start();
        }
        if (jobManageConfig.isEnableResourceWatch()) {
            watchBizEvent();
            watchHostEvent();
            watchBizSetEvent();
        } else {
            log.info("resourceWatch not enabled, you can enable it in config file");
        }
    }

    /**
     * 监听业务相关的事件
     */
    private void watchBizEvent() {
        // 开一个常驻线程监听业务资源变动事件
        bizWatchThread = new BizWatchThread(applicationService, redisTemplate);
        bizWatchThread.start();
    }

    /**
     * 监听主机相关的事件
     */
    private void watchHostEvent() {
        // 开一个常驻线程监听主机资源变动事件
        hostWatchThread = new HostWatchThread(
            dslContext,
            applicationHostDAO,
            queryAgentStatusClient,
            redisTemplate,
            hostCache
        );
        hostWatchThread.start();

        // 开一个常驻线程监听主机关系资源变动事件
        hostRelationWatchThread = new HostRelationWatchThread(
            dslContext,
            applicationHostDAO,
            hostTopoDAO,
            redisTemplate,
            this,
            hostCache
        );
        hostRelationWatchThread.start();
    }

    /**
     * 监听业务集相关的事件
     */
    private void watchBizSetEvent() {
        // 开一个常驻线程监听业务集变动事件
        bizSetEventWatcher.start();
        bizSetRelationEventWatcher.start();
    }

    public boolean addExtraSyncBizHostsTask(Long bizId) {
        if (extraSyncAppQueue.contains(bizId)) {
            return true;
        } else if (extraSyncAppQueue.remainingCapacity() > 0) {
            boolean result = extraSyncAppQueue.add(bizId);
            if (extraSyncAppQueue.size() > 10) {
                log.warn("extraSyncAppQueue.size={},queue={}", extraSyncAppQueue.size(), extraSyncAppQueue.toString());
            } else {
                log.debug("extraSyncAppQueue.size={},queue={}", extraSyncAppQueue.size(), extraSyncAppQueue.toString());
            }
            return result;
        }
        return false;
    }

    private ThreadFactory getThreadFactoryByNameAndSeq(String namePrefix, AtomicInteger seq) {
        return r -> {
            Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                namePrefix + seq.getAndIncrement(),
                0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        };
    }

    @Override
    public ThreadPoolExecutor getSyncAppExecutor() {
        return syncAppExecutor;
    }

    @Override
    public ThreadPoolExecutor getSyncHostExecutor() {
        return syncHostExecutor;
    }

    @Override
    public ThreadPoolExecutor getSyncAgentStatusExecutor() {
        return syncAgentStatusExecutor;
    }

    @Override
    public Long syncApp() {
        if (!enableSyncApp) {
            log.info("syncApp not enabled, skip, you can enable it in config file");
            return -1L;
        }
        log.info("syncApp arranged");
        boolean lockGotten = LockUtils.tryGetDistributedLock(
            REDIS_KEY_SYNC_APP_JOB_LOCK, machineIp, 5000);
        if (!lockGotten) {
            log.info("syncApp lock not gotten, return");
            return -1L;
        }
        String runningMachine = redisTemplate.opsForValue().get(REDIS_KEY_SYNC_APP_JOB_RUNNING_MACHINE);
        try {
            if (StringUtils.isNotBlank(runningMachine)) {
                //已有同步线程在跑，不再同步
                log.info("sync app thread already running on {}", runningMachine);
                return 1L;
            }
            syncAppExecutor.execute(() -> {
                // 开一个心跳子线程，维护当前机器正在同步业务的状态
                RedisKeyHeartBeatThread appSyncRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                    redisTemplate,
                    REDIS_KEY_SYNC_APP_JOB_RUNNING_MACHINE,
                    machineIp,
                    5000L,
                    4000L
                );
                appSyncRedisKeyHeartBeatThread.setName("[" + appSyncRedisKeyHeartBeatThread.getId() +
                    "]-appSyncRedisKeyHeartBeatThread");
                appSyncRedisKeyHeartBeatThread.start();
                log.info("start sync app at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("syncApp");
                watch.start("total");
                try {
                    // 从CMDB同步业务信息
                    bizSyncService.syncBizFromCMDB();
                    // 从CMDB同步业务集信息
                    if (FeatureToggle.isCmdbBizSetEnabled()) {
                        bizSetSyncService.syncBizSetFromCMDB();
                    } else {
                        log.info("Cmdb biz set is disabled, skip sync apps!");
                    }
                    log.info(Thread.currentThread().getName() + ":Finished:sync app from cmdb");
                    // 将最后同步时间写入Redis
                    redisTemplate.opsForValue().set(REDIS_KEY_LAST_FINISH_TIME_SYNC_APP,
                        "" + System.currentTimeMillis());
                } catch (Throwable t) {
                    log.error("FATAL: syncApp thread fail", t);
                } finally {
                    // 失败的业务进行补偿同步
                    handleFailedSyncAppHosts();
                    appSyncRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("syncApp time consuming:" + watch.prettyPrint());
                }
            });
        } finally {
            applicationCache.refreshCache();
            //释放锁
            LockUtils.releaseDistributedLock(REDIS_KEY_SYNC_APP_JOB_LOCK, machineIp);
        }
        return 1L;
    }

    /**
     * 同步主机失败的业务的补偿性同步
     */
    private void handleFailedSyncAppHosts() {
        if (appHostFailQueue.isEmpty()) {
            return;
        }
        Pair<ApplicationDTO, Integer> appInfoRetryCountPair = appHostFailQueue.poll();
        int maxCount = 1000;
        int count = 0;
        while (appInfoRetryCountPair != null && count < maxCount) {
            ApplicationDTO applicationDTO = appInfoRetryCountPair.getFirst();
            int retryCount = appInfoRetryCountPair.getSecond();
            try {
                if (retryCount > 0) {
                    arrangeSyncAppHostsTask(applicationDTO);
                } else {
                    log.warn("syncAppHost retry over max count, appId={}", applicationDTO.getId());
                }
            } catch (Throwable t) {
                count += 1;
                try {
                    appHostFailQueue.put(Pair.of(applicationDTO, retryCount - 1));
                } catch (InterruptedException e) {
                    log.error("appHostFailQueue.put(Pair.of(applicationInfoDTO,retryCount-1)) fail", e);
                }
            }
            appInfoRetryCountPair = appHostFailQueue.poll();
        }
        if (count >= 1000) {
            log.error("too many FailedSyncAppHosts, watch for a dead loop!");
        }
        log.info("handleFailedSyncAppHosts end");
    }

    private Future<Pair<Long, Long>> arrangeSyncAppHostsTask(ApplicationDTO applicationDTO) {
        return syncHostExecutor.submit(() ->
            hostSyncService.syncBizHostsAtOnce(applicationDTO));
    }

    @Override
    public Long syncHost() {
        if (!enableSyncHost) {
            log.info("syncHost not enabled, skip, you can enable it in config file");
            return -1L;
        }
        log.info("syncHost arranged");
        boolean lockGotten = LockUtils.tryGetDistributedLock(
            REDIS_KEY_SYNC_HOST_JOB_LOCK, machineIp, 5000);
        if (!lockGotten) {
            log.info("syncHost lock not gotten, return");
            return -1L;
        }
        String runningMachine = redisTemplate.opsForValue().get(REDIS_KEY_SYNC_HOST_JOB_RUNNING_MACHINE);
        try {
            if (StringUtils.isNotBlank(runningMachine)) {
                //已有同步线程在跑，不再同步
                log.info("sync host thread already running on {}", runningMachine);
                return 1L;
            }
            syncHostExecutor.execute(() -> {
                // 开一个心跳子线程，维护当前机器正在同步主机的状态
                RedisKeyHeartBeatThread hostSyncRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                    redisTemplate,
                    REDIS_KEY_SYNC_HOST_JOB_RUNNING_MACHINE,
                    machineIp,
                    5000L,
                    4000L
                );
                hostSyncRedisKeyHeartBeatThread.setName("[" + hostSyncRedisKeyHeartBeatThread.getId() +
                    "]-hostSyncRedisKeyHeartBeatThread");
                hostSyncRedisKeyHeartBeatThread.start();
                log.info("start sync host at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("syncHost");
                watch.start("total");
                try {
                    log.info(Thread.currentThread().getName() + ":begin to sync host from cc");
                    List<ApplicationDTO> localApps = applicationDAO.listAllBizApps();
                    Set<Long> localAppIds =
                        localApps.stream().filter(app ->
                            app.getAppType() == AppTypeEnum.NORMAL).map(ApplicationDTO::getId)
                            .collect(Collectors.toSet());
                    log.info(String.format("localAppIds:%s", String.join(",",
                        localAppIds.stream().map(Object::toString).collect(Collectors.toSet()))));
                    List<ApplicationDTO> localNormalApps =
                        localApps.stream().filter(app ->
                            app.getAppType() == AppTypeEnum.NORMAL).collect(Collectors.toList());
                    long cmdbInterfaceTimeConsuming = 0L;
                    long writeToDBTimeConsuming = 0L;
                    List<Pair<ApplicationDTO, Future<Pair<Long, Long>>>> appFutureList = new ArrayList<>();
                    for (ApplicationDTO applicationDTO : localNormalApps) {
                        Future<Pair<Long, Long>> future = arrangeSyncAppHostsTask(applicationDTO);
                        appFutureList.add(Pair.of(applicationDTO, future));
                    }
                    for (Pair<ApplicationDTO, Future<Pair<Long, Long>>> appFuture : appFutureList) {
                        ApplicationDTO applicationDTO = appFuture.getFirst();
                        Future<Pair<Long, Long>> future = appFuture.getSecond();
                        try {
                            Pair<Long, Long> timeConsumingPair = future.get(30, TimeUnit.MINUTES);
                            cmdbInterfaceTimeConsuming += timeConsumingPair.getFirst();
                            writeToDBTimeConsuming += timeConsumingPair.getSecond();
                        } catch (Throwable t) {
                            appHostFailQueue.add(Pair.of(applicationDTO, MAX_RETRY_COUNT));
                            log.error("syncHost of app fail:appId=" + applicationDTO.getId(), t);
                        }
                    }
                    log.info(
                        Thread.currentThread().getName() +
                            ":Finished:sync host from cc," +
                            "cmdbInterfaceTimeConsuming={}ms,writeToDBTimeConsuming={}ms,rate={}",
                        cmdbInterfaceTimeConsuming,
                        writeToDBTimeConsuming,
                        cmdbInterfaceTimeConsuming / (0. + writeToDBTimeConsuming)
                    );
                    // 将最后同步时间写入Redis
                    redisTemplate.opsForValue().set(REDIS_KEY_LAST_FINISH_TIME_SYNC_HOST,
                        "" + System.currentTimeMillis());
                } catch (Throwable t) {
                    log.error("syncHost thread fail", t);
                } finally {
                    //失败的同步任务补偿处理
                    handleFailedSyncAppHosts();
                    hostSyncRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("syncHost time consuming:" + watch.prettyPrint());
                }
            });
        } finally {
            //释放锁
            LockUtils.releaseDistributedLock(REDIS_KEY_SYNC_HOST_JOB_LOCK, machineIp);
        }
        return 1L;
    }

    @Override
    public Future<Pair<Long, Long>> arrangeSyncBizHostsTask(Long bizId) {
        log.info("arrangeSyncAppHostsTask:appId={}", bizId);
        return arrangeSyncAppHostsTask(applicationDAO.getAppById(bizId));
    }

    @Override
    public Boolean enableBizWatch() {
        if (bizWatchThread != null) {
            log.info("appWatch enabled by op");
            bizWatchThread.setWatchFlag(true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean disableBizWatch() {
        if (bizWatchThread != null) {
            log.info("appWatch disabled by op");
            bizWatchThread.setWatchFlag(false);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean enableHostWatch() {
        if (hostWatchThread != null && hostRelationWatchThread != null) {
            log.info("hostWatch enabled by op");
            hostWatchThread.setWatchFlag(true);
            hostRelationWatchThread.setWatchFlag(true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean disableHostWatch() {
        if (hostWatchThread != null && hostRelationWatchThread != null) {
            log.info("hostWatch disabled by op");
            hostWatchThread.setWatchFlag(false);
            hostRelationWatchThread.setWatchFlag(false);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean enableSyncApp() {
        enableSyncApp = true;
        return true;
    }

    @Override
    public Boolean disableSyncApp() {
        enableSyncApp = false;
        return true;
    }

    @Override
    public Boolean enableSyncHost() {
        enableSyncHost = true;
        return true;
    }

    @Override
    public Boolean disableSyncHost() {
        enableSyncHost = false;
        return true;
    }

    @Override
    public Boolean enableSyncAgentStatus() {
        enableSyncAgentStatus = true;
        return true;
    }

    @Override
    public Boolean disableSyncAgentStatus() {
        enableSyncAgentStatus = false;
        return true;
    }

    @Override
    public Boolean syncBizHosts(Long bizId) {
        log.info("syncBizHosts:bizId={}", bizId);
        ApplicationDTO applicationDTO = applicationDAO.getAppByScope(
            new ResourceScope(ResourceScopeTypeEnum.BIZ, bizId.toString())
        );
        Pair<Long, Long> pair = hostSyncService.syncBizHostsAtOnce(applicationDTO);
        Long cmdbInterfaceTimeConsuming = pair.getFirst();
        Long writeToDBTimeConsuming = pair.getSecond();
        log.info("syncBizHosts:cmdbInterfaceTimeConsuming={},writeToDBTimeConsuming={}", cmdbInterfaceTimeConsuming,
            writeToDBTimeConsuming);
        return true;
    }

    @Override
    public Long syncAgentStatus() {
        if (!enableSyncAgentStatus) {
            log.info("syncAgentStatus not enabled, skip, you can enable it in config file");
            return -1L;
        }
        log.info("syncAgentStatus arranged");
        boolean lockGotten = LockUtils.tryGetDistributedLock(
            REDIS_KEY_SYNC_AGENT_STATUS_JOB_LOCK, machineIp, 5000);
        if (!lockGotten) {
            log.info("syncAgentStatus lock not gotten, return");
            return -1L;
        }
        String runningMachine = redisTemplate.opsForValue().get(REDIS_KEY_SYNC_AGENT_STATUS_JOB_RUNNING_MACHINE);
        try {
            if (StringUtils.isNotBlank(runningMachine)) {
                //已有同步线程在跑，不再同步
                log.info("syncAgentStatus thread already running on {}", runningMachine);
                return 1L;
            }
            syncAgentStatusExecutor.execute(() -> {
                // 开一个心跳子线程，维护当前机器正在同步主机的状态
                RedisKeyHeartBeatThread agentStatusSyncRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
                    redisTemplate,
                    REDIS_KEY_SYNC_AGENT_STATUS_JOB_RUNNING_MACHINE,
                    machineIp,
                    5000L,
                    4000L
                );
                agentStatusSyncRedisKeyHeartBeatThread.setName(
                    "[" + agentStatusSyncRedisKeyHeartBeatThread.getId()
                        + "]-agentStatusSyncRedisKeyHeartBeatThread"
                );
                agentStatusSyncRedisKeyHeartBeatThread.start();
                log.info("start sync agentStatus at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("syncAgentStatus");
                watch.start("total");
                try {
                    // 从GSE同步Agent状态
                    agentStatusSyncService.syncAgentStatusFromGSE();
                    // 将最后同步时间写入Redis
                    redisTemplate.opsForValue().set(REDIS_KEY_LAST_FINISH_TIME_SYNC_AGENT_STATUS,
                        "" + System.currentTimeMillis());
                } catch (Throwable t) {
                    log.error("syncAgentStatus thread fail", t);
                } finally {
                    agentStatusSyncRedisKeyHeartBeatThread.setRunFlag(false);
                    watch.stop();
                    log.info("syncAgentStatus time consuming:" + watch.prettyPrint());
                }
            });
        } finally {
            //释放锁
            LockUtils.releaseDistributedLock(REDIS_KEY_SYNC_AGENT_STATUS_JOB_LOCK, machineIp);
        }
        return 1L;
    }

    private Long getLastFinishTime(String key) {
        String lastFinishTimeStr = redisTemplate.opsForValue().get(key);
        if (lastFinishTimeStr == null) {
            return null;
        } else {
            return Long.parseLong(lastFinishTimeStr);
        }
    }

    @Override
    public Long getLastFinishTimeSyncApp() {
        return getLastFinishTime(REDIS_KEY_LAST_FINISH_TIME_SYNC_APP);
    }

    @Override
    public Long getLastFinishTimeSyncHost() {
        return getLastFinishTime(REDIS_KEY_LAST_FINISH_TIME_SYNC_HOST);
    }

    @Override
    public Long getLastFinishTimeSyncAgentStatus() {
        return getLastFinishTime(REDIS_KEY_LAST_FINISH_TIME_SYNC_AGENT_STATUS);
    }

}
