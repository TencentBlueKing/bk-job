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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.manager.app.ApplicationCache;
import com.tencent.bk.job.manage.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("FieldCanBeLocal")
@Slf4j
@Service
public class SyncServiceImpl implements SyncService {

    private static final String REDIS_KEY_SYNC_APP_JOB_LOCK = "sync-app-job-lock";
    private static final String REDIS_KEY_SYNC_HOST_JOB_LOCK = "sync-host-job-lock";
    private static final String REDIS_KEY_SYNC_AGENT_STATUS_JOB_LOCK = "sync-agent-status-job-lock";
    private static final String REDIS_KEY_LAST_FINISH_TIME_SYNC_APP = "last-finish-time-sync-app";
    private static final String REDIS_KEY_LAST_FINISH_TIME_SYNC_HOST = "last-finish-time-sync-host";
    private static final String REDIS_KEY_LAST_FINISH_TIME_SYNC_AGENT_STATUS = "last-finish-time-sync-agent-status";
    private static final String machineIp = IpUtils.getFirstMachineIP();

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

    private final ApplicationDAO applicationDAO;
    private final ThreadPoolExecutor syncAppExecutor;
    private final ThreadPoolExecutor syncHostExecutor;
    private final JobManageConfig jobManageConfig;
    private final RedisTemplate<String, String> redisTemplate;
    private final String REDIS_KEY_SYNC_APP_JOB_RUNNING_MACHINE = "sync-app-job-running-machine";
    private final String REDIS_KEY_SYNC_HOST_JOB_RUNNING_MACHINE = "sync-host-job-running-machine";
    private final String REDIS_KEY_SYNC_AGENT_STATUS_JOB_RUNNING_MACHINE = "sync-agent-status-job-running-machine";
    private volatile boolean enableSyncApp;
    private volatile boolean enableSyncHost;
    private volatile boolean enableSyncAgentStatus;
    private final BizEventWatcher bizEventWatcher;
    private final HostEventWatcher hostEventWatcher;
    private final HostRelationEventWatcher hostRelationEventWatcher;
    private final ApplicationCache applicationCache;
    private final BizSyncService bizSyncService;
    private final BizSetSyncService bizSetSyncService;
    private final HostSyncService hostSyncService;
    private final AgentStatusSyncService agentStatusSyncService;
    private final BizSetEventWatcher bizSetEventWatcher;
    private final BizSetRelationEventWatcher bizSetRelationEventWatcher;

    @Autowired
    public SyncServiceImpl(BizSyncService bizSyncService,
                           BizSetSyncService bizSetSyncService,
                           HostSyncService hostSyncService,
                           AgentStatusSyncService agentStatusSyncService,
                           ApplicationDAO applicationDAO,
                           JobManageConfig jobManageConfig,
                           RedisTemplate<String, String> redisTemplate,
                           ApplicationCache applicationCache,
                           BizEventWatcher bizEventWatcher,
                           BizSetEventWatcher bizSetEventWatcher,
                           BizSetRelationEventWatcher bizSetRelationEventWatcher,
                           HostEventWatcher hostEventWatcher,
                           HostRelationEventWatcher hostRelationEventWatcher,
                           @Qualifier("syncAppExecutor") ThreadPoolExecutor syncAppExecutor,
                           @Qualifier("syncHostExecutor") ThreadPoolExecutor syncHostExecutor) {
        this.applicationDAO = applicationDAO;
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
        this.bizEventWatcher = bizEventWatcher;
        this.bizSetEventWatcher = bizSetEventWatcher;
        this.bizSetRelationEventWatcher = bizSetRelationEventWatcher;
        this.hostEventWatcher = hostEventWatcher;
        this.hostRelationEventWatcher = hostRelationEventWatcher;
        // 同步业务的线程池配置
        this.syncAppExecutor = syncAppExecutor;
        // 同步主机的线程池配置
        this.syncHostExecutor = syncHostExecutor;
    }

    @Override
    public void init() {
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
        bizEventWatcher.start();
    }

    /**
     * 监听主机相关的事件
     */
    private void watchHostEvent() {
        // 开一个常驻线程监听主机资源变动事件
        hostEventWatcher.start();
        // 开一个常驻线程监听主机关系资源变动事件
        hostRelationEventWatcher.start();
    }

    /**
     * 监听业务集相关的事件
     */
    private void watchBizSetEvent() {
        // 开一个常驻线程监听业务集变动事件
        bizSetEventWatcher.start();
        bizSetRelationEventWatcher.start();
    }

