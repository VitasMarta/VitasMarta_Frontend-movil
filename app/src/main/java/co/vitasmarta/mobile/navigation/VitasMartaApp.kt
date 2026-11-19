package co.vitasmarta.mobile.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import co.vitasmarta.mobile.R
import co.vitasmarta.mobile.core.auth.SessionState
import co.vitasmarta.mobile.core.preferences.ThemeMode
import co.vitasmarta.mobile.feature.auth.presentation.AccountRoute
import co.vitasmarta.mobile.feature.auth.presentation.AuthViewModel
import co.vitasmarta.mobile.feature.auth.presentation.RecoveryRoute
import co.vitasmarta.mobile.feature.auth.presentation.RegistrationRoute
import co.vitasmarta.mobile.feature.booking.presentation.BookingDetailRoute
import co.vitasmarta.mobile.feature.booking.presentation.BookingListRoute
import co.vitasmarta.mobile.feature.booking.presentation.BookingOffersRoute
import co.vitasmarta.mobile.feature.catalog.presentation.CatalogDetailRoute
import co.vitasmarta.mobile.feature.catalog.presentation.CatalogRoute
import co.vitasmarta.mobile.feature.catalog.presentation.CatalogSearchRoute
import co.vitasmarta.mobile.feature.recommendations.presentation.RecommendationsRoute
import co.vitasmarta.mobile.feature.settings.presentation.AppearanceSheet
import co.vitasmarta.mobile.feature.settings.presentation.AppearanceState
import co.vitasmarta.mobile.feature.settings.presentation.labelRes
import kotlinx.serialization.Serializable

@Serializable data class CatalogSearch(val category: String? = null)

@Serializable data class CatalogDetail(val id: String)

@Serializable data object Explore

@Serializable
data class BookingOffers(val serviceRef: String, val existingReference: String? = null)

@Serializable data class BookingDetails(val reference: String)

@Serializable data object Reservations

@Serializable data object Recommendations

@Serializable data object Registration

@Serializable data object Recovery

@Serializable data object Profile

private data class Destination(val route: Any, val label: Int, val icon: ImageVector)
private val destinations = listOf(
    Destination(Explore, R.string.explore, Icons.Outlined.Explore),
    Destination(Reservations, R.string.reservations, Icons.Outlined.CalendarMonth),
    Destination(Recommendations, R.string.recommendations, Icons.Outlined.AutoAwesome),
    Destination(Profile, R.string.profile, Icons.Outlined.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitasMartaApp(appearance: AppearanceState, onThemeSelected: (ThemeMode) -> Unit) {
    val controller = rememberNavController()
    val auth: AuthViewModel = hiltViewModel()
    val session by auth.session.collectAsStateWithLifecycle()
    var previousSession by rememberSaveable { mutableStateOf<String?>(null) }
    val sessionKey = (session as? SessionState.Authenticated)?.let {
        "${it.userId}:${it.generation}"
    }
    LaunchedEffect(sessionKey) {
        if (previousSession != null && previousSession != sessionKey) {
            controller.navigate(Explore) {
                popUpTo(controller.graph.id) { inclusive = false }
                launchSingleTop = true
            }
        }
        previousSession = sessionKey
    }
    val entry by controller.currentBackStackEntryAsState()
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    val selectedRoute = when {
        entry?.destination?.hasRoute<Reservations>() == true ||
            entry?.destination?.hasRoute<BookingOffers>() == true ||
            entry?.destination?.hasRoute<BookingDetails>() == true -> Reservations
        entry?.destination?.hasRoute<Recommendations>() == true -> Recommendations
        entry?.destination?.hasRoute<Profile>() == true ||
            entry?.destination?.hasRoute<Registration>() == true ||
            entry?.destination?.hasRoute<Recovery>() == true -> Profile
        else -> Explore
    }
    val modeName = stringResource(appearance.mode.labelRes())
    val colors = MaterialTheme.colorScheme
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(containerColor = colors.background, topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            title = {
                Row {
                    Icon(
                        painterResource(R.drawable.ic_brand),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.city),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = { showAppearance = true },
                    modifier = Modifier.semantics { stateDescription = modeName }
                ) {
                    Icon(Icons.Outlined.Palette, stringResource(R.string.appearance))
                }
            }
        )
    }, bottomBar = {
        if (!keyboardOpen) {
            NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                destinations.forEach { item ->
                    NavigationBarItem(
                        selected = selectedRoute == item.route,
                        onClick = {
                            controller.navigate(item.route) {
                                popUpTo(controller.graph.findStartDestination().id) {
                                    saveState =
                                        false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.onPrimary,
                            selectedTextColor = colors.onSurface,
                            indicatorColor = colors.primary,
                            unselectedIconColor = colors.onSurfaceVariant,
                            unselectedTextColor = colors.onSurfaceVariant
                        ),
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) }
                    )
                }
            }
        }
    }) { insets ->
        NavHost(controller, startDestination = Explore, modifier = Modifier.padding(insets)) {
            composable<Explore> {
                CatalogRoute(
                    onCategory = { controller.navigate(CatalogSearch(it)) },
                    onSearch = { controller.navigate(CatalogSearch()) }
                )
            }
            composable<CatalogSearch> { backStack ->
                val route = backStack.toRoute<CatalogSearch>()
                CatalogSearchRoute(
                    initialCategory = route.category,
                    onBack = { controller.popBackStack() },
                    onDetail = { controller.navigate(CatalogDetail(it)) }
                )
            }
            composable<CatalogDetail> { backStack ->
                val route = backStack.toRoute<CatalogDetail>()
                CatalogDetailRoute(
                    id = route.id,
                    onBack = { controller.popBackStack() },
                    onBook = { controller.navigate(BookingOffers(it)) }
                )
            }
            composable<Registration> { RegistrationRoute(onBack = { controller.popBackStack() }) }
            composable<Recovery> { RecoveryRoute(onBack = { controller.popBackStack() }) }
            composable<Reservations> {
                AccountRoute(
                    onRecover = { controller.navigate(Recovery) },
                    onRegister = { controller.navigate(Registration) }
                ) {
                    BookingListRoute(
                        onOpen = { controller.navigate(BookingDetails(it)) },
                        onExplore = {
                            controller.navigate(Explore) {
                                popUpTo(controller.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            composable<BookingOffers> { backStack ->
                val route = backStack.toRoute<BookingOffers>()
                AccountRoute(
                    onRecover = { controller.navigate(Recovery) },
                    onRegister = { controller.navigate(Registration) }
                ) {
                    BookingOffersRoute(
                        serviceRef = route.serviceRef,
                        existingReference = route.existingReference,
                        onBack = { controller.popBackStack() },
                        onCreated = { reference ->
                            controller.navigate(BookingDetails(reference)) {
                                popUpTo<BookingOffers> { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            composable<BookingDetails> { backStack ->
                val route = backStack.toRoute<BookingDetails>()
                AccountRoute(
                    onRecover = { controller.navigate(Recovery) },
                    onRegister = { controller.navigate(Registration) }
                ) {
                    PdfDocumentHost("VitasMarta-reserva.pdf") { save ->
                        BookingDetailRoute(
                            reference = route.reference,
                            onBack = { controller.popBackStack() },
                            onModify = { service, reference ->
                                controller.navigate(BookingOffers(service, reference))
                            },
                            onDocument = save
                        )
                    }
                }
            }
            composable<Recommendations> {
                AccountRoute(
                    onRecover = { controller.navigate(Recovery) },
                    onRegister = { controller.navigate(Registration) }
                ) {
                    RecommendationsRoute(
                        onAttraction = { controller.navigate(CatalogDetail(it)) },
                        onProfile = {
                            controller.navigate(Profile) {
                                popUpTo(controller.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            composable<Profile> {
                AccountRoute(
                    onRecover = { controller.navigate(Recovery) },
                    onRegister = { controller.navigate(Registration) }
                ) { ProfileDestination(onLogout = auth::logout) }
            }
        }
    }
    if (showAppearance) AppearanceSheet(appearance, onThemeSelected) { showAppearance = false }
}
