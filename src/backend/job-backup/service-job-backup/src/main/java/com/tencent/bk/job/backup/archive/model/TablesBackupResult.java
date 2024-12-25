package com.tencent.bk.job.backup.archive.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TablesBackupResult {

    private Map<String, BackupResult> tables = new HashMap<>();

    public void add(String tableName, BackupResult backupResult) {
        this.tables.put(tableName, backupResult);
    }

}
