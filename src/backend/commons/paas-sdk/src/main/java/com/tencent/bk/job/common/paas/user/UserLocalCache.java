package com.tencent.bk.job.common.paas.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.BkUserDTO;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 租户信息缓存
 */
public class UserLocalCache {

    private final UserMgrApiClient userMgrApiClient;

    public UserLocalCache(UserMgrApiClient userMgrApiClient) {
        this.userMgrApiClient = userMgrApiClient;
    }

    private final LoadingCache<String, BkUserDTO> userCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).
        build(new CacheLoader<String, BkUserDTO>() {
                  @Override
                  public BkUserDTO load(String username) {
                      return userMgrApiClient.getUserByUsername(username);
                  }

                  @Override
                  public Map<String, BkUserDTO> loadAll(Iterable<? extends String> usernames) {
                      Set<String> keys = Sets.newHashSet(usernames);
                      return userMgrApiClient.listUsersByUsernames(keys);
                  }
              }
        );


    public BkUserDTO getBkUser(String username) {
        return userCache.getUnchecked(username);
    }

    public User getUser(String username) {
        BkUserDTO bkUser = userCache.getUnchecked(username);
        if (bkUser == null) {
            return null;
        }
        return new User(bkUser.getTenantId(), bkUser.getUsername());
    }
}
