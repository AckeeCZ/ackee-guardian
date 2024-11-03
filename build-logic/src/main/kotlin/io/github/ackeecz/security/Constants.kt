package io.github.ackeecz.security

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object Constants {

    const val COMPILE_SDK = 35
    const val MIN_SDK = 24
    const val TARGET_SDK = 35

    val JAVA_VERSION = JavaVersion.VERSION_11
    val JVM_TARGET = JvmTarget.JVM_11
}
