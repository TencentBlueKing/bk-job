package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.cc.model.req.ResourceWatchReq;
import com.tencent.bk.job.common.cc.model.result.BizSetRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.cc.sdk.BizSetCmdbClient;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationAttrsDO;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.impl.BizSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务集事件监听
 */
@Slf4j
@Component
public class BizSetRelationEventWatcher extends AbstractCmdbResourceEventWatcher<BizSetRelationEventDetail> {
    private final ApplicationService applicationService;
    private final BizSetService bizSetService;
    private final BizSetCmdbClient bizSetCmdbClient;

    @Autowired
    public BizSetRelationEventWatcher(RedisTemplate<String, String> redisTemplate,
                                      Tracer tracer,
                                      ApplicationService applicationService,
                                      BizSetService bizSetService,
                                      BizSetCmdbClient bizSetCmdbClient) {
        super("bizSetRelation", redisTemplate, tracer);
        this.applicationService = applicationService;
        this.bizSetService = bizSetService;
        this.bizSetCmdbClient = bizSetCmdbClient;
    }

    @Override
    protected ResourceWatchResult<BizSetRelationEventDetail> fetchEventsByCursor(String startCursor) {
        return bizSetCmdbClient.getBizSetRelationEvents(null, startCursor);
    }

    @Override
    protected ResourceWatchResult<BizSetRelationEventDetail> fetchEventsByStartTime(Long startTime) {
        return bizSetCmdbClient.getBizSetRelationEvents(startTime, null);
    }

    @Override
    protected void handleEvent(ResourceEvent<BizSetRelationEventDetail> event) {
        log.info("Handle BizSetRelationEvent: {}", event);

        String eventType = event.getEventType();
        switch (eventType) {
            case ResourceWatchReq.EVENT_TYPE_UPDATE:
                try {
                    Long bizSetId = event.getDetail().getBizSetId();
                    List<Long> latestSubBizIds = event.getDetail().getBizIds();
                    ApplicationDTO cacheApplication =
                        applicationService.getAppByScopeIncludingDeleted(
                            new ResourceScope(ResourceScopeTypeEnum.BIZ_SET.getValue(), String.valueOf(bizSetId))
                        );
                    if (cacheApplication == null || cacheApplication.isDeleted()) {
                        return;
                    }
                    cacheApplication.setSubBizIds(latestSubBizIds);
                    ApplicationAttrsDO attrs = cacheApplication.getAttrs();
                    if (attrs != null) {
                        attrs.setSubBizIds(latestSubBizIds);
                    }
                    applicationService.updateApp(cacheApplication);
                } catch (Throwable t) {
                    log.error("Handle biz_set_relation event fail", t);
                }
                break;
            default:
                log.info("No need to handle event: {}", event);
                break;
        }
    }

    @Override
    protected boolean isWatchingEnabled() {
        boolean isBizSetMigratedToCMDB = bizSetService.isBizSetMigratedToCMDB();
        if (!isBizSetMigratedToCMDB) {
            log.info("Watching biz set disabled, isBizSetMigratedToCMDB: {}",
                isBizSetMigratedToCMDB);
        }
        return isBizSetMigratedToCMDB;
    }
}
