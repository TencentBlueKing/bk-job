/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.controller;

import com.tencent.bk.job.controller.api.IssueStatusResource;
import com.tencent.bk.job.controller.api.MigrateBizResource;
import com.tencent.bk.job.controller.api.PullRequestResource;
import com.tencent.bk.job.controller.api.ReleaseStatusResource;
import com.tencent.bk.job.controller.api.RepoTagResource;
import com.tencent.bk.job.controller.api.VersionBranchResource;
import com.tencent.bk.job.controller.open.CheckServiceDependencyResource;
import com.tencent.bk.job.controller.web.WebIssueStatusResource;
import com.tencent.bk.job.controller.web.WebPullRequestResource;
import com.tencent.bk.job.controller.web.WebUserResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMappingTest {

    @Test
    @DisplayName("API Resource统一位于api包并使用/api命名空间")
    void shouldMapApiResources() {
        assertResource(IssueStatusResource.class, "api", Set.of("/api/issueStatus"));
        assertResource(PullRequestResource.class, "api", Set.of(
            "/api/pullRequest", "/api/pullRequest/status"
        ));
        assertThat(resolveMethodPaths(PullRequestResource.class)).containsExactlyInAnyOrder(
            "POST /api/pullRequest",
            "GET /api/pullRequest",
            "POST /api/pullRequest/status"
        ).doesNotContain("POST /api/pullRequest/delete");
        assertResource(ReleaseStatusResource.class, "api", Set.of("/api/releaseStatus"));
        assertResource(VersionBranchResource.class, "api", Set.of(
            "/api/versionBranch", "/api/versionBranch/list",
            "/api/versionBranch/update", "/api/versionBranch/delete"
        ));
        assertResource(RepoTagResource.class, "api", Set.of(
            "/api/repoTag/batchAddTags", "/api/repoTag/batchDeleteTags",
            "/api/repoTag/listTags", "/api/repoTag/nextTag"
        ));
        assertResource(MigrateBizResource.class, "api", Set.of(
            "/api/migrate/add_migrate_biz", "/api/migrate/delete_migrate_biz", "/api/migrate/is_migrated"
        ));
    }

    @Test
    @DisplayName("Web Resource统一位于web包并使用/web命名空间")
    void shouldMapWebResources() {
        assertResource(WebIssueStatusResource.class, "web", Set.of(
            "/web/issueStatus/list", "/web/issueStatus/delete"
        ));
        assertResource(WebPullRequestResource.class, "web", Set.of(
            "/web/pullRequest/list", "/web/pullRequest/delete"
        ));
        assertThat(resolveMethodPaths(WebPullRequestResource.class))
            .containsExactlyInAnyOrder(
                "GET /web/pullRequest/list",
                "POST /web/pullRequest/delete"
            );
        assertResource(WebUserResource.class, "web", Set.of("/web/userInfo"));
    }

    @Test
    @DisplayName("公开Resource位于open包并使用/public命名空间")
    void shouldMapOpenResources() {
        assertResource(CheckServiceDependencyResource.class, "open",
            Set.of("/public/checkServiceDependency"));
    }

    private void assertResource(Class<?> resourceType, String packageSegment, Set<String> expectedPaths) {
        assertThat(resourceType.getPackageName())
            .startsWith("com.tencent.bk.job.controller." + packageSegment);
        assertThat(resolvePaths(resourceType)).containsExactlyInAnyOrderElementsOf(expectedPaths);
    }

    private Set<String> resolvePaths(Class<?> resourceType) {
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(resourceType, RequestMapping.class);
        assertThat(classMapping).isNotNull();
        String prefix = firstPath(classMapping);
        Set<String> paths = new LinkedHashSet<>();
        for (Method method : resourceType.getMethods()) {
            RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (methodMapping != null) {
                paths.add(prefix + firstPath(methodMapping));
            }
        }
        return paths;
    }

    private Set<String> resolveMethodPaths(Class<?> resourceType) {
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(resourceType, RequestMapping.class);
        assertThat(classMapping).isNotNull();
        String prefix = firstPath(classMapping);
        Set<String> methodPaths = new LinkedHashSet<>();
        for (Method method : resourceType.getMethods()) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping != null) {
                RequestMethod httpMethod = mapping.method()[0];
                methodPaths.add(httpMethod.name() + " " + prefix + firstPath(mapping));
            }
        }
        return methodPaths;
    }

    private String firstPath(RequestMapping mapping) {
        String[] paths = mapping.path().length == 0 ? mapping.value() : mapping.path();
        return paths.length == 0 ? "" : paths[0];
    }
}
