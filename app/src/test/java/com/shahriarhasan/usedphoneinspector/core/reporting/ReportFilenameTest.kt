package com.shahriarhasan.usedphoneinspector.core.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportFilenameTest {
    @Test fun filename_isSanitizedAndStable() {
        val value = ReportFilename.create("Acme/Phone", "Model:*?", 0, "UPI-12345678")
        assertTrue(value.startsWith("Inspection_Acme_Phone_Model_"))
        assertTrue(value.endsWith("12345678.pdf"))
        assertFalse(value.contains('/'))
        assertFalse(value.contains('*'))
    }

    @Test fun sanitizer_preservesBanglaLetters() {
        assertEquals("ফোন_মডেল", ReportFilename.sanitize("ফোন মডেল"))
    }
}

