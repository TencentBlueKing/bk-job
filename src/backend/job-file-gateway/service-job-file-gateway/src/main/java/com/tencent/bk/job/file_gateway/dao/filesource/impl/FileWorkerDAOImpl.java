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

package com.tencent.bk.job.file_gateway.dao.filesource.impl;

import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.file_gateway.consts.FileWorkerOnlineStatusEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.util.JooqTypeUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record22;
import org.jooq.Result;
import org.jooq.UpdateConditionStep;
import org.jooq.UpdateSetFirstStep;
import org.jooq.UpdateSetMoreStep;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.FileWorker;
import org.jooq.generated.tables.FileWorkerAbility;
import org.jooq.generated.tables.records.FileWorkerRecord;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
@Slf4j
public class FileWorkerDAOImpl implements FileWorkerDAO {

    public static final String GROUP_CONCAT_SEPARATOR = ",";
    public static final String KEY_FILE_WORKER_ID = "id";
    public static final String KEY_FILE_WORKER_APP_ID = "appId";
    public static final String KEY_FILE_WORKER_NAME = "name";
    public static final String KEY_FILE_WORKER_DESCRIPTION = "description";
    public static final String KEY_FILE_WORKER_TOKEN = "token";
    public static final String KEY_FILE_WORKER_ACCESS_HOST = "accessHost";
    public static final String KEY_FILE_WORKER_ACCESS_PORT = "accessPort";
    public static final String KEY_FILE_WORKER_CLOUD_AREA_ID = "cloudAreaId";
    public static final String KEY_FILE_WORKER_INNER_IP = "innerIp";
    public static final String KEY_FILE_WORKER_CPU_OVERLOAD = "cpuOverload";
    public static final String KEY_FILE_WORKER_MEM_RATE = "memRate";
    public static final String KEY_FILE_WORKER_MEM_FREE_SPACE = "memFreeSpace";
    public static final String KEY_FILE_WORKER_DISK_RATE = "diskRate";
    public static final String KEY_FILE_WORKER_DISK_FREE_SPACE = "diskFreeSpace";
    public static final String KEY_FILE_WORKER_VERSION = "version";
    public static final String KEY_FILE_WORKER_ONLINE_STATUS = "onlineStatus";
    public static final String KEY_FILE_WORKER_LAST_HEART_BEAT = "lastHeartBeat";
    public static final String KEY_FILE_WORKER_CREATOR = "creator";
    public static final String KEY_FILE_WORKER_CREATE_TIME = "createTime";
    public static final String KEY_FILE_WORKER_LAST_MODIFY_USER = "lastModifyUser";
    public static final String KEY_FILE_WORKER_LAST_MODIFY_TIME = "lastModifyTime";
    public static final String KEY_FILE_WORKER_ABILITY_TAGS = "abilityTags";
    private static final FileWorker defaultTable = FileWorker.FILE_WORKER;
    private static final FileWorkerAbility tFileWorkerAbility = FileWorkerAbility.FILE_WORKER_ABILITY;
    private final DSLContext defaultContext;

    @Autowired
    public FileWorkerDAOImpl(DSLContext dslContext) {
        this.defaultContext = dslContext;
    }

    private void setDefaultValue(FileWorkerDTO fileWorkerDTO) {
        if (fileWorkerDTO.getAppId() == null) {
            fileWorkerDTO.setAppId(-1L);
        }
    }

