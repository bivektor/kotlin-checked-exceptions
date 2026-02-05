package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING

object CheckedExceptionsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("CheckedExceptions") { map ->
        map.put(
            CheckedExceptionsErrors.CALL_THROWS_CHECKED,
            "Call may throw checked exceptions: {0}. Handle or declare.",
            STRING
        )
    }
}
