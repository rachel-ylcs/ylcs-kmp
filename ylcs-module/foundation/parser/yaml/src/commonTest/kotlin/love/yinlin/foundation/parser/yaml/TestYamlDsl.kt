package love.yinlin.foundation.parser.yaml

import kotlin.test.*

class TestYamlDsl {
    @Test
    fun jsonStyleBuildersAndCollectionInterfaces() {
        val root = yaml {
            "name" with "demo"
            "nullable" with null
            "enabled" with true
            "count" with 42L
            obj("server") { "port" with 8080 }
            arr("items") {
                add("first")
                add(2)
                add(false)
                add(null)
                obj { "key" with "value" }
            }
        }
        val list = root.arr("items")
        assertEquals(6, root.size)
        assertEquals(5, list.size)
        assertEquals(42L, root["count"].Long)
        assertSame(YamlNull, list[3])
        assertNull(list[3].StringNull)
        assertEquals("value", list.last().Object["key"].String)
        assertEquals(8080, root.obj("server")["port"].Int)
        assertFailsWith<Throwable> { root["name"].Object }
    }

    @Test
    fun mergeHelpersAndTypedNulls() {
        val text: String? = null
        val flag: Boolean? = null
        val number: Number? = null
        val root = buildYamlObject {
            merge(buildYamlObject { "a" with 1 })
            "text" with text
            "flag" with flag
            "number" with number
            arr("items") {
                merge(buildYamlArray {
                    add(1)
                    add(2)
                })
                arr {
                    add("nested")
                }
            }
        }
        assertSame(YamlNull, root["text"])
        assertSame(YamlNull, root["flag"])
        assertSame(YamlNull, root["number"])
        assertSame(YamlNull, YamlPrimitive(null))
        assertEquals("nested", root.arr("items")[2].Array[0].String)
        assertEquals(root, Yaml.parse(Yaml.encodeToString(root)))
        assertEquals(root.hashCode(), Yaml.parse(root.toString()).hashCode())
    }
}
