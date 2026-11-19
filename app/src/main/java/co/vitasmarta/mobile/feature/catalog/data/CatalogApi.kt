package co.vitasmarta.mobile.feature.catalog.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class CategoryDto(
    val slug: String,
    val nombre: Map<String, String>,
    @SerialName("primer_nivel") val topLevel: Boolean
)

interface CatalogApi {
    @GET("api/v1/catalogo/categorias")
    suspend fun categories(): Response<List<CategoryDto>>

    @GET("api/v1/catalogo/busqueda")
    suspend fun search(
        @Query("texto") text: String?,
        @Query("categorias") categories: List<String>,
        @Query("precio_minimo") minimumPrice: String?,
        @Query("precio_maximo") maximumPrice: String?,
        @Query("orden") order: String,
        @Query("descendente") descending: Boolean,
        @Query("limite") limit: Int,
        @Query("cursor") cursor: String?
    ): Response<CatalogPageDto>

    @GET("api/v1/catalogo/{id}")
    suspend fun detail(
        @Path("id") id: String,
        @Query("idioma") language: String
    ): Response<CatalogDetailDto>

    @GET("api/v1/catalogo/{id}/franjas")
    suspend fun slots(@Path("id") id: String): Response<SlotsDto>
}
