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
import com.tencent.bk.job.logsvr.model.TaskHostLog;
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
    public void saveLog(TaskHostLog taskHostLog) {
        if (taskHostLog.getLogType().equals(LogTypeEnum.SCRIPT.getValue())) {
            writeScriptLog(taskHostLog);
        } else if (taskHostLog.getLogType().equals(LogTypeEnum.FILE.getValue())) {
            writeFileLog(taskHostLog);
        }
    }


    @Override
    public void saveLogs(LogTypeEnum logType, List<TaskHostLog> taskHostLogs) {
        if (logType == LogTypeEnum.SCRIPT) {
            batchWriteScriptLogs(taskHostLogs);
        } else if (logType == LogTypeEnum.FILE) {
            batchWriteFileLogs(taskHostLogs);
        }
    }

    private void batchWriteScriptLogs(List<TaskHostLog> taskHostLogs) {
        String jobCreateDate = taskHostLogs.get(0).getJobCreateDate();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.SCRIPT);
        List<Document> scriptLogDocList = taskHostLogs.stream()
            .map(taskHostLog -> buildScriptLogDoc(taskHostLog.getScriptTaskLog())).collect(Collectors.toList());
        List<List<Document>> batchDocList = CollectionUtil.partitionList(scriptLogDocList, BATCH_SIZE);
        long start = System.currentTimeMillis();
        batchDocList.forEach(docs ->
            logCollectionFactory.getCollection(collectionName)
                .insertMany(docs, new InsertManyOptions().ordered(false)));
        long end = System.currentTimeMillis();
        log.info("Batch write script logs, docSize: {}, cost: {} ms", scriptLogDocList.size(), end - start);
    }


    private void batchWriteFileLogs(List<TaskHostLog> taskHostLogs) {
        String jobCreateDate = taskHostLogs.get(0).getJobCreateDate();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.FILE);

        List<WriteModel<Document>> updateOps = buildUpdateOpsFileTask(taskHostLogs);
        List<List<WriteModel<Document>>> batchList = CollectionUtil.partitionList(updateOps, BATCH_SIZE);

        long start = System.currentTimeMillis();
        batchList.forEach(batchOps -> logCollectionFactory.getCollection(collectionName)
            .bulkWrite(batchOps, new BulkWriteOptions().ordered(false)));
        long end = System.currentTimeMillis();
        log.warn("Batch write file logs, stepInstanceId: {}, opSize: {}, cost: {} ms",
            taskHostLogs.get(0).getStepInstanceId(), updateOps.size(), end - start);
    }

    private List<WriteModel<Document>> buildUpdateOpsFileTask(List<TaskHostLog> taskHostLogs) {
        List<WriteModel<Document>> updateOps = new ArrayList<>();
        taskHostLogs.forEach(taskHostLog -> {
            long stepInstanceId = taskHostLog.getStepInstanceId();
            String ip = taskHostLog.getIp();
            int executeCount = taskHostLog.getExecuteCount();
            Integer batch = taskHostLog.getBatch();
            List<FileTaskLogDoc> fileTaskLogs = taskHostLog.getFileTaskLogs();

            if (CollectionUtils.isNotEmpty(taskHostLog.getFileTaskLogs())) {
                fileTaskLogs.forEach(fileTaskLog -> {
                    BasicDBObject filter = buildQueryDocForFileTaskLog(stepInstanceId, executeCount, batch,
                        fileTaskLog);
                    BasicDBObject update = buildUpdateDocForFileTaskLog(stepInstanceId, executeCount, batch,
                        taskHostLog.getHostId(), ip, fileTaskLog);
                    UpdateOneModel<Document> updateOp = new UpdateOneModel<>(filter, update,
                        new UpdateOptions().upsert(true));
                    updateOps.add(updateOp);
                });
            }
        });
        return updateOps;
    }

    private void writeScriptLog(TaskHostLog taskHostLog) {
        if (taskHostLog == null || taskHostLog.getScriptTaskLog() == null) {
            return;
        }
        LogTypeEnum logType = LogTypeEnum.getLogType(taskHostLog.getLogType());

        long start = System.currentTimeMillis();
        long stepInstanceId = taskHostLog.getStepInstanceId();
        String collectionName = buildLogCollectionName(taskHostLog.getJobCreateDate(), logType);

        try {
            Document scriptLogDoc = buildScriptLogDoc(taskHostLog.getScriptTaskLog());
            logCollectionFactory.getCollection(collectionName).insertOne(scriptLogDoc);
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 20L) {
                log.warn("Save script task log slow, stepInstanceId: {}, cost: {} ms", stepInstanceId, cost);
            }
        }
    }

    private void writeFileLog(TaskHostLog taskHostLog) {
        if (taskHostLog.getFileTaskLogs().size() == 1) {
            taskHostLog.getFileTaskLogs().forEach(
                fileTaskLog -> writeFileLog(taskHostLog.getJobCreateDate(), taskHostLog.getStepInstanceId(),
                    taskHostLog.getExecuteCount(), taskHostLog.getBatch(), taskHostLog.getHostId(), taskHostLog.getIp(),
                    fileTaskLog));
        } else {
            batchWriteFileLogs(Collections.singletonList(taskHostLog));
        }
    }

    private Document buildScriptLogDoc(ScriptTaskLogDoc scriptTaskLog) {
        Document doc = new Document();
        doc.put(ScriptTaskLogDocField.STEP_ID, scriptTaskLog.getStepInstanceId());
        doc.put(ScriptTaskLogDocField.EXECUTE_COUNT, scriptTaskLog.getExecuteCount());
        if (scriptTaskLog.getBatch() != null && scriptTaskLog.getBatch() > 0) {
            doc.put(ScriptTaskLogDocField.BATCH, scriptTaskLog.getBatch());
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
                              Long hostId,
                              String ip,
                              FileTaskLogDoc fileTaskLog) {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.FILE);
        try {
            BasicDBObject filter = buildQueryDocForFileTaskLog(stepInstanceId, executeCount, batch, fileTaskLog);
            BasicDBObject update = buildUpdateDocForFileTaskLog(stepInstanceId, executeCount, batch, hostId, ip,
                fileTaskLog);
            logCollectionFactory.getCollection(collectionName)
                .updateOne(filter, update, new UpdateOptions().upsert(true));
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 10L) {
                log.warn("Save log slow, stepInstanceId:{}, cost: {} ms", stepInstanceId, cost);
            }
        }
    }

    private BasicDBObject buildQueryDocForFileTaskLog(long stepInstanceId, int executeCount, Integer batch,
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
                                                       Long hostId,
                                                       String ip,
                                                       FileTaskLogDoc fileTaskLog) {
        BasicDBObject update = new BasicDBObject();
        BasicDBObject setDBObject = new BasicDBObject();
        BasicDBObject pushDBObject = new BasicDBObject();
        setDBObject.append(FileTaskLogDocField.STEP_ID, stepInstanceId)
            .append(FileTaskLogDocField.EXECUTE_COUNT, executeCount)
            .append(FileTaskLogDocField.MODE, fileTaskLog.getMode())
            .append(FileTaskLogDocField.TASK_ID, fileTaskLog.getTaskId());
        if (hostId != null) {
            setDBObject.append(FileTaskLogDocField.HOST_ID, hostId);
        }
        if (StringUtils.isNotEmpty(ip)) {
            // tmp: 发布完成后不需要写入
            setDBObject.append(FileTaskLogDocField.IP, ip);
        }
        if (batch != null && batch > 0) {
            setDBObject.append(FileTaskLogDocField.BATCH, batch);
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
    public List<TaskHostLog> listScriptLogs(ScriptLogQuery scriptLogQuery) throws ServiceException {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(scriptLogQuery.getJobCreateDate(), LogTypeEnum.SCRIPT);

        try {
            Query query = buildScriptLogMongoQuery(scriptLogQuery);

            List<ScriptTaskLogDoc> scriptLogs = mongoTemplate.find(query, ScriptTaskLogDoc.class, collectionName);

            if (CollectionUtils.isEmpty(scriptLogs)) {
                return Collections.emptyList();
            }

            return groupScriptTaskLogsByHost(scriptLogQuery.getStepInstanceId(), scriptLogQuery.getExecuteCount(),
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
        List<String> ips = scriptLogQuery.getIps();
        List<Long> hostIds = scriptLogQuery.getHostIds();

        Query query = new Query();
        query.addCriteria(Criteria.where(ScriptTaskLogDocField.STEP_ID).is(stepInstanceId));
        query.addCriteria(Criteria.where(ScriptTaskLogDocField.EXECUTE_COUNT).is(executeCount));
        if (batch != null && batch > 0) {
            query.addCriteria(Criteria.where(ScriptTaskLogDocField.BATCH).is(batch));
        }
        if (CollectionUtils.isNotEmpty(hostIds)) {
            if (hostIds.size() == 1) {
                query.addCriteria(Criteria.where(ScriptTaskLogDocField.HOST_ID).is(hostIds.get(0)));
            } else {
                query.addCriteria(Criteria.where(ScriptTaskLogDocField.HOST_ID).in(hostIds));
            }
        } else if (CollectionUtils.isNotEmpty(ips)) {
            if (ips.size() == 1) {
                query.addCriteria(Criteria.where(ScriptTaskLogDocField.IP).is(ips.get(0)));
            } else {
                query.addCriteria(Criteria.where(ScriptTaskLogDocField.IP).in(ips));
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
        if (CollectionUtils.isNotEmpty(getLogRequest.getHostIds())) {
            if (getLogRequest.getHostIds().size() > 1) {
                query.addCriteria(Criteria.where(FileTaskLogDocField.HOST_ID).in(getLogRequest.getHostIds()));
            } else {
                query.addCriteria(Criteria.where(FileTaskLogDocField.HOST_ID).is(getLogRequest.getHostIds().get(0)));
            }
        } else if (CollectionUtils.isNotEmpty(getLogRequest.getIps())) {
            if (getLogRequest.getIps().size() > 1) {
                query.addCriteria(Criteria.where(FileTaskLogDocField.IP).in(getLogRequest.getIps()));
            } else {
                query.addCriteria(Criteria.where(FileTaskLogDocField.IP).is(getLogRequest.getIps().get(0)));
            }
        }
        if (getLogRequest.getBatch() != null && getLogRequest.getBatch() > 0) {
            query.addCriteria(Criteria.where(FileTaskLogDocField.BATCH).is(getLogRequest.getBatch()));
        }

        return query;
    }

    private List<TaskHostLog> groupScriptTaskLogsByHost(long stepInstanceId,
                                                        int executeCount,
                                                        Integer batch,
                                                        List<ScriptTaskLogDoc> scriptTaskLogs) {
        List<TaskHostLog> taskHostLogs = new ArrayList<>();
        boolean existHostIdField = scriptTaskLogs.get(0).getHostId() != null;
        if (existHostIdField) {
            Map<Long, List<ScriptTaskLogDoc>> scriptLogsGroups = new HashMap<>();
            scriptTaskLogs.forEach(scriptTaskLog -> {
                List<ScriptTaskLogDoc> scriptLogGroup = scriptLogsGroups.computeIfAbsent(scriptTaskLog.getHostId(),
                    k -> new ArrayList<>());
                scriptLogGroup.add(scriptTaskLog);
            });
            scriptLogsGroups.forEach(
                (hostId, scriptLogGroup) ->
                    taskHostLogs.add(
                        buildTaskHostLog(stepInstanceId, executeCount, batch, scriptLogGroup)));
        } else {
            Map<String, List<ScriptTaskLogDoc>> scriptLogsGroups = new HashMap<>();
            scriptTaskLogs.forEach(scriptTaskLog -> {
                List<ScriptTaskLogDoc> scriptLogGroup = scriptLogsGroups.computeIfAbsent(scriptTaskLog.getIp(),
                    k -> new ArrayList<>());
                scriptLogGroup.add(scriptTaskLog);
            });
            scriptLogsGroups.forEach(
                (ip, scriptLogGroup) ->
                    taskHostLogs.add(
                        buildTaskHostLog(stepInstanceId, executeCount, batch, scriptLogGroup)));
        }

        return taskHostLogs;
    }

    private TaskHostLog buildTaskHostLog(long stepInstanceId, int executeCount, Integer batch,
                                         List<ScriptTaskLogDoc> scriptLogs) {
        TaskHostLog taskHostLog = new TaskHostLog();
        taskHostLog.setStepInstanceId(stepInstanceId);
        taskHostLog.setExecuteCount(executeCount);
        taskHostLog.setBatch(batch);
        taskHostLog.setHostId(scriptLogs.get(0).getHostId());
        taskHostLog.setIp(scriptLogs.get(0).getIp());
        taskHostLog.setIpv6(scriptLogs.get(0).getIpv6());

        scriptLogs.sort(ScriptTaskLogDoc.LOG_OFFSET_COMPARATOR);
        taskHostLog.setScriptContent(scriptLogs.stream().map(ScriptTaskLogDoc::getContent).collect(Collectors.joining("")));

        return taskHostLog;
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
    public List<HostDTO> getIpsByKeyword(String jobCreateDate, long stepInstanceId, int executeCount,
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
}
