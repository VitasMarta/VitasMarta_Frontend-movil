package co.vitasmarta.mobile.core.di

import co.vitasmarta.mobile.core.auth.SessionStore
import co.vitasmarta.mobile.core.auth.TokenRefresher
import co.vitasmarta.mobile.core.network.ApiExecutor
import co.vitasmarta.mobile.core.network.AuthenticatedApiExecutor
import co.vitasmarta.mobile.core.network.GatewayServices
import co.vitasmarta.mobile.core.preferences.AppearanceRepository
import co.vitasmarta.mobile.core.preferences.DataStoreAppearanceRepository
import co.vitasmarta.mobile.feature.auth.data.IdentityApi
import co.vitasmarta.mobile.feature.auth.data.RegistrationApi
import co.vitasmarta.mobile.feature.auth.data.RemoteAccountRecoveryRepository
import co.vitasmarta.mobile.feature.auth.data.RemoteAuthRepository
import co.vitasmarta.mobile.feature.auth.data.RemoteRegistrationRepository
import co.vitasmarta.mobile.feature.auth.domain.AccountRecoveryRepository
import co.vitasmarta.mobile.feature.auth.domain.AuthRepository
import co.vitasmarta.mobile.feature.auth.domain.RegistrationRepository
import co.vitasmarta.mobile.feature.booking.data.BookingApi
import co.vitasmarta.mobile.feature.booking.data.RemoteBookingRepository
import co.vitasmarta.mobile.feature.booking.domain.BookingRepository
import co.vitasmarta.mobile.feature.catalog.data.CatalogApi
import co.vitasmarta.mobile.feature.catalog.data.RemoteCatalogContentRepository
import co.vitasmarta.mobile.feature.catalog.data.RemoteCatalogRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogContentRepository
import co.vitasmarta.mobile.feature.catalog.domain.CatalogRepository
import co.vitasmarta.mobile.feature.profile.data.ProfileApi
import co.vitasmarta.mobile.feature.profile.data.ProfileCategoriesApi
import co.vitasmarta.mobile.feature.profile.data.RemoteProfileRepository
import co.vitasmarta.mobile.feature.profile.domain.ProfileRepository
import co.vitasmarta.mobile.feature.recommendations.data.RecommendationsApi
import co.vitasmarta.mobile.feature.recommendations.data.RemoteRecommendationsRepository
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendationsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {
    @Provides @Singleton
    fun booking(
        services: GatewayServices,
        authenticated: AuthenticatedApiExecutor,
        sessions: SessionStore
    ): BookingRepository = RemoteBookingRepository(
        services.create(BookingApi::class.java),
        authenticated,
        sessions
    )

    @Provides @Singleton
    fun profile(
        services: GatewayServices,
        authenticated: AuthenticatedApiExecutor,
        executor: ApiExecutor
    ): ProfileRepository = RemoteProfileRepository(
        services.create(ProfileApi::class.java),
        authenticated,
        services.create(ProfileCategoriesApi::class.java),
        executor
    )

    @Provides @Singleton
    fun recommendations(
        services: GatewayServices,
        authenticated: AuthenticatedApiExecutor
    ): RecommendationsRepository = RemoteRecommendationsRepository(
        services.create(RecommendationsApi::class.java),
        authenticated
    )

    @Provides
    fun appearance(repository: DataStoreAppearanceRepository): AppearanceRepository = repository

    @Provides
    fun registration(services: GatewayServices, executor: ApiExecutor): RegistrationRepository =
        RemoteRegistrationRepository(services.create(RegistrationApi::class.java), executor)

    @Provides
    fun recovery(services: GatewayServices, executor: ApiExecutor): AccountRecoveryRepository =
        RemoteAccountRecoveryRepository(services.create(IdentityApi::class.java), executor)

    @Provides @Singleton
    fun auth(
        services: GatewayServices,
        sessions: SessionStore,
        executor: ApiExecutor
    ): RemoteAuthRepository =
        RemoteAuthRepository(services.create(IdentityApi::class.java), sessions, executor)

    @Provides
    fun authRepository(repository: RemoteAuthRepository): AuthRepository = repository

    @Provides
    fun refresher(repository: RemoteAuthRepository): TokenRefresher = repository

    @Provides @Singleton
    fun catalogContent(services: GatewayServices, executor: ApiExecutor): CatalogContentRepository =
        RemoteCatalogContentRepository(services.create(CatalogApi::class.java), executor)

    @Provides @Singleton
    fun catalog(services: GatewayServices, executor: ApiExecutor): CatalogRepository =
        RemoteCatalogRepository(services.create(CatalogApi::class.java), executor)
}
