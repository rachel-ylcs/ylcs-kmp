package love.yinlin.common.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
enum class RhymeDifficulty(val title: String) {
    Easy("简单"),
    Medium("普通"),
    Hard("困难"),
    Extra("极限");
}