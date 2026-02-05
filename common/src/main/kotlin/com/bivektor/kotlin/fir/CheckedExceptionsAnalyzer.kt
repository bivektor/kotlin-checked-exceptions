package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractClassesFromArgument
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod

class CheckedExceptionsAnalyzer(
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter
) {
    private val throwsClassIds = setOf(
        ClassId.topLevel(FqName("kotlin.Throws")),
        ClassId.topLevel(FqName("kotlin.jvm.Throws"))
    )
    private val throwsParamName = Name.identifier("exceptionClasses")

    private val throwableType: ConeKotlinType? = resolveType("kotlin.Throwable")
        ?: resolveType("java.lang.Throwable")

    private val runtimeExceptionType: ConeKotlinType? = resolveType("kotlin.RuntimeException")
        ?: resolveType("java.lang.RuntimeException")

    private val errorType: ConeKotlinType? = resolveType("kotlin.Error")
        ?: resolveType("java.lang.Error")

    private val typeContext = context.session.typeContext

    fun analyze(function: FirFunction) {
        val body = function.body ?: return
        val state = AnalysisState()
        val declaredInFunction = declaredThrows(function)
        val catches = emptyList<ConeKotlinType>()
        visitBlock(body, catches, declaredInFunction, state)

        state.flushReports(reporter, context)
    }

    fun analyzeBody(body: FirBlock?) {
        body ?: return
        val state = AnalysisState()
        visitBlock(body, emptyList(), emptyList(), state)
        state.flushReports(reporter, context)
    }

    fun analyzeExpression(expression: FirExpression) {
        val state = AnalysisState()
        expression.accept(Visitor(emptyList(), emptyList(), state), Unit)
        state.flushReports(reporter, context)
    }

    private fun visitBlock(
        block: FirBlock,
        catches: List<ConeKotlinType>,
        declaredInFunction: List<ExceptionType>,
        state: AnalysisState
    ) {
        for (statement in block.statements) {
            visitStatement(statement, catches, declaredInFunction, state)
        }
    }

    private fun visitStatement(
        statement: FirStatement,
        catches: List<ConeKotlinType>,
        declaredInFunction: List<ExceptionType>,
        state: AnalysisState
    ) {
        when (statement) {
            is FirTryExpression -> visitTry(statement, catches, declaredInFunction, state)
            is FirThrowExpression ->
                recordThrown(statement.source, statement.exception.resolvedType, catches, declaredInFunction, state)
            is FirFunctionCall -> {
                recordCall(statement, catches, declaredInFunction, state)
                statement.acceptChildren(Visitor(catches, declaredInFunction, state), Unit)
            }
            is FirPropertyAccessExpression -> recordPropertyGetter(statement, catches, declaredInFunction, state)
            is FirVariableAssignment -> recordVariableAssignment(statement, catches, declaredInFunction, state)
            is FirAnonymousFunctionExpression ->
                statement.anonymousFunction.body?.let { visitBlock(it, catches, declaredInFunction, state) }
            is FirBlock -> visitBlock(statement, catches, declaredInFunction, state)
            else -> statement.acceptChildren(Visitor(catches, declaredInFunction, state), Unit)
        }
    }

    private fun visitTry(
        expression: FirTryExpression,
        catches: List<ConeKotlinType>,
        declaredInFunction: List<ExceptionType>,
        state: AnalysisState
    ) {
        val catchTypes = expression.catches.map { it.parameter.returnTypeRef.coneType }
        visitBlock(expression.tryBlock, catches + catchTypes, declaredInFunction, state)
        for (catchClause in expression.catches) {
            visitBlock(catchClause.block, catches, declaredInFunction, state)
        }
        expression.finallyBlock?.let { visitBlock(it, catches, declaredInFunction, state) }
    }

    private fun recordCall(
        call: FirFunctionCall,
        catches: List<ConeKotlinType>,
        declaredInFunction: List<ExceptionType>,
        state: AnalysisState
    ) {
        val calleeReference = call.calleeReference
        if (calleeReference.isError()) return
        val symbol = calleeReference.toResolvedFunctionSymbol() ?: return

        val thrownTypes = collectThrowsTypes(symbol)
        for (type in thrownTypes) {
            recordThrown(call.source, type, catches, declaredInFunction, state)
        }
    }

    private fun recordPropertyGetter(
        access: FirPropertyAccessExpression,
        catches: List<ConeKotlinType>,
        declaredInFunction: List<ExceptionType>,
        state: AnalysisState
    ) {
        val propertySymbol = access.calleeReference.toResolvedPropertySymbol() ?: return
        val getter = propertySymbol.getterSymbol ?: return
        val thrownTypes = collectThrowsTypes(getter)
        for (type in thrownTypes) {
            recordThrown(access.source, type, catches, declaredInFunction, state)
        }
    }

    private fun recordVariableAssignment(
        assignment: FirVariableAssignment,
        catches: List<ConeKotlinType>,
        declaredInFunction: List<ExceptionType>,
        state: AnalysisState
    ) {
        val access = assignment.lValue as? FirPropertyAccessExpression ?: return
        val propertySymbol = access.calleeReference.toResolvedPropertySymbol() ?: return
        val setter = propertySymbol.setterSymbol ?: return
        val thrownTypes = collectThrowsTypes(setter)
        for (type in thrownTypes) {
            recordThrown(assignment.source, type, catches, declaredInFunction, state)
        }
    }

    private fun recordThrown(
        source: KtSourceElement?,
        type: ConeKotlinType,
        catches: List<ConeKotlinType>,
        declaredInFunction: List<ExceptionType>,
        state: AnalysisState
    ) {
        val exceptionType = toExceptionType(type) ?: return
        if (!exceptionType.isChecked) return
        if (isCaught(exceptionType.type, catches)) return
        if (isDeclaredInFunction(exceptionType.type, declaredInFunction)) return

        state.addReport(source, exceptionType)
        state.uncaught += exceptionType
    }


    private fun isCaught(thrownType: ConeKotlinType, catches: List<ConeKotlinType>): Boolean {
        return catches.any { catchType -> isSubtype(thrownType, catchType) }
    }

    private fun isSubtype(subType: ConeKotlinType, superType: ConeKotlinType): Boolean {
        val expandedSub = subType.fullyExpandedType(context.session)
        val expandedSuper = superType.fullyExpandedType(context.session)
        if (AbstractTypeChecker.isSubtypeOf(typeContext, expandedSub, expandedSuper)) return true
        return isSubtypeByReflection(expandedSub, expandedSuper)
    }

    private fun isSubtypeByReflection(subType: ConeKotlinType, superType: ConeKotlinType): Boolean {
        val subFqName = subType.classLikeLookupTagIfAny?.classId?.asSingleFqName()?.asString() ?: return false
        val superFqName = superType.classLikeLookupTagIfAny?.classId?.asSingleFqName()?.asString() ?: return false
        val subCandidates = candidateJvmClassNames(subFqName)
        val superCandidates = candidateJvmClassNames(superFqName)
        return try {
            val loader = Thread.currentThread().contextClassLoader ?: CheckedExceptionsAnalyzer::class.java.classLoader
            val subClass = subCandidates.firstNotNullOfOrNull { name -> loadClass(name, loader) } ?: return false
            val superClass = superCandidates.firstNotNullOfOrNull { name -> loadClass(name, loader) } ?: return false
            superClass.isAssignableFrom(subClass)
        } catch (_: Throwable) {
            false
        }
    }

    private fun candidateJvmClassNames(fqName: String): List<String> {
        if (!fqName.startsWith("kotlin.io.")) return listOf(fqName)
        val simple = fqName.removePrefix("kotlin.io.")
        return listOf(fqName, "java.io.$simple")
    }

    private fun loadClass(name: String, loader: ClassLoader): Class<*>? {
        return try {
            Class.forName(name, false, loader)
        } catch (_: Throwable) {
            null
        }
    }

    private fun toExceptionType(type: ConeKotlinType): ExceptionType? {
        val throwable = throwableType ?: return null
        if (!isSubtype(type, throwable)) return null

        val displayName = type.classLikeLookupTagIfAny
            ?.classId
            ?.asSingleFqName()
            ?.asString()
            ?: type.toString()

        val isUnchecked = (runtimeExceptionType?.let { isSubtype(type, it) } == true) ||
            (errorType?.let { isSubtype(type, it) } == true)

        return ExceptionType(type = type, displayName = displayName, isChecked = !isUnchecked)
    }

    private fun declaredThrows(function: FirFunction): List<ExceptionType> {
        val annotation = function.symbol.resolvedAnnotationsWithArguments.firstOrNull {
            isThrowsAnnotation(it)
        }
            ?: return emptyList()
        return annotation.extractThrownExceptionTypes(context).mapNotNull { toExceptionType(it) }
    }

    private fun collectThrowsTypes(
        symbol: org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol<*>
    ): List<ConeKotlinType> {
        val fromAnnotation = symbol.resolvedAnnotationsWithArguments.firstOrNull { isThrowsAnnotation(it) }
            ?.extractThrownExceptionTypes(context)
            .orEmpty()
        val fromJava = extractJavaThrows(symbol)
        return fromAnnotation + fromJava
    }

    private fun extractJavaThrows(
        symbol: org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol<*>
    ): List<ConeKotlinType> {
        val source = symbol.source as? KtPsiSourceElement ?: return emptyList()
        val psiMethod = source.psi as? PsiMethod ?: return emptyList()
        val referenced = psiMethod.throwsList.referencedTypes
        if (referenced.isEmpty()) return emptyList()
        return referenced.mapNotNull { type ->
            val fqName = type.canonicalText.substringBefore('<')
            resolveType(fqName)
        }
    }

    private fun isThrowsAnnotation(annotation: FirAnnotation): Boolean {
        val classId = annotation.toAnnotationClassId(context.session) ?: return false
        if (classId in throwsClassIds) return true
        return classId.shortClassName.asString() == "Throws"
    }

    private fun isDeclaredInFunction(
        thrownType: ConeKotlinType,
        declaredInFunction: List<ExceptionType>
    ): Boolean {
        return declaredInFunction.any { declaredType -> isSubtype(thrownType, declaredType.type) }
    }

    private fun FirAnnotation.extractThrownExceptionTypes(context: CheckerContext): List<ConeKotlinType> {
        val argument = findArgumentByName(throwsParamName)
            ?: findArgumentByName(StandardClassIds.Annotations.ParameterNames.value)
            ?: return emptyList()
        return argument.extractClassesFromArgument(context.session)
            .mapNotNull { it.defaultType() }
    }

    private fun resolveType(fqName: String): ConeKotlinType? {
        val classId = ClassId.topLevel(FqName(fqName))
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return null
        return symbol.defaultType()
    }

    private inner class Visitor(
        private val catches: List<ConeKotlinType>,
        private val declaredInFunction: List<ExceptionType>,
        private val state: AnalysisState
    ) : FirVisitor<Unit, Unit>() {
        override fun visitElement(element: FirElement, data: Unit) {
            element.acceptChildren(this, data)
        }

        override fun visitBlock(block: FirBlock, data: Unit) {
            this@CheckedExceptionsAnalyzer.visitBlock(block, catches, declaredInFunction, state)
        }

        override fun visitTryExpression(tryExpression: FirTryExpression, data: Unit) {
            this@CheckedExceptionsAnalyzer.visitTry(tryExpression, catches, declaredInFunction, state)
        }

        override fun visitThrowExpression(throwExpression: FirThrowExpression, data: Unit) {
            this@CheckedExceptionsAnalyzer.recordThrown(
                throwExpression.source,
                throwExpression.exception.resolvedType,
                catches,
                declaredInFunction,
                state
            )
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall, data: Unit) {
            this@CheckedExceptionsAnalyzer.recordCall(functionCall, catches, declaredInFunction, state)
            functionCall.acceptChildren(this, data)
        }

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: Unit) {
            this@CheckedExceptionsAnalyzer.recordPropertyGetter(propertyAccessExpression, catches, declaredInFunction, state)
            propertyAccessExpression.acceptChildren(this, data)
        }

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: Unit) {
            this@CheckedExceptionsAnalyzer.recordVariableAssignment(variableAssignment, catches, declaredInFunction, state)
            variableAssignment.acceptChildren(this, data)
        }

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: Unit) {
            anonymousFunctionExpression.anonymousFunction.body?.let {
                this@CheckedExceptionsAnalyzer.visitBlock(it, catches, declaredInFunction, state)
            }
        }
    }

    private data class ExceptionType(
        val type: ConeKotlinType,
        val displayName: String,
        val isChecked: Boolean
    )

    private class AnalysisState {
        val uncaught: MutableList<ExceptionType> = mutableListOf()
        private val reports: MutableMap<KtSourceElement?, MutableSet<ExceptionType>> = linkedMapOf()

        fun addReport(source: KtSourceElement?, exceptionType: ExceptionType) {
            val bucket = reports.getOrPut(source) { linkedSetOf() }
            bucket += exceptionType
        }

        fun flushReports(reporter: DiagnosticReporter, context: CheckerContext) {
            for ((source, exceptions) in reports) {
                val message = exceptions.joinToString { it.displayName }
                reporter.reportOn(
                    source,
                    CheckedExceptionsErrors.CALL_THROWS_CHECKED,
                    message,
                    context
                )
            }
        }
    }
}
