package com.son.boilerplate.core.enum

enum class WordCountTopics(val topic: String) {
    INPUT("streams-plaintext-input"),
    OUTPUT("streams-wordcount-output")
}

enum class AnotherTopologyTopics(val topic: String) {
    INPUT_1("input-topic-1"),
    OUTPUT("output-topic")
}
