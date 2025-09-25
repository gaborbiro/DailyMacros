package dev.gaborbiro.dailymacros.features.common

object ThreeWordId {
    private val fruits = listOf(
        "apple", "banana", "cherry", "orange", "lemon", "mango",
        "peach", "grape", "kiwi", "melon", "berry", "coconut"
    )

    private val nature = listOf(
        "forest", "river", "mountain", "valley", "desert", "ocean",
        "island", "beach", "meadow", "canyon", "jungle", "cave",
        "breeze", "pond", "cliff", "stream", "garden", "field"
    )

    private val sky = listOf(
        "sun", "moon", "star", "sky", "cloud", "storm",
        "rain", "snow", "frost", "flame", "stone", "shadow"
    )

    private val animals = listOf(
        "cat", "dog", "fox", "bear", "wolf", "owl",
        "eagle", "dolphin", "turtle", "rabbit", "panda", "koala",
        "lion", "tiger", "zebra", "giraffe", "horse", "sheep",
        "cow", "pig", "duck", "goose", "chicken", "mouse"
    )

    private val food = listOf(
        "coffee", "tea", "sugar", "honey", "cocoa", "cookie",
        "bread", "butter", "cheese", "milk", "cake", "pie",
        "pasta", "noodle", "soup", "pizza", "burger", "taco"
    )

    private val cozy = listOf(
        "candle", "book", "pen", "pencil", "paper", "lamp",
        "chair", "table", "cup", "plate", "clock", "watch",
        "shoe", "hat", "ring", "key"
    )

    private val categories = listOf(fruits, nature, sky, animals, food, cozy)

    fun random(): String {
        val chosenCategories = categories.shuffled().take(3)
        val words = chosenCategories.map { it.random() }
        return words.joinToString("-")
    }
}