    @Override
    public ThreadPoolExecutor getSyncHostExecutor() {
        return syncHostExecutor;
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
                    bizSetSyncService.syncBizSetFromCMDB();
                    log.info(Thread.currentThread().getName() + ":Finished:sync app from cmdb");
                    // 将最后同步时间写入Redis
                    redisTemplate.opsForValue().set(REDIS_KEY_LAST_FINISH_TIME_SYNC_APP,
                        "" + System.currentTimeMillis());
                } catch (Throwable t) {
                    log.error("FATAL: syncApp thread fail", t);
                } finally {
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

    private Future<Triple<Set<BasicHostDTO>, Long, Long>> arrangeSyncBizHostsTask(ApplicationDTO bizApp) {
        return syncHostExecutor.submit(() ->
            hostSyncService.syncBizHostsAtOnce(bizApp)
        );
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
                        localApps.stream().filter(ApplicationDTO::isBiz).map(ApplicationDTO::getId)
                            .collect(Collectors.toSet());
                    log.info(String.format("localAppIds:%s", String.join(",",
                        localAppIds.stream().map(Object::toString).collect(Collectors.toSet()))));
                    List<ApplicationDTO> localBizApps =
                        localApps.stream().filter(ApplicationDTO::isBiz).collect(Collectors.toList());
                    long cmdbInterfaceTimeConsuming = 0L;
                    long writeToDBTimeConsuming = 0L;
                    List<Pair<ApplicationDTO, Future<Triple<Set<BasicHostDTO>, Long, Long>>>> bizAppFutureList =
                        new ArrayList<>();
                    Set<BasicHostDTO> allBizCmdbBasicHosts = new HashSet<>();
                    int failedBizNum = 0;
                    long cmdbHostsFetchTimeMills = System.currentTimeMillis();
                    for (ApplicationDTO bizApp : localBizApps) {
                        Future<Triple<Set<BasicHostDTO>, Long, Long>> future = arrangeSyncBizHostsTask(bizApp);
                        bizAppFutureList.add(Pair.of(bizApp, future));
                    }
                    for (Pair<ApplicationDTO, Future<Triple<Set<BasicHostDTO>, Long, Long>>> bizAppFuture :
                        bizAppFutureList) {
                        ApplicationDTO bizApp = bizAppFuture.getFirst();
                        Future<Triple<Set<BasicHostDTO>, Long, Long>> future = bizAppFuture.getSecond();
                        try {
                            Triple<Set<BasicHostDTO>, Long, Long> timeConsumingPair = future.get(30, TimeUnit.MINUTES);
                            Set<BasicHostDTO> cmdbBasicHosts = timeConsumingPair.getLeft();
                            if (!CollectionUtils.isEmpty(cmdbBasicHosts)) {
                                allBizCmdbBasicHosts.addAll(cmdbBasicHosts);
                            }
                            cmdbInterfaceTimeConsuming += timeConsumingPair.getMiddle();
                            writeToDBTimeConsuming += timeConsumingPair.getRight();
                        } catch (Throwable t) {
                            log.error("syncHost of biz fail:bizId=" + bizApp.getBizIdIfBizApp(), t);
                            failedBizNum += 1;
                        }
                    }
                    if (failedBizNum == 0) {
                        // 删除CMDB中不存在的主机
                        hostSyncService.clearHostNotInCmdb(allBizCmdbBasicHosts, cmdbHostsFetchTimeMills);
                        log.info(
                            Thread.currentThread().getName() +
                                ":Finished:sync host from cc, bizNum={}, failedBizNum={}, " +
                                "cmdbInterfaceTimeConsuming={}ms,writeToDBTimeConsuming={}ms,rate={}",
                            localBizApps.size(),
                            failedBizNum,
                            cmdbInterfaceTimeConsuming,
                            writeToDBTimeConsuming,
                            cmdbInterfaceTimeConsuming / (0. + writeToDBTimeConsuming)
                        );
                    } else {
                        log.warn(
                            Thread.currentThread().getName() +
                                ":Finished:sync host from cc, bizNum={}, failedBizNum={}, " +
                                "cmdbInterfaceTimeConsuming={}ms,writeToDBTimeConsuming={}ms,rate={}",
                            localBizApps.size(),
                            failedBizNum,
                            cmdbInterfaceTimeConsuming,
                            writeToDBTimeConsuming,
                            cmdbInterfaceTimeConsuming / (0. + writeToDBTimeConsuming)
                        );
                    }
                    // 将最后同步时间写入Redis
                    redisTemplate.opsForValue().set(REDIS_KEY_LAST_FINISH_TIME_SYNC_HOST,
                        "" + System.currentTimeMillis());
                } catch (Throwable t) {
                    log.error("syncHost thread fail", t);
                } finally {
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
    public Boolean enableBizWatch() {
        log.info("appWatch enabled by op");
        bizEventWatcher.setWatchFlag(true);
        return true;
    }

    @Override
    public Boolean disableBizWatch() {
        log.info("appWatch disabled by op");
        bizEventWatcher.setWatchFlag(false);
        return true;
    }

    @Override
    public Boolean enableHostWatch() {
        log.info("hostWatch enabled by op");
        hostEventWatcher.setWatchFlag(true);
        hostRelationEventWatcher.setWatchFlag(true);
        return true;
    }

    @Override
    public Boolean disableHostWatch() {
        log.info("hostWatch disabled by op");
        hostEventWatcher.setWatchFlag(false);
        hostRelationEventWatcher.setWatchFlag(false);
        return true;
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
        Triple<Set<BasicHostDTO>, Long, Long> triple = hostSyncService.syncBizHostsAtOnce(applicationDTO);
        Set<BasicHostDTO> cmdbBasicHosts = triple.getLeft();
        Long cmdbInterfaceTimeConsuming = triple.getMiddle();
        Long writeToDBTimeConsuming = triple.getRight();
        log.info(
            "syncBizHosts:cmdbInterfaceTimeConsuming={},writeToDBTimeConsuming={}, {} hosts, cmdbHostIds={}",
            cmdbInterfaceTimeConsuming,
            writeToDBTimeConsuming,
            cmdbBasicHosts.size(),
            cmdbBasicHosts.stream().map(BasicHostDTO::getHostId).collect(Collectors.toList())
        );
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