    @Override
    public Long insertFileWorker(DSLContext dslContext, FileWorkerDTO fileWorkerDTO) {
        setDefaultValue(fileWorkerDTO);
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.NAME,
            defaultTable.DESCRIPTION,
            defaultTable.TOKEN,
            defaultTable.ACCESS_HOST,
            defaultTable.ACCESS_PORT,
            defaultTable.CLOUD_AREA_ID,
            defaultTable.INNER_IP,
            defaultTable.CPU_OVERLOAD,
            defaultTable.MEM_RATE,
            defaultTable.MEM_FREE_SPACE,
            defaultTable.DISK_RATE,
            defaultTable.DISK_FREE_SPACE,
            defaultTable.VERSION,
            defaultTable.ONLINE_STATUS,
            defaultTable.LAST_HEART_BEAT,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME,
            defaultTable.CONFIG_STR
        ).values(
            fileWorkerDTO.getId(),
            fileWorkerDTO.getAppId(),
            fileWorkerDTO.getName(),
            fileWorkerDTO.getDescription(),
            fileWorkerDTO.getToken(),
            fileWorkerDTO.getAccessHost(),
            fileWorkerDTO.getAccessPort(),
            fileWorkerDTO.getCloudAreaId(),
            fileWorkerDTO.getInnerIp(),
            JooqTypeUtil.convertToDouble(fileWorkerDTO.getCpuOverload()),
            JooqTypeUtil.convertToDouble(fileWorkerDTO.getMemRate()),
            JooqTypeUtil.convertToDouble(fileWorkerDTO.getMemFreeSpace()),
            JooqTypeUtil.convertToDouble(fileWorkerDTO.getDiskRate()),
            JooqTypeUtil.convertToDouble(fileWorkerDTO.getDiskFreeSpace()),
            fileWorkerDTO.getVersion(),
            fileWorkerDTO.getOnlineStatus(),
            fileWorkerDTO.getLastHeartBeat(),
            fileWorkerDTO.getCreator(),
            System.currentTimeMillis(),
            fileWorkerDTO.getLastModifyUser(),
            System.currentTimeMillis(),
            fileWorkerDTO.getConfigStr()
        ).returning(defaultTable.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            Long id = query.fetchOne().getId();
            updateWorkerAbilityTagByWorkerId(dslContext, id, fileWorkerDTO.getAbilityTagList());
            return id;
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileWorker(DSLContext dslContext, FileWorkerDTO fileWorkerDTO) {
        UpdateSetFirstStep<FileWorkerRecord> query = dslContext.update(defaultTable);
        UpdateSetMoreStep<FileWorkerRecord> updateSetMoreStep = query.set(defaultTable.APP_ID,
            fileWorkerDTO.getAppId());
        if (fileWorkerDTO.getAccessHost() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.ACCESS_HOST, fileWorkerDTO.getAccessHost());
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.ACCESS_HOST, fileWorkerDTO.getAccessHost());
            }
        }
        if (fileWorkerDTO.getAccessPort() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.ACCESS_PORT, fileWorkerDTO.getAccessPort());
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.ACCESS_PORT, fileWorkerDTO.getAccessPort());
            }
        }
        if (fileWorkerDTO.getCloudAreaId() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.CLOUD_AREA_ID, fileWorkerDTO.getCloudAreaId());
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.CLOUD_AREA_ID, fileWorkerDTO.getCloudAreaId());
            }
        }
        if (fileWorkerDTO.getInnerIp() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.INNER_IP, fileWorkerDTO.getInnerIp());
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.INNER_IP, fileWorkerDTO.getInnerIp());
            }
        }
        if (fileWorkerDTO.getCpuOverload() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.CPU_OVERLOAD,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getCpuOverload()));
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.CPU_OVERLOAD,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getCpuOverload()));
            }
        }
        if (fileWorkerDTO.getMemRate() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.MEM_RATE,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getMemRate()));
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.MEM_RATE,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getMemRate()));
            }
        }
        if (fileWorkerDTO.getMemFreeSpace() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.MEM_FREE_SPACE,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getMemFreeSpace()));
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.MEM_FREE_SPACE,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getMemFreeSpace()));
            }
        }
        if (fileWorkerDTO.getDiskRate() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.DISK_RATE,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getDiskRate()));
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.DISK_RATE,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getDiskRate()));
            }
        }
        if (fileWorkerDTO.getDiskFreeSpace() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.DISK_FREE_SPACE,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getDiskFreeSpace()));
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.DISK_FREE_SPACE,
                    JooqTypeUtil.convertToDouble(fileWorkerDTO.getDiskFreeSpace()));
            }
        }
        if (fileWorkerDTO.getVersion() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.VERSION, fileWorkerDTO.getVersion());
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.VERSION, fileWorkerDTO.getVersion());
            }
        }
        if (fileWorkerDTO.getOnlineStatus() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.ONLINE_STATUS, fileWorkerDTO.getOnlineStatus());
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.ONLINE_STATUS, fileWorkerDTO.getOnlineStatus());
            }
        }
        if (fileWorkerDTO.getLastHeartBeat() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.LAST_HEART_BEAT, fileWorkerDTO.getLastHeartBeat());
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.LAST_HEART_BEAT,
                    fileWorkerDTO.getLastHeartBeat());
            }
        }
        if (fileWorkerDTO.getConfigStr() != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = query.set(defaultTable.CONFIG_STR, fileWorkerDTO.getConfigStr());
            } else {
                updateSetMoreStep = updateSetMoreStep.set(defaultTable.CONFIG_STR, fileWorkerDTO.getConfigStr());
            }
        }
        UpdateConditionStep<FileWorkerRecord> updateConditionStep = null;
        if (updateSetMoreStep == null) {
            return 0;
        } else {
            updateConditionStep = updateSetMoreStep.where(defaultTable.ID.eq(fileWorkerDTO.getId()));
        }
        val sql = updateConditionStep.getSQL(ParamType.INLINED);
        try {
            int affectedRowNum = updateConditionStep.execute();
            // 更新能力标签
            List<String> abilityTagList = fileWorkerDTO.getAbilityTagList();
            updateWorkerAbilityTagByWorkerId(dslContext, fileWorkerDTO.getId(), abilityTagList);
            return affectedRowNum;
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    private int updateWorkerAbilityTagByWorkerId(DSLContext dslContext, Long workerId, List<String> abilityTagList) {
        if (abilityTagList == null) {
            return 0;
        }
        AtomicInteger affectedNum = new AtomicInteger(0);
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            context.deleteFrom(tFileWorkerAbility).where(
                tFileWorkerAbility.WORKER_ID.eq(workerId)
            ).execute();
            val insertQuery = context.insertInto(tFileWorkerAbility,
                tFileWorkerAbility.ID,
                tFileWorkerAbility.WORKER_ID,
                tFileWorkerAbility.TAG,
                tFileWorkerAbility.DESCRIPTION
            ).values(
                (Long) null,
                null,
                null,
                null
            );
            BatchBindStep batchQuery = context.batch(insertQuery);
            for (String abilityTag : abilityTagList) {
                batchQuery = batchQuery.bind(
                    (Long) null,
                    workerId,
                    abilityTag,
                    ""
                );
            }
            if (!abilityTagList.isEmpty()) {
                int[] results = batchQuery.execute();
                for (int result : results) {
                    affectedNum.set(affectedNum.get() + result);
                }
            }
        });
        return affectedNum.get();
    }

    @Override
    public int updateAllFileWorkerOnlineStatus(DSLContext dslContext, Integer onlineStatus, Long aliveTime) {
        val updateQuery = dslContext.update(defaultTable)
            .set(defaultTable.ONLINE_STATUS, JooqTypeUtil.convertToByte(onlineStatus))
            .where(defaultTable.LAST_HEART_BEAT.le(aliveTime));
        return updateQuery.execute();
    }

    @Override
    public int deleteFileWorkerById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    public List<String> listWorkerAbilityTags(DSLContext dslContext, Long workerId) {
        val records = dslContext.select(tFileWorkerAbility.TAG)
            .from(tFileWorkerAbility)
            .where(tFileWorkerAbility.WORKER_ID.eq(workerId))
            .fetch();
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(record -> record.get(tFileWorkerAbility.TAG));
        }
    }

    @Override
    public FileWorkerDTO getFileWorkerById(DSLContext dslContext, Long id) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ID.eq(id));
        Record record = dslContext.selectFrom(defaultTable).where(conditions).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record, listWorkerAbilityTags(dslContext, id));
        }
    }

    @Override
    public List<FileWorkerDTO> listFileWorkers(DSLContext dslContext) {
        return listFileWorkersByConditions(dslContext, null);
    }

    @Override
    public List<FileWorkerDTO> listPublicFileWorkers(DSLContext dslContext) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.APP_ID.eq(-1L));
        return listFileWorkersByConditions(dslContext, conditions);
    }

    @Override
    public List<FileWorkerDTO> listFileWorkers(DSLContext dslContext, Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.APP_ID.eq(appId));
        return listFileWorkersByConditions(dslContext, conditions);
    }

    @Override
    public List<FileWorkerDTO> listPublicFileWorkersByAbilityTag(DSLContext dslContext, String tag) {
        return listFileWorkersByAbilityTag(dslContext, -1L, tag);
    }

    @Override
    public List<FileWorkerDTO> listFileWorkersByAbilityTag(DSLContext dslContext, Long appId, String tag) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(tFileWorkerAbility.TAG.eq(tag));
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        return listFileWorkersByConditions(dslContext, conditions);
    }

    @Override
    public List<FileWorkerDTO> listFileWorkersByAccess(DSLContext dslContext, String accessHost, Integer accessPort) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ACCESS_HOST.eq(accessHost));
        conditions.add(defaultTable.ACCESS_PORT.eq(accessPort));
        return listFileWorkersByConditions(dslContext, conditions);
    }

    private Long countFileWorkersByConditions(Collection<Condition> conditions) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        return defaultContext.selectCount().from(defaultTable).where(conditions).fetchOne(0, Long.class);
    }

    @Override
    public Long countFileWorkers() {
        return countFileWorkersByConditions(null);
    }

    @Override
    public Long countOnlineFileWorkers() {
        Collection<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ONLINE_STATUS.eq(FileWorkerOnlineStatusEnum.ONLINE.getStatus()));
        return countFileWorkersByConditions(conditions);
    }

    public boolean existsFileWorker(DSLContext dslContext, String accessHost, Integer accessPort) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ACCESS_HOST.eq(accessHost));
        conditions.add(defaultTable.ACCESS_PORT.eq(accessPort));
        return existsFileWorkerByConditions(dslContext, conditions);
    }

    @Override
    public FileWorkerDTO getFileWorker(DSLContext dslContext, String accessHost, Integer accessPort) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ACCESS_HOST.eq(accessHost));
        conditions.add(defaultTable.ACCESS_PORT.eq(accessPort));
        List<FileWorkerDTO> fileWorkerDTOList = listFileWorkersByConditions(dslContext, conditions);
        if (fileWorkerDTOList.isEmpty()) {
            return null;
        } else {
            return fileWorkerDTOList.get(0);
        }
    }

    private boolean existsFileWorkerByConditions(DSLContext dslContext, Collection<Condition> conditions) {
        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        return dslContext.fetchExists(dslContext.selectOne().from(defaultTable).where(conditions));
    }

    public List<FileWorkerDTO> listFileWorkersByConditions(DSLContext dslContext, Collection<Condition> conditions) {
        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        Result<Record22<Long, Long, String, String, String, String, Integer, Long, String, Double, Double, Double,
            Double, Double, String, Byte, Long, String, Long, String, Long, String>> records = null;
        val query = dslContext.select(
            defaultTable.ID.as(KEY_FILE_WORKER_ID),
            defaultTable.APP_ID.as(KEY_FILE_WORKER_APP_ID),
            defaultTable.NAME.as(KEY_FILE_WORKER_NAME),
            defaultTable.DESCRIPTION.as(KEY_FILE_WORKER_DESCRIPTION),
            defaultTable.TOKEN.as(KEY_FILE_WORKER_TOKEN),
            defaultTable.ACCESS_HOST.as(KEY_FILE_WORKER_ACCESS_HOST),
            defaultTable.ACCESS_PORT.as(KEY_FILE_WORKER_ACCESS_PORT),
            defaultTable.CLOUD_AREA_ID.as(KEY_FILE_WORKER_CLOUD_AREA_ID),
            defaultTable.INNER_IP.as(KEY_FILE_WORKER_INNER_IP),
            defaultTable.CPU_OVERLOAD.as(KEY_FILE_WORKER_CPU_OVERLOAD),
            defaultTable.MEM_RATE.as(KEY_FILE_WORKER_MEM_RATE),
            defaultTable.MEM_FREE_SPACE.as(KEY_FILE_WORKER_MEM_FREE_SPACE),
            defaultTable.DISK_RATE.as(KEY_FILE_WORKER_DISK_RATE),
            defaultTable.DISK_FREE_SPACE.as(KEY_FILE_WORKER_DISK_FREE_SPACE),
            defaultTable.VERSION.as(KEY_FILE_WORKER_VERSION),
            defaultTable.ONLINE_STATUS.as(KEY_FILE_WORKER_ONLINE_STATUS),
            defaultTable.LAST_HEART_BEAT.as(KEY_FILE_WORKER_LAST_HEART_BEAT),
            defaultTable.CREATOR.as(KEY_FILE_WORKER_CREATOR),
            defaultTable.CREATE_TIME.as(KEY_FILE_WORKER_CREATE_TIME),
            defaultTable.LAST_MODIFY_USER.as(KEY_FILE_WORKER_LAST_MODIFY_USER),
            defaultTable.LAST_MODIFY_TIME.as(KEY_FILE_WORKER_LAST_MODIFY_TIME),
            DSL.groupConcat(tFileWorkerAbility.TAG).separator(GROUP_CONCAT_SEPARATOR).as(KEY_FILE_WORKER_ABILITY_TAGS)
        )
            .from(defaultTable)
            .leftJoin(tFileWorkerAbility)
            .on(defaultTable.ID.eq(tFileWorkerAbility.WORKER_ID))
            .where(conditions)
            .groupBy(defaultTable.ID)
            .orderBy(defaultTable.LAST_HEART_BEAT.desc());
        try {
            String sql = query.getSQL(ParamType.INLINED);
            log.debug("SQL=" + sql);
            records = query.fetch();
        } catch (Exception e) {
            log.error(String.format("Fail to execute SQL:%s", query.getSQL(ParamType.INLINED)), e);
        }
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    private FileWorkerDTO convertRecordToDto(Record record, List<String> abilityTagList) {
        FileWorkerDTO fileWorkerDTO = new FileWorkerDTO();
        fileWorkerDTO.setId(record.get(defaultTable.ID));
        fileWorkerDTO.setAppId(record.get(defaultTable.APP_ID));
        fileWorkerDTO.setName(record.get(defaultTable.NAME));
        fileWorkerDTO.setDescription(record.get(defaultTable.DESCRIPTION));
        fileWorkerDTO.setToken(record.get(defaultTable.TOKEN));
        fileWorkerDTO.setAccessHost(record.get(defaultTable.ACCESS_HOST));
        fileWorkerDTO.setAccessPort(record.get(defaultTable.ACCESS_PORT));
        fileWorkerDTO.setCloudAreaId(record.get(defaultTable.CLOUD_AREA_ID));
        fileWorkerDTO.setInnerIp(record.get(defaultTable.INNER_IP));
        fileWorkerDTO.setCpuOverload(JooqTypeUtil.convertToFloat(record.get(defaultTable.CPU_OVERLOAD)));
        fileWorkerDTO.setMemRate(JooqTypeUtil.convertToFloat((record.get(defaultTable.MEM_RATE))));
        fileWorkerDTO.setMemFreeSpace(JooqTypeUtil.convertToFloat((record.get(defaultTable.MEM_FREE_SPACE))));
        fileWorkerDTO.setDiskRate(JooqTypeUtil.convertToFloat((record.get(defaultTable.DISK_RATE))));
        fileWorkerDTO.setDiskFreeSpace(JooqTypeUtil.convertToFloat((record.get(defaultTable.DISK_FREE_SPACE))));
        fileWorkerDTO.setVersion((record.get(defaultTable.VERSION)));
        fileWorkerDTO.setOnlineStatus((record.get(defaultTable.ONLINE_STATUS)));
        fileWorkerDTO.setLastHeartBeat((record.get(defaultTable.LAST_HEART_BEAT)));
        fileWorkerDTO.setCreator((record.get(defaultTable.CREATOR)));
        fileWorkerDTO.setCreateTime((record.get(defaultTable.CREATE_TIME)));
        fileWorkerDTO.setLastModifyUser((record.get(defaultTable.LAST_MODIFY_USER)));
        fileWorkerDTO.setLastModifyTime((record.get(defaultTable.LAST_MODIFY_TIME)));
        fileWorkerDTO.setConfigStr((record.get(defaultTable.CONFIG_STR)));
        fileWorkerDTO.setAbilityTagList(abilityTagList);
        return fileWorkerDTO;
    }

    private FileWorkerDTO convertRecordToDto(Record22<Long, Long, String, String, String, String, Integer, Long,
        String, Double, Double, Double, Double, Double, String, Byte, Long, String, Long, String, Long, String> record) {
        FileWorkerDTO fileWorkerDTO = new FileWorkerDTO();
        fileWorkerDTO.setId((Long) (record.get(KEY_FILE_WORKER_ID)));
        fileWorkerDTO.setAppId((Long) (record.get(KEY_FILE_WORKER_APP_ID)));
        fileWorkerDTO.setName((String) (record.get(KEY_FILE_WORKER_NAME)));
        fileWorkerDTO.setDescription((String) (record.get(KEY_FILE_WORKER_DESCRIPTION)));
        fileWorkerDTO.setToken((String) (record.get(KEY_FILE_WORKER_TOKEN)));
        fileWorkerDTO.setAccessHost((String) (record.get(KEY_FILE_WORKER_ACCESS_HOST)));
        fileWorkerDTO.setAccessPort((Integer) (record.get(KEY_FILE_WORKER_ACCESS_PORT)));
        fileWorkerDTO.setCloudAreaId((Long) (record.get(KEY_FILE_WORKER_CLOUD_AREA_ID)));
        fileWorkerDTO.setInnerIp((String) (record.get(KEY_FILE_WORKER_INNER_IP)));
        fileWorkerDTO.setCpuOverload(JooqTypeUtil.convertToFloat((Double) (record.get(KEY_FILE_WORKER_CPU_OVERLOAD))));
        fileWorkerDTO.setMemRate(JooqTypeUtil.convertToFloat((Double) (record.get(KEY_FILE_WORKER_MEM_RATE))));
        fileWorkerDTO.setMemFreeSpace(JooqTypeUtil.convertToFloat((Double) (record.get(KEY_FILE_WORKER_MEM_FREE_SPACE))));
        fileWorkerDTO.setDiskRate(JooqTypeUtil.convertToFloat((Double) (record.get(KEY_FILE_WORKER_DISK_RATE))));
        fileWorkerDTO.setDiskFreeSpace(JooqTypeUtil.convertToFloat((Double) (record.get(KEY_FILE_WORKER_DISK_FREE_SPACE))));
        fileWorkerDTO.setVersion((String) (record.get(KEY_FILE_WORKER_VERSION)));
        fileWorkerDTO.setOnlineStatus((Byte) (record.get(KEY_FILE_WORKER_ONLINE_STATUS)));
        fileWorkerDTO.setLastHeartBeat((Long) (record.get(KEY_FILE_WORKER_LAST_HEART_BEAT)));
        fileWorkerDTO.setCreator((String) (record.get(KEY_FILE_WORKER_CREATOR)));
        fileWorkerDTO.setCreateTime((Long) (record.get(KEY_FILE_WORKER_CREATE_TIME)));
        fileWorkerDTO.setLastModifyUser((String) (record.get(KEY_FILE_WORKER_LAST_MODIFY_USER)));
        fileWorkerDTO.setLastModifyTime((Long) (record.get(KEY_FILE_WORKER_LAST_MODIFY_TIME)));
        String abilityTagStr = (String) (record.get(KEY_FILE_WORKER_ABILITY_TAGS));
        fileWorkerDTO.setAbilityTagList(StringUtil.strToList(abilityTagStr, String.class, GROUP_CONCAT_SEPARATOR));
        return fileWorkerDTO;
    }
}
