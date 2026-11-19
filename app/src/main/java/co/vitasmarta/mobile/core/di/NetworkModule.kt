package co.vitasmarta.mobile.core.di

import android.content.Context
import co.vitasmarta.mobile.BuildConfig
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.core.network.GatewayBoundaryInterceptor
import co.vitasmarta.mobile.core.network.GatewayConfiguration
import co.vitasmarta.mobile.core.network.GatewayServices
import co.vitasmarta.mobile.core.preferences.AppLanguage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun configuration(): GatewayConfiguration = GatewayConfiguration.from(BuildConfig.API_BASE_URL)

    @Provides @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }

    @Provides @Singleton
    fun client(
        configuration: GatewayConfiguration,
        @ApplicationContext context: Context
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .addInterceptor(
            GatewayBoundaryInterceptor(configuration) {
                AppLanguage.current(context)
            }
        ).build()

    @Provides @Singleton
    fun services(
        configuration: GatewayConfiguration,
        client: OkHttpClient,
        json: Json
    ): GatewayServices = GatewayServices(configuration, client, json)

    @Provides @Singleton
    fun executor(json: Json): ApiExecutor = ApiExecutor(json)
}
