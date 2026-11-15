package com.viaword.demo.data

import com.viaword.demo.R
import com.viaword.demo.practice.Question

internal object AppCatalog {
    val title: String? = null
    val units = sampleUnits
    val audio = mapOf(
        "sample-1-01" to R.raw.sample_1_01,
        "sample-1-02" to R.raw.sample_1_02,
        "sample-1-03" to R.raw.sample_1_03,
        "sample-1-04" to R.raw.sample_1_04,
        "sample-1-05" to R.raw.sample_1_05,
        "sample-1-06" to R.raw.sample_1_06,
        "sample-2-01" to R.raw.sample_2_01,
        "sample-2-02" to R.raw.sample_2_02,
        "sample-2-03" to R.raw.sample_2_03,
    )
    val aliases = emptyMap<Question, List<String>>()
}
