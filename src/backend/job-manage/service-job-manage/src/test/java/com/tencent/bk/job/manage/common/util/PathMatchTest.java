package com.tencent.bk.job.manage.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import static org.assertj.core.api.Assertions.assertThat;

public class PathMatchTest {

    @Test
    public void testPathMatch() {
        PathMatcher pathMatcher = new AntPathMatcher();
        // 正例
        assertThat(pathMatcher.match("/web/dangerousRule/**", "/web/dangerousRule/info/aa//"));
        assertThat(pathMatcher.match("/web/dangerousRule/**", "/web/dangerousRule/info/aa/"));
        assertThat(pathMatcher.match("/web/dangerousRule/**", "/web/dangerousRule/info/aa"));
        assertThat(pathMatcher.match("/web/dangerousRule/**", "/web/dangerousRule//info"));
        assertThat(pathMatcher.match("/web/dangerousRule/**", "/web/dangerousRule/info"));
        assertThat(pathMatcher.match("/web/dangerousRule/**", "/web/dangerousRule/"));
        assertThat(pathMatcher.match("/web/dangerousRule/**", "/web//dangerousRule/"));
        assertThat(pathMatcher.match("/web/dangerousRule/**", "/web/dangerousRule"));
        // 负例
        assertThat(!pathMatcher.match("/web/dangerousRule/**", "/web/dangerousRulf"));
    }
}
