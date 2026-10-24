package org.jetbrains.skiko

/** Runs every release step while retaining the first failure for the owner. */
internal inline fun releaseRendererResource(previousFailure: Throwable?, action: () -> Unit): Throwable? = try {
    action()
    previousFailure
}
catch (failure: Throwable) {
    previousFailure ?: failure
}