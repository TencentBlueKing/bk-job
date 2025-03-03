package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.model.dto.BkUserDTO;

import java.util.Collection;
import java.util.List;

public interface UserDAO {

    void saveUser(BkUserDTO user);

    int deleteUser(String username);

    List<BkUserDTO> listTenantUsers(String tenantId);

    List<BkUserDTO> listUsersByDisplayNamePrefix(String tenantId, String prefixStr, Long limit);

    List<BkUserDTO> listUsersByDisplayNames(String tenantId, Collection<String> usernames);

    List<String> listExistUserName(String tenantId, Collection<String> usernames);
}
