package co.vitasmarta.mobile.feature.catalog.data

import co.vitasmarta.mobile.core.common.AppError
import co.vitasmarta.mobile.core.common.AppResult
import co.vitasmarta.mobile.core.common.ErrorKind
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.feature.catalog.domain.CatalogRepository
import co.vitasmarta.mobile.feature.catalog.domain.Category

class RemoteCatalogRepository(private val api: CatalogApi?, private val executor: ApiExecutor) :
    CatalogRepository {
    override suspend fun categories(): AppResult<List<Category>> {
        val remote = api ?: return AppResult.Failure(AppError(ErrorKind.Configuration))
        return when (val result = executor.execute { remote.categories() }) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                if (result.value.any { it.slug.isBlank() || it.nombre.isEmpty() } ||
                    result.value.map { it.slug }.distinct().size != result.value.size
                ) {
                    AppResult.Failure(AppError(ErrorKind.InvalidResponse))
                } else {
                    AppResult.Success(
                        result.value.map {
                            Category(it.slug, it.nombre, it.topLevel)
                        }
                    )
                }
            }
        }
    }
}
