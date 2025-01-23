package com.tencent.bk.job.common.service;

import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.dto.ResourceScope;

public interface AppCacheService extends AppScopeMappingService {

    BasicApp getApp(ResourceScope resourceScope);

    BasicApp getApp(Long appId);


}
