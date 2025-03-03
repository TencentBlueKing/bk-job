package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.manage.dao.UserDAO;
import com.tencent.bk.job.manage.model.tables.User;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class UserDAOImpl implements UserDAO {

    private static final User T_USER = User.USER;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        T_USER.USERNAME,
        T_USER.TENANT_ID,
        T_USER.DISPLAY_NAME,
        T_USER.LAST_MODIFY_TIME
    };

    private final DSLContext dslContext;

    @Autowired
    public UserDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void saveUser(BkUserDTO user) {
        dslContext
            .insertInto(
                T_USER,
                T_USER.USERNAME,
                T_USER.DISPLAY_NAME,
                T_USER.TENANT_ID,
                T_USER.LAST_MODIFY_TIME
            ).values(
                user.getUsername(),
                user.getDisplayName(),
                user.getTenantId(),
                ULong.valueOf(System.currentTimeMillis())
            ).execute();
    }

    @Override
    public int deleteUser(String username) {
        return dslContext
            .deleteFrom(T_USER)
            .where(T_USER.USERNAME.eq(username))
            .execute();
    }

    @Override
    public List<BkUserDTO> listTenantUsers(String tenantId) {
        Result<Record> result = dslContext
            .select(ALL_FIELDS)
            .from(T_USER)
            .where(T_USER.TENANT_ID.eq(tenantId))
            .fetch();
        if (result.isEmpty()) {
            return null;
        }
        return result.stream().map(this::extract).collect(Collectors.toList());
    }

    private BkUserDTO extract(Record record) {
        BkUserDTO user = new BkUserDTO();
        user.setUsername(record.get(T_USER.USERNAME));
        user.setDisplayName(record.get(T_USER.DISPLAY_NAME));
        user.setTenantId(record.get(T_USER.TENANT_ID));
        return user;
    }

    @Override
    public List<BkUserDTO> listUsersByDisplayNamePrefix(String tenantId, String prefixStr, Long limit) {
        Result<Record> result = dslContext
            .select(ALL_FIELDS)
            .from(T_USER)
            .where(T_USER.TENANT_ID.eq(tenantId))
            .and(T_USER.DISPLAY_NAME.startsWith(prefixStr))
            .fetch();
        if (result.isEmpty()) {
            return new ArrayList<>();
        }
        return result.stream().map(this::extract).collect(Collectors.toList());
    }

    @Override
    public List<BkUserDTO> listUsersByDisplayNames(String tenantId, Collection<String> displayNames) {
        Result<Record> result = dslContext
            .select(ALL_FIELDS)
            .from(T_USER)
            .where(T_USER.TENANT_ID.eq(tenantId))
            .and(T_USER.DISPLAY_NAME.in(displayNames))
            .fetch();
        if (result.isEmpty()) {
            return new ArrayList<>();
        }
        return result.stream().map(this::extract).collect(Collectors.toList());
    }

    @Override
    public List<String> listExistUserName(String tenantId, Collection<String> usernames) {
        Result<Record> result = dslContext
            .select(ALL_FIELDS)
            .from(T_USER)
            .where(T_USER.TENANT_ID.eq(tenantId))
            .and(T_USER.USERNAME.in(usernames))
            .fetch();
        if (result.isEmpty()) {
            return null;
        }
        return result.stream().map(record -> record.get(T_USER.USERNAME)).collect(Collectors.toList());
    }
}
