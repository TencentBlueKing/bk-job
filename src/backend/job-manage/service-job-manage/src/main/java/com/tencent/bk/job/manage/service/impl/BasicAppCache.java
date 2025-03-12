package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.AbstractBasicAppCache;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BasicAppCache extends AbstractBasicAppCache {

    private final ApplicationService applicationService;


    public BasicAppCache(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    protected BasicApp loadAppToCache(ResourceScope resourceScope) {
        ApplicationDTO app = applicationService.getAppByScope(resourceScope);
        if (app == null) {
            return null;
        }

        BasicApp basicApp = new BasicApp();
        basicApp.setId(app.getId());
        basicApp.setName(app.getName());
        basicApp.setScope(app.getScope());
        basicApp.setTenantId(app.getTenantId());
        return basicApp;
    }
}
