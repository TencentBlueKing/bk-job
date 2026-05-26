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

package com.tencent.bk.job.crontab.passwordrotation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldBatchRow;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldRewriter;
import com.tencent.bk.job.common.crypto.passwordrotation.ReEncryptResult;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.tables.CronJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * cron_job 表 variable_value 字段密码轮换。
 *
 * <p>variable_value 是 {@code List<CronJobVariableDTO>} 的 JSON 字符串，
 * 其中 type=CIPHER 的元素的 value 才是真正的密文。
 * 因此需要重写 {@link #reEncryptToActive}，
 * 对每个 CIPHER 元素分别做重加密后再序列化为 JSON。
 */
@Slf4j
@Component
@Qualifier("jobCrontabPasswordRotationRewriter")
public class CronJobVariableValueFieldRewriter implements FieldRewriter {

    private static final CronJob TB_CRON_JOB = CronJob.CRON_JOB;

    private static final TypeReference<List<CronJobVariableDTO>> VARIABLE_LIST_TYPE_REF =
        new TypeReference<List<CronJobVariableDTO>>() {
        };

    private final DSLContext ctx;

    @Autowired
    public CronJobVariableValueFieldRewriter(@Qualifier("job-crontab-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String tableName() {
        return TB_CRON_JOB.getName();
    }

    @Override
    public String fieldName() {
        return TB_CRON_JOB.VARIABLE_VALUE.getName();
    }

    @Override
    public List<FieldBatchRow> fetchBatch(String lastProcessedPkCursor, int batchSize) {
        SelectConditionStep<Record2<ULong, String>> query = ctx
            .select(TB_CRON_JOB.ID, TB_CRON_JOB.VARIABLE_VALUE)
            .from(TB_CRON_JOB)
            .where(TB_CRON_JOB.VARIABLE_VALUE.isNotNull());
        if (lastProcessedPkCursor != null) {
            query = query.and(TB_CRON_JOB.ID.lt(ULong.valueOf(Long.parseLong(lastProcessedPkCursor))));
        }
        Result<Record2<ULong, String>> records = query
            .orderBy(TB_CRON_JOB.ID.desc())
            .limit(batchSize)
            .fetch();

        List<FieldBatchRow> rows = new ArrayList<>(records.size());
        for (Record2<ULong, String> r : records) {
            rows.add(new FieldBatchRow(
                String.valueOf(r.get(TB_CRON_JOB.ID).longValue()), r.get(TB_CRON_JOB.VARIABLE_VALUE)));
        }
        return rows;
    }

    @Override
    public int updateRow(String pkCursor, String oldCipher, String newCipher) {
        long pk = Long.parseLong(pkCursor);
        return ctx.update(TB_CRON_JOB)
            .set(TB_CRON_JOB.VARIABLE_VALUE, newCipher)
            .where(TB_CRON_JOB.ID.eq(ULong.valueOf(pk)))
            .and(TB_CRON_JOB.VARIABLE_VALUE.eq(oldCipher))
            .execute();
    }

    @Override
    public long countRemaining() {
        // 与 fetchBatch 同样的过滤条件，统计本表本字段在主密钥指纹下需要迁移的总行数
        Long count = ctx.selectCount()
            .from(TB_CRON_JOB)
            .where(TB_CRON_JOB.VARIABLE_VALUE.isNotNull())
            .fetchOne(0, Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public ReEncryptResult reEncryptToActive(SymmetricCryptoService svc, String value) {
        if (StringUtils.isBlank(value) || "null".equals(value)) {
            // 空值不应被 orchestrator 调到这里（已在 processSingleRow 提前拦截），保险起见返回 unchanged
            return ReEncryptResult.unchanged();
        }
        List<CronJobVariableDTO> vars = parseSafely(value);
        if (CollectionUtils.isEmpty(vars)) {
            // 解析失败或空数组：不发起 UPDATE，避免乱写
            return ReEncryptResult.unchanged();
        }
        boolean changed = false;
        for (CronJobVariableDTO var : vars) {
            if (var == null || var.getType() != TaskVariableTypeEnum.CIPHER) {
                continue;
            }
            String v = var.getValue();
            if (StringUtils.isBlank(v)) {
                continue;
            }
            // 子字段走"上一次密码优先"的迁移试错链；不再做子项级"主密钥单次解密判定"的 fast path：
            // 进度表 + 行级落库保证整行原子状态，混合"部分已 active + 部分未 active"的情况几乎不存在，
            // 边界数据被试错链末位的主密钥兜底命中后会做一次幂等无害的重加密
            String reEncrypted = svc.reEncryptToActiveForRotation(v);
            var.setValue(reEncrypted);
            changed = true;
        }
        // 整行内没有任何 CIPHER 子项需要重加密 -> 跳过 UPDATE，避免以原值更新触发无意义的乐观锁 + binlog
        return changed ? ReEncryptResult.changed(JsonUtils.toJson(vars)) : ReEncryptResult.unchanged();
    }

    private List<CronJobVariableDTO> parseSafely(String json) {
        try {
            return JsonUtils.fromJson(json, VARIABLE_LIST_TYPE_REF);
        } catch (Exception e) {
            log.warn("cron_job.variable_value parse failed, skip. value={}", json, e);
            return null;
        }
    }
}
