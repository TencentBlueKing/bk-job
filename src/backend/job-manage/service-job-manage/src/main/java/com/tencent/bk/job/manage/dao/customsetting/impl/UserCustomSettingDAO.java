/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.manage.dao.customsetting.impl;

import com.tencent.bk.job.common.util.ListUtil;
import com.tencent.bk.job.manage.model.dto.customsetting.UserCustomSettingDTO;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Batch;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.generated.tables.UserCustomSetting;
import org.jooq.generated.tables.records.UserCustomSettingRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserCustomSettingDAO {

    private final UserCustomSetting defaultTable = UserCustomSetting.USER_CUSTOM_SETTING;
    private final DSLContext dslContext;

    @Autowired
    public UserCustomSettingDAO(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Transactional
    public int batchSave(List<UserCustomSettingDTO> customSettingList) {
        List<String> keyList = extractKeyList(customSettingList);
        Set<String> existKeys = new HashSet<>(listExistKeys(keyList));
        Pair<List<UserCustomSettingDTO>, List<UserCustomSettingDTO>> pair =
            ListUtil.separate(customSettingList, it -> existKeys.contains(it.getKey()));
        List<UserCustomSettingDTO> updateList = pair.getLeft();
        List<UserCustomSettingDTO> insertList = pair.getRight();
        int updateAffectedNum = batchUpdate(updateList);
        int insertAffectedNum = batchInsert(insertList);
        return updateAffectedNum + insertAffectedNum;
    }

    public int batchDelete(Collection<String> keyList) {
        if (CollectionUtils.isEmpty(keyList)) {
            return 0;
        }
        return dslContext.deleteFrom(defaultTable)
            .where(defaultTable.KEY.in(keyList))
            .execute();
    }

    public List<UserCustomSettingDTO> listAll() {
        return dslContext.select(
            defaultTable.USERNAME, defaultTable.APP_ID, defaultTable.MODULE, defaultTable.VALUE,
            defaultTable.LAST_MODIFY_USER, defaultTable.LAST_MODIFY_TIME)
            .from(defaultTable)
            .fetch()
            .map(this::convert);
    }

    public List<UserCustomSettingDTO> batchGet(Collection<String> keyList) {
        if (CollectionUtils.isEmpty(keyList)) {
            return Collections.emptyList();
        }
        return dslContext.select(
            defaultTable.USERNAME, defaultTable.APP_ID, defaultTable.MODULE, defaultTable.VALUE,
            defaultTable.LAST_MODIFY_USER, defaultTable.LAST_MODIFY_TIME)
            .from(defaultTable)
            .where(defaultTable.KEY.in(keyList))
            .fetch()
            .map(this::convert);
    }

    private List<String> extractKeyList(List<UserCustomSettingDTO> customSettingList) {
        if (CollectionUtils.isEmpty(customSettingList)) {
            return Collections.emptyList();
        }
        return customSettingList.parallelStream().map(UserCustomSettingDTO::getKey).collect(Collectors.toList());
    }

    private List<String> listExistKeys(List<String> keyList) {
        if (CollectionUtils.isEmpty(keyList)) {
            return Collections.emptyList();
        }
        return Arrays.asList(dslContext.select(defaultTable.KEY).from(defaultTable)
            .where(defaultTable.KEY.in(keyList))
            .fetch()
            .intoArray(defaultTable.KEY));
    }

    private int batchInsert(List<UserCustomSettingDTO> customSettingList) {
        if (CollectionUtils.isEmpty(customSettingList)) {
            return 0;
        }
        int affectedNum = 0;
        val insertQuery = dslContext.insertInto(defaultTable,
            defaultTable.USERNAME,
            defaultTable.APP_ID,
            defaultTable.MODULE,
            defaultTable.KEY,
            defaultTable.VALUE,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).values((String) null, null, null, null, null, null, null);
        BatchBindStep batchQuery = dslContext.batch(insertQuery);
        for (UserCustomSettingDTO userCustomSettingDTO : customSettingList) {
            batchQuery = batchQuery.bind(
                userCustomSettingDTO.getUsername(),
                userCustomSettingDTO.getAppId(),
                userCustomSettingDTO.getModule(),
                userCustomSettingDTO.getKey(),
                userCustomSettingDTO.getValue(),
                userCustomSettingDTO.getLastModifyUser(),
                System.currentTimeMillis()
            );
        }
        int[] results = batchQuery.execute();
        for (int result : results) {
            affectedNum += result;
        }
        return affectedNum;
    }

    private int batchUpdate(List<UserCustomSettingDTO> customSettingList) {
        if (CollectionUtils.isEmpty(customSettingList)) {
            return 0;
        }
        int affectedNum = 0;
        List<UserCustomSettingRecord> records = new ArrayList<>();
        for (UserCustomSettingDTO userCustomSettingDTO : customSettingList) {
            UserCustomSettingRecord record = new UserCustomSettingRecord();
            record.set(defaultTable.USERNAME, userCustomSettingDTO.getUsername());
            record.set(defaultTable.APP_ID, userCustomSettingDTO.getAppId());
            record.set(defaultTable.MODULE, userCustomSettingDTO.getModule());
            record.set(defaultTable.KEY, userCustomSettingDTO.getKey());
            record.set(defaultTable.VALUE, userCustomSettingDTO.getValue());
            record.set(defaultTable.LAST_MODIFY_USER, userCustomSettingDTO.getLastModifyUser());
            record.set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis());
            records.add(record);
        }
        Batch batchQuery = dslContext.batchUpdate(records);
        int[] results = batchQuery.execute();
        for (int result : results) {
            affectedNum += result;
        }
        return affectedNum;
    }

    private UserCustomSettingDTO convert(Record record) {
        UserCustomSettingDTO userCustomSettingDTO = new UserCustomSettingDTO();
        userCustomSettingDTO.setUsername(record.get(defaultTable.USERNAME));
        userCustomSettingDTO.setAppId(record.get(defaultTable.APP_ID));
        userCustomSettingDTO.setModule(record.get(defaultTable.MODULE));
        userCustomSettingDTO.setValue(record.get(defaultTable.VALUE));
        userCustomSettingDTO.setLastModifyUser(record.get(defaultTable.LAST_MODIFY_USER));
        userCustomSettingDTO.setLastModifyTime(record.get(defaultTable.LAST_MODIFY_TIME));
        return userCustomSettingDTO;
    }
}
