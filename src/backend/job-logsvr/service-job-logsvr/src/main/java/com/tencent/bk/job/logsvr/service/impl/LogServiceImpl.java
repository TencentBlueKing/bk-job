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
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.BatchUtil;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.*;
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

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LogServiceImpl implements LogService {
    private static final int BATCH_SIZE = 100;
    private final MongoTemplate mongoTemplate;
    private final LogCollectionFactory logCollectionFactory;

    @Autowired
    public LogServiceImpl(MongoTemplate mongoTemplate, LogCollectionFactory logCollectionFactory) {
        this.mongoTemplate = mongoTemplate;
        this.logCollectionFactory = logCollectionFactory;
    }

    @Override
    public void saveLog(TaskIpLog taskIpLog) {
        if (taskIpLog.getLogType().equals(LogTypeEnum.SCRIPT.getValue())) {
            writeScriptLog(taskIpLog);
        } else if (taskIpLog.getLogType().equals(LogTypeEnum.FILE.getValue())) {
            writeFileLog(taskIpLog);
        }
    }

    @Override
    public void saveLogs(LogTypeEnum logType, List<TaskIpLog> taskIpLogs) {
        if (logType == LogTypeEnum.SCRIPT) {
            batchWriteScriptLogs(taskIpLogs);
        } else if (logType == LogTypeEnum.FILE) {
            batchWriteFileLogs(taskIpLogs);
        }
    }

    private void batchWriteScriptLogs(List<TaskIpLog> taskIpLogs) {
        String jobCreateDate = taskIpLogs.get(0).getJobCreateDate();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.SCRIPT);
        List<Document> scriptLogDocList = taskIpLogs.stream()
            .map(taskIpLog -> buildScriptLogDoc(taskIpLog.getScriptTaskLog())).collect(Collectors.toList());
        List<List<Document>> batchDocList = BatchUtil.buildBatchList(scriptLogDocList, BATCH_SIZE);
        long start = System.currentTimeMillis();
        batchDocList.parallelStream().forEach(docs -> {
            logCollectionFactory.getCollection(collectionName).insertMany(docs, new InsertManyOptions().ordered(false));
        });
        long end = System.currentTimeMillis();
        log.info("Batch write script logs, docSize: {}, cost: {} ms", scriptLogDocList.size(), end - start);
    }


    private void batchWriteFileLogs(List<TaskIpLog> taskIpLogs) {
        String jobCreateDate = taskIpLogs.get(0).getJobCreateDate();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.FILE);

        List<WriteModel<Document>> updateOps = buildUpdateOpsFileTask(taskIpLogs);
        List<List<WriteModel<Document>>> batchList = BatchUtil.buildBatchList(updateOps, BATCH_SIZE);

        long start = System.currentTimeMillis();
        batchList.parallelStream().forEach(batchOps -> {
            logCollectionFactory.getCollection(collectionName)
                .bulkWrite(batchOps, new BulkWriteOptions().ordered(false));
        });
        long end = System.currentTimeMillis();
        log.warn("Batch write file logs, stepInstanceId: {}, opSize: {}, cost: {} ms",
            taskIpLogs.get(0).getStepInstanceId(), updateOps.size(), end - start);
    }

    private List<WriteModel<Document>> buildUpdateOpsFileTask(List<TaskIpLog> taskIpLogs) {
        List<WriteModel<Document>> updateOps = new ArrayList<>();
        taskIpLogs.forEach(taskIpLog -> {
            long stepInstanceId = taskIpLog.getStepInstanceId();
            String ip = taskIpLog.getIp();
            int executeCount = taskIpLog.getExecuteCount();
            List<FileTaskLog> fileTaskLogs = taskIpLog.getFileTaskLogs();

            if (CollectionUtils.isNotEmpty(taskIpLog.getFileTaskLogs())) {
                fileTaskLogs.forEach(fileTaskLog -> {
                    BasicDBObject filter = buildQueryDocForFileTaskLog(stepInstanceId, executeCount, fileTaskLog);
                    BasicDBObject update = buildUpdateDocForFileTaskLog(stepInstanceId, executeCount, ip, fileTaskLog);
                    UpdateOneModel<Document> updateOp = new UpdateOneModel<>(filter, update,
                        new UpdateOptions().upsert(true));
                    updateOps.add(updateOp);
                });
            }
        });
        return updateOps;
    }

    private void writeScriptLog(TaskIpLog taskIpLog) {
        if (taskIpLog == null || taskIpLog.getScriptTaskLog() == null) {
            return;
        }
        LogTypeEnum logType = LogTypeEnum.getLogType(taskIpLog.getLogType());
        if (logType == null) {
            return;
        }

        long start = System.currentTimeMillis();
        long stepInstanceId = taskIpLog.getStepInstanceId();
        int executeCount = taskIpLog.getExecuteCount();
        String ip = taskIpLog.getIp();
        if (log.isDebugEnabled()) {
            log.debug("Save script log, stepInstanceId: {}, executeCount: {}, ip: {}", stepInstanceId, executeCount,
                ip);
        }
        String collectionName = buildLogCollectionName(taskIpLog.getJobCreateDate(), logType);

        try {
            Document scriptLogDoc = buildScriptLogDoc(taskIpLog.getScriptTaskLog());
            logCollectionFactory.getCollection(collectionName).insertOne(scriptLogDoc);
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 10L) {
                log.warn("Save log slow, stepInstanceId: {}, executeCount: {}, ip: {}, cost: {} ms", stepInstanceId,
                    executeCount, ip, cost);
            }
        }
    }

    private void writeFileLog(TaskIpLog taskIpLog) {
        long stepInstanceId = taskIpLog.getStepInstanceId();
        int executeCount = taskIpLog.getExecuteCount();
        String ip = taskIpLog.getIp();
        if (taskIpLog.getFileTaskLogs().size() == 1) {
            if (log.isDebugEnabled()) {
                log.debug("Save file log, stepInstanceId: {}, executeCount: {}, ip: {}", stepInstanceId, executeCount
                    , ip);
            }
            taskIpLog.getFileTaskLogs().parallelStream()
                .forEach(fileTaskLog -> writeFileLog(taskIpLog.getJobCreateDate(),
                    taskIpLog.getStepInstanceId(), taskIpLog.getExecuteCount(), taskIpLog.getIp(), fileTaskLog));
        } else {
            batchWriteFileLogs(Collections.singletonList(taskIpLog));
        }
    }

    private Document buildScriptLogDoc(ScriptTaskLog scriptTaskLog) {
        Document doc = new Document();
        doc.put("stepId", scriptTaskLog.getStepInstanceId());
        doc.put("executeCount", scriptTaskLog.getExecuteCount());
        doc.put("ip", scriptTaskLog.getIp());
        doc.put("content", scriptTaskLog.getContent());
        doc.put("offset", scriptTaskLog.getOffset());
        return doc;
    }

    private void writeFileLog(String jobCreateDate, long stepInstanceId, int executeCount, String ip,
                              FileTaskLog fileTaskLog) {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.FILE);
        try {
            BasicDBObject filter = buildQueryDocForFileTaskLog(stepInstanceId, executeCount, fileTaskLog);
            BasicDBObject update = buildUpdateDocForFileTaskLog(stepInstanceId, executeCount, ip, fileTaskLog);
            logCollectionFactory.getCollection(collectionName)
                .updateOne(filter, update, new UpdateOptions().upsert(true));
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 10L) {
                log.warn("Save log slow, stepInstanceId:{}, executeCount: {}, ip: {}, cost: {} ms", stepInstanceId,
                    executeCount, ip, cost);
            }
        }
    }

    private BasicDBObject buildQueryDocForFileTaskLog(long stepInstanceId, int executeCount,
                                                      FileTaskLog fileTaskLog) {
        BasicDBObject filter = new BasicDBObject();
        filter.append("stepId", stepInstanceId);
        filter.append("executeCount", executeCount);
        filter.append("taskId", fileTaskLog.getTaskId());
        return filter;
    }

    private BasicDBObject buildUpdateDocForFileTaskLog(long stepInstanceId, int executeCount, String ip,
                                                       FileTaskLog fileTaskLog) {
        BasicDBObject update = new BasicDBObject();
        BasicDBObject setDBObject = new BasicDBObject();
        BasicDBObject pushDBObject = new BasicDBObject();
        setDBObject.append("stepId", stepInstanceId)
            .append("executeCount", executeCount)
            .append("mode", fileTaskLog.getMode())
            .append("ip", ip)
            .append("taskId", fileTaskLog.getTaskId());
        if (StringUtils.isNotEmpty(fileTaskLog.getSrcIp())) {
            setDBObject.append("srcIp", fileTaskLog.getSrcIp());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getDisplaySrcIp())) {
            setDBObject.append("displaySrcIp", fileTaskLog.getDisplaySrcIp());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSrcFile())) {
            setDBObject.append("srcFile", fileTaskLog.getSrcFile());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getDisplaySrcFile())) {
            setDBObject.append("displaySrcFile", fileTaskLog.getDisplaySrcFile());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getDestFile())) {
            setDBObject.append("destFile", fileTaskLog.getDestFile());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSize())) {
            setDBObject.append("size", fileTaskLog.getSize());
        }
        if (fileTaskLog.getStatus() != null) {
            setDBObject.append("status", fileTaskLog.getStatus());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getStatusDesc())) {
            setDBObject.append("statusDesc", fileTaskLog.getStatusDesc());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getSpeed())) {
            setDBObject.append("speed", fileTaskLog.getSpeed());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getProcess())) {
            setDBObject.append("process", fileTaskLog.getProcess());
        }
        if (StringUtils.isNotEmpty(fileTaskLog.getContent())) {
            pushDBObject.append("contentList", fileTaskLog.getContent());
        }

        update.put("$set", setDBObject);
        if (pushDBObject.size() > 0) {
            update.put("$push", pushDBObject);
        }
        return update;
    }

    private String buildLogCollectionName(String jobCreateDate, LogTypeEnum logType) {
        return "job_log_" + logType.getName() + "_" + jobCreateDate;
    }

    @Override
    public TaskIpLog getScriptLogByIp(ScriptLogQuery query) {
        return getScriptTaskLogByIp(query);
    }

    @Override
    public List<TaskIpLog> batchGetScriptLogByIps(ScriptLogQuery query) throws ServiceException {
        return getScriptTaskLogByIps(query);
    }

    @Override
    public TaskIpLog getFileLogByIp(FileLogQuery query) {
        return getFileTaskLog(query);
    }

    private TaskIpLog getFileTaskLog(FileLogQuery getLogRequest) {
        long stepInstanceId = getLogRequest.getStepInstanceId();
        int executeCount = getLogRequest.getExecuteCount();
        String ip = getLogRequest.getIp();
        List<FileTaskLog> fileTaskLogs = getFileLogs(getLogRequest);
        TaskIpLog taskIpLog = new TaskIpLog();
        taskIpLog.setStepInstanceId(stepInstanceId);
        taskIpLog.setExecuteCount(executeCount);
        taskIpLog.setIp(ip);
        taskIpLog.setFileTaskLogs(fileTaskLogs);
        if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
            taskIpLog.getFileTaskLogs().forEach(fileTaskDetailLog ->
                fileTaskDetailLog.setContent(StringUtils.join(fileTaskDetailLog.getContentList(), null)));
        }
        return taskIpLog;
    }

    @Override
    public List<FileTaskLog> getFileLogs(FileLogQuery getLogRequest) {
        String collectionName = buildLogCollectionName(getLogRequest.getJobCreateDate(), LogTypeEnum.FILE);
        long stepInstanceId = getLogRequest.getStepInstanceId();
        int executeCount = getLogRequest.getExecuteCount();
        String ip = getLogRequest.getIp();

        long start = System.currentTimeMillis();
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("stepId").is(stepInstanceId));
            query.addCriteria(Criteria.where("executeCount").is(executeCount));
            if (getLogRequest.getMode() != null) {
                query.addCriteria(Criteria.where("mode").is(getLogRequest.getMode()));
            }
            if (StringUtils.isNotEmpty(getLogRequest.getIp())) {
                query.addCriteria(Criteria.where("ip").is(ip));
            }
            List<FileTaskLog> fileTaskLogs = mongoTemplate.find(query, FileTaskLog.class, collectionName);
            if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
                fileTaskLogs.forEach(fileTaskLog ->
                    fileTaskLog.setContent(StringUtils.join(fileTaskLog.getContentList(), null)));
            }
            return fileTaskLogs;
        } finally {
            long cost = (System.currentTimeMillis() - start);

            if (cost > 10L) {
                log.warn("Get file log slow, query: {}, cost: {} ms", getLogRequest, cost);
            }
        }
    }

    private TaskIpLog getScriptTaskLogByIp(ScriptLogQuery getLogRequest) {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(getLogRequest.getJobCreateDate(), LogTypeEnum.SCRIPT);
        long stepInstanceId = getLogRequest.getStepInstanceId();
        int executeCount = getLogRequest.getExecuteCount();
        String ip = getLogRequest.getIps().get(0);

        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("stepId").is(stepInstanceId));
            query.addCriteria(Criteria.where("executeCount").is(executeCount));
            query.addCriteria(Criteria.where("ip").is(ip));
            List<ScriptTaskLog> scriptLogs = mongoTemplate.find(query, ScriptTaskLog.class, collectionName);

            TaskIpLog taskIpLog = buildTaskIpLog(stepInstanceId, executeCount, ip, scriptLogs);

            if (log.isDebugEnabled()) {
                log.debug("Get log by ip, stepInstanceId: {}, executeCount: {}, ip: {}, scriptLogs: {}",
                    stepInstanceId, executeCount,
                    ip, scriptLogs);
            }
            taskIpLog.setScriptContent(scriptLogs.stream().map(ScriptTaskLog::getContent).collect(Collectors.joining(
                "")));
            return taskIpLog;
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 10L) {
                log.warn("Get script log by ip slow, stepInstanceId: {}, ip: {}, cost: {} ms", stepInstanceId, ip,
                    cost);
            }
        }
    }

    private List<TaskIpLog> getScriptTaskLogByIps(ScriptLogQuery getLogRequest) {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(getLogRequest.getJobCreateDate(), LogTypeEnum.SCRIPT);
        long stepInstanceId = getLogRequest.getStepInstanceId();
        int executeCount = getLogRequest.getExecuteCount();
        List<String> ips = getLogRequest.getIps();

        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("stepId").is(stepInstanceId));
            query.addCriteria(Criteria.where("executeCount").is(executeCount));
            query.addCriteria(Criteria.where("ip").in(ips));
            query.limit(1000);
            List<ScriptTaskLog> scriptLogs = mongoTemplate.find(query, ScriptTaskLog.class, collectionName);

            if (CollectionUtils.isEmpty(scriptLogs)) {
                return Collections.emptyList();
            }

            Map<String, List<ScriptTaskLog>> scriptTaskLogGroups = groupScriptTaskLogsByIp(scriptLogs);

            List<TaskIpLog> taskIpLogs = new ArrayList<>(scriptTaskLogGroups.size());
            scriptTaskLogGroups.forEach((ip, scriptLogGroup) -> {
                taskIpLogs.add(buildTaskIpLog(stepInstanceId, executeCount, ip, scriptLogGroup));
            });
            return taskIpLogs;
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 100L) {
                log.warn("Get script log by ips slow, stepInstanceId: {}, ip: {}, cost: {} ms", stepInstanceId, ips,
                    cost);
            }
        }
    }

    private Map<String, List<ScriptTaskLog>> groupScriptTaskLogsByIp(List<ScriptTaskLog> scriptTaskLogs) {
        Map<String, List<ScriptTaskLog>> scriptLogsGroups = new HashMap<>();
        scriptTaskLogs.forEach(scriptTaskLog -> {
            List<ScriptTaskLog> scriptLogGroup = scriptLogsGroups.computeIfAbsent(scriptTaskLog.getIp(),
                k -> new ArrayList<>());
            scriptLogGroup.add(scriptTaskLog);
        });
        return scriptLogsGroups;
    }

    private TaskIpLog buildTaskIpLog(long stepInstanceId, int executeCount, String ip, List<ScriptTaskLog> scriptLogs) {
        TaskIpLog taskIpLog = new TaskIpLog();
        taskIpLog.setStepInstanceId(stepInstanceId);
        taskIpLog.setExecuteCount(executeCount);
        taskIpLog.setIp(ip);

        scriptLogs.sort(ScriptTaskLog.LOG_OFFSET_COMPARATOR);
        taskIpLog.setScriptContent(scriptLogs.stream().map(ScriptTaskLog::getContent).collect(Collectors.joining("")));

        return taskIpLog;
    }

    @Override
    public List<FileTaskLog> getFileLogsByTaskIds(String jobCreateDate, long stepInstanceId, int executeCount,
                                                  List<String> taskIds) {
        long start = System.currentTimeMillis();
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.FILE);
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("stepId").is(stepInstanceId));
            query.addCriteria(Criteria.where("executeCount").is(executeCount));
            query.addCriteria(Criteria.where("taskId").in(taskIds));
            List<FileTaskLog> fileTaskLogs = mongoTemplate.find(query, FileTaskLog.class, collectionName);
            if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
                fileTaskLogs.forEach(taskTaskLog ->
                    taskTaskLog.setContent(StringUtils.join(taskTaskLog.getContentList(), null)));
            }
            return fileTaskLogs;
        } finally {
            long cost = (System.currentTimeMillis() - start);
            if (cost > 10L) {
                log.warn("Get file log by task ids slow, stepInstanceId: {}, executeCount: {}, taskIds: {} cost: {} ms",
                    stepInstanceId, executeCount, taskIds, cost);
            }
        }
    }

    @Override
    public long deleteStepContent(Long stepInstanceId, Integer executeCount, String jobCreateDate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("stepId").is(stepInstanceId));
        query.addCriteria(Criteria.where("executeCount").is(executeCount));
        DeleteResult deleteResult = mongoTemplate.remove(query, buildLogCollectionName(jobCreateDate,
            LogTypeEnum.SCRIPT));
        return deleteResult.getDeletedCount();
    }

    @Override
    public List<IpDTO> getIpsByKeyword(long stepInstanceId, Integer executeCount, String jobCreateDate,
                                       String keyword) {
        String collectionName = buildLogCollectionName(jobCreateDate, LogTypeEnum.SCRIPT);
        Query query = buildQueryForKeywordSearch(stepInstanceId, executeCount, keyword);
        query.fields().include("ip");
        List<ScriptTaskLog> logs = mongoTemplate.find(query, ScriptTaskLog.class, collectionName);
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logs.stream()
            .map(ScriptTaskLog::getIp)
            .distinct()
            .map(cloudIp -> new IpDTO(Long.valueOf(cloudIp.split(":")[0]), cloudIp.split(":")[1]))
            .collect(Collectors.toList());
    }

    private Query buildQueryForKeywordSearch(long stepInstanceId, int executeCount, String keyword) {
        Query query = new Query();
        query.addCriteria(Criteria.where("stepId").is(stepInstanceId));
        if (executeCount == 0) {
            query.addCriteria(Criteria.where("executeCount").is(executeCount));
        } else {
            query.addCriteria(Criteria.where("executeCount").lte(executeCount));
        }
        Pattern pattern = Pattern.compile(keyword.replaceAll("['$&|`;#]", ""),
            Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
        query.addCriteria(Criteria.where("content").regex(pattern));
        return query;
    }
}
