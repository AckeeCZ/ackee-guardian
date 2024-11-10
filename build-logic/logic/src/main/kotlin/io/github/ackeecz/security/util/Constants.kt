package io.github.ackeecz.security.util

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

public object Constants {

    public const val COMPILE_SDK: Int = 35
    public const val MIN_SDK: Int = 24
    public const val TARGET_SDK: Int = 35

    public val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_11
    public val JVM_TARGET: JvmTarget = JvmTarget.JVM_11
}
