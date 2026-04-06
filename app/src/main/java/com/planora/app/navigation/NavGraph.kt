package com.planora.app.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.planora.app.R
import com.planora.app.feature.calendar.CalendarScreen
import com.planora.app.feature.dashboard.DashboardScreen
import com.planora.app.feature.money.AddTransactionScreen
import com.planora.app.feature.money.MoneyScreen
import com.planora.app.feature.notes.NoteEditorScreen
import com.planora.app.feature.notes.NotesScreen
import com.planora.app.feature.savings.SavingsScreen
import com.planora.app.feature.settings.SettingsScreen
import com.planora.app.feature.tasks.AddEditTaskScreen
import com.planora.app.feature.tasks.TasksScreen
import com.planora.app.feature.welcome.WelcomeScreen
import com.planora.app.core.utils.PrefsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Routes and Destinations
sealed class Screen(val route: String) {
    data object Welcome    : Screen("welcome")
    data object Main       : Screen("main")       // hosts the swipeable pager
    data object Dashboard  : Screen("dashboard")
    data object Tasks      : Screen("tasks")
    data object Money      : Screen("money")
    data object Savings    : Screen("savings")
    data object Calendar   : Screen("calendar")
    data object Notes      : Screen("notes")
    data object Settings   : Screen("settings")

    data object AddEditTask : Screen("add_edit_task?taskId={taskId}") {
        fun createRoute(id: Long? = null) = if (id != null) "add_edit_task?taskId=$id" else "add_edit_task"
    }
    data object AddEditTransaction : Screen("add_edit_transaction?transactionId={transactionId}") {
        fun createRoute(id: Long? = null) = if (id != null) "add_edit_transaction?transactionId=$id" else "add_edit_transaction"
    }
    data object AddEditEvent : Screen("add_edit_event?eventId={eventId}&date={date}") {
        fun createRoute(id: Long? = null, date: Long? = null) = when {
            id != null -> "add_edit_event?eventId=$id"
            date != null -> "add_edit_event?date=$date"
            else -> "add_edit_event"
        }
    }
    data object AddEditGoal : Screen("add_edit_goal?goalId={goalId}") {
        fun createRoute(id: Long? = null) = if (id != null) "add_edit_goal?goalId=$id" else "add_edit_goal"
    }
    data object NoteEditor : Screen("note_editor?noteId={noteId}") {
        fun createRoute(id: Long? = null) = if (id != null) "note_editor?noteId=$id" else "note_editor"
    }
}

// Swipeable pages configuration  --  order matches the navbar left→right
// Dashboard(0) Â· Tasks(1) Â· Money(2) Â· Savings(3) Â· Calendar(4) Â· Notes(5)
private val ScrimColor = Color.Black.copy(alpha = 0.5f)

private val swipePages = listOf(
    Screen.Dashboard,
    Screen.Tasks,
    Screen.Money,
    Screen.Savings,
    Screen.Calendar,
    Screen.Notes
)
// O(1) lookup  --  avoids linear scan on every nav-bar tap
private val swipePageIndex: Map<Screen, Int> = swipePages.withIndex().associate { (i, s) -> s to i }

// Navbar items  --  Dashboard intentionally excluded from pills
data class NavItem(val screen: Screen, val label: String, @param:DrawableRes val icon: Int)

val navItems = listOf(
    NavItem(Screen.Tasks,    "Tasks",    R.drawable.ic_check_circle),
    NavItem(Screen.Money,    "Money",    R.drawable.ic_account_balance),
    NavItem(Screen.Savings,  "Savings",  R.drawable.ic_savings),
    NavItem(Screen.Calendar, "Calendar", R.drawable.ic_calendar_month),
    NavItem(Screen.Notes,    "Notes",    R.drawable.ic_sticky_note)
)

