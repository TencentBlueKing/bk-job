package com.tencent.bk.job.common.paas.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.paas.exception.PaasException;
import com.tencent.bk.job.common.paas.model.SimpleUserInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String CACHE_DELIMITER = ":";

    public UserLocalCache(IUserApiClient userMgrApiClient) {
        this.userMgrApiClient = userMgrApiClient;
    }

    private final LoadingCache<String, SimpleUserInfo> userCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).
        build(new CacheLoader<String, SimpleUserInfo>() {
                  @Override
                  public SimpleUserInfo load(String key) {
                      String[] l = key.split(CACHE_DELIMITER);
                      String tenantId = l[0];
                      String username = l[1];
                      return userMgrApiClient.getUserByUsername(tenantId, username);
                  }

                  @Override
                  public Map<String, SimpleUserInfo> loadAll(Iterable<? extends String> keys) {
                      Set<String> keySet = Sets.newHashSet(keys);
                      if (CollectionUtils.isEmpty(keySet)) {
                          return new HashMap<>();
                      }
                      String tenantId = null;
                      for (String key : keys) {
                          tenantId = key.split(CACHE_DELIMITER)[0];
                          break;
                      }
                      Set<String> usernames = keySet.stream()
                          .map(key -> key.split(CACHE_DELIMITER)[1])
                          .collect(Collectors.toSet());
                      log.info("[UserLocalCache] loadAll, tenantId={}, usernames={}", tenantId, usernames);
                      List<SimpleUserInfo> users = userMgrApiClient.listUsersByUsernames(tenantId, usernames);
                      Map<String, SimpleUserInfo> result = new HashMap<>();
                      for (SimpleUserInfo user : users) {
                          result.put(user.getBkUsername(), user);
                      }
                      return result;
                  }
              }
        );


    public User getUser(String tenantId, String username) {
        String key = tenantId + CACHE_DELIMITER + username;
        SimpleUserInfo bkUser = userCache.getUnchecked(key);
        if (bkUser == null) {
            return null;
        }
        return new User(tenantId, bkUser.getBkUsername());
    }

    public Set<SimpleUserInfo> batchGetUser(String tenantId, Collection<String> usernames) {
        Set<String> keys = usernames.stream()
            .map(username -> tenantId + CACHE_DELIMITER + username)
            .collect(Collectors.toSet());
        try {
            return new HashSet<>(userCache.getAll(keys).values());
        } catch (ExecutionException e) {
            log.error("[UserLocalCache]batchGetUser failed, when batch get {} of tenant:{}", usernames, tenantId, e);
            throw new PaasException(
                ErrorType.INTERNAL,
                ErrorCode.BK_USER_CACHE_LOADING_ERROR,
                new Object[]{}
            );
        }
    }

    public String getDisplayName(String tenantId, String username) {
        String key = tenantId + CACHE_DELIMITER + username;
        SimpleUserInfo bkUser = userCache.getUnchecked(key);
        String displayName = username;
        if (bkUser != null && StringUtils.isNotEmpty(bkUser.getDisplayName())) {
            displayName = bkUser.getDisplayName();
        }
        return displayName;
    }
}
