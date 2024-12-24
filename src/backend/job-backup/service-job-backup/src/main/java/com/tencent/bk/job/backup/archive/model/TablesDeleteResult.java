package com.tencent.bk.job.backup.archive.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TablesDeleteResult {

    private Map<String, DeleteResult> tables = new HashMap<>();

    public void add(String tableName, DeleteResult deleteResult) {
        this.tables.put(tableName, deleteResult);
    }
}
