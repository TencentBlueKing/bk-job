package com.tencent.bk.job.common.paas.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.paas.exception.PaasException;
import com.tencent.bk.job.common.paas.model.SimpleUserInfo;
import com.tencent.bk.job.common.paas.model.UserCacheQuery;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final TenantEnvService tenantEnvService;

    public UserLocalCache(IUserApiClient userMgrApiClient, TenantEnvService tenantEnvService) {
        this.userMgrApiClient = userMgrApiClient;
        this.tenantEnvService = tenantEnvService;
    }

    private final LoadingCache<UserCacheQuery, SimpleUserInfo> userCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(10, TimeUnit.MINUTES).
        build(new CacheLoader<UserCacheQuery, SimpleUserInfo>() {
                  @Override
                  public @NonNull SimpleUserInfo load(@NonNull UserCacheQuery query) {
                      String tenantId = query.getTenantId();
                      String username = query.getUsername();
                      SimpleUserInfo user = userMgrApiClient.getUserByUsername(tenantId, username);
                      // 用户在用户管理中不存在，直接缓存中存空对象
                      if (user == null) {
                          log.info(
                              "cannot find user(tenantId={}, username={}) in bk-user, store empty user info in cache",
                              tenantId,
                              username
                          );
                          return new SimpleUserInfo();
                      }
                      return user;
                  }

                  @Override
                  public @NonNull Map<UserCacheQuery, SimpleUserInfo> loadAll(
                      @NonNull Iterable<? extends UserCacheQuery> querys
                  ) {
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

                      Set<String> invalidUsernameSet = new HashSet<>(usernames);
                      invalidUsernameSet.removeAll(existUserMap.keySet());
                      if (!invalidUsernameSet.isEmpty()) {
                          log.warn("UserLocalCache try to loadAll, invalidUsernameSet={}", invalidUsernameSet);
                      }

                      Map<UserCacheQuery, SimpleUserInfo> result = new HashMap<>();
                      for (UserCacheQuery query : querys) {
                          String username = query.getUsername();
                          // 返回的 Map 中，keys 和 values 都不能为null
                          result.put(query, existUserMap.getOrDefault(username, new SimpleUserInfo()));
                      }
                      return result;
                  }
              }
        );

    /**
     * 获取用户信息：先针对比较特殊的用户直接返回，再走缓存获取逻辑
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户信息
     */
    public User getUser(String tenantId, String username) {
        // 单租户环境下的admin账号无需查询用户管理，直接返回即可
        if (!tenantEnvService.isTenantEnabled() && JobConstants.DEFAULT_SYSTEM_USER_ADMIN.equals(username)) {
            return new User(tenantId, username, username);
        }
        return getUserPreferCache(tenantId, username);
    }

    /**
     * 优先从缓存中获取用户信息
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户信息
     */
    private User getUserPreferCache(String tenantId, String username) {
        UserCacheQuery query = new UserCacheQuery(tenantId, username);
        SimpleUserInfo bkUser = userCache.getUnchecked(query);
        // 缓存了空对象，说明用户不存在
        if (!bkUser.isNotEmpty()) {
            log.info("user(tenantId={}, username={}) not found in bk-user, fill by username", tenantId, username);
            return new User(tenantId, username, username);
        }

        return new User(tenantId, bkUser.getBkUsername(), bkUser.getDisplayName());
    }

    public Set<SimpleUserInfo> batchGetUser(String tenantId, Collection<String> usernames) {
        Set<UserCacheQuery> querySet = usernames.stream()
            .map(username -> new UserCacheQuery(tenantId, username))
            .collect(Collectors.toSet());
        try {
            return userCache.getAll(querySet).values()
                .stream()
                .filter(SimpleUserInfo::isNotEmpty)
                .collect(Collectors.toSet());
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

}
