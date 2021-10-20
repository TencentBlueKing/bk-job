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

import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.sdk.CcClient;
import com.tencent.bk.job.common.cc.sdk.CcClientFactory;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static List<Long> allAppInsertFailHostIds = new ArrayList<>();
    private static List<Long> allAppUpdateFailHostIds = new ArrayList<>();
    private static List<Long> allAppDeleteFailHostIds = new ArrayList<>();

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
    private final ApplicationInfoDAO applicationInfoDAO;
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
    private final BlockingQueue<Pair<ApplicationInfoDTO, Integer>> appHostFailQueue = new LinkedBlockingDeque<>();
    private volatile LinkedBlockingQueue<Long> extraSyncAppQueue;
    private AppHostsUpdateHelper appHostsUpdateHelper;
    private volatile boolean enableSyncApp;
    private volatile boolean enableSyncHost;
    private volatile boolean enableSyncAgentStatus;
    private AppWatchThread appWatchThread = null;
    private HostWatchThread hostWatchThread = null;
    private HostRelationWatchThread hostRelationWatchThread = null;

    @Autowired
    public SyncServiceImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext,
                           ApplicationInfoDAO applicationInfoDAO, ApplicationHostDAO applicationHostDAO,
                           HostTopoDAO hostTopoDAO, ApplicationService applicationService,
                           QueryAgentStatusClient queryAgentStatusClient, JobManageConfig jobManageConfig,
                           RedisTemplate<String,
                               String> redisTemplate) {
        this.dslContext = dslContext;
        this.applicationInfoDAO = applicationInfoDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.hostTopoDAO = hostTopoDAO;
        this.applicationService = applicationService;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.jobManageConfig = jobManageConfig;
        this.redisTemplate = redisTemplate;
        this.enableSyncApp = jobManageConfig.isEnableSyncApp();
        this.enableSyncHost = jobManageConfig.isEnableSyncHost();
        this.enableSyncAgentStatus = jobManageConfig.isEnableSyncAgentStatus();
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
            AppHostsSyncer extraAppHostsSyncer = new AppHostsSyncer(extraSyncAppQueue);
            extraAppHostsSyncer.setName("[" + extraAppHostsSyncer.getId() + "]-extraAppHostsSyncer-" + (i + 1));
            extraAppHostsSyncer.start();
        }
        appHostsUpdateHelper = new AppHostsUpdateHelper(redisTemplate);
        if (jobManageConfig.isEnableResourceWatch()) {
            // 开一个常驻线程监听业务资源变动事件
            appWatchThread = new AppWatchThread(dslContext, applicationInfoDAO, applicationService, redisTemplate);
            appWatchThread.start();
            // 开一个常驻线程监听主机资源变动事件
            hostWatchThread = new HostWatchThread(dslContext, applicationHostDAO, queryAgentStatusClient,
                redisTemplate, appHostsUpdateHelper);
            hostWatchThread.start();
            // 开一个常驻线程监听主机关系资源变动事件
            hostRelationWatchThread = new HostRelationWatchThread(dslContext, applicationHostDAO, hostTopoDAO,
                redisTemplate, this, appHostsUpdateHelper);
            hostRelationWatchThread.start();
        } else {
            log.info("resourceWatch not enabled, you can enable it in config file");
        }
    }

    public boolean addExtraSyncAppHostsTask(Long appId) {
        if (extraSyncAppQueue.contains(appId)) {
            return true;
        } else if (extraSyncAppQueue.remainingCapacity() > 0) {
            boolean result = extraSyncAppQueue.add(appId);
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
                createBkPlatformAppIfNotExist();
                log.info("start sync app at {},{}", TimeUtil.getCurrentTimeStr("HH:mm:ss"),
                    System.currentTimeMillis());
                StopWatch watch = new StopWatch("syncApp");
                watch.start("total");
                try {
                    log.info(Thread.currentThread().getName() + ":begin to sync app from cc");
                    CcClient ccClient = CcClientFactory.getCcClient();
                    List<ApplicationInfoDTO> ccApps = ccClient.getAllApps();

                    //对比业务信息，分出要删的/要改的/要新增的分别处理
                    List<ApplicationInfoDTO> insertList;
                    List<ApplicationInfoDTO> updateList;
                    List<ApplicationInfoDTO> deleteList;
                    //对比库中数据与接口数据
                    List<ApplicationInfoDTO> localApps = applicationInfoDAO.listAppInfo();
                    Set<Long> ccAppIds = ccApps.stream().map(ApplicationInfoDTO::getId).collect(Collectors.toSet());
                    //CC接口空数据保护
                    if (ccAppIds.isEmpty()) {
                        log.warn("CC App data is empty, quit sync");
                        return;
                    }
                    log.info(String.format("ccAppIds:%s", String.join(",",
                        ccAppIds.stream().map(Object::toString).collect(Collectors.toSet()))));
                    Set<Long> localAppIds =
                        localApps.stream().map(ApplicationInfoDTO::getId).collect(Collectors.toSet());
                    log.info(String.format("localAppIds:%s", String.join(",",
                        localAppIds.stream().map(Object::toString).collect(Collectors.toSet()))));
                    insertList =
                        ccApps.stream().filter(applicationInfoDTO ->
                            !localAppIds.contains(applicationInfoDTO.getId())).collect(Collectors.toList());
                    log.info(String.format("app insertList:%s", String.join(",",
                        insertList.stream().map(applicationInfoDTO -> applicationInfoDTO.getId().toString())
                            .collect(Collectors.toSet()))));
                    // 当前CC无业务类型数据，业务类型数据只能从本地数据判断
                    List<Long> intersectLocalAppIds =
                        localAppIds.stream().filter(id -> ccAppIds.contains(id)).collect(Collectors.toList());
                    List<Long> updateIdList =
                        localApps.stream().filter(applicationInfoDTO ->
                            applicationInfoDTO.getAppType() == AppTypeEnum.NORMAL
                                && intersectLocalAppIds.contains(applicationInfoDTO.getId()))
                            .map(it -> it.getId()).collect(Collectors.toList());
                    updateList =
                        ccApps.stream().filter(applicationInfoDTO ->
                            updateIdList.contains(applicationInfoDTO.getId())).collect(Collectors.toList());
                    log.info(String.format("app updateList:%s", String.join(",",
                        updateList.stream().map(applicationInfoDTO ->
                            applicationInfoDTO.getId().toString()).collect(Collectors.toSet()))));
                    deleteList =
                        localApps.stream().filter(applicationInfoDTO ->
                            applicationInfoDTO.getAppType() == AppTypeEnum.NORMAL
                                && !ccAppIds.contains(applicationInfoDTO.getId())).collect(Collectors.toList());
                    log.info(String.format("app deleteList:%s", String.join(",",
                        deleteList.stream().map(applicationInfoDTO ->
                            applicationInfoDTO.getId().toString()).collect(Collectors.toSet()))));
                    insertList.forEach(applicationInfoDTO -> {
                        try {
                            addAppToDb(applicationInfoDTO, Collections.emptyList());
                        } catch (Throwable t) {
                            log.error("FATAL: insertApp fail:appId=" + applicationInfoDTO.getId(), t);
                        }
                    });
                    updateList.forEach(applicationInfoDTO -> {
                        try {
                            applicationInfoDAO.updateAppInfo(dslContext, applicationInfoDTO);
                        } catch (Throwable t) {
                            log.error("FATAL: updateApp fail:appId=" + applicationInfoDTO.getId(), t);
                        }
                    });
                    deleteList.forEach(applicationInfoDTO -> {
                        try {
                            deleteAppFromDb(applicationInfoDTO);
                        } catch (Throwable t) {
                            log.error("FATAL: deleteApp fail:appId=" + applicationInfoDTO.getId(), t);
                        }
                    });
                    log.info(Thread.currentThread().getName() + ":Finished:sync app from cc");
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
            //释放锁
            LockUtils.releaseDistributedLock(REDIS_KEY_SYNC_APP_JOB_LOCK, machineIp);
        }
        return 1L;
    }

    /*
     * 创建蓝鲸全业务，用于平台业务的调用
     */
    private void createBkPlatformAppIfNotExist() {
        ApplicationInfoDTO bkApp = applicationService.getAppInfoById(9991001L);
        if (bkApp == null) {
            bkApp = new ApplicationInfoDTO();
            bkApp.setAppType(AppTypeEnum.ALL_APP);
            bkApp.setId(9991001L);
            bkApp.setMaintainers("admin");
            bkApp.setBkSupplierAccount("0");
            bkApp.setName("BlueKing");
            addAppToDb(bkApp, Collections.emptyList());
        }
    }

    /**
     * 同步主机失败的业务的补偿性同步
     */
    private void handleFailedSyncAppHosts() {
        if (appHostFailQueue.isEmpty()) {
            return;
        }
        Pair<ApplicationInfoDTO, Integer> appInfoRetryCountPair = appHostFailQueue.poll();
        int maxCount = 1000;
        int count = 0;
        while (appInfoRetryCountPair != null && count < maxCount) {
            ApplicationInfoDTO applicationInfoDTO = appInfoRetryCountPair.getFirst();
            Integer retryCount = appInfoRetryCountPair.getSecond();
            try {
                if (retryCount > 0) {
                    arrangeSyncAppHostsTask(applicationInfoDTO);
                } else {
                    log.warn("syncAppHost retry over max count, appId={}", applicationInfoDTO.getId());
                }
            } catch (Throwable t) {
                count += 1;
                try {
                    appHostFailQueue.put(Pair.of(applicationInfoDTO, retryCount - 1));
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

    private Future<Pair<Long, Long>> arrangeSyncAppHostsTask(ApplicationInfoDTO applicationInfoDTO) {
        return syncHostExecutor.submit(() -> syncAppHostsAtOnce(applicationInfoDTO));
    }

    private Pair<Long, Long> syncAppHostsAtOnce(ApplicationInfoDTO applicationInfoDTO) {
        Long appId = applicationInfoDTO.getId();
        try {
            appHostsUpdateHelper.waitAndStartAppHostsUpdating(appId);
            return syncAppHostsIndeed(applicationInfoDTO);
        } catch (Throwable t) {
            log.error("Fail to syncAppHosts of appId " + appId, t);
            return null;
        } finally {
            appHostsUpdateHelper.endToUpdateAppHosts(appId);
        }
    }

    private Pair<Long, Long> syncAppHostsIndeed(ApplicationInfoDTO applicationInfoDTO) {
        Long appId = applicationInfoDTO.getId();
        Long cmdbInterfaceTimeConsuming = 0L;
        Long writeToDBTimeConsuming = 0L;
        CcClient ccClient = CcClientFactory.getCcClient();
        StopWatch appHostsWatch = new StopWatch();
        appHostsWatch.start("getHostsByAppInfo from CMDB");
        Long startTime = System.currentTimeMillis();
        log.info("begin to syncAppHosts:appId={}", appId);
        List<ApplicationHostInfoDTO> hosts = getHostsByAppInfo(ccClient, applicationInfoDTO);
        cmdbInterfaceTimeConsuming += (System.currentTimeMillis() - startTime);
        appHostsWatch.stop();
        appHostsWatch.start("updateHosts to local DB");
        startTime = System.currentTimeMillis();
        refreshAppHosts(appId, hosts);
        writeToDBTimeConsuming += (System.currentTimeMillis() - startTime);
        appHostsWatch.stop();
        log.info("Performance:syncAppHosts:appId={},{}", appId, appHostsWatch.toString());
        return Pair.of(cmdbInterfaceTimeConsuming, writeToDBTimeConsuming);
    }

    private Pair<Long, Long> syncAppHostAgentStatus(Long appId) {
        Long gseInterfaceTimeConsuming = 0L;
        Long writeToDBTimeConsuming = 0L;
        StopWatch appHostAgentStatusWatch = new StopWatch();
        appHostAgentStatusWatch.start("listHostInfoByAppId");
        List<ApplicationHostInfoDTO> localAppHosts = applicationHostDAO.listHostInfoByAppId(appId);
        appHostAgentStatusWatch.stop();
        appHostAgentStatusWatch.start("getAgentStatusByAppInfo from GSE");
        Long startTime = System.currentTimeMillis();
        applicationService.fillAgentStatus(localAppHosts);
        gseInterfaceTimeConsuming += (System.currentTimeMillis() - startTime);
        appHostAgentStatusWatch.stop();
        appHostAgentStatusWatch.start("updateHosts to local DB");
        startTime = System.currentTimeMillis();
        updateHostsInApp(appId, localAppHosts);
        writeToDBTimeConsuming += (System.currentTimeMillis() - startTime);
        appHostAgentStatusWatch.stop();
        log.debug("Performance:syncAppHostAgentStatus:appId={},{}", appId,
            appHostAgentStatusWatch.toString());
        return Pair.of(gseInterfaceTimeConsuming, writeToDBTimeConsuming);
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
                allAppInsertFailHostIds.clear();
                allAppUpdateFailHostIds.clear();
                allAppDeleteFailHostIds.clear();
                try {
                    log.info(Thread.currentThread().getName() + ":begin to sync host from cc");
                    List<ApplicationInfoDTO> localApps = applicationInfoDAO.listAppInfo();
                    Set<Long> localAppIds =
                        localApps.stream().filter(app ->
                            app.getAppType() == AppTypeEnum.NORMAL).map(ApplicationInfoDTO::getId)
                            .collect(Collectors.toSet());
                    log.info(String.format("localAppIds:%s", String.join(",",
                        localAppIds.stream().map(Object::toString).collect(Collectors.toSet()))));
                    List<ApplicationInfoDTO> localNormalApps =
                        localApps.stream().filter(app ->
                            app.getAppType() == AppTypeEnum.NORMAL).collect(Collectors.toList());
                    //删除已移除业务的主机，部分测试主机放在业务集下，不删除
                    if (!localNormalApps.isEmpty()) {
                        applicationHostDAO.deleteAppHostInfoNotInApps(dslContext,
                            localApps.stream().map(ApplicationInfoDTO::getId).collect(Collectors.toSet()));
                    }
                    Long cmdbInterfaceTimeConsuming = 0L;
                    Long writeToDBTimeConsuming = 0L;
                    List<Pair<ApplicationInfoDTO, Future<Pair<Long, Long>>>> appFutureList = new ArrayList<>();
                    for (ApplicationInfoDTO applicationInfoDTO : localNormalApps) {
                        Future<Pair<Long, Long>> future = arrangeSyncAppHostsTask(applicationInfoDTO);
                        appFutureList.add(Pair.of(applicationInfoDTO, future));
                    }
                    for (Pair<ApplicationInfoDTO, Future<Pair<Long, Long>>> appFuture : appFutureList) {
                        ApplicationInfoDTO applicationInfoDTO = appFuture.getFirst();
                        Future<Pair<Long, Long>> future = appFuture.getSecond();
                        try {
                            Pair<Long, Long> timeConsumingPair = future.get(30, TimeUnit.MINUTES);
                            cmdbInterfaceTimeConsuming += timeConsumingPair.getFirst();
                            writeToDBTimeConsuming += timeConsumingPair.getSecond();
                        } catch (Throwable t) {
                            appHostFailQueue.add(Pair.of(applicationInfoDTO, MAX_RETRY_COUNT));
                            log.error("syncHost of app fail:appId=" + applicationInfoDTO.getId(), t);
                        }
                    }
                    log.info(Thread.currentThread().getName() + ":Finished:sync host from cc," +
                            "cmdbInterfaceTimeConsuming={}ms,writeToDBTimeConsuming={}ms,rate={}",
                        cmdbInterfaceTimeConsuming, writeToDBTimeConsuming,
                        cmdbInterfaceTimeConsuming / (0. + writeToDBTimeConsuming));
                    log.info(Thread.currentThread().getName() + ":Finished:Statistics:allAppInsertFailHostIds={}," +
                            "allAppUpdateFailHostIds={},allAppDeleteFailHostIds={}", allAppInsertFailHostIds,
                        allAppUpdateFailHostIds, allAppDeleteFailHostIds);
                    allAppInsertFailHostIds.clear();
                    allAppUpdateFailHostIds.clear();
                    allAppDeleteFailHostIds.clear();
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
    public Future<Pair<Long, Long>> arrangeSyncAppHostsTask(Long appId) {
        log.info("arrangeSyncAppHostsTask:appId={}", appId);
        return arrangeSyncAppHostsTask(applicationInfoDAO.getAppInfoById(appId));
    }

    @Override
    public Boolean enableAppWatch() {
        if (appWatchThread != null) {
            log.info("appWatch enabled by op");
            appWatchThread.setWatchFlag(true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean disableAppWatch() {
        if (appWatchThread != null) {
            log.info("appWatch disabled by op");
            appWatchThread.setWatchFlag(false);
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
    public Boolean syncAppHosts(Long appId) {
        log.info("syncAppHosts:appId={}", appId);
        Pair<Long, Long> pair = syncAppHostsAtOnce(applicationInfoDAO.getAppInfoById(appId));
        Long cmdbInterfaceTimeConsuming = pair.getFirst();
        Long writeToDBTimeConsuming = pair.getSecond();
        log.info("syncAppHosts:cmdbInterfaceTimeConsuming={},writeToDBTimeConsuming={}", cmdbInterfaceTimeConsuming,
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
                    log.info(Thread.currentThread().getName() + ":begin to sync agentStatus from GSE");
                    List<ApplicationInfoDTO> localApps = applicationInfoDAO.listAppInfo();
                    Set<Long> localAppIds =
                        localApps.stream().filter(app ->
                            app.getAppType() == AppTypeEnum.NORMAL).map(ApplicationInfoDTO::getId)
                            .collect(Collectors.toSet());
                    log.info(String.format("localAppIds:%s", String.join(",",
                        localAppIds.stream().map(Object::toString).collect(Collectors.toSet()))));
                    List<ApplicationInfoDTO> localNormalApps =
                        localApps.stream().filter(app ->
                            app.getAppType() == AppTypeEnum.NORMAL).collect(Collectors.toList());
                    Long gseInterfaceTimeConsuming = 0L;
                    Long writeToDBTimeConsuming = 0L;
                    for (ApplicationInfoDTO applicationInfoDTO : localNormalApps) {
                        try {
                            Pair<Long, Long> timeConsumingPair = syncAppHostAgentStatus(applicationInfoDTO.getId());
                            gseInterfaceTimeConsuming += timeConsumingPair.getFirst();
                            writeToDBTimeConsuming += timeConsumingPair.getSecond();
                        } catch (Throwable t) {
                            log.error("syncAgentStatus of app fail:appId=" + applicationInfoDTO.getId(), t);
                        }
                    }
                    log.info(Thread.currentThread().getName() + ":Finished:sync agentStatus from GSE," +
                            "gseInterfaceTimeConsuming={}ms,writeToDBTimeConsuming={}ms,rate={}",
                        gseInterfaceTimeConsuming, writeToDBTimeConsuming,
                        gseInterfaceTimeConsuming / (0. + writeToDBTimeConsuming));
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

    private List<ApplicationHostInfoDTO> getHostsByAppInfo(CcClient ccClient, ApplicationInfoDTO applicationInfoDTO) {
        List<CcInstanceDTO> ccInstanceDTOList = new ArrayList<>();
        ccInstanceDTOList.add(new CcInstanceDTO(CcNodeTypeEnum.APP.getType(), applicationInfoDTO.getId()));
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList = ccClient.getHosts(applicationInfoDTO.getId(),
            ccInstanceDTOList);
        // 获取Agent状态
        applicationService.fillAgentStatus(applicationHostInfoDTOList);
        return applicationHostInfoDTOList;
    }

    private int deleteAppFromDb(ApplicationInfoDTO applicationInfoDTO) {
        log.info("deleteAppFromDb:" + applicationInfoDTO.getId());
        //先删业务对应主机
        applicationHostDAO.deleteAppHostInfoByAppId(dslContext, applicationInfoDTO.getId());
        //再删业务本身
        applicationInfoDAO.deleteAppInfoById(dslContext, applicationInfoDTO.getId());
        return 1;
    }

    private int addAppToDb(ApplicationInfoDTO applicationInfoDTO,
                           List<ApplicationHostInfoDTO> applicationHostInfoDTOList) {
        log.info("addAppToDb:" + applicationInfoDTO.getId() + "," + applicationHostInfoDTOList.size() + "hosts");
        //先添加业务本身
        log.info("insertAppInfo:" + JsonUtils.toJson(applicationInfoDTO));
        applicationService.createApp(applicationInfoDTO);
        //再添加业务对应主机
        applicationHostInfoDTOList.forEach(applicationHostInfoDTO -> {
            log.info("insertAppHostInfo:" + JsonUtils.toJson(applicationHostInfoDTO));
            applicationHostDAO.insertAppHostInfo(dslContext, applicationHostInfoDTO);
        });
        return 1;
    }

    private void updateHostsInApp(Long appId, List<ApplicationHostInfoDTO> updateList) {
        StopWatch watch = new StopWatch();
        watch.start("updateAppHostInfo");
        // 更新主机
        long updateCount = 0L;
        List<Long> updateHostIds = new ArrayList<>();
        long errorCount = 0L;
        List<Long> errorHostIds = new ArrayList<>();
        long notChangeCount = 0L;
        boolean batchUpdated = false;
        try {
            // 尝试批量更新
            if (!updateList.isEmpty()) {
                applicationHostDAO.batchUpdateAppHostInfoByHostId(dslContext, updateList);
            }
            batchUpdated = true;
        } catch (Throwable throwable) {
            if (throwable instanceof DataAccessException) {
                String errorMessage = throwable.getMessage();
                if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                    log.info("Fail to batchUpdateAppHostInfoByHostId, try to update one by one");
                } else {
                    log.warn("Fail to batchUpdateAppHostInfoByHostId, try to update one by one.", throwable);
                }
            } else {
                log.warn("Fail to batchUpdateAppHostInfoByHostId, try to update one by one..", throwable);
            }
            // 批量更新失败，尝试逐条更新
            for (ApplicationHostInfoDTO hostInfoDTO : updateList) {
                try {
                    if (!applicationHostDAO.existAppHostInfoByHostId(dslContext, hostInfoDTO)) {
                        applicationHostDAO.updateAppHostInfoByHostId(dslContext, hostInfoDTO.getAppId(), hostInfoDTO);
                        updateCount += 1;
                        updateHostIds.add(hostInfoDTO.getHostId());
                    } else {
                        notChangeCount += 1;
                    }
                } catch (Throwable t) {
                    log.error(String.format("updateHost fail:appId=%d,hostInfo=%s", appId, hostInfoDTO), t);
                    errorCount += 1;
                    errorHostIds.add(hostInfoDTO.getHostId());
                }
            }
        }
        watch.stop();
        if (!batchUpdated) {
            watch.start("log updateAppHostInfo");
            allAppUpdateFailHostIds.addAll(errorHostIds);
            log.info("Update host of appId={},errorCount={},updateCount={},notChangeCount={},errorHostIds={}," +
                    "updateHostIds={}", appId, errorCount, updateCount, notChangeCount,
                errorHostIds
                , updateHostIds);
            watch.stop();
        }
        log.debug("Performance:updateHostsInApp:appId={},{}", appId, watch.prettyPrint());
    }

    private void deleteHostsFromApp(Long appId, List<ApplicationHostInfoDTO> deleteList) {
        StopWatch watch = new StopWatch();
        // 删除主机
        watch.start("deleteAppHostInfo");
        List<Long> deleteFailHostIds = new ArrayList<>();
        boolean batchDeleted = false;
        try {
            // 尝试批量删除
            if (!deleteList.isEmpty()) {
                applicationHostDAO.batchDeleteAppHostInfoById(dslContext, appId,
                    deleteList.stream().map(ApplicationHostInfoDTO::getHostId).collect(Collectors.toList()));
            }
            batchDeleted = true;
        } catch (Throwable throwable) {
            log.warn("Fail to batchDeleteAppHostInfoById, try to delete one by one", throwable);
            // 批量删除失败，尝试逐条删除
            for (ApplicationHostInfoDTO applicationHostInfoDTO : deleteList) {
                try {
                    applicationHostDAO.deleteAppHostInfoById(dslContext, appId, applicationHostInfoDTO.getHostId());
                } catch (Throwable t) {
                    log.error("deleteHost fail:appId={},hostInfo={}", appId,
                        applicationHostInfoDTO, t);
                    deleteFailHostIds.add(applicationHostInfoDTO.getHostId());
                }
            }
        }
        watch.stop();
        if (!batchDeleted) {
            watch.start("log deleteAppHostInfo");
            if (!deleteFailHostIds.isEmpty()) {
                allAppDeleteFailHostIds.addAll(deleteFailHostIds);
                log.warn(String.format("appId=%s,deleteFailHostIds.size=%d,deleteFailHostIds=%s",
                    appId, deleteFailHostIds.size(), String.join(",",
                        deleteFailHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
            }
            watch.stop();
        }
        log.debug("Performance:deleteHostsFromApp:appId={},{}", appId, watch.prettyPrint());
    }

    private boolean insertOrUpdateOneAppHost(Long appId, ApplicationHostInfoDTO infoDTO) {
        try {
            applicationHostDAO.insertAppHostInfo(dslContext, infoDTO);
        } catch (DataAccessException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                log.warn(String.format(
                    "insertHost fail, try to update:Duplicate entry:appId=%d," +
                        "insert hostInfo=%s, old " +
                        "hostInfo=%s", appId, infoDTO,
                    applicationHostDAO.getHostById(infoDTO.getHostId())), e);
                try {
                    // 插入失败了就应当更新，以后来的数据为准
                    applicationHostDAO.updateAppHostInfoByHostId(dslContext, appId, infoDTO);
                } catch (Throwable t) {
                    log.error(String.format("update after insert fail:appId=%d,hostInfo=%s", appId, infoDTO), t);
                    return false;
                }
            } else {
                log.error(String.format("insertHost fail:appId=%d,hostInfo=%s", appId, infoDTO), e);
                return false;
            }
        } catch (Throwable t) {
            log.error(String.format("insertHost fail:appId=%d,hostInfo=%s", appId, infoDTO), t);
            return false;
        }
        return true;
    }

    private void insertHostsToApp(Long appId, List<ApplicationHostInfoDTO> insertList) {
        StopWatch watch = new StopWatch();
        // 插入主机
        watch.start("insertAppHostInfo");
        List<Long> insertFailHostIds = new ArrayList<>();
        boolean batchInserted = false;
        try {
            //尝试批量插入
            if (!insertList.isEmpty()) {
                applicationHostDAO.batchInsertAppHostInfo(dslContext, insertList);
            }
            batchInserted = true;
        } catch (Throwable throwable) {
            if (throwable instanceof DataAccessException) {
                String errorMessage = throwable.getMessage();
                if (errorMessage.contains("Duplicate entry") && errorMessage.contains("PRIMARY")) {
                    log.info("Fail to batchInsertAppHostInfo, try to insert one by one");
                } else {
                    log.warn("Fail to batchInsertAppHostInfo, try to insert one by one.", throwable);
                }
            } else {
                log.warn("Fail to batchInsertAppHostInfo, try to insert one by one..", throwable);
            }
            //批量插入失败，尝试逐条插入
            for (ApplicationHostInfoDTO infoDTO : insertList) {
                if (!insertOrUpdateOneAppHost(appId, infoDTO)) {
                    insertFailHostIds.add(infoDTO.getHostId());
                }
            }
        }
        watch.stop();
        if (!batchInserted) {
            watch.start("log insertAppHostInfo");
            if (!insertFailHostIds.isEmpty()) {
                allAppInsertFailHostIds.addAll(insertFailHostIds);
                log.warn(String.format("appId=%s,insertFailHostIds.size=%d,insertFailHostIds=%s",
                    appId, insertFailHostIds.size(), String.join(",",
                        insertFailHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
            }
            watch.stop();
        }
        log.debug("Performance:insertHostsToApp:appId={},{}", appId, watch.prettyPrint());
    }

    private List<ApplicationHostInfoDTO> computeInsertList(
        Long appId,
        Set<Long> localAppHostIds,
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostInfoDTO> insertList =
            applicationHostInfoDTOList.stream().filter(applicationHostInfoDTO ->
                !localAppHostIds.contains(applicationHostInfoDTO.getHostId())).collect(Collectors.toList());
        watch.start("log insertList");
        log.info(String.format("appId=%s,insertHostIds=%s", appId, String.join(",",
            insertList.stream().map(ApplicationHostInfoDTO::getHostId).map(Object::toString)
                .collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return insertList;
    }

    private List<ApplicationHostInfoDTO> computeUpdateList(
        Long appId,
        Set<Long> localAppHostIds,
        List<ApplicationHostInfoDTO> applicationHostInfoDTOList
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostInfoDTO> updateList =
            applicationHostInfoDTOList.stream().filter(applicationHostInfoDTO ->
                localAppHostIds.contains(applicationHostInfoDTO.getHostId())).collect(Collectors.toList());
        watch.start("log updateList");
        log.info(String.format("appId=%s,updateHostIds=%s", appId, String.join(",",
            updateList.stream().map(ApplicationHostInfoDTO::getHostId)
                .map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return updateList;
    }

    private List<ApplicationHostInfoDTO> computeDeleteList(
        Long appId,
        Set<Long> ccAppHostIds,
        List<ApplicationHostInfoDTO> localAppHosts
    ) {
        StopWatch watch = new StopWatch();
        List<ApplicationHostInfoDTO> deleteList =
            localAppHosts.stream().filter(applicationHostInfoDTO ->
                !ccAppHostIds.contains(applicationHostInfoDTO.getHostId())).collect(Collectors.toList());
        watch.start("log deleteList");
        log.info(String.format("appId=%s,deleteHostIds=%s", appId, String.join(",",
            deleteList.stream().map(ApplicationHostInfoDTO::getHostId).map(Object::toString)
                .collect(Collectors.toSet()))));
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn("Write log too slow, {}", watch.prettyPrint());
        }
        return deleteList;
    }

    private int refreshAppHosts(Long appId,
                                List<ApplicationHostInfoDTO> applicationHostInfoDTOList) {
        StopWatch watch = new StopWatch();
        //找出要删除的/更新的/新增的分别处理
        //对比库中数据与接口数据
        watch.start("listHostInfoByAppId");
        List<ApplicationHostInfoDTO> localAppHosts = applicationHostDAO.listHostInfoByAppId(appId);
        watch.stop();
        watch.start("mapTo ccAppHostIds");
        Set<Long> ccAppHostIds =
            applicationHostInfoDTOList.stream().map(ApplicationHostInfoDTO::getHostId).collect(Collectors.toSet());
        watch.stop();
        watch.start("mapTo localAppHostIds");
        Set<Long> localAppHostIds =
            localAppHosts.stream().map(ApplicationHostInfoDTO::getHostId).collect(Collectors.toSet());
        watch.stop();
        watch.start("log ccAppHostIds");
        log.info(String.format("appId=%s,ccAppHostIds=%s", appId, String.join(",",
            ccAppHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        watch.start("log localAppHostIds");
        log.info(String.format("appId=%s,localAppHostIds=%s", appId, String.join(",",
            localAppHostIds.stream().map(Object::toString).collect(Collectors.toSet()))));
        watch.stop();
        watch.start("compute insertList");
        List<ApplicationHostInfoDTO> insertList = computeInsertList(appId, localAppHostIds, applicationHostInfoDTOList);
        watch.stop();
        watch.start("compute updateList");
        List<ApplicationHostInfoDTO> updateList = computeUpdateList(appId, localAppHostIds, applicationHostInfoDTOList);
        watch.stop();
        watch.start("compute deleteList");
        List<ApplicationHostInfoDTO> deleteList = computeDeleteList(appId, ccAppHostIds, localAppHosts);
        watch.stop();
        watch.start("deleteHostsFromApp");
        // 需要删除的主机
        deleteHostsFromApp(appId, deleteList);
        watch.stop();
        watch.start("insertHostsToApp");
        // 需要新增的主机
        insertHostsToApp(appId, insertList);
        watch.stop();
        watch.start("updateHostsInApp");
        // 需要更新的主机
        updateHostsInApp(appId, updateList);
        watch.stop();
        if (watch.getTotalTimeMillis() > 10000) {
            log.info("Performance:refreshAppHosts:appId={},{}", appId, watch.prettyPrint());
        } else {
            log.debug("Performance:refreshAppHosts:appId={},{}", appId, watch.prettyPrint());
        }
        return 1;
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

    class AppHostsSyncer extends Thread {

        volatile BlockingQueue<Long> queue;

        public AppHostsSyncer(BlockingQueue<Long> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Long appId = queue.take();
                    Pair<Long, Long> timeConsumingPair = syncAppHostsAtOnce(applicationService.getAppInfoById(appId));
                    Long cmdbInterfaceTimeConsuming = timeConsumingPair.getFirst();
                    Long writeToDBTimeConsuming = timeConsumingPair.getSecond();
                    log.info("Sync appHosts of {}:cmdbInterfaceTimeConsuming={}ms,writeToDBTimeConsuming={}ms," +
                            "rate={}", appId, cmdbInterfaceTimeConsuming, writeToDBTimeConsuming,
                        cmdbInterfaceTimeConsuming / (0. + writeToDBTimeConsuming));
                } catch (InterruptedException e) {
                    log.warn("queue.take interrupted", e);
                } catch (Throwable t) {
                    log.warn("Fail to syncAppHostsAtOnce", t);
                }
            }
        }
    }
}
