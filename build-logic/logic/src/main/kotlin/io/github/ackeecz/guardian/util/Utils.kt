package io.github.ackeecz.guardian.util

import org.gradle.api.Task

public fun Class<out Task>.getTaskName(): String {
    return simpleName
        .substringBeforeLast("Task")
        .lowercaseFirstChar()
}

public fun String.lowercaseFirstChar(): String {
    return if (isEmpty()) {
        this
    } else {
        this[0].lowercaseChar() + substring(1)
    }
}
