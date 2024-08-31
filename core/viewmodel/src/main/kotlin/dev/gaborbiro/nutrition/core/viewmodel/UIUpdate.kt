package dev.gaborbiro.nutrition.core.viewmodel


interface UIUpdate<T> {
    fun get(): T?
    fun peek(): T
}