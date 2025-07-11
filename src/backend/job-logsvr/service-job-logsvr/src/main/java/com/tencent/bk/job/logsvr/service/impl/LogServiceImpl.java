/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.logsvr.service.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.FileLogQuery;
import com.tencent.bk.job.logsvr.model.FileTaskLogDoc;
import com.tencent.bk.job.logsvr.model.FileTaskLogDocField;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.ScriptTaskLogDoc;
import com.tencent.bk.job.logsvr.model.ScriptTaskLogDocField;
import com.tencent.bk.job.logsvr.model.TaskExecuteObjectLog;
import com.tencent.bk.job.logsvr.mongo.LogCollectionFactory;
import com.tencent.bk.job.logsvr.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LogServiceImpl implements LogService {
    private static final int BATCH_SIZE = 100;
    private static final char[] SPECIAL_CHAR = {'*', '(', ')', '+', '?', '\\', '$', '^', '>', '.'};
    private static final String[] ESCAPE_CHAR = {"\\*", "\\(", "\\)", "\\+", "\\?", "\\\\", "\\$", "\\^", "\\>", "\\."};
    private final MongoTemplate mongoTemplate;
    private final LogCollectionFactory logCollectionFactory;

    @Autowired
    public LogServiceImpl(MongoTemplate mongoTemplate, LogCollectionFactory logCollectionFactory) {
        this.mongoTemplate = mongoTemplate;
        this.logCollectionFactory = logCollectionFactory;
    }

    @Override
    public void saveLog(TaskExecuteObjectLog taskExecuteObjectLog) {
        if (taskExecuteObjectLog.getLogType().equals(LogTypeEnum.SCRIPT.getValue())) {
            writeScriptLog(taskExecuteObjectLog);
        } else if (taskExecuteObjectLog.getLogType().equals(LogTypeEnum.FILE.getValue())) {
            writeFileLog(taskExecuteObjectLog);
        }
    }


    @Override
    public void saveLogs(LogTypeEnum logType, List<TaskExecuteObjectLog> taskExecuteObjectLogs) {
        if (logType == LogTypeEnum.SCRIPT) {
            batchWriteScriptLogs(taskExecuteObjectLogs);
        } else if (logType == LogTypeEnum.FILE) {
            batchWriteFileLogs(taskExecuteObjectLogs);
        }
    }

    private void batchWriteScriptLogs(List<TaskExecuteObjectLog> taskExecuteObjectLogs) {
        String jobCreateDate = taskExecuteObjectLogs.get(0).getJobCreateDate();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.SCRIPT);
        List<Document> scriptLogDocList = taskExecuteObjectLogs.stream()
            .map(taskHostLog -> buildScriptLogDoc(taskHostLog.getScriptTaskLog())).collect(Collectors.toList());
        List<List<Document>> batchDocList = CollectionUtil.partitionList(scriptLogDocList, BATCH_SIZE);
        long start = System.currentTimeMillis();
        batchDocList.forEach(docs ->
            logCollectionFactory.getCollection(collectionName)
                .insertMany(docs, new InsertManyOptions().ordered(false)));
        long end = System.currentTimeMillis();
        log.info("Batch write script logs, docSize: {}, cost: {} ms", scriptLogDocList.size(), end - start);
    }


    private void batchWriteFileLogs(List<TaskExecuteObjectLog> taskExecuteObjectLogs) {
        String jobCreateDate = taskExecuteObjectLogs.get(0).getJobCreateDate();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.FILE);

        List<WriteModel<Document>> updateOps = buildUpdateOpsFileTask(taskExecuteObjectLogs);
        List<List<WriteModel<Document>>> batchList = CollectionUtil.partitionList(updateOps, BATCH_SIZE);

        long start = System.currentTimeMillis();
        batchList.forEach(batchOps -> logCollectionFactory.getCollection(collectionName)
            .bulkWrite(batchOps, new BulkWriteOptions().ordered(false)));
        long end = System.currentTimeMillis();
        log.info("Batch write file logs, stepInstanceId: {}, opSize: {}, cost: {} ms",
            taskExecuteObjectLogs.get(0).getStepInstanceId(), updateOps.size(), end - start);
    }

    private List<WriteModel<Document>> buildUpdateOpsFileTask(List<TaskExecuteObjectLog> taskExecuteObjectLogs) {
        List<WriteModel<Document>> updateOps = new ArrayList<>();
        taskExecuteObjectLogs.forEach(taskExecuteObjectLog -> {
            long stepInstanceId = taskExecuteObjectLog.getStepInstanceId();
            int executeCount = taskExecuteObjectLog.getExecuteCount();
            Integer batch = taskExecuteObjectLog.getBatch();
            List<FileTaskLogDoc> fileTaskLogs = taskExecuteObjectLog.getFileTaskLogs();

            if (CollectionUtils.isNotEmpty(taskExecuteObjectLog.getFileTaskLogs())) {
                fileTaskLogs.forEach(fileTaskLog -> {
                    BasicDBObject filter = buildQueryDocForFileTaskLog(stepInstanceId, executeCount, batch,
                        fileTaskLog);
                    BasicDBObject update = buildUpdateDocForFileTaskLog(stepInstanceId, executeCount, batch,
                        taskExecuteObjectLog.getExecuteObjectId(), taskExecuteObjectLog.getHostId(), fileTaskLog);
                    UpdateOneModel<Document> updateOp = new UpdateOneModel<>(filter, update,
                        new UpdateOptions().upsert(true));
                    updateOps.add(updateOp);
                });
            }
        });
        return updateOps;
    }

    private void writeScriptLog(TaskExecuteObjectLog taskExecuteObjectLog) {
        if (taskExecuteObjectLog == null || taskExecuteObjectLog.getScriptTaskLog() == null) {
            return;
        }
        LogTypeEnum logType = LogTypeEnum.getLogType(taskExecuteObjectLog.getLogType());

        long start = System.currentTimeMillis();
        long stepInstanceId = taskExecuteObjectLog.getStepInstanceId();
        String collectionName = buildLogCollectionName(taskExecuteObjectLog.getJobCreateDate(), logType);

        try {
            Document scriptLogDoc = buildScriptLogDoc(taskExecuteObjectLog.getScriptTaskLog());
            logCollectionFactory.getCollection(collectionName).insertOne(scriptLogDoc);
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 20L) {
                log.warn("Save script task log slow, stepInstanceId: {}, cost: {} ms", stepInstanceId, cost);
            }
        }
    }

    private void writeFileLog(TaskExecuteObjectLog taskExecuteObjectLog) {
        if (taskExecuteObjectLog.getFileTaskLogs().size() == 1) {
            taskExecuteObjectLog.getFileTaskLogs().forEach(
                fileTaskLog -> writeFileLog(
                    taskExecuteObjectLog.getJobCreateDate(),
                    taskExecuteObjectLog.getStepInstanceId(),
                    taskExecuteObjectLog.getExecuteCount(),
                    taskExecuteObjectLog.getBatch(),
                    taskExecuteObjectLog.getExecuteObjectId(),
                    taskExecuteObjectLog.getHostId(),
                    fileTaskLog));
        } else {
            batchWriteFileLogs(Collections.singletonList(taskExecuteObjectLog));
        }
    }

    private Document buildScriptLogDoc(ScriptTaskLogDoc scriptTaskLog) {
        Document doc = new Document();
        doc.put(ScriptTaskLogDocField.STEP_ID, scriptTaskLog.getStepInstanceId());
        doc.put(ScriptTaskLogDocField.EXECUTE_COUNT, scriptTaskLog.getExecuteCount());
        if (scriptTaskLog.getBatch() != null && scriptTaskLog.getBatch() > 0) {
            doc.put(ScriptTaskLogDocField.BATCH, scriptTaskLog.getBatch());
        }
        if (StringUtils.isNotEmpty(scriptTaskLog.getExecuteObjectId())) {
            doc.put(ScriptTaskLogDocField.EXECUTE_OBJECT_ID, scriptTaskLog.getExecuteObjectId());
        }
        if (scriptTaskLog.getHostId() != null) {
            doc.put(ScriptTaskLogDocField.HOST_ID, scriptTaskLog.getHostId());
        }
        if (StringUtils.isNotEmpty(scriptTaskLog.getIp())) {
            doc.put(ScriptTaskLogDocField.IP, scriptTaskLog.getIp());
        }
        if (StringUtils.isNotEmpty(scriptTaskLog.getIpv6())) {
            doc.put(ScriptTaskLogDocField.IPV6, scriptTaskLog.getIpv6());
        }
        doc.put(ScriptTaskLogDocField.CONTENT, scriptTaskLog.getContent());
        doc.put(ScriptTaskLogDocField.OFFSET, scriptTaskLog.getOffset());
        return doc;
    }

    private void writeFileLog(String jobCreateDate,
                              long stepInstanceId,
                              int executeCount,
                              Integer batch,
                              String executeObjectId,
                              Long hostId,
                              FileTaskLogDoc fileTaskLog) {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.FILE);
        try {
            BasicDBObject filter = buildQueryDocForFileTaskLog(stepInstanceId, executeCount, batch, fileTaskLog);
            BasicDBObject update = buildUpdateDocForFileTaskLog(stepInstanceId, executeCount, batch, executeObjectId,
                hostId, fileTaskLog);
            logCollectionFactory.getCollection(collectionName)
                .updateOne(filter, update, new UpdateOptions().upsert(true));
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 10L) {
                log.warn("Save log slow, stepInstanceId:{}, cost: {} ms", stepInstanceId, cost);
            }
        }
    }

    private BasicDBObject buildQueryDocForFileTaskLog(long stepInstanceId,
                                                      int executeCount,
                                                      Integer batch,
                                                      FileTaskLogDoc fileTaskLog) {
        BasicDBObject filter = new BasicDBObject();
        filter.append(FileTaskLogDocField.STEP_ID, stepInstanceId);
        filter.append(FileTaskLogDocField.EXECUTE_COUNT, executeCount);
        if (batch != null && batch > 0) {
            filter.append(FileTaskLogDocField.BATCH, batch);
        }
        filter.append(FileTaskLogDocField.TASK_ID, fileTaskLog.getTaskId());
        return filter;
    }

    private BasicDBObject buildUpdateDocForFileTaskLog(long stepInstanceId,
                                                       int executeCount,
                                                       Integer batch,
                                                       String executeObjectId,
                                                       Long hostId,
                                                       FileTaskLogDoc fileTaskLog) {
        BasicDBObject update = new BasicDBObject();
        BasicDBObject setDBObject = new BasicDBObject();
        BasicDBObject pushDBObject = new BasicDBObject();
        setDBObject.append(FileTaskLogDocField.STEP_ID, stepInstanceId)
            .append(FileTaskLogDocField.EXECUTE_COUNT, executeCount)
            .append(FileTaskLogDocField.MODE, fileTaskLog.getMode())
            .append(FileTaskLogDocField.TASK_ID, fileTaskLog.getTaskId());
        if (StringUtils.isNotEmpty(executeObjectId)) {
            setDBObject.append(FileTaskLogDocField.EXECUTE_OBJECT_ID, executeObjectId);
        }
        if (hostId != null) {
            setDBObject.append(FileTaskLogDocField.HOST_ID, hostId);
        }
        if (batch != null && batch > 0) {
            setDBObject.append(FileTaskLogDocField.BATCH, batch);
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSrcExecuteObjectId())) {
            setDBObject.append(FileTaskLogDocField.SRC_EXECUTE_OBJECT_ID, fileTaskLog.getSrcExecuteObjectId());
        }
        if (fileTaskLog.getSrcHostId() != null) {
            setDBObject.append(FileTaskLogDocField.SRC_HOST_ID, fileTaskLog.getSrcHostId());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSrcIp())) {
            setDBObject.append(FileTaskLogDocField.SRC_IP, fileTaskLog.getSrcIp());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSrcIpv6())) {
            setDBObject.append(FileTaskLogDocField.SRC_IPV6, fileTaskLog.getSrcIpv6());
        }
        if (fileTaskLog.getSrcFileType() != null) {
            setDBObject.append(FileTaskLogDocField.SRC_FILE_TYPE, fileTaskLog.getSrcFileType());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSrcFile())) {
            setDBObject.append(FileTaskLogDocField.SRC_FILE, fileTaskLog.getSrcFile());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getDisplaySrcFile())) {
            setDBObject.append(FileTaskLogDocField.DISPLAY_SRC_FILE, fileTaskLog.getDisplaySrcFile());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getDestExecuteObjectId())) {
            setDBObject.append(FileTaskLogDocField.DEST_EXECUTE_OBJECT_ID, fileTaskLog.getDestExecuteObjectId());
        }
        if (fileTaskLog.getDestHostId() != null) {
            setDBObject.append(FileTaskLogDocField.DEST_HOST_ID, fileTaskLog.getDestHostId());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSrcIp())) {
            setDBObject.append(FileTaskLogDocField.DEST_IP, fileTaskLog.getDestIp());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSrcIpv6())) {
            setDBObject.append(FileTaskLogDocField.DEST_IPV6, fileTaskLog.getDestIpv6());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getDestFile())) {
            setDBObject.append(FileTaskLogDocField.DEST_FILE, fileTaskLog.getDestFile());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSize())) {
            setDBObject.append(FileTaskLogDocField.SIZE, fileTaskLog.getSize());
        }
        if (fileTaskLog.getStatus() != null) {
            setDBObject.append(FileTaskLogDocField.STATUS, fileTaskLog.getStatus());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getStatusDesc())) {
            setDBObject.append(FileTaskLogDocField.STATUS_DESC, fileTaskLog.getStatusDesc());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSpeed())) {
            setDBObject.append(FileTaskLogDocField.SPEED, fileTaskLog.getSpeed());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getProcess())) {
            setDBObject.append(FileTaskLogDocField.PROCESS, fileTaskLog.getProcess());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getContent())) {
            pushDBObject.append(FileTaskLogDocField.CONTENT_LIST, fileTaskLog.getContent());
        }

        update.put("$set", setDBObject);
        if (pushDBObject.size() > 0) {
            update.put("$push", pushDBObject);
        }
        return update;
    }

    private String buildLogCollectionName(String jobCreateDate, LogTypeEnum logType) {
        return "job_log_" + getLogTypeName(logType) + "_" + jobCreateDate;
    }

    private String getLogTypeName(LogTypeEnum logType) {
        if (logType == LogTypeEnum.SCRIPT) {
            return "script";
        } else if (logType == LogTypeEnum.FILE) {
            return "file";
        } else {
            throw new IllegalArgumentException("Invalid logType");
        }
    }

    @Override
    public List<TaskExecuteObjectLog> listScriptLogs(ScriptLogQuery scriptLogQuery) throws ServiceException {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(scriptLogQuery.getJobCreateDate(), LogTypeEnum.SCRIPT);

        try {
            Query query = buildScriptLogMongoQuery(scriptLogQuery);

            List<ScriptTaskLogDoc> scriptLogs = mongoTemplate.find(query, ScriptTaskLogDoc.class, collectionName);

            if (CollectionUtils.isEmpty(scriptLogs)) {
                return Collections.emptyList();
            }

            return groupScriptTaskLogsByExecuteObject(scriptLogQuery.getStepInstanceId(),
                scriptLogQuery.getExecuteCount(),
                scriptLogQuery.getBatch(), scriptLogs);
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 100L) {
                log.warn("Get script log by hosts slow, req: {}, cost: {} ms", scriptLogQuery, cost);
            }
        }
    }

    private Query buildScriptLogMongoQuery(ScriptLogQuery scriptLogQuery) {
        long stepInstanceId = scriptLogQuery.getStepInstanceId();
        int executeCount = scriptLogQuery.getExecuteCount();
        Integer batch = scriptLogQuery.getBatch();
        List<Long> hostIds = scriptLogQuery.getHostIds();
        List<String> executeObjectIds = scriptLogQuery.getExecuteObjectIds();

        Query query = new Query();
        query.addCriteria(Criteria.where(ScriptTaskLogDocField.STEP_ID).is(stepInstanceId));
        query.addCriteria(Criteria.where(ScriptTaskLogDocField.EXECUTE_COUNT).is(executeCount));
        if (batch != null && batch > 0) {
            query.addCriteria(Criteria.where(ScriptTaskLogDocField.BATCH).is(batch));
        }
        // executeObjectIds/hostIds 两个参数二选一，优先使用 executeObjectIds
        if (CollectionUtils.isNotEmpty(scriptLogQuery.getExecuteObjectIds())) {
            if (executeObjectIds.size() == 1) {
                query.addCriteria(Criteria.where(ScriptTaskLogDocField.EXECUTE_OBJECT_ID).is(executeObjectIds.get(0)));
            } else {
                query.addCriteria(Criteria.where(ScriptTaskLogDocField.EXECUTE_OBJECT_ID).in(executeObjectIds));
            }
        } else if (CollectionUtils.isNotEmpty(hostIds)) {
            if (hostIds.size() == 1) {
                query.addCriteria(Criteria.where(ScriptTaskLogDocField.HOST_ID).is(hostIds.get(0)));
            } else {
                query.addCriteria(Criteria.where(ScriptTaskLogDocField.HOST_ID).in(hostIds));
            }
        }

        return query;
    }

    @Override
    public List<FileTaskLogDoc> listFileLogs(FileLogQuery getLogRequest) {
        String collectionName = buildLogCollectionName(getLogRequest.getJobCreateDate(), LogTypeEnum.FILE);

        long start = System.currentTimeMillis();
        try {
            Query query = buildFileLogMongoQuery(getLogRequest);

            List<FileTaskLogDoc> fileTaskLogs = mongoTemplate.find(query, FileTaskLogDoc.class, collectionName);
            if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
                fileTaskLogs.forEach(fileTaskLog ->
                    fileTaskLog.setContent(StringUtils.join(fileTaskLog.getContentList(), null)));
            }
            return fileTaskLogs;
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 50L) {
                log.warn("Get file log slow, query: {}, cost: {} ms", getLogRequest, cost);
            }
        }
    }

    private Query buildFileLogMongoQuery(FileLogQuery getLogRequest) {
        Query query = new Query();
        query.addCriteria(Criteria.where(FileTaskLogDocField.STEP_ID).is(getLogRequest.getStepInstanceId()));
        query.addCriteria(Criteria.where(FileTaskLogDocField.EXECUTE_COUNT).is(getLogRequest.getExecuteCount()));
        if (getLogRequest.getMode() != null) {
            query.addCriteria(Criteria.where(FileTaskLogDocField.MODE).is(getLogRequest.getMode()));
        }
        // executeObjectIds/hostIds 两个参数二选一，优先使用 executeObjectIds
        if (CollectionUtils.isNotEmpty(getLogRequest.getExecuteObjectIds())) {
            if (getLogRequest.getExecuteObjectIds().size() > 1) {
                query.addCriteria(Criteria.where(FileTaskLogDocField.EXECUTE_OBJECT_ID)
                    .in(getLogRequest.getExecuteObjectIds()));
            } else {
                query.addCriteria(Criteria.where(FileTaskLogDocField.EXECUTE_OBJECT_ID)
                    .is(getLogRequest.getExecuteObjectIds().get(0)));
            }
        } else if (CollectionUtils.isNotEmpty(getLogRequest.getHostIds())) {
            if (getLogRequest.getHostIds().size() > 1) {
                query.addCriteria(Criteria.where(FileTaskLogDocField.HOST_ID).in(getLogRequest.getHostIds()));
            } else {
                query.addCriteria(Criteria.where(FileTaskLogDocField.HOST_ID).is(getLogRequest.getHostIds().get(0)));
            }
        }
        if (getLogRequest.getBatch() != null && getLogRequest.getBatch() > 0) {
            query.addCriteria(Criteria.where(FileTaskLogDocField.BATCH).is(getLogRequest.getBatch()));
        }

        return query;
    }

    private List<TaskExecuteObjectLog> groupScriptTaskLogsByExecuteObject(long stepInstanceId,
                                                                          int executeCount,
                                                                          Integer batch,
                                                                          List<ScriptTaskLogDoc> scriptTaskLogs) {
        List<TaskExecuteObjectLog> taskExecuteObjectLogs = new ArrayList<>();
        boolean existExecuteObjectIdField = scriptTaskLogs.get(0).getExecuteObjectId() != null;
        if (existExecuteObjectIdField) {
            Map<String, List<ScriptTaskLogDoc>> scriptLogsGroups = new HashMap<>();
            scriptTaskLogs.forEach(scriptTaskLog -> {
                List<ScriptTaskLogDoc> scriptLogGroup = scriptLogsGroups.computeIfAbsent(
                    scriptTaskLog.getExecuteObjectId(), k -> new ArrayList<>());
                scriptLogGroup.add(scriptTaskLog);
            });
            scriptLogsGroups.forEach(
                (executeObjectId, scriptLogGroup) ->
                    taskExecuteObjectLogs.add(
                        buildTaskExecuteObjectLog(stepInstanceId, executeCount, batch, scriptLogGroup)));
        } else {
            // 兼容 hostId
            Map<Long, List<ScriptTaskLogDoc>> scriptLogsGroups = new HashMap<>();
            scriptTaskLogs.forEach(scriptTaskLog -> {
                List<ScriptTaskLogDoc> scriptLogGroup = scriptLogsGroups.computeIfAbsent(scriptTaskLog.getHostId(),
                    k -> new ArrayList<>());
                scriptLogGroup.add(scriptTaskLog);
            });
            scriptLogsGroups.forEach(
                (hostId, scriptLogGroup) ->
                    taskExecuteObjectLogs.add(
                        buildTaskExecuteObjectLog(stepInstanceId, executeCount, batch, scriptLogGroup)));
        }

        return taskExecuteObjectLogs;
    }

    private TaskExecuteObjectLog buildTaskExecuteObjectLog(long stepInstanceId,
                                                           int executeCount,
                                                           Integer batch,
                                                           List<ScriptTaskLogDoc> scriptLogs) {
        TaskExecuteObjectLog taskExecuteObjectLog = new TaskExecuteObjectLog();
        taskExecuteObjectLog.setStepInstanceId(stepInstanceId);
        taskExecuteObjectLog.setExecuteCount(executeCount);
        taskExecuteObjectLog.setBatch(batch);
        taskExecuteObjectLog.setExecuteObjectId(scriptLogs.get(0).getExecuteObjectId());
        taskExecuteObjectLog.setHostId(scriptLogs.get(0).getHostId());
        taskExecuteObjectLog.setIp(scriptLogs.get(0).getIp());
        taskExecuteObjectLog.setIpv6(scriptLogs.get(0).getIpv6());

        scriptLogs.sort(ScriptTaskLogDoc.LOG_OFFSET_COMPARATOR);
        taskExecuteObjectLog.setScriptContent(scriptLogs.stream()
            .map(ScriptTaskLogDoc::getContent).collect(Collectors.joining("")));

        return taskExecuteObjectLog;
    }

    @Override
    public List<FileTaskLogDoc> getFileLogsByTaskIds(String jobCreateDate, long stepInstanceId, int executeCount,
                                                     Integer batch, List<String> taskIds) {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.FILE);
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where(FileTaskLogDocField.STEP_ID).is(stepInstanceId));
            query.addCriteria(Criteria.where(FileTaskLogDocField.EXECUTE_COUNT).is(executeCount));
            query.addCriteria(Criteria.where(FileTaskLogDocField.TASK_ID).in(taskIds));
            if (batch != null && batch > 0) {
                query.addCriteria(Criteria.where(FileTaskLogDocField.BATCH).is(batch));
            }
            List<FileTaskLogDoc> fileTaskLogs = mongoTemplate.find(query, FileTaskLogDoc.class, collectionName);
            if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
                fileTaskLogs.forEach(taskTaskLog ->
                    taskTaskLog.setContent(StringUtils.join(taskTaskLog.getContentList(), null)));
            }
            return fileTaskLogs;
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 20L) {
                log.warn("Get file log by task ids slow, stepInstanceId: {}, executeCount: {}, taskIds: {} cost: {} ms",
                    stepInstanceId, executeCount, taskIds, cost);
            }
        }
    }

    @Override
    public List<HostDTO> getHostsByKeyword(String jobCreateDate, long stepInstanceId, int executeCount,
                                           Integer batch, String keyword) {
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.SCRIPT);
        Query query = buildQueryForKeywordSearch(stepInstanceId, executeCount, batch, keyword);
        query.fields().include(ScriptTaskLogDocField.IP, ScriptTaskLogDocField.HOST_ID);
        List<ScriptTaskLogDoc> logs = mongoTemplate.find(query, ScriptTaskLogDoc.class, collectionName);
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logs.stream()
            .map(this::extractHostInfo)
            .distinct()
            .collect(Collectors.toList());
    }

    private HostDTO extractHostInfo(ScriptTaskLogDoc log) {
        HostDTO host;
        if (log.getHostId() != null) {
            host = new HostDTO();
            host.setHostId(log.getHostId());
        } else {
            host = HostDTO.fromCloudIp(log.getIp());
        }
        return host;
    }

    private Query buildQueryForKeywordSearch(long stepInstanceId, int executeCount, Integer batch, String keyword) {
        Query query = new Query();
        query.addCriteria(Criteria.where(ScriptTaskLogDocField.STEP_ID).is(stepInstanceId));
        if (executeCount == 0) {
            query.addCriteria(Criteria.where(ScriptTaskLogDocField.EXECUTE_COUNT).is(executeCount));
        } else {
            query.addCriteria(Criteria.where(ScriptTaskLogDocField.EXECUTE_COUNT).lte(executeCount));
        }
        if (batch != null && batch > 0) {
            query.addCriteria(Criteria.where(ScriptTaskLogDocField.BATCH).is(batch));
        }
        keyword = StringUtil.escape(keyword, SPECIAL_CHAR, ESCAPE_CHAR);
        Pattern pattern = Pattern.compile(keyword, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
        query.addCriteria(Criteria.where("content").regex(pattern));
        return query;
    }

    @Override
    public List<String> getExecuteObjectIdsByKeyword(String jobCreateDate,
                                                     long stepInstanceId,
                                                     int executeCount,
                                                     Integer batch,
                                                     String keyword) {
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.SCRIPT);
        Query query = buildQueryForKeywordSearch(stepInstanceId, executeCount, batch, keyword);
        query.fields().include(ScriptTaskLogDocField.EXECUTE_OBJECT_ID);
        List<ScriptTaskLogDoc> logs = mongoTemplate.find(query, ScriptTaskLogDoc.class, collectionName);
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logs.stream()
            .map(ScriptTaskLogDoc::getExecuteObjectId)
            .distinct()
            .collect(Collectors.toList());
    }
}
