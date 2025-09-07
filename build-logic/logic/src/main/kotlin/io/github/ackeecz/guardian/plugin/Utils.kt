package io.github.ackeecz.guardian.plugin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.plugin.use.PluginDependency

internal val Project.libs get() = the<org.gradle.accessors.dm.LibrariesForLibs>()

internal fun PluginManager.apply(plugin: Provider<PluginDependency>) {
    apply(plugin.get().pluginId)
}

internal fun Project.androidCommon(action: CommonExtension<*, *, *, *, *, *>.() -> Unit) {
    extensions.configure(CommonExtension::class, action)
}

internal fun Project.androidBase(action: BaseExtension.() -> Unit) {
    extensions.configure(BaseExtension::class, action)
}

internal fun Project.androidApp(action: ApplicationExtension.() -> Unit) {
    extensions.configure(ApplicationExtension::class, action)
}

internal fun Project.androidLibrary(action: LibraryExtension.() -> Unit) {
    extensions.configure(LibraryExtension::class, action)
}

internal fun DependencyHandlerScope.testImplementation(
    provider: Provider<MinimalExternalModuleDependency>,
    configure: ExternalModuleDependency.() -> Unit = {},
) {
    add("testImplementation", provider.get(), configure)
}

internal fun DependencyHandlerScope.testRuntimeOnly(
    provider: Provider<MinimalExternalModuleDependency>,
    configure: ExternalModuleDependency.() -> Unit = {},
) {
    add("testRuntimeOnly", provider.get(), configure)
}

internal fun DependencyHandlerScope.compileOnly(
    provider: Provider<MinimalExternalModuleDependency>,
    configure: ExternalModuleDependency.() -> Unit = {},
) {
    add("compileOnly", provider.get(), configure)
}

internal fun DependencyHandlerScope.implementation(
    provider: Provider<MinimalExternalModuleDependency>,
    configure: ExternalModuleDependency.() -> Unit = {},
) {
    add("implementation", provider.get(), configure)
}
