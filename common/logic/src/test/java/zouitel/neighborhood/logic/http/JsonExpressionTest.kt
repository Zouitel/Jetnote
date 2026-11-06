package zouitel.neighborhood.logic.http

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonExpressionTest {
    private val root = Json.parseToJsonElement(
        """{"x":1.2345,"name":"note","items":[10,20]}"""
    ).jsonObject

    @Test
    fun preservesJsonNumberPrecision() {
        assertEquals("1.2345", evaluateJsonExpr("x", root))
    }

    @Test
    fun supportsArithmeticAndMultiplicationOperator() {
        assertEquals("6", evaluateJsonExpr("2 * 3", root))
    }

    @Test
    fun rejectsDivisionByZero() {
        assertThrows(RuntimeException::class.java) {
            evaluateJsonExpr("2 / 0", root)
        }
    }

    @Test
    fun rejectsNonNumericStringArithmetic() {
        assertThrows(RuntimeException::class.java) {
            evaluateJsonExpr("name + 1", root)
        }
    }

    @Test
    fun allowsFieldNamedX() {
        assertTrue(isJsonExprValid("x"))
        assertEquals("1.2345", evaluateJsonExpr("x", root))
    }

    @Test
    fun negativeArrayIndexIsParsedButDoesNotResolve() {
        assertTrue(isJsonExprValid("items[-1]"))
        assertEquals("", evaluateJsonExpr("items[-1]", root))
    }

    @Test
    fun invalidExponentIsRejected() {
        assertFalse(isJsonExprValid("1e+"))
    }
}
