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

package com.tencent.bk.gradle.optional;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

public class OptionalDependenciesPlugin implements Plugin<Project> {

    public static final String OPTIONAL_CONFIGURATION_NAME = "optional";

    @Override
    public void apply(Project project) {
        Configuration optional = project.getConfigurations().create(OPTIONAL_CONFIGURATION_NAME);
        optional.attributes((attribute) -> {
            attribute.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
        });

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            SourceSetContainer sourceSets =
                project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
            sourceSets.all(sourceSet -> {
                sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(optional));
                sourceSet.setRuntimeClasspath(sourceSet.getRuntimeClasspath().plus(optional));
            });

            project.getTasks().withType(Javadoc.class)
                .all(javadoc -> javadoc.setClasspath(javadoc.getClasspath().plus(optional)));
        });

        project.getPlugins().withType(EclipsePlugin.class,
            (eclipsePlugin) -> project.getExtensions().getByType(EclipseModel.class)
                .classpath((classpath) -> classpath.getPlusConfigurations().add(optional)));
    }
}
