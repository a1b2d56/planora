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
import androidx.compose.runtime.saveable.rememberSaveable
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


sealed class Screen(val route: String) {
    data object Welcome    : Screen("welcome")
    data object Main       : Screen("main")
    data object Dashboard  : Screen("dashboard")
    data object Tasks      : Screen("tasks")
    data object Money      : Screen("money")
    data object Savings    : Screen("savings")
    data object Calendar   : Screen("calendar")
    data object Notes      : Screen("notes")
    data object Settings   : Screen("settings")

    data object Accounts   : Screen("accounts")
    data object AccountDetail : Screen("account_detail/{accountId}")
    data object Budgets    : Screen("budgets")
    data object Insights   : Screen("insights")
    data object Subscriptions : Screen("subscriptions")
    data object SmsImport : Screen("sms_import")

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


private val ScrimColor = Color.Black.copy(alpha = 0.5f)

data class NavItem(val screen: Screen, val label: String, @param:DrawableRes val icon: Int)

val allScreensMap = mapOf(
    "dashboard" to NavItem(Screen.Dashboard, "Dashboard", R.drawable.ic_dashboard),
    "tasks" to NavItem(Screen.Tasks, "Tasks", R.drawable.ic_check_circle),
    "money" to NavItem(Screen.Money, "Money", R.drawable.ic_account_balance),
    "savings" to NavItem(Screen.Savings, "Savings", R.drawable.ic_savings),
    "calendar" to NavItem(Screen.Calendar, "Calendar", R.drawable.ic_calendar_month),
    "notes" to NavItem(Screen.Notes, "Notes", R.drawable.ic_sticky_note)
)

@Composable
fun PlanoraNavGraph(prefsManager: PrefsManager) {
    val navbarPages by prefsManager.navbarPages.collectAsState(initial = setOf("tasks", "money", "savings", "calendar"))
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


    NavHost(
        navController    = navController,
        startDestination = startDest!!,
        modifier         = Modifier.fillMaxSize(),
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


        composable(Screen.Main.route) {
            MainPagerHost(
                navbarPagesSet               = navbarPages,
                onNavigateToAddTask          = { navController.navigate(Screen.AddEditTask.createRoute()) },
                onNavigateToEditTask         = { navController.navigate(Screen.AddEditTask.createRoute(it)) },
                onNavigateToAddTransaction   = { navController.navigate(Screen.AddEditTransaction.createRoute()) },
                onNavigateToEditTransaction  = { navController.navigate(Screen.AddEditTransaction.createRoute(it)) },
                onNavigateToAddEvent         = { date -> navController.navigate(Screen.AddEditEvent.createRoute(date = date)) },
                onNavigateToEditEvent        = { id -> navController.navigate(Screen.AddEditEvent.createRoute(id = id)) },
                onNavigateToAddGoal          = { navController.navigate(Screen.AddEditGoal.createRoute()) },
                onNavigateToEditGoal         = { id -> navController.navigate(Screen.AddEditGoal.createRoute(id = id)) },
                onNavigateToNote             = { navController.navigate(Screen.NoteEditor.createRoute(it)) },
                onNavigateToSettings         = { navController.navigate(Screen.Settings.route) },
                onNavigateToAccounts         = { navController.navigate(Screen.Accounts.route) },
                onNavigateToBudgets          = { navController.navigate(Screen.Budgets.route) },
                onNavigateToInsights         = { navController.navigate(Screen.Insights.route) },
                onNavigateToSubscriptions    = { navController.navigate(Screen.Subscriptions.route) },
                onNavigateToSmsImport        = { navController.navigate(Screen.SmsImport.route) }
            )
        }

        composable(Screen.Accounts.route) { com.planora.app.feature.money.AccountsScreen(onBack = { navController.popBackStack() }, onNavigateToDetail = { id -> navController.navigate("account_detail/$id") }) }
        composable(Screen.AccountDetail.route) { back ->
            val id = back.arguments?.getString("accountId")?.toLongOrNull() ?: -1L
            com.planora.app.feature.money.AccountDetailScreen(
                accountId = id,
                onBack = { navController.popBackStack() },
                onNavigateToEditTransaction = { txnId -> navController.navigate("add_edit_transaction?transactionId=$txnId") }
            )
        }
        composable(Screen.Budgets.route) { com.planora.app.feature.money.BudgetScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Insights.route) { com.planora.app.feature.money.InsightsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Subscriptions.route) { com.planora.app.feature.money.SubscriptionsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.SmsImport.route) { com.planora.app.feature.money.SmsImportScreen(onBack = { navController.popBackStack() }) }


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


@Composable
private fun MainPagerHost(
    navbarPagesSet: Set<String>,
    onNavigateToAddTask: () -> Unit,
    onNavigateToEditTask: (Long) -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToAddEvent: (Long?) -> Unit,
    onNavigateToEditEvent: (Long) -> Unit,
    onNavigateToAddGoal: () -> Unit,
    onNavigateToEditGoal: (Long) -> Unit,
    onNavigateToNote: (Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToSmsImport: () -> Unit
) {
    val scope      = rememberCoroutineScope()
    var sidebarOpen by remember { mutableStateOf(false) }

    var activeUnpinnedScreen by rememberSaveable { mutableStateOf<String?>(null) }

    val dynamicNavItems = remember(navbarPagesSet) {
        val orderedKeys = listOf("tasks", "money", "savings", "calendar", "notes")
        orderedKeys.filter { it in navbarPagesSet }.take(5).mapNotNull { allScreensMap[it] }
    }

    val dynamicSwipePages = remember(dynamicNavItems, activeUnpinnedScreen) {
        val pages = mutableListOf<Screen>(Screen.Dashboard)
        pages.addAll(dynamicNavItems.map { it.screen })
        val unpinned: Screen? = if (activeUnpinnedScreen != null) allScreensMap[activeUnpinnedScreen]?.screen else null
        if (unpinned != null && !pages.contains(unpinned)) {
            pages.add(unpinned)
        }
        pages
    }

    val swipePageIndex = remember(dynamicSwipePages) { dynamicSwipePages.withIndex().associate { (i, s) -> s to i } }

    val pagerState = rememberPagerState(initialPage = 0) { dynamicSwipePages.size }
    val currentPageIndex = pagerState.currentPage

    val navigateToPage: (Int) -> Unit = remember(scope, pagerState) {
        { index -> scope.launch { pagerState.animateScrollToPage(index) } }
    }

    var targetScrollScreen by remember { mutableStateOf<Screen?>(null) }
    
    LaunchedEffect(dynamicSwipePages, targetScrollScreen) {
        if (targetScrollScreen != null) {
            val index = dynamicSwipePages.indexOf(targetScrollScreen)
            if (index != -1) {
                pagerState.scrollToPage(index)
                targetScrollScreen = null
            }
        }
    }

    LaunchedEffect(currentPageIndex) {
        val currentScreen = dynamicSwipePages.getOrNull(currentPageIndex)
        if (activeUnpinnedScreen != null) {
            val unpinned = allScreensMap[activeUnpinnedScreen]?.screen
            if (currentScreen != null && currentScreen != unpinned) {
                activeUnpinnedScreen = null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {

        // Swipeable pages
        HorizontalPager(
            state            = pagerState,
            modifier         = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            beyondViewportPageCount = 0
        ) { page ->
            when (dynamicSwipePages[page]) {
                Screen.Dashboard -> DashboardScreen(
                    onNavigateToMoney   = { navigateToPage(swipePageIndex[Screen.Money] ?: 0) },
                    onNavigateToTasks   = { navigateToPage(swipePageIndex[Screen.Tasks] ?: 0) },
                    onNavigateToSavings = { navigateToPage(swipePageIndex[Screen.Savings] ?: 0) }
                )
                Screen.Tasks    -> TasksScreen(
                    onNavigateToAddTask  = onNavigateToAddTask,
                    onNavigateToEditTask = onNavigateToEditTask
                )
                Screen.Money    -> MoneyScreen(
                    onNavigateToAddTransaction  = onNavigateToAddTransaction,
                    onNavigateToEditTransaction = onNavigateToEditTransaction,
                    onNavigateToAccounts = onNavigateToAccounts,
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToInsights = onNavigateToInsights,
                    onNavigateToSubscriptions = onNavigateToSubscriptions,
                    onNavigateToSmsImport = onNavigateToSmsImport
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

        // Floating nav bar
        FloatingNavBar(
            items          = dynamicNavItems,
            currentScreen  = dynamicSwipePages.getOrNull(currentPageIndex),
            onMenuClick    = { sidebarOpen = true },
            onNavigate     = { screen -> 
                activeUnpinnedScreen = null
                navigateToPage(swipePageIndex[screen] ?: 0) 
            },
            modifier       = Modifier.align(Alignment.BottomCenter)
        )

        val currentScreen = dynamicSwipePages.getOrNull(currentPageIndex)
        val onSidebarNavigate: (String) -> Unit = { key ->
            sidebarOpen = false
            val screenObj = allScreensMap[key]
            if (screenObj != null) {
                val screen = screenObj.screen
                if (dynamicNavItems.any { it.screen == screen } || screen == Screen.Dashboard) {
                    activeUnpinnedScreen = null
                    val index = dynamicSwipePages.indexOf(screen)
                    if (index != -1) navigateToPage(index)
                } else {
                    activeUnpinnedScreen = key
                    targetScrollScreen = screen
                }
            }
        }

        // Sidebar
        SidebarOverlay(
            isOpen               = sidebarOpen,
            onClose              = { sidebarOpen = false },
            currentScreen        = currentScreen,
            onNavigateToPage     = onSidebarNavigate,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

// Floating navbar
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    currentScreen: Screen?,
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
                items.forEach { item ->
                    NavPill(
                        iconRes  = item.icon,
                        label    = item.label,
                        selected = currentScreen == item.screen,
                        onClick  = { onNavigate(item.screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Nav pill
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

// Sidebar Overlay
@Composable
private fun SidebarOverlay(
    isOpen: Boolean,
    onClose: () -> Unit,
    currentScreen: Screen?,
    onNavigateToPage: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    AnimatedVisibility(visible = isOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(ScrimColor)
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) { onClose() }
        )
    }
    AnimatedVisibility(
        visible  = isOpen,
        enter    = slideInHorizontally(tween(280, easing = EaseOutCubic)) { -it },
        exit     = slideOutHorizontally(tween(220, easing = EaseInCubic)) { -it },
        modifier = Modifier.fillMaxSize()
    ) {
        Sidebar(
            currentScreen      = currentScreen,
            onNavigateToPage = onNavigateToPage,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

// Sidebar
@Composable
private fun Sidebar(
    currentScreen: Screen?,
    onNavigateToPage: (String) -> Unit,
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


            SidebarItem(R.drawable.ic_dashboard, "Dashboard", currentScreen == Screen.Dashboard) { onNavigateToPage("dashboard") }

            Spacer(Modifier.height(8.dp))
            Text("General", style = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))

            SidebarItem(R.drawable.ic_check_circle, "Tasks", currentScreen == Screen.Tasks) { onNavigateToPage("tasks") }
            SidebarItem(R.drawable.ic_account_balance, "Money", currentScreen == Screen.Money) { onNavigateToPage("money") }
            SidebarItem(R.drawable.ic_savings, "Savings", currentScreen == Screen.Savings) { onNavigateToPage("savings") }

            Spacer(Modifier.height(12.dp))
            Text("Tools & Personal", style = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))

            SidebarItem(R.drawable.ic_calendar_month, "Events", currentScreen == Screen.Calendar) { onNavigateToPage("calendar") }
            SidebarItem(R.drawable.ic_sticky_note, "Notes", currentScreen == Screen.Notes) { onNavigateToPage("notes") }

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
