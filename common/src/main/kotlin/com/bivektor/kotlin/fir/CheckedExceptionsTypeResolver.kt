package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class CheckedExceptionsTypeResolver(
    private val session: FirSession
) {
    fun resolveType(fqName: String): ConeKotlinType? {
        val classId = ClassId.topLevel(FqName(fqName))
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return symbol.defaultType()
    }
}
