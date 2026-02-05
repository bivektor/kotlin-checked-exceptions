package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isNonLocal
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction

object CheckedExceptionsSimpleFunctionChecker : FirDeclarationChecker<FirSimpleFunction>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        CheckedExceptionsAnalyzer(context, reporter).analyze(declaration)
    }
}

object CheckedExceptionsPropertyAccessorChecker : FirDeclarationChecker<FirPropertyAccessor>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirPropertyAccessor) {
        CheckedExceptionsAnalyzer(context, reporter).analyze(declaration)
    }
}

object CheckedExceptionsConstructorChecker : FirDeclarationChecker<FirConstructor>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirConstructor) {
        CheckedExceptionsAnalyzer(context, reporter).analyze(declaration)
    }
}

object CheckedExceptionsAnonymousInitializerChecker : FirDeclarationChecker<FirAnonymousInitializer>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirAnonymousInitializer) {
        CheckedExceptionsAnalyzer(context, reporter).analyzeBody(declaration.body)
    }
}

object CheckedExceptionsPropertyChecker : FirDeclarationChecker<FirProperty>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (!declaration.isNonLocal) return
        val initializer = declaration.initializer ?: return
        CheckedExceptionsAnalyzer(context, reporter).analyzeExpression(initializer)
    }
}
