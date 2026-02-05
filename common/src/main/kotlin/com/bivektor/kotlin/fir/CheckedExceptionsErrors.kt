package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

object CheckedExceptionsErrors : KtDiagnosticsContainer() {
    val CALL_THROWS_CHECKED: KtDiagnosticFactory1<String> = KtDiagnosticFactory1(
        "KOTLIN_CHECKED_EXCEPTIONS",
        Severity.WARNING,
        SourceElementPositioningStrategies.DEFAULT,
        PsiElement::class,
        getRendererFactory()
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = CheckedExceptionsDefaultMessages
}
