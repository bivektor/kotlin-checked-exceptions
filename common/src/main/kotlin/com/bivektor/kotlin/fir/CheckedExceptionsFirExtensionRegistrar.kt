package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class CheckedExceptionsFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        registerDiagnosticContainers(CheckedExceptionsErrors)
        +::CheckedExceptionsCache
        +::CheckedExceptionsAdditionalCheckersExtension
    }
}
