package ca.bcparkspot.app.data.remote

import ca.bcparkspot.app.domain.ReportPolicies
import ca.bcparkspot.app.sync.reportReadWindowMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** 4.17: every limit the rules enforce is the app's own constant. */
class RulesConstantsTest {
    private val rulesFile = listOf(File("../firestore.rules"), File("firestore.rules")).first { it.exists() }
    private val functions: Map<String, String> = Regex("""function (\w+)\(\) \{ return (.+?); \}""")
        .findAll(rulesFile.readText())
        .associate { it.groupValues[1] to it.groupValues[2] }

    private fun number(name: String): Long = functions[name]?.toLong() ?: error("firestore.rules has no $name()")
    private fun seconds(ms: Long) = TimeUnit.MILLISECONDS.toSeconds(ms)

    private val policy = ReportPolicies.DEFAULT

    @Test
    fun `time windows match the report policy`() {
        assertEquals(seconds(policy.validityWindowMs), number("reportValiditySeconds"))
        assertEquals(seconds(policy.clockSkewAllowanceMs), number("clockSkewSeconds"))
        assertEquals(seconds(reportReadWindowMs(policy)), number("reportReadWindowSeconds"))
    }

    @Test
    fun `field limits match the app`() {
        mapOf(
            "maxIdLength" to CloudLimits.MAX_ID_LENGTH,
            "maxNameLength" to CloudLimits.MAX_NAME_LENGTH,
            "maxRegionLength" to CloudLimits.MAX_REGION_LENGTH,
            "maxNotesLength" to CloudLimits.MAX_NOTES_LENGTH,
            "maxStallLength" to CloudLimits.MAX_STALL_LENGTH,
            "maxReporterNameLength" to CloudLimits.MAX_REPORTER_NAME_LENGTH,
            "minCommentLength" to CloudLimits.MIN_COMMENT_LENGTH,
            "maxCommentLength" to CloudLimits.MAX_COMMENT_LENGTH,
            "maxSpotsEstimate" to CloudLimits.MAX_SPOTS_ESTIMATE
        ).forEach { (name, value) -> assertEquals(name, value.toLong(), number(name)) }
    }

    @Test
    fun `reportable statuses and the id format match the app`() {
        val statuses = Regex("'(\\w+)'").findAll(functions.getValue("reportableStatuses")).map { it.groupValues[1] }.toList()
        assertEquals(CloudLimits.REPORTABLE_STATUSES.map { it.name }, statuses)
        assertEquals("'${CloudLimits.REPORT_ID_PATTERN}'", functions.getValue("reportIdPattern"))
    }

    @Test
    fun `the app's report ids match the rules' id format`() {
        assertTrue(UUID.randomUUID().toString().matches(Regex(CloudLimits.REPORT_ID_PATTERN)))
        assertFalse("r1".matches(Regex(CloudLimits.REPORT_ID_PATTERN)))
    }

    @Test
    fun `the parser sees every limit function`() {
        val expected = 14
        assertEquals(expected, functions.size)
    }

    @Test
    fun `car pin and comment checks use the same limits`() {
        assertNull(carPinProblem("A".repeat(CloudLimits.MAX_STALL_LENGTH), "N".repeat(CloudLimits.MAX_NOTES_LENGTH)))
        assertEquals(CarPinProblem.STALL_TOO_LONG, carPinProblem("A".repeat(CloudLimits.MAX_STALL_LENGTH + 1), ""))
        assertEquals(CarPinProblem.NOTES_TOO_LONG, carPinProblem("", "N".repeat(CloudLimits.MAX_NOTES_LENGTH + 1)))
        assertTrue(isValidReportComment(" ".repeat(CloudLimits.MIN_COMMENT_LENGTH) + "abc"))
        assertFalse(isValidReportComment("a".repeat(CloudLimits.MIN_COMMENT_LENGTH - 1)))
        assertFalse(isValidReportComment("a".repeat(CloudLimits.MAX_COMMENT_LENGTH + 1)))
    }
}
