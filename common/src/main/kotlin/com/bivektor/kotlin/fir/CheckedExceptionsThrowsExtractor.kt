package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractClassesFromArgument
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod

class CheckedExceptionsThrowsExtractor(
    private val session: FirSession
) {
    private val throwsClassIds = setOf(
        ClassId.topLevel(FqName("kotlin.Throws")),
        ClassId.topLevel(FqName("kotlin.jvm.Throws"))
    )
    private val throwsParamName = Name.identifier("exceptionClasses")
    private val resolver = CheckedExceptionsTypeResolver(session)

    fun collectThrowsTypes(
        symbol: org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol<*>,
        context: CheckerContext
    ): List<ConeKotlinType> {
        val fromAnnotation = symbol.resolvedAnnotationsWithArguments.firstOrNull { isThrowsAnnotation(it) }
            ?.extractThrownExceptionTypes(context)
            .orEmpty()
        val fromJava = extractJavaThrows(symbol)
        return fromAnnotation + fromJava
    }

    fun declaredThrows(
        function: org.jetbrains.kotlin.fir.declarations.FirFunction,
        context: CheckerContext
    ): List<ConeKotlinType> {
        val annotation = function.symbol.resolvedAnnotationsWithArguments.firstOrNull { isThrowsAnnotation(it) }
            ?: return emptyList()
        return annotation.extractThrownExceptionTypes(context)
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
            resolver.resolveType(fqName)
        }
    }

    private fun isThrowsAnnotation(annotation: FirAnnotation): Boolean {
        val classId = annotation.toAnnotationClassId(session) ?: return false
        if (classId in throwsClassIds) return true
        return classId.shortClassName.asString() == "Throws"
    }

    private fun FirAnnotation.extractThrownExceptionTypes(context: CheckerContext): List<ConeKotlinType> {
        val argument = findArgumentByName(throwsParamName)
            ?: findArgumentByName(StandardClassIds.Annotations.ParameterNames.value)
            ?: return emptyList()
        return argument.extractClassesFromArgument(context.session)
            .mapNotNull { it.defaultType() }
    }
}