// Main Navigation Entry
@Composable
fun PlanoraNavGraph(prefsManager: PrefsManager) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var startDest by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val onboarded = prefsManager.hasOnboarded.first()
        startDest = if (onboarded) Screen.Main.route else Screen.Welcome.route
    }
    if (startDest == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    // NavHost only handles non-swipeable destinations.
    // The pager lives inside Screen.Main and handles all tab-level navigation.
    NavHost(
        navController    = navController,
        startDestination = startDest!!,
        modifier         = Modifier.fillMaxSize(),
        // scaleIn/Out is GPU-only (graphicsLayer); slideInVertically forces a layout pass every frame
        enterTransition    = { fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.94f) + slideInVertically(tween(260)) { it / 24 } },
        exitTransition     = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.94f) },
        popExitTransition  = { fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.94f) + slideOutVertically(tween(200)) { it / 24 } }
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(onComplete = { name ->
                scope.launch {
                    prefsManager.setUserName(name)
                    prefsManager.setHasOnboarded(true)
                }
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            })
        }

        // Main pager host component
        composable(Screen.Main.route) {
            MainPagerHost(
                onNavigateToAddTask          = { navController.navigate(Screen.AddEditTask.createRoute()) },
                onNavigateToEditTask         = { navController.navigate(Screen.AddEditTask.createRoute(it)) },
                onNavigateToAddTransaction   = { navController.navigate(Screen.AddEditTransaction.createRoute()) },
                onNavigateToEditTransaction  = { navController.navigate(Screen.AddEditTransaction.createRoute(it)) },
                onNavigateToAddEvent         = { date -> navController.navigate(Screen.AddEditEvent.createRoute(date = date)) },
                onNavigateToEditEvent        = { id -> navController.navigate(Screen.AddEditEvent.createRoute(id = id)) },
                onNavigateToAddGoal          = { navController.navigate(Screen.AddEditGoal.createRoute()) },
                onNavigateToEditGoal         = { id -> navController.navigate(Screen.AddEditGoal.createRoute(id = id)) },
                onNavigateToNote             = { navController.navigate(Screen.NoteEditor.createRoute(it)) },
                onNavigateToSettings         = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Detail / editor screens
        composable(Screen.AddEditTask.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            AddEditTaskScreen(
                taskId = back.arguments?.getLong("taskId").takeIf { it != -1L },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddEditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            AddTransactionScreen(
                transactionId = back.arguments?.getLong("transactionId").takeIf { it != -1L },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddEditEvent.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("date") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { back ->
            com.planora.app.feature.calendar.AddEditEventScreen(
                eventId = back.arguments?.getLong("eventId").takeIf { it != -1L },
                selectedDate = back.arguments?.getLong("date").takeIf { it != -1L },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddEditGoal.route,
            arguments = listOf(navArgument("goalId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            com.planora.app.feature.savings.AddEditGoalScreen(
                goalId = back.arguments?.getLong("goalId").takeIf { it != -1L },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.NoteEditor.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = -1L })
        ) { back ->
            NoteEditorScreen(
                noteId = back.arguments?.getLong("noteId").takeIf { it != -1L },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}

// Pager host  --  handles swipeable pages, navigation UI, and sidebar
@Composable
private fun MainPagerHost(
    onNavigateToAddTask: () -> Unit,
    onNavigateToEditTask: (Long) -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToAddEvent: (Long?) -> Unit,
    onNavigateToEditEvent: (Long) -> Unit,
    onNavigateToAddGoal: () -> Unit,
    onNavigateToEditGoal: (Long) -> Unit,
    onNavigateToNote: (Long?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scope      = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { swipePages.size }
    var sidebarOpen by remember { mutableStateOf(false) }

    val currentPageIndex = pagerState.currentPage

    // Stable lambda: captures only remembered objects, not recreated on every recompose
    val navigateToPage: (Int) -> Unit = remember(scope, pagerState) {
        { index -> scope.launch { pagerState.animateScrollToPage(index) } }
    }

    Box(Modifier.fillMaxSize()) {

        // â”€â”€ Swipeable pages â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        HorizontalPager(
            state            = pagerState,
            modifier         = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            beyondViewportPageCount = 0   // compose only the visible page; adjacent pages resume from state on swipe
        ) { page ->
            when (swipePages[page]) {
                Screen.Dashboard -> DashboardScreen(
                    onNavigateToMoney   = { navigateToPage(2) },
                    onNavigateToTasks   = { navigateToPage(1) },
                    onNavigateToSavings = { navigateToPage(3) }
                )
                Screen.Tasks    -> TasksScreen(
                    onNavigateToAddTask  = onNavigateToAddTask,
                    onNavigateToEditTask = onNavigateToEditTask
                )
                Screen.Money    -> MoneyScreen(
                    onNavigateToAddTransaction  = onNavigateToAddTransaction,
                    onNavigateToEditTransaction = onNavigateToEditTransaction
                )
                Screen.Savings  -> SavingsScreen(
                    onNavigateToAddGoal  = onNavigateToAddGoal,
                    onNavigateToEditGoal = onNavigateToEditGoal
                )
                Screen.Calendar -> CalendarScreen(
                    onNavigateToAddEvent  = onNavigateToAddEvent,
                    onNavigateToEditEvent = onNavigateToEditEvent
                )
                Screen.Notes    -> NotesScreen(onNavigateToNote = onNavigateToNote)
                else            -> Unit
            }
        }

        // â”€â”€ Floating nav bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        FloatingNavBar(
            items          = navItems,
            currentPage    = currentPageIndex,
            onMenuClick    = { sidebarOpen = true },
            onNavigate     = { screen -> navigateToPage(swipePageIndex[screen] ?: 0) },
            modifier       = Modifier.align(Alignment.BottomCenter)
        )

        // â”€â”€ Sidebar scrim â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        AnimatedVisibility(visible = sidebarOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(ScrimColor)
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() }) { sidebarOpen = false }
            )
        }
        AnimatedVisibility(
            visible  = sidebarOpen,
            enter    = slideInHorizontally(tween(280, easing = EaseOutCubic)) { -it },
            exit     = slideOutHorizontally(tween(220, easing = EaseInCubic)) { -it },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Sidebar(
                currentPage      = currentPageIndex,
                onNavigateToPage = { index -> sidebarOpen = false; navigateToPage(index) },
                onNavigateToSettings = { sidebarOpen = false; onNavigateToSettings() }
            )
        }
    }
}

// â”€â”€ Floating navbar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    currentPage: Int,
    onMenuClick: () -> Unit,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Surface(
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(32.dp),
            color           = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            tonalElevation  = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                NavPill(
                    iconRes  = R.drawable.ic_menu,
                    label    = "Menu",
                    selected = false,
                    onClick  = onMenuClick,
                    modifier = Modifier.weight(1f)
                )
                items.forEachIndexed { i, item ->
                    // navItems start at Tasks = page 1, so offset by 1
                    val pageIndex = i + 1
                    NavPill(
                        iconRes  = item.icon,
                        label    = item.label,
                        selected = currentPage == pageIndex,
                        onClick  = { onNavigate(item.screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// â”€â”€ Nav pill â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun NavPill(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "scale_$label"
    )
    val iconColor by animateColorAsState(
        targetValue   = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label         = "color_$label"
    )
    val pillColor = MaterialTheme.colorScheme.primary

    Box(
        modifier         = modifier.fillMaxHeight()
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(pillColor)
        )
        Icon(painterResource(iconRes), label, tint = iconColor, modifier = Modifier.size(22.dp))
    }
}

// â”€â”€ Sidebar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun Sidebar(
    currentPage: Int,
    onNavigateToPage: (Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Surface(
        modifier        = Modifier.fillMaxHeight().width(280.dp),
        shape           = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 20.dp,
        tonalElevation  = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .statusBarsPadding().navigationBarsPadding()
                .padding(vertical = 24.dp)
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_splash_icon), null,
                            tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp))
                    }
                }
                Text("Planora", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Dashboard (page 0)
            SidebarItem(R.drawable.ic_dashboard, "Dashboard", currentPage == 0) { onNavigateToPage(0) }

            Spacer(Modifier.height(4.dp))
            Text("Main", style = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))

            // Tab pages (pages 1—5)
            navItems.forEachIndexed { i, item ->
                SidebarItem(item.icon, item.label, currentPage == i + 1) { onNavigateToPage(i + 1) }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            SidebarItem(R.drawable.ic_settings, "Settings", false, onNavigateToSettings)
        }
    }
}

@Composable
private fun SidebarItem(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val bgTarget = remember(selected, primary) {
        if (selected) primary.copy(alpha = 0.15f) else Color.Transparent
    }
    val bgColor by animateColorAsState(bgTarget, tween(180), label = "sb_bg_$label")
    val contentColor by animateColorAsState(
        if (selected) primary else MaterialTheme.colorScheme.onSurface,
        tween(180), label = "sb_col_$label"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(painterResource(iconRes), label, tint = contentColor, modifier = Modifier.size(20.dp))
        Text(label,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = contentColor)
    }
}
