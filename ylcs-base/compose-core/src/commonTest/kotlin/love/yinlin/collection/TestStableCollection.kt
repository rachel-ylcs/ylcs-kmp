package love.yinlin.collection

import love.yinlin.extension.parseJson
import love.yinlin.extension.to
import love.yinlin.extension.toJsonString
import kotlin.test.Test
import kotlin.test.assertEquals

class TestStableCollection {
    @Test
    fun testSerialize() {
        run {
            val list = listOf("123", "456", "789")
            val stableList = stableListOf("123", "456", "789")
            val jsonString = stableList.toJsonString()
            val json = jsonString.parseJson
            val value1 = json.to<StableList<String>>()
            val value2 = json.to<List<String>>()
            assertEquals(jsonString, list.toJsonString())
            assertEquals(stableList, list)
            assertEquals(stableList, value1)
            assertEquals(stableList, value2)
            assertEquals(value1, value2)
        }

        run {
            val map = mapOf("123" to 1, "456" to 2, "789" to 3)
            val stableMap = stableMapOf("123" to 1, "456" to 2, "789" to 3)
            val jsonString = stableMap.toJsonString()
            val json = jsonString.parseJson
            val value1 = json.to<StableMap<String, Int>>()
            val value2 = json.to<Map<String, Int>>()
            assertEquals(jsonString, map.toJsonString())
            assertEquals(stableMap, map)
            assertEquals(stableMap, value1)
            assertEquals(stableMap, value2)
            assertEquals(value1, value2)
        }

        run {
            val set = setOf("123", "456", "789")
            val stableSet = stableSetOf("123", "456", "789")
            val jsonString = stableSet.toJsonString()
            val json = jsonString.parseJson
            val value1 = json.to<StableSet<String>>()
            val value2 = json.to<Set<String>>()
            assertEquals(jsonString, set.toJsonString())
            assertEquals(stableSet, set)
            assertEquals(stableSet, value1)
            assertEquals(stableSet, value2)
            assertEquals(value1, value2)
        }
    }
}