package com.gg.example.lyrics

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

/** Captured outputs from the pre-refactor plugin, including known bugs. These assert compatibility, not correctness. */
class LyricCompatibilityTest {
    @TestFactory
    fun `retains the six reviewed edge case outputs`(): List<DynamicTest> {
        val source = requireNotNull(javaClass.getResource("/lyrics/legacy-output.json")).readText()
        return JsonParser.parseString(source).asJsonArray.map { element ->
            val case = element.asJsonObject
            DynamicTest.dynamicTest(case.get("name").asString) {
                val payload = LyricPayload(
                    original = case.get("lrc").asString,
                    translation = case.get("translation").asString,
                    wordSynced = case.get("yrc").asString,
                )
                assertEquals(case.get("output").asString, LyricMerger().merge(payload))
            }
        }
    }
}
