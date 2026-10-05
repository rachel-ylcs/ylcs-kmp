package love.yinlin.foundation.cryptography

class RC4(private val key: ByteArray): Cipher() {
    constructor(key: String) : this(key.encodeToByteArray())

    init {
        require(key.isNotEmpty()) { "RC4 key cannot be empty." }
    }

    // KSA
    private val initialState: IntArray = run {
        val state = IntArray(256)

        var i = 0
        while (i < 256) {
            state[i] = i
            i++
        }

        var j = 0
        var keyIndex = 0
        val keySize = key.size

        i = 0
        while (i < 256) {
            j = (j + state[i] + (key[keyIndex].toInt() and 0xFF)) and 0xFF

            val temp = state[i]
            state[i] = state[j]
            state[j] = temp

            i++
            keyIndex++

            if (keyIndex == keySize) keyIndex = 0
        }

        state
    }

    // PRGA
    private fun run(data: ByteArray): ByteArray {
        val state = initialState.copyOf()
        val output = ByteArray(data.size)

        var i = 0
        var j = 0
        var index = 0

        val size = data.size

        while (index < size) {
            i = (i + 1) and 0xFF
            val si = state[i]

            j = (j + si) and 0xFF
            val sj = state[j]

            state[i] = sj
            state[j] = si

            val keyByte = state[(si + sj) and 0xFF]
            output[index] = (data[index].toInt() xor keyByte).toByte()
            index++
        }

        return output
    }

    override fun encode(data: ByteArray): ByteArray = run(data)

    override fun decode(data: ByteArray): ByteArray = run(data)
}