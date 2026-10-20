package com.tencent.bk.job.controller.impl;

import com.tencent.bk.job.config.web.WebAuthInterceptor;
import com.tencent.bk.job.controller.IssueStatusResource;
import com.tencent.bk.job.dao.IssueStatusMapper;
import com.tencent.bk.job.model.DeleteIssueStatusReq;
import com.tencent.bk.job.model.DeleteIssueStatusResp;
import com.tencent.bk.job.model.IssueStatusDTO;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import com.tencent.bk.job.service.model.PageData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class IssueStatusResourceImpl implements IssueStatusResource {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 200;

    /**
     * 关键字长度上限，超出部分截断，避免构造超长参数
     */
    private static final int MAX_KEYWORD_LENGTH = 128;

    private final IssueStatusMapper issueStatusMapper;

    public IssueStatusResourceImpl(IssueStatusMapper issueStatusMapper) {
        this.issueStatusMapper = issueStatusMapper;
    }

    @Override
    public IssueStatusResp getIssueStatus(String id) {
        IssueStatusDTO dto = issueStatusMapper.selectById(id);
        if (dto == null) {
            return new IssueStatusResp(id, null, null, null);
        }
        return toResp(dto);
    }

    @Override
    public IssueStatusResp setIssueStatus(SetIssueStatusReq req) {
        // 更新时间由服务端生成，不从请求体接收：调用方的时钟不可信
        IssueStatusDTO dto = new IssueStatusDTO(
            req.getId(),
            req.getStatus(),
            req.getStatusDescription(),
            System.currentTimeMillis()
        );
        issueStatusMapper.upsert(dto);
        return toResp(dto);
    }

    @Override
    public PageData<IssueStatusResp> listIssueStatus(String idKeyword, Integer start, Integer pageSize) {
        // 只读查询接口，且被前端轮询调用，参数非法时静默纠正而不抛400，避免轮询期间参数抖动造成错误风暴
        int startIndex = (start == null || start < 0) ? 0 : start;
        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        String keyword = normalizeKeyword(idKeyword);

        int total = issueStatusMapper.countByKeyword(keyword);
        if (startIndex >= total) {
            return new PageData<>(total, startIndex, size, Collections.emptyList());
        }
        List<IssueStatusResp> records = issueStatusMapper.listByKeyword(keyword, startIndex, size).stream()
            .map(this::toResp)
            .collect(Collectors.toList());
        return new PageData<>(total, startIndex, size, records);
    }

    @Override
    public DeleteIssueStatusResp deleteIssueStatus(DeleteIssueStatusReq req) {
        String id = req == null ? null : StringUtils.trimToNull(req.getId());
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        int deleted = issueStatusMapper.deleteById(id);
        log.info("Delete issue status, username={}, id={}, deleted={}", getCurrentUsername(), id, deleted);
        return new DeleteIssueStatusResp(deleted);
    }

    private String getCurrentUsername() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return "<unknown>";
        }
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        Object username = request.getAttribute(WebAuthInterceptor.ATTR_USERNAME);
        return StringUtils.defaultIfBlank(username == null ? null : username.toString(), "<unknown>");
    }

    private IssueStatusResp toResp(IssueStatusDTO dto) {
        return new IssueStatusResp(dto.getId(), dto.getStatus(), dto.getStatusDescription(), dto.getUpdateTime());
    }

    /**
     * 归一化关键字：空白视为不过滤，超长截断，并转义LIKE元字符使其退化为纯contains匹配
     */
    private String normalizeKeyword(String idKeyword) {
        String keyword = StringUtils.trimToNull(idKeyword);
        if (keyword == null) {
            return null;
        }
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        return escapeLike(keyword);
    }

    /**
     * 转义LIKE模式中的元字符。转义符本身必须最先替换，否则会把后续替换产生的反斜杠再转义一遍
     */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

}
