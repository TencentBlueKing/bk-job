package com.tencent.bk.job.common.paas.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.paas.exception.PaasException;
import com.tencent.bk.job.common.paas.exception.UserNotFoundException;
import com.tencent.bk.job.common.paas.model.SimpleUserInfo;
import com.tencent.bk.job.common.paas.model.UserCacheQuery;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    public UserLocalCache(IUserApiClient userMgrApiClient) {
        this.userMgrApiClient = userMgrApiClient;
    }

    private final LoadingCache<UserCacheQuery, SimpleUserInfo> userCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).
        build(new CacheLoader<UserCacheQuery, SimpleUserInfo>() {
                  @Override
                  public @NonNull SimpleUserInfo load(@NonNull UserCacheQuery query) {
                      String tenantId = query.getTenantId();
                      String username = query.getUsername();
                      SimpleUserInfo user = userMgrApiClient.getUserByUsername(tenantId, username);
                      // 用户在用户管理中不存在
                      if (user == null) {
                          log.info("cannot find user(tenantId={}, username={}) in bk-user by ", tenantId, username);
                          throw new UserNotFoundException(
                              "user(tenantId=" + tenantId + ", username=" + username + ") not found in bk-user");
                      }
                      return userMgrApiClient.getUserByUsername(tenantId, username);
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


    public User getUser(String tenantId, String username) {
        UserCacheQuery query = new UserCacheQuery(tenantId, username);
        SimpleUserInfo bkUser;
        User result = new User();
        try {
            bkUser = userCache.getUnchecked(query);
            result.setTenantId(tenantId);
            result.setUsername(bkUser.getBkUsername());
            result.setDisplayName(bkUser.getDisplayName());
        } catch (UncheckedExecutionException e) {
            if (e.getCause() instanceof UserNotFoundException) {
                // 用户不存在，使用username作为loginName兜底
                log.info(
                    "user(tenantId={}, username={}) not found in bk-user, fill loginName by username({})",
                    tenantId,
                    username,
                    username);
                result.setTenantId(tenantId);
                result.setUsername(username);
                result.setDisplayName(username);
                return result;
            }
            throw e;
        }
        return result;
    }

    public SimpleUserInfo getSingleUser(String tenantId, String username) {
        UserCacheQuery query = new UserCacheQuery(tenantId, username);
        try {
            return userCache.getUnchecked(query);
        } catch (UncheckedExecutionException e) {
            if (e.getCause() instanceof UserNotFoundException) {
                // 用户不存在，使用username作为loginName兜底
                log.info(
                    "user(tenantId={}, username={}) not found in bk-user, fill loginName by username({})",
                    tenantId,
                    username,
                    username
                );
                return new SimpleUserInfo(username, username, username);
            }
            throw e;
        }
    }

    public Set<SimpleUserInfo> batchGetUser(String tenantId, Collection<String> usernames) {
        Set<UserCacheQuery> querySet = usernames.stream()
            .map(username -> new UserCacheQuery(tenantId,username))
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

    public String getDisplayName(String tenantId, String username) {
        UserCacheQuery query = new UserCacheQuery(tenantId, username);
        SimpleUserInfo bkUser;
        try {
             bkUser = userCache.getUnchecked(query);
        } catch (UncheckedExecutionException e) {
            if (e.getCause() instanceof UserNotFoundException) {
                log.info(
                    "user(tenantId={}, username={}) not found in bk-user, fill displayName by username({})",
                    tenantId,
                    username,
                    username);
                return username;
            }
            throw e;
        }
        return StringUtils.isNotBlank(bkUser.getDisplayName()) ? bkUser.getDisplayName() : username;
    }
}
