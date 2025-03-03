package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.manage.dao.UserDAO;
import com.tencent.bk.job.manage.service.UserCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UserCacheServiceImpl implements UserCacheService {

    private final UserDAO userDAO;

    public UserCacheServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public void saveUser(BkUserDTO user) {
        userDAO.saveUser(user);
    }

    @Override
    public int deleteUser(String username) {
        return userDAO.deleteUser(username);
    }

    @Override
    public List<BkUserDTO> listTenantUsers(String tenantId) {
        return userDAO.listTenantUsers(tenantId);
    }

    @Override
    public List<BkUserDTO> listUsersByDisplayNamePrefix(String tenantId, String prefixStr, Long limit) {
        return userDAO.listUsersByDisplayNamePrefix(tenantId, prefixStr, limit);
    }

    @Override
    public List<BkUserDTO> listUsersByDisplayNames(String tenantId, Collection<String> displayNames) {
        return userDAO.listUsersByDisplayNames(tenantId, displayNames);
    }

    @Override
    public List<String> listExistUserName(String tenantId, Collection<String> usernames) {
        return userDAO.listExistUserName(tenantId, usernames);
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void batchPatchUsers(Set<BkUserDTO> deleteUsers, Set<BkUserDTO> addUsers) {
        deleteUsers.forEach(user -> userDAO.deleteUser(user.getUsername()));
        addUsers.forEach(userDAO::saveUser);
    }
}
