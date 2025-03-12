package com.tencent.bk.job.manage;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicAppCache extends AbstractBasicAppCache {

    private final ServiceApplicationResource applicationResource;


    public BasicAppCache(ServiceApplicationResource applicationResource) {
        this.applicationResource = applicationResource;
    }

    @Override
    protected BasicApp loadAppToCache(ResourceScope resourceScope) {
        ServiceApplicationDTO app =
            applicationResource.queryAppByScope(resourceScope.getType().getValue(), resourceScope.getId());
        if (app == null) {
            return null;
        }

        BasicApp basicApp = new BasicApp();
        basicApp.setId(app.getId());
        basicApp.setName(app.getName());
        basicApp.setScope(new ResourceScope(app.getScopeType(), app.getScopeId()));
        basicApp.setTenantId(app.getTenantId());
        return basicApp;
    }
}
