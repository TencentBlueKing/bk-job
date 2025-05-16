package com.tencent.bk.job.common.paas.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.paas.exception.PaasException;
import com.tencent.bk.job.common.paas.model.SimpleUserInfo;
import com.tencent.bk.job.common.paas.model.UserCacheQuery;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 租户信息缓存
 */
public class UserLocalCache {

    private static final Logger log = LoggerFactory.getLogger(UserLocalCache.class);
    private final IUserApiClient userMgrApiClient;

    public UserLocalCache(IUserApiClient userMgrApiClient) {
        this.userMgrApiClient = userMgrApiClient;
    }

    private final LoadingCache<UserCacheQuery, SimpleUserInfo> userCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).
        build(new CacheLoader<UserCacheQuery, SimpleUserInfo>() {
                  @Override
                  public SimpleUserInfo load(UserCacheQuery query) {
                      String tenantId = query.getTenantId();
                      String username = query.getUsername();
                      return userMgrApiClient.getUserByUsername(tenantId, username);
                  }

                  @Override
                  public Map<UserCacheQuery, SimpleUserInfo> loadAll(Iterable<? extends UserCacheQuery> querys) {
                      Set<UserCacheQuery> querySet = Sets.newHashSet(querys);
                      if (CollectionUtils.isEmpty(querySet)) {
                          return new HashMap<>();
                      }
                      String tenantId = null;
                      for (UserCacheQuery query : querys) {
                          tenantId = query.getTenantId();
                          break;
                      }
                      Set<String> usernames = querySet.stream()
                          .map(UserCacheQuery::getUsername)
                          .collect(Collectors.toSet());
                      log.info("[UserLocalCache] loadAll, tenantId={}, usernames={}", tenantId, usernames);
                      List<SimpleUserInfo> userList = userMgrApiClient.listUsersByUsernames(tenantId, usernames);
                      Map<String, SimpleUserInfo> existUserMap = userList.stream()
                          .collect(Collectors.toMap(SimpleUserInfo::getBkUsername, userInfo -> userInfo));
                      Map<UserCacheQuery, SimpleUserInfo> result = new HashMap<>();
                      for (UserCacheQuery query : querys) {
                          String username = query.getUsername();
                          result.put(query, existUserMap.getOrDefault(username, null));
                      }
                      return result;
                  }
              }
        );


    public User getUser(String tenantId, String username) {
        UserCacheQuery query = new UserCacheQuery(tenantId, username);
        SimpleUserInfo bkUser = userCache.getUnchecked(query);
        if (bkUser == null) {
            return null;
        }
        return new User(tenantId, bkUser.getBkUsername(), bkUser.getDisplayName());
    }

    public SimpleUserInfo getSingleUser(String tenantId, String username) {
        UserCacheQuery query = new UserCacheQuery(tenantId, username);
        return userCache.getUnchecked(query);
    }

    public Set<SimpleUserInfo> batchGetUser(String tenantId, Collection<String> usernames) {
        Set<UserCacheQuery> querySet = usernames.stream()
            .map(username -> new UserCacheQuery(tenantId,username))
            .collect(Collectors.toSet());
        try {
            return userCache.getAll(querySet).values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        } catch (ExecutionException | UncheckedExecutionException e) {
            log.error("[UserLocalCache]batchGetUser failed, throws ExecutionException, when batch get {} of tenant:{}",
                usernames, tenantId, e);
            Throwable cause = e.getCause();
            if (cause instanceof PaasException) {
                log.error("[UserLocalCache]batchGetUser from bk-user failed , because of ", cause);
                throw (PaasException) cause;
            } else {
                // 缓存异常，直接请求用户管理API
                log.error("[UserLocalCache]cache error, try to request bk-user directly");
                return new HashSet<>(userMgrApiClient.listUsersByUsernames(tenantId, usernames));
            }
        } catch (Exception e) {
            log.error("[UserLocalCache]batchGetUser failed, throws Exception, when batch get {} of tenant:{}",
                usernames, tenantId, e);
            throw e;
        }
    }

    public String getDisplayName(String tenantId, String username) {
        UserCacheQuery query = new UserCacheQuery(tenantId, username);
        SimpleUserInfo bkUser = userCache.getUnchecked(query);
        String displayName = username;
        if (bkUser != null && StringUtils.isNotEmpty(bkUser.getDisplayName())) {
            displayName = bkUser.getDisplayName();
        }
        return displayName;
    }
}
