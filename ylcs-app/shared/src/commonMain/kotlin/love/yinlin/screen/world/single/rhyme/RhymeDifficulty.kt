package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable

@Stable
enum class RhymeDifficulty(val title: String) {
    Easy("简单"),
    Medium("普通"),
    Hard("困难"),
    Extra("极限");
}