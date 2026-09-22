package com.tencent.bk.job.controller.impl;

import com.tencent.bk.job.controller.IssueStatusResource;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import com.tencent.bk.job.service.model.PageData;
import com.tencent.bk.job.utils.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class IssueStatusResourceImpl implements IssueStatusResource {

    private static final String KEY_PREFIX = "job:issue:status:";

    /**
     * Issue状态数据在Redis中的过期时间，避免数据无限堆积
     */
    private static final Duration EXPIRE_DURATION = Duration.ofDays(30);

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    /**
     * 单次SCAN的COUNT，越大则往返次数越少、单次阻塞越久
     */
    private static final int SCAN_BATCH_SIZE = 500;

    /**
     * SCAN结果条数上限。超过后停止扫描，防止同前缀key意外暴涨时打爆OP内存。
     * 触发截断时 {@link PageData#getTotal()} 的语义退化为“截断后的近似值（真实总数的下界）”，
     * 而不是精确总数，同时会打WARN日志。
     */
    private static final int MAX_SCAN_KEYS = 10000;

    /**
     * id形如 issue_4523，按其中的数字降序排列，让新Issue排在前面
     */
    private static final Pattern ISSUE_ID_PATTERN = Pattern.compile("^issue_(\\d+)$");

    /**
     * 关键字长度上限，超出部分截断，避免构造超长参数
     */
    private static final int MAX_KEYWORD_LENGTH = 128;

    /**
     * id列表缓存的有效期。
     *
     * <p>{@code SCAN MATCH} 的开销是 O(整个keyspace) 而非 O(匹配到的key数)：MATCH 是服务端扫描之后
     * 再做过滤，游标必须走完整个keyspace。OP连接的是共用的PaaS Redis（database 0），keyspace规模不可控，
     * 而页面是3秒轮询、在线用户数又不确定，直接每次请求都扫一遍会把开销放大到不可预期。
     * 这里用一个很短的进程内缓存把扫描频率钳制在“每实例每1秒最多一次”，与3秒刷新的实时性要求相比
     * 误差可忽略，但把Redis侧开销与在线用户数解耦。</p>
     */
    private static final long ID_CACHE_TTL_MILLS = 1000L;

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 缓存的id列表（已排序）。只读使用，每次刷新整体替换引用，无需加锁
     */
    private volatile List<String> cachedIds = null;

    private volatile long cachedIdsTime = 0L;

    public IssueStatusResourceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public IssueStatusResp getIssueStatus(String id) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + id);
        if (value == null) {
            return new IssueStatusResp(id, null, null, null);
        }
        IssueStatusResp resp = JsonUtils.fromJson(value, IssueStatusResp.class);
        resp.setId(id);
        return resp;
    }

    @Override
    public IssueStatusResp setIssueStatus(SetIssueStatusReq req) {
        // 更新时间由服务端生成，不从请求体接收：调用方的时钟不可信
        IssueStatusResp resp = new IssueStatusResp(
            req.getId(),
            req.getStatus(),
            req.getStatusDescription(),
            System.currentTimeMillis()
        );
        redisTemplate.opsForValue().set(KEY_PREFIX + req.getId(), JsonUtils.toJson(resp), EXPIRE_DURATION);
        return resp;
    }

    @Override
    public PageData<IssueStatusResp> listIssueStatus(String idKeyword, Integer start, Integer pageSize) {
        // 只读查询接口，且被前端轮询调用，参数非法时静默纠正而不抛400，避免轮询期间参数抖动造成错误风暴
        int startIndex = (start == null || start < 0) ? 0 : start;
        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        String keyword = normalizeKeyword(idKeyword);

        List<String> ids = getSortedIssueIds();
        if (keyword != null) {
            ids = ids.stream()
                .filter(id -> StringUtils.containsIgnoreCase(id, keyword))
                .collect(Collectors.toList());
        }

        int total = ids.size();
        if (startIndex >= total) {
            return new PageData<>(total, startIndex, size, Collections.emptyList());
        }
        List<String> pageIds = ids.subList(startIndex, Math.min(startIndex + size, total));
        return new PageData<>(total, startIndex, size, loadByIds(pageIds));
    }

    private String normalizeKeyword(String idKeyword) {
        String keyword = StringUtils.trimToNull(idKeyword);
        if (keyword == null) {
            return null;
        }
        // 纯contains匹配，不解释正则与通配符
        return keyword.length() > MAX_KEYWORD_LENGTH ? keyword.substring(0, MAX_KEYWORD_LENGTH) : keyword;
    }

    /**
     * 只对当前页的id批量取值，避免把全量数据都读进内存
     */
    private List<IssueStatusResp> loadByIds(List<String> pageIds) {
        List<String> keys = pageIds.stream().map(id -> KEY_PREFIX + id).collect(Collectors.toList());
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        List<IssueStatusResp> records = new ArrayList<>(pageIds.size());
        for (int i = 0; i < pageIds.size(); i++) {
            String id = pageIds.get(i);
            String value = (values == null || i >= values.size()) ? null : values.get(i);
            if (value == null) {
                // 扫描与取值之间key已过期，降级为空状态，不让整个列表请求失败
                records.add(new IssueStatusResp(id, null, null, null));
                continue;
            }
            IssueStatusResp resp = JsonUtils.fromJson(value, IssueStatusResp.class);
            resp.setId(id);
            records.add(resp);
        }
        return records;
    }

    /**
     * 获取全量Issue标识，按Issue编号降序排列。结果带一个极短的进程内缓存
     */
    private List<String> getSortedIssueIds() {
        List<String> cached = cachedIds;
        if (cached != null && System.currentTimeMillis() - cachedIdsTime < ID_CACHE_TTL_MILLS) {
            return cached;
        }
        List<String> ids = scanAllIssueIds();
        // SCAN返回顺序不确定，必须整体排序，否则翻页时会出现记录重复或遗漏
        ids.sort(issueIdComparator());
        cachedIds = ids;
        cachedIdsTime = System.currentTimeMillis();
        return ids;
    }

    /**
     * 用SCAN游标扫描全部Issue状态key。严禁改用KEYS：KEYS是O(N)阻塞命令，
     * 在3秒轮询下会持续挤占Redis单线程，拖垮同一实例上所有依赖Redis的功能
     */
    private List<String> scanAllIssueIds() {
        ScanOptions options = ScanOptions.scanOptions()
            .match(KEY_PREFIX + "*")
            .count(SCAN_BATCH_SIZE)
            .build();
        // SCAN语义上允许返回重复key，这里用LinkedHashSet去重
        Set<String> ids = new LinkedHashSet<>();
        boolean truncated = false;
        // Cursor必须关闭：连接池max-active只有8且max-wait为1ms，泄漏几条连接就会让所有Redis操作报错
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                if (ids.size() >= MAX_SCAN_KEYS) {
                    truncated = true;
                    break;
                }
                ids.add(cursor.next().substring(KEY_PREFIX.length()));
            }
        }
        if (truncated) {
            log.warn(
                "Issue status key count reaches limit {}, the list result and total are truncated approximations",
                MAX_SCAN_KEYS
            );
        }
        return new ArrayList<>(ids);
    }

    /**
     * 能解析出Issue编号的按编号降序在前，其余按字符串升序排在后面，保证全序、翻页稳定
     */
    private Comparator<String> issueIdComparator() {
        return Comparator.comparing((String id) -> parseIssueNo(id) == null)
            .thenComparing(
                id -> parseIssueNo(id) == null ? 0L : parseIssueNo(id),
                Comparator.reverseOrder()
            )
            .thenComparing(Comparator.naturalOrder());
    }

    private Long parseIssueNo(String id) {
        Matcher matcher = ISSUE_ID_PATTERN.matcher(id);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
