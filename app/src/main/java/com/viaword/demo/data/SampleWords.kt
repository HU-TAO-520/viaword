package com.viaword.demo.data

data class WordUnit(val id: String, val title: String, val entries: List<WordEntry>)

data class WordEntry(
    val id: String,
    val english: String,
    val ipa: String,
    val meaningZhHans: String,
    val partOfSpeech: String?,
)

val sampleUnits = listOf(
    WordUnit("sample-unit-1", "Unit 1", listOf(
        WordEntry("sample-1-01", "protect", "/prəˈtekt/", "保护", "动词"),
        WordEntry("sample-1-02", "prepare", "/prɪˈpeə/", "准备", "动词"),
        WordEntry("sample-1-03", "share", "/ʃeə/", "分享", "动词"),
        WordEntry("sample-1-04", "library", "/ˈlaɪbrəri/", "图书馆", "名词"),
        WordEntry("sample-1-05", "improve", "/ɪmˈpruːv/", "改善", "动词"),
        WordEntry("sample-1-06", "together", "/təˈɡeðə/", "一起", "副词"),
    )),
    WordUnit("sample-unit-2", "Unit 2", listOf(
        WordEntry("sample-2-01", "notice", "/ˈnəʊtɪs/", "注意到", "动词"),
        WordEntry("sample-2-02", "useful", "/ˈjuːsfəl/", "有用的", "形容词"),
        WordEntry("sample-2-03", "take care of", "/teɪk keə əv/", "照顾", null),
    )),
)
