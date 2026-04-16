package com.vandendaelen.nicephore.platform

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PlatformContextTest {
    @Test
    fun `current throws a descriptive error when no service implementation is registered`() {
        val ex = assertThrows(IllegalStateException::class.java) {
            PlatformContext.current
        }
        assert(ex.message?.contains("No PlatformContext implementation") == true) {
            "Expected error message to mention missing implementation, got: ${ex.message}"
        }
    }
}
