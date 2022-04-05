package com.tencent.bk.job.manage.manager.app;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 业务所属的组织架构-本地缓存
 */
@CompatibleImplementation(
    explain = "老的Job业务集的实现，按照cmdb业务的bk_operate_dept_id实现的动态业务集，等业务集全部迁移cmdb之后可删除",
    version = "3.6.x")
@Component
@Slf4j
public class BizOperateDeptLocalCache {
    private final ApplicationDAO applicationDAO;

    @Autowired
    public BizOperateDeptLocalCache(ApplicationDAO applicationDAO) {
        this.applicationDAO = applicationDAO;
    }

    /**
     * 组织架构与业务的缓存。
     * 使用本地缓存实现，考虑到使用场景对数据实时性以及一致性要求不高，本地缓存的查询性能更高。
     */
    private final LoadingCache<Long, List<Long>> deptIdAndBizIdCache =
        CacheBuilder.newBuilder().maximumSize(1_000).expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, List<Long>>() {
                       @Override
                       public List<Long> load(Long deptId) {
                           List<Long> bizIds = applicationDAO.getBizIdsByOptDeptId(deptId);
                           return CollectionUtils.isEmpty(bizIds) ? Collections.emptyList() : bizIds;
                       }
                   }
            );

    /**
     * 获取同组织架构下的业务ID
     *
     * @param deptId 组织架构ID
     * @return cmdb业务ID
     */
    public List<Long> getBizIdsWithDeptId(Long deptId) {
        try {
            return deptIdAndBizIdCache.get(deptId);
        } catch (ExecutionException e) {
            log.error("Get biz id by deptId error", e);
            return Collections.emptyList();
        }
    }
}
