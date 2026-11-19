package co.vitasmarta.mobile

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.security.NetworkSecurityPolicy
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import co.vitasmarta.mobile.app.MainActivity
import co.vitasmarta.mobile.core.designsystem.VitasMartaTheme
import co.vitasmarta.mobile.core.network.GatewayConfiguration
import co.vitasmarta.mobile.feature.booking.domain.BookingOffer
import co.vitasmarta.mobile.feature.booking.domain.BookingPolicy
import co.vitasmarta.mobile.feature.booking.presentation.BookingOffersScreen
import co.vitasmarta.mobile.feature.booking.presentation.BookingOffersState
import co.vitasmarta.mobile.feature.recommendations.domain.RecommendationFeed
import co.vitasmarta.mobile.feature.recommendations.presentation.RecommendationsScreen
import co.vitasmarta.mobile.feature.recommendations.presentation.RecommendationsState
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeUiTest {
    @get:Rule(order = 0)
    val foreground = object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val packageName = instrumentation.targetContext.packageName
            val component = "$packageName/${MainActivity::class.java.name}"
            // Some OEMs block an instrumentation-originated launch while the app is backgrounded.
            // Foreground only our own app via the shell before ActivityScenario recreates it.
            val command = "am start -W -a android.intent.action.MAIN " +
                "-c android.intent.category.LAUNCHER -n $component -f 0x10008000"
            val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use {
                check(!it.readText().contains("Error:")) { "Unable to foreground test activity" }
            }
        }
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()
    private fun text(resource: Int) = compose.activity.getString(resource)

    @Test
    fun invalidLoginKeepsLabelsAndShowsFieldValidation() {
        compose.onNodeWithText(text(R.string.profile)).performClick()
        compose.onNodeWithText(text(R.string.email)).assertIsDisplayed()
        compose.onNode(
            hasText(text(R.string.login)) and hasClickAction()
        ).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.invalid_email)).assertIsDisplayed()
    }

    @Test
    fun unavailableCatalogExposesAnActionableRetry() {
        assumeTrue("Requires a build without a Gateway origin", BuildConfig.API_BASE_URL.isBlank())
        compose.onNodeWithText(text(R.string.retry)).assertIsDisplayed().performClick()
        compose.onNodeWithText(text(R.string.service_unavailable_body)).assertIsDisplayed()
    }

    @Test
    fun appearancePersistsAcrossActivityRecreationAndCanReturnToSystem() {
        val appearance = text(R.string.appearance)
        val dark = text(R.string.theme_dark)
        val light = text(R.string.theme_light)
        val system = text(R.string.theme_system)
        fun select(label: String) {
            compose.onNodeWithContentDescription(appearance).performClick()
            compose.onNodeWithText(label).performClick().assertIsSelected()
            compose.onNodeWithText(text(R.string.done)).performClick()
        }
        select(dark)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithContentDescription(appearance).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, dark)
        )
        capture("explore-dark")
        select(light)
        compose.onNodeWithContentDescription(appearance).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, light)
        )
        capture("explore-light")
        select(system)
        compose.onNodeWithContentDescription(appearance).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, system)
        )
    }

    @Test
    fun androidCleartextPolicyOnlyAllowsTheConfiguredHttpHost() {
        val origin = GatewayConfiguration.from(BuildConfig.API_BASE_URL).baseUrl
        val policy = NetworkSecurityPolicy.getInstance()
        assertFalse(policy.isCleartextTrafficPermitted("unrelated.invalid"))
        if (origin != null) {
            assertEquals(!origin.isHttps, policy.isCleartextTrafficPermitted(origin.host))
            assertFalse(policy.isCleartextTrafficPermitted("child.${origin.host}"))
        }
    }

    @Test
    fun publicCatalogLoadsWhenExplicitGatewaySmokeIsEnabled() {
        assumeTrue(
            "Live smoke requires explicit runner argument gatewaySmoke=true",
            InstrumentationRegistry.getArguments().getString("gatewaySmoke") == "true"
        )
        check(BuildConfig.API_BASE_URL.isNotBlank())
        val search = text(R.string.search_categories)
        compose.waitUntil(timeoutMillis = 35_000) {
            compose.onAllNodesWithText(search).fetchSemanticsNodes().any {
                !it.config.contains(SemanticsProperties.Disabled)
            }
        }
        compose.onNodeWithText(search).assertIsEnabled()
        capture("explore-connected")
    }

    @Test
    fun registrationIsReachableAndRequiresExplicitTreatmentConsentWithoutPosting() {
        compose.onNodeWithText(text(R.string.profile)).performClick()
        compose.onNodeWithText(text(R.string.create_account)).assertIsDisplayed().performClick()
        compose.onNodeWithText(text(R.string.invalid_email)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.registration_password_error)).assertDoesNotExist()
        fun selectTheme(label: Int) {
            compose.onNodeWithContentDescription(text(R.string.appearance)).performClick()
            compose.onNodeWithText(text(label)).performClick().assertIsSelected()
            compose.onNodeWithText(text(R.string.done)).performClick()
        }
        selectTheme(R.string.theme_dark)
        capture("registration-dark")
        selectTheme(R.string.theme_light)
        capture("registration-light")
        selectTheme(R.string.theme_dark)
        for (consent in listOf(
            R.string.consent_treatment,
            R.string.consent_commercial,
            R.string.consent_profiling
        )) {
            compose.onNodeWithText(text(consent)).performScrollTo().assertIsOff()
        }
        compose.onNodeWithText(text(R.string.consent_commercial)).performScrollTo()
            .performClick().assertIsOn()
        compose.onNodeWithText(text(R.string.consent_profiling)).assertIsOff()
        compose.onNode(hasText(text(R.string.create_account)) and hasClickAction())
            .performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.invalid_email)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.registration_password_error)).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(text(R.string.consent_treatment_error)).performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(text(R.string.back)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.login_title)).assertIsDisplayed()
    }

    @Test
    fun aRealCategoryOpensFilteredSearchAndBackReturnsToExplore() {
        assumeTrue(
            "Requires public Gateway smoke",
            InstrumentationRegistry.getArguments().getString("gatewaySmoke") == "true"
        )
        val category = SemanticsMatcher("published category") {
            it.config.contains(SemanticsProperties.TestTag) &&
                it.config[SemanticsProperties.TestTag].startsWith("catalog-category-")
        }
        compose.waitUntil(timeoutMillis = 35_000) {
            compose.onAllNodes(category).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(category)[0].performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.catalog_results_title)).assertIsDisplayed()
        capture("catalog-results")
        compose.onNodeWithText(text(R.string.back)).performClick()
        compose.onNodeWithText(text(R.string.native_explore_title)).assertIsDisplayed()
    }

    @Test
    fun publishedCatalogContentOpensItsRealDetailAndContextualBookingAccess() {
        assumeTrue(
            "Requires public Gateway smoke with published content",
            InstrumentationRegistry.getArguments().getString("gatewaySmoke") == "true"
        )
        compose.onNodeWithText(text(R.string.catalog_search_attractions))
            .performScrollTo().performClick()
        val detailAction = hasText(text(R.string.catalog_open_detail)) and hasClickAction()
        compose.waitUntil(15_000) {
            compose.onAllNodes(detailAction).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(detailAction)[0].performScrollTo()
        capture("catalog-published-results")
        compose.onAllNodes(detailAction)[0].performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text(R.string.catalog_about))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollAction()).performScrollToNode(
            hasText(text(R.string.catalog_about))
        )
        capture("catalog-published-detail")
        compose.onNode(hasScrollAction()).performScrollToNode(
            hasText(text(R.string.catalog_booking_options))
        )
        compose.onNodeWithText(text(R.string.catalog_booking_options)).performClick()
        compose.onNodeWithText(text(R.string.login_title)).assertIsDisplayed()
    }

    @Test
    fun privateDestinationsAskForLoginInsteadOfShowingAStaticServiceFailure() {
        for (destination in listOf(R.string.reservations, R.string.recommendations)) {
            compose.onNodeWithText(text(destination)).performClick()
            compose.onNodeWithText(text(R.string.login_title)).assertIsDisplayed()
            compose.onNodeWithText(text(R.string.service_unavailable_body)).assertDoesNotExist()
        }
    }

    // Fixtures belong only to androidTest; these screens do not create remote reservations.
    @Test
    fun offerPolicyAndAcceptanceRemainReadableOnTheDevice() {
        val policy = BookingPolicy("test-policy", "1", "Política de prueba de interfaz", 24, 0)
        val offer = BookingOffer(
            "test-service", "test-slot", Instant.parse("2099-01-01T10:00:00Z"),
            Instant.parse("2099-01-01T11:00:00Z"), BigDecimal("12000.50"), "COP",
            "POR_CUPO", 5, 5, policy, true, false, false
        )
        val accepted = AtomicBoolean(false)
        compose.runOnIdle {
            compose.activity.setContent {
                VitasMartaTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        BookingOffersScreen(
                            state = BookingOffersState(offers = listOf(offer), selected = offer),
                            onBack = {}, onReload = {}, onSelect = {}, onQuantity = {},
                            onAccepted = { accepted.set(it) }, onReserve = {}, onReset = {},
                            onCreated = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(policy.content).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.booking_accept_policy))
            .performScrollTo().assertIsOff().performClick()
        org.junit.Assert.assertTrue(accepted.get())
        capture("fixture-booking-policy-dark")
    }

    @Test
    fun degradedRecommendationsRemainContentAndCanAlsoBeEmpty() {
        val feed = RecommendationFeed(
            "DEGRADADO_FALLBACK",
            true,
            false,
            "",
            false,
            false,
            emptyList(),
            emptyList()
        )
        compose.runOnIdle {
            compose.activity.setContent {
                VitasMartaTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        RecommendationsScreen(
                            RecommendationsState(loading = false, feed = feed),
                            onReload = {},
                            onAttraction = {},
                            onProfile = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText(text(R.string.reco_degraded)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(
            text(R.string.reco_empty_title)
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.service_unavailable_body)).assertDoesNotExist()
        capture("fixture-recommendations-empty-dark")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(compose.activity.cacheDir, "screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
