package com.tencent.bk.job.manage.service.impl.sync;

import brave.Tracing;
import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.BizSetEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.BizSetCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.impl.BizSetService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 业务集事件监听
 */
@Slf4j
@Component
public class BizSetEventWatcher extends AbstractCmdbResourceEventWatcher<BizSetEventDetail> {
    private final ApplicationService applicationService;
    private final BizSetService bizSetService;
    private final BizSetCmdbClient bizSetCmdbClient;

    @Autowired
    public BizSetEventWatcher(RedisTemplate<String, String> redisTemplate,
                              Tracing tracing,
                              ApplicationService applicationService,
                              BizSetService bizSetService,
                              BizSetCmdbClient bizSetCmdbClient) {
        super("bizSet", redisTemplate, tracing);
        this.applicationService = applicationService;
        this.bizSetService = bizSetService;
        this.bizSetCmdbClient = bizSetCmdbClient;
    }

    @Override
    protected ResourceWatchResult<BizSetEventDetail> fetchEventsByCursor(String startCursor) {
        return bizSetCmdbClient.getBizSetEvents(null, startCursor);
    }

    @Override
    protected ResourceWatchResult<BizSetEventDetail> fetchEventsByStartTime(Long startTime) {
        return bizSetCmdbClient.getBizSetEvents(startTime, null);
    }

    @Override
    protected void handleEvent(ResourceEvent<BizSetEventDetail> event) {
        log.info("Handle BizSetEvent: {}", event);
        ApplicationDTO latestApp = event.getDetail().toApplicationDTO();
        String eventType = event.getEventType();
        ApplicationDTO cachedApp =
            applicationService.getAppByScopeIncludingDeleted(latestApp.getScope());
        if (cachedApp != null && !FeatureToggle.isCmdbBizSetEnabledForApp(cachedApp.getId())) {
            log.info("Ignore cmdb biz set event, app[{}] cmdb biz set feature toggle is disabled", cachedApp.getId());
            return;
        }

        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_CREATE:
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                try {
                    if (cachedApp != null) {
                        updateBizSetProps(cachedApp, latestApp);
                        if (!cachedApp.isDeleted()) {
                            log.info("Update app for bizSet, app: {}", cachedApp);
                            applicationService.updateApp(cachedApp);
                        } else {
                            log.info("Restore deleted app for bizSet: {}", latestApp);
                            applicationService.updateApp(latestApp);
                            applicationService.restoreDeletedApp(latestApp.getId());
                        }
                    } else {
                        try {
                            log.info("Create app for bizSet, app: {}", latestApp);
                            applicationService.createApp(latestApp);
                        } catch (DataAccessException e) {
                            // 若已存在则忽略
                            log.error("Insert app fail", e);
                        }
                    }
                } catch (Throwable t) {
                    log.error("Handle bizSet event fail", t);
                }
                break;
            case ResourceWatchReq.EVENT_TYPE_DELETE:
                if (cachedApp != null) {
                    log.info("Delete app for bizSet, app: {}", cachedApp);
                    applicationService.deleteApp(cachedApp.getId());
                }
                break;
            default:
                log.info("No need to handle event: {}", event);
                break;
        }
    }

    private void updateBizSetProps(ApplicationDTO originApp, ApplicationDTO updateApp) {
        originApp.setName(updateApp.getName());
        originApp.setBkSupplierAccount(updateApp.getBkSupplierAccount());
        originApp.setLanguage(updateApp.getLanguage());
        originApp.setMaintainers(updateApp.getMaintainers());
        originApp.setTimeZone(updateApp.getTimeZone());
    }

    @Override
    protected boolean isWatchingEnabled() {
        boolean isBizSetMigratedToCMDB = bizSetService.isBizSetMigratedToCMDB();
        boolean isCmdbBizSetEnabled = FeatureToggle.isCmdbBizSetEnabled();
        boolean isWatchingEnabled = isBizSetMigratedToCMDB && isCmdbBizSetEnabled;
        if (!isWatchingEnabled) {
            log.info("Watching biz set disabled, isBizSetMigratedToCMDB: {}, isCmdbBizSetEnabled: {}",
                isBizSetMigratedToCMDB, isCmdbBizSetEnabled);
        }
        return isWatchingEnabled;
    }
}
