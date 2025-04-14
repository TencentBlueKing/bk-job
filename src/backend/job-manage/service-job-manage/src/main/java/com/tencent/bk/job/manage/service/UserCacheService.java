package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.model.dto.BkUserDTO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 用户信息缓存服务
 */
public interface UserCacheService {
    void saveUser(BkUserDTO user);

    int deleteUser(String username);

    List<BkUserDTO> listTenantUsers(String tenantId);

    List<BkUserDTO> listUsersByDisplayNamePrefix(String tenantId, String prefixStr, Long limit);

    List<BkUserDTO> listUsersByDisplayNames(String tenantId, Collection<String> displayNames);

    List<String> listExistUserName(String tenantId, Collection<String> usernames);

    void batchPatchUsers(Set<BkUserDTO> deleteUsers, Set<BkUserDTO> addUsers);

    List<BkUserDTO> listUsersByUsernames(Collection<String> usernames);
}
