package io.github.ackeecz.guardian.util

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

public object Constants {

    public const val COMPILE_SDK: Int = 35
    public const val MIN_SDK: Int = 24
    public const val TARGET_SDK: Int = 35

    public val JAVA_VERSION: JavaVersion = JavaVersion.VERSION_11
    public val JVM_TARGET: JvmTarget = JvmTarget.JVM_11

    public const val ACKEE_TASKS_GROUP: String = "ackee"
}
