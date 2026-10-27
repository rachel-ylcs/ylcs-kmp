package androidx.navigation3.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent.SwipeEdge

actual fun <T : Any> defaultTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(EnterTransition.None, ExitTransition.None)
}

actual fun <T : Any> defaultPopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(EnterTransition.None, ExitTransition.None)
}

actual fun <T : Any> defaultPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<T>>.(@SwipeEdge Int) -> ContentTransform = {
    ContentTransform(EnterTransition.None, ExitTransition.None)
}
