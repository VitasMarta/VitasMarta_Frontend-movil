package co.vitasmarta.mobile.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class GatewayServices(configuration: GatewayConfiguration, client: OkHttpClient, json: Json) {
    private val retrofit = configuration.baseUrl?.let {
        Retrofit.Builder().baseUrl(it).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    }

    fun <T> create(service: Class<T>): T? = retrofit?.create(service)
}
