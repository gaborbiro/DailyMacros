package dev.gaborbiro.dailymacros.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

fun <T1, T2> Flow<T1>.combine(flow: Flow<T2>): Flow<Pair<T1, T2>> {
    return combine(flow) { a: T1, b: T2 -> Pair(a, b) }
}