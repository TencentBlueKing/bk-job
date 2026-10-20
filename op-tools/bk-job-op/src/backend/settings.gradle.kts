pluginManagement {
    val pluginRepoUrl = providers.gradleProperty("gradlePluginRepoUrl")
        .orElse(providers.environmentVariable("GRADLE_PLUGIN_REPO_URL"))
        .orNull
        ?.takeIf { it.isNotBlank() }
    val pluginRepoUsername = providers.environmentVariable("GRADLE_PLUGIN_REPO_USERNAME")
        .orNull
        ?.takeIf { it.isNotBlank() }
    val pluginRepoPassword = providers.environmentVariable("GRADLE_PLUGIN_REPO_PASSWORD")
        .orNull
        ?.takeIf { it.isNotBlank() }

    if ((pluginRepoUsername == null) != (pluginRepoPassword == null)) {
        throw GradleException(
            "GRADLE_PLUGIN_REPO_USERNAME and GRADLE_PLUGIN_REPO_PASSWORD must be configured together"
        )
    }

    repositories {
        if (pluginRepoUrl != null) {
            clear()
            maven {
                name = "customGradlePluginRepository"
                url = uri(pluginRepoUrl)
                if (pluginRepoUsername != null && pluginRepoPassword != null) {
                    credentials {
                        username = pluginRepoUsername
                        password = pluginRepoPassword
                    }
                }
            }
        } else {
            gradlePluginPortal()
        }
    }
}

rootProject.name = "bk-job-op"
