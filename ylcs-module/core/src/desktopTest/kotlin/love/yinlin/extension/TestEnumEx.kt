package love.yinlin.extension

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestEnumEx {
    enum class Foo(val text: String) {
        A("1"), B("23"), C("456");
    }

    @Test
    fun testEnum() {
        assertEquals(Foo.A, enum(0))
        assertEquals(Foo.B, enum<Foo>(1))
        assertFailsWith<IndexOutOfBoundsException> { enum<Foo>(3)  }
        assertEquals(Foo.C, enum(-1, Foo.C))

        assertEquals(Foo.B, enum { it.text.length == 2 })
        assertEquals(Foo.C, enum(Foo.C) { false })

        assertEquals(Foo.B, enum("23") { it.text })
        assertEquals(Foo.C, enum("", Foo.C) { it.text })
    }
}