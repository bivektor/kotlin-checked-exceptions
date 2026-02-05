package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class CheckedExceptionsAdditionalCheckersExtension(
    session: FirSession
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val simpleFunctionCheckers = setOf(CheckedExceptionsSimpleFunctionChecker)
        override val propertyAccessorCheckers = setOf(CheckedExceptionsPropertyAccessorChecker)
        override val constructorCheckers = setOf(CheckedExceptionsConstructorChecker)
        override val anonymousInitializerCheckers = setOf(CheckedExceptionsAnonymousInitializerChecker)
        override val propertyCheckers = setOf(CheckedExceptionsPropertyChecker)
    }
}
