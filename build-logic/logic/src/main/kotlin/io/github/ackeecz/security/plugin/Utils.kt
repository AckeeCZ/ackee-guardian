package io.github.ackeecz.security.plugin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import java.util.Optional

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

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

internal fun PluginManager.apply(provider: Optional<Provider<PluginDependency>>) {
    apply(provider.get().get().pluginId)
}

internal fun DependencyHandlerScope.testImplementation(provider: Optional<Provider<MinimalExternalModuleDependency>>) {
    testImplementation(provider.get())
}

internal fun DependencyHandlerScope.testImplementation(provider: Provider<MinimalExternalModuleDependency>) {
    add("testImplementation", provider.get())
}

internal fun DependencyHandlerScope.testRuntimeOnly(provider: Optional<Provider<MinimalExternalModuleDependency>>) {
    add("testRuntimeOnly", provider.get().get())
}
