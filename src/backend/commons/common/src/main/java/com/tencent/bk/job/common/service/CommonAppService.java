package com.tencent.bk.job.common.service;

import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.dto.ResourceScope;

/**
 * 通用业务服务，抽象出最基础的业务信息查询接口，会在common-*模块中被引用
 */
public interface CommonAppService extends AppScopeMappingService {

    BasicApp getApp(ResourceScope resourceScope);

    BasicApp getApp(Long appId);


}
