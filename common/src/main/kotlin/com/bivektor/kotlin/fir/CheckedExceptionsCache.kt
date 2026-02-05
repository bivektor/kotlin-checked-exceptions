package com.bivektor.kotlin.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

class CheckedExceptionsCache(session: FirSession) : FirSessionComponent {
    private val caches = session.firCachesFactory
    private val typeContext = session.typeContext

    val resolvedTypeByFqName: FirCache<String, ConeKotlinType?, Nothing?> =
        caches.createCache { fqName ->
            CheckedExceptionsTypeResolver(session).resolveType(fqName)
        }

    val subtypeCache: FirCache<Pair<ConeKotlinType, ConeKotlinType>, Boolean, Nothing?> =
        caches.createCache { (subType, superType) ->
            AbstractTypeChecker.isSubtypeOf(typeContext, subType, superType)
        }

    val throwsTypesByFunction: FirCache<FirFunctionSymbol<*>, List<ConeKotlinType>, CheckerContext> =
        caches.createCache { symbol, context ->
            CheckedExceptionsThrowsExtractor(session).collectThrowsTypes(symbol, context)
        }

    val getterThrowsByProperty: FirCache<FirPropertySymbol, List<ConeKotlinType>, CheckerContext> =
        caches.createCache { symbol, context ->
            symbol.getterSymbol?.let { CheckedExceptionsThrowsExtractor(session).collectThrowsTypes(it, context) }.orEmpty()
        }

    val setterThrowsByProperty: FirCache<FirPropertySymbol, List<ConeKotlinType>, CheckerContext> =
        caches.createCache { symbol, context ->
            symbol.setterSymbol?.let { CheckedExceptionsThrowsExtractor(session).collectThrowsTypes(it, context) }.orEmpty()
        }
}

val FirSession.checkedExceptionsCache: CheckedExceptionsCache by FirSession.sessionComponentAccessor()
