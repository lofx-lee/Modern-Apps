package com.vayunmathur.calendar.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.ListItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.format
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vayunmathur.calendar.R
import com.vayunmathur.calendar.Route
import com.vayunmathur.calendar.data.Calendar
import com.vayunmathur.calendar.data.Event
import com.vayunmathur.calendar.data.Instance
import com.vayunmathur.calendar.util.CalendarViewModel
import com.vayunmathur.library.ui.IconAdd
import com.vayunmathur.library.ui.IconSettings
import com.vayunmathur.library.util.NavBackStack
import com.vayunmathur.library.util.ResultEffect
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, backStack: NavBackStack<Route>) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val events by viewModel.events.collectAsState()
    val calendarsList by viewModel.calendars.collectAsState()
    val calendars = calendarsList.associateBy { it.id }
    val calendarVisibility by viewModel.calendarVisibility.collectAsState()
    val currentLayout by viewModel.currentLayout.collectAsState()

    val vEventsByID = events.associateBy { it.id!! }


    // compute today's date and restore last viewed date if available
    val lastViewed by viewModel.lastViewedDate.collectAsState()
    val initialDate =
        lastViewed ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var dateViewing by remember { mutableStateOf(initialDate) }

    // state for which week to show; 0 = current week, +1 = next week, -1 = previous week

    // shared vertical scroll so hour labels and grid scroll together
    val verticalState = rememberScrollState()

    ResultEffect<LocalDate>("GotoDate") { result ->
        dateViewing = result
    }
    val visibleSunday = dateViewing - DatePeriod(days = dateViewing.dayOfWeek.isoDayNumber % 7)

    Scaffold(
        Modifier,
        {
            TopAppBar(
                {
                    // show month/year of the currently visible week's Sunday
                    val mon = MonthNames.ENGLISH_ABBREVIATED.names[visibleSunday.month.number - 1]
                    Row(
                        Modifier.clickable { backStack.add(Route.Calendar.GotoDialog(dateViewing)) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.month_year_format, mon, visibleSunday.year), fontWeight = FontWeight.Bold)
                        Icon(painterResource(R.drawable.arrow_drop_down_24px), null)
                    }
                }, actions = {
                    var showLayoutMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { showLayoutMenu = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currentLayout.shortName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Icon(painterResource(R.drawable.arrow_drop_down_24px), null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        DropdownMenu(expanded = showLayoutMenu, onDismissRequest = { showLayoutMenu = false }) {
                            CalendarViewModel.CalendarLayout.entries.forEach { layout ->
                                DropdownMenuItem(
                                    text = { Text(layout.prettyName) },
                                    onClick = {
                                        viewModel.setLayout(layout)
                                        showLayoutMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton({ backStack.add(Route.Settings) }) {
                        IconSettings()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton({
                // persist currently viewed date before navigating to the new event page
                viewModel.setLastViewedDate(dateViewing)
                backStack.add(Route.EditEvent(null))
            }) {
                IconAdd()
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentLayout) {
                CalendarViewModel.CalendarLayout.Agenda -> AgendaView(context, events, calendars, calendarVisibility, dateViewing, onEventClick = {
                    viewModel.setLastViewedDate(dateViewing)
                    backStack.add(Route.Event(it))
                })
                CalendarViewModel.CalendarLayout.Month -> MonthView(context, events, calendars, calendarVisibility, dateViewing, onEventClick = {
                    viewModel.setLastViewedDate(dateViewing)
                    backStack.add(Route.Event(it))
                }, onDayClick = {
                    dateViewing = it
                    viewModel.setLayout(CalendarViewModel.CalendarLayout.Day)
                }, onSwipe = {
                    dateViewing = if (it > 0) dateViewing.minus(DatePeriod(months = 1)) else dateViewing.plus(DatePeriod(months = 1))
                })
                CalendarViewModel.CalendarLayout.WorkWeekSummary, CalendarViewModel.CalendarLayout.FullWeekSummary -> {
                    val daysToShow = if (currentLayout == CalendarViewModel.CalendarLayout.WorkWeekSummary) 5 else 7
                    val startDay = if (daysToShow == 5) {
                        dateViewing - DatePeriod(days = (dateViewing.dayOfWeek.isoDayNumber - 1) % 7)
                    } else visibleSunday
                    
                    SummaryView(context, events, calendars, calendarVisibility, startDay, daysToShow, onEventClick = {
                        viewModel.setLastViewedDate(dateViewing)
                        backStack.add(Route.Event(it))
                    }, onSwipe = {
                        val period = if (daysToShow == 5) DatePeriod(days = 7) else DatePeriod(days = 7)
                        dateViewing = if (it > 0) dateViewing.minus(period) else dateViewing.plus(period)
                    })
                }
                else -> {
                    // Hourly views (Day, WorkWeek, FullWeek)
                    val daysToShow = when (currentLayout) {
                        CalendarViewModel.CalendarLayout.Day -> 1
                        CalendarViewModel.CalendarLayout.WorkWeek -> 5
                        else -> 7
                    }
                    val startDay = when (currentLayout) {
                        CalendarViewModel.CalendarLayout.Day -> dateViewing
                        CalendarViewModel.CalendarLayout.WorkWeek -> dateViewing - DatePeriod(days = (dateViewing.dayOfWeek.isoDayNumber - 1) % 7)
                        else -> visibleSunday
                    }

                    HourlyCalendarView(context, events, calendars, calendarVisibility, startDay, daysToShow, verticalState, onEventClick = {
                        viewModel.setLastViewedDate(dateViewing)
                        backStack.add(Route.Event(it))
                    }, onSwipe = {
                        val period = if (daysToShow == 1) DatePeriod(days = 1) else DatePeriod(days = 7)
                        dateViewing = if (it > 0) dateViewing.minus(period) else dateViewing.plus(period)
                    })
                }
            }
        }
    }
}

@Composable
fun HourlyCalendarView(
    context: android.content.Context,
    events: List<Event>,
    calendars: Map<Long, Calendar>,
    calendarVisibility: Map<Long, Boolean>,
    startDay: LocalDate,
    daysToShow: Int,
    verticalState: ScrollState,
    onEventClick: (Instance) -> Unit,
    onSwipe: (Float) -> Unit
) {
    val vEventsByID = events.associateBy { it.id!! }
    val weekDays = (0 until daysToShow).map { startDay.plus(DatePeriod(days = it)) }

    val weekInstances = Instance.getInstances(
        context,
        weekDays.first().atStartOfDayIn(TimeZone.currentSystemDefault()),
        weekDays.last().atEndOfDayIn(TimeZone.currentSystemDefault())
    )
        .filter { it.eventID in vEventsByID }
        .filter { calendarVisibility[vEventsByID[it.eventID]!!.calendarID] ?: true }

    val (allDay, notAllDay) = weekInstances.partition { it.allDay }

    val allDayByDate: Map<LocalDate, List<Instance>> = weekDays.associateWith { d ->
        allDay.filter { instance -> d in instance.spanDays }
    }

    val timedByDateHour: Map<LocalDate, Map<Int, List<Instance>>> =
        weekDays.associateWith { d ->
            val timed = notAllDay.filter { instance -> d in instance.spanDays }
            timed.groupBy { ev ->
                ev.startDateTime.hour
            }
        }

    Row(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var dragTotal = 0f
                detectHorizontalDragGestures({}, {
                    val threshold = 100f
                    if (kotlin.math.abs(dragTotal) >= threshold) {
                        onSwipe(dragTotal)
                    }
                    dragTotal = 0f
                }, { dragTotal = 0f }, { change, delta ->
                    dragTotal += delta
                    change.consume()
                })
            }
    ) {
        var yOffset by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        // Hour labels column
        Column {
            Spacer(Modifier.height(yOffset))
            Column(
                Modifier
                    .verticalScroll(verticalState)
                    .padding(bottom = 16.dp)
            ) {
                for (hour in 0..23) {
                    Box(modifier = Modifier
                        .height(56.dp)
                        .width(56.dp)) {
                        val hourString = if(DateFormat.is24HourFormat(context)) {
                            "%02d:00".format(hour)
                        } else {
                            if (hour == 0) context.getString(R.string.twelve_am) else if (hour < 12) context.getString(R.string.hour_am, hour) else if (hour == 12) context.getString(R.string.twelve_pm) else context.getString(R.string.hour_pm, hour - 12)
                        }
                        Text(
                            text = hourString,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            WeekHeader(weekDays)
            AllDayRow(allDayByDate, vEventsByID, calendars, weekDays, onEventClick)
            Spacer(Modifier.onGloballyPositioned {
                yOffset = with(density) { it.positionInParent().y.toDp() }
            })
            HourlyGrid(
                timedByDateHour,
                weekDays,
                verticalState,
                onEventClick,
                PaddingValues(0.dp)
            )
        }
    }
}

@Composable
fun AgendaView(
    context: android.content.Context,
    events: List<Event>,
    calendars: Map<Long, Calendar>,
    calendarVisibility: Map<Long, Boolean>,
    startDate: LocalDate,
    onEventClick: (Instance) -> Unit
) {
    val vEventsByID = events.associateBy { it.id!! }
    val endDate = startDate.plus(DatePeriod(months = 1))
    
    val instances = remember(events, calendarVisibility, startDate) {
        Instance.getInstances(
            context,
            startDate.atStartOfDayIn(TimeZone.currentSystemDefault()),
            endDate.atEndOfDayIn(TimeZone.currentSystemDefault())
        )
            .filter { it.eventID in vEventsByID }
            .filter { calendarVisibility[vEventsByID[it.eventID]!!.calendarID] ?: true }
            .sortedBy { it.startDateTime }
    }

    val groupedInstances = remember(instances) {
        instances.groupBy { it.startDateTime.date }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        groupedInstances.forEach { (date, dayInstances) ->
            item {
                Text(
                    text = date.format(dateFormat),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(dayInstances) { instance ->
                val ev = vEventsByID[instance.eventID]!!
                ListItem(
                    headlineContent = { Text(ev.title.ifEmpty { context.getString(R.string.no_title) }) },
                    supportingContent = {
                        Text(dateRangeString(context, instance.startDateTimeDisplay.date, instance.endDateTimeDisplay.date, instance.startDateTimeDisplay.time, instance.endDateTimeDisplay.time, instance.allDay))
                    },
                    leadingContent = {
                        Box(Modifier.size(16.dp).background(Color(ev.color ?: calendars[ev.calendarID]!!.color), CircleShape))
                    },
                    modifier = Modifier.clickable { onEventClick(instance) }
                )
            }
        }
    }
}

@Composable
fun MonthView(
    context: android.content.Context,
    events: List<Event>,
    calendars: Map<Long, Calendar>,
    calendarVisibility: Map<Long, Boolean>,
    dateViewing: LocalDate,
    onEventClick: (Instance) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onSwipe: (Float) -> Unit
) {
    val firstOfMonth = LocalDate(dateViewing.year, dateViewing.month, 1)
    val lastOfMonth = firstOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    val startDay = firstOfMonth - DatePeriod(days = firstOfMonth.dayOfWeek.isoDayNumber % 7)
    val endDay = lastOfMonth + DatePeriod(days = (6 - lastOfMonth.dayOfWeek.isoDayNumber % 7))

    val instances = remember(events, calendarVisibility, startDay, endDay) {
        val vEventsByID = events.associateBy { it.id!! }
        Instance.getInstances(
            context,
            startDay.atStartOfDayIn(TimeZone.currentSystemDefault()),
            endDay.atEndOfDayIn(TimeZone.currentSystemDefault())
        )
            .filter { it.eventID in vEventsByID }
            .filter { calendarVisibility[vEventsByID[it.eventID]!!.calendarID] ?: true }
    }

    Column(Modifier.fillMaxSize().pointerInput(Unit) {
        var dragTotal = 0f
        detectHorizontalDragGestures({}, {
            if (kotlin.math.abs(dragTotal) >= 100f) onSwipe(dragTotal)
            dragTotal = 0f
        }, { dragTotal = 0f }, { change, delta ->
            dragTotal += delta
            change.consume()
        })
    }) {
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(day, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
        
        var current = startDay
        while (current <= endDay) {
            Row(Modifier.weight(1f)) {
                for (i in 0..6) {
                    val day = current
                    val isCurrentMonth = day.month == dateViewing.month
                    val dayInstances = instances.filter { day in it.spanDays }

                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, Color.DarkGray)
                            .background(if (isCurrentMonth) Color.Transparent else Color.Black.copy(alpha = 0.1f))
                            .clickable { onDayClick(day) }
                            .padding(2.dp)
                    ) {
                        Text(
                            day.day.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentMonth) Color.White else Color.Gray
                        )
                        dayInstances.take(3).forEach { instance ->
                            val ev = events.find { it.id == instance.eventID } ?: return@forEach
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 1.dp)
                                    .background(Color(ev.color ?: calendars[ev.calendarID]!!.color), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 2.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    ev.title.ifEmpty { context.getString(R.string.no_title) },
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    current = current.plus(DatePeriod(days = 1))
                }
            }
        }
    }
}

@Composable
fun SummaryView(
    context: android.content.Context,
    events: List<Event>,
    calendars: Map<Long, Calendar>,
    calendarVisibility: Map<Long, Boolean>,
    startDay: LocalDate,
    daysToShow: Int,
    onEventClick: (Instance) -> Unit,
    onSwipe: (Float) -> Unit
) {
    val weekDays = (0 until daysToShow).map { startDay.plus(DatePeriod(days = it)) }
    val vEventsByID = events.associateBy { it.id!! }

    val instances = remember(events, calendarVisibility, startDay) {
        Instance.getInstances(
            context,
            startDay.atStartOfDayIn(TimeZone.currentSystemDefault()),
            weekDays.last().atEndOfDayIn(TimeZone.currentSystemDefault())
        )
            .filter { it.eventID in vEventsByID }
            .filter { calendarVisibility[vEventsByID[it.eventID]!!.calendarID] ?: true }
    }

    Column(Modifier.fillMaxSize().pointerInput(Unit) {
        var dragTotal = 0f
        detectHorizontalDragGestures({}, {
            if (kotlin.math.abs(dragTotal) >= 100f) onSwipe(dragTotal)
            dragTotal = 0f
        }, { dragTotal = 0f }, { change, delta ->
            dragTotal += delta
            change.consume()
        })
    }) {
        WeekHeader(weekDays)
        Row(Modifier.fillMaxSize(), Arrangement.spacedBy(4.dp)) {
            weekDays.forEach { day ->
                val dayInstances = instances.filter { day in it.spanDays }.sortedBy { it.startDateTime }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color.DarkGray)
                        .padding(2.dp)
                ) {
                    dayInstances.forEach { instance ->
                        val ev = vEventsByID[instance.eventID]!!
                        Box(
                            Modifier
                                .padding(bottom = 2.dp)
                                .background(Color(ev.color ?: calendars[ev.calendarID]!!.color), RoundedCornerShape(4.dp))
                                .fillMaxWidth()
                                .clickable { onEventClick(instance) }
                                .padding(4.dp)
                        ) {
                            Text(
                                ev.title.ifEmpty { context.getString(R.string.no_title) },
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

fun LocalDate.atEndOfDayIn(currentSystemDefault: TimeZone): Instant {
    return this.plus(DatePeriod(days = 1)).atStartOfDayIn(currentSystemDefault)
}

@Composable
private fun WeekHeader(weekDays: List<LocalDate>) {
    Row(modifier = Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
        weekDays.forEach { d ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(d.dayOfWeek.name.take(3), Modifier, Color.Gray, fontSize = 12.sp)
                Text(d.day.toString(), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AllDayRow(
    allDayByDate: Map<LocalDate, List<Instance>>,
    events: Map<Long, Event>,
    calendars: Map<Long, Calendar>,
    weekDays: List<LocalDate>,
    onEventClick: (Instance) -> Unit
) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
        weekDays.forEach { d ->
            val instances = allDayByDate[d].orEmpty()
            Column(Modifier.weight(1f)) {
                if (instances.isEmpty()) {
                    Box(modifier = Modifier
                        .height(32.dp)
                        .border(1.dp, Color.DarkGray)) {}
                } else {
                    Column {
                        instances.forEach { instance ->
                            val ev = events[instance.eventID]!!
                            Box(
                                Modifier
                                    .padding(bottom = 4.dp)
                                    .border(1.dp, Color.Black)
                                    .background(Color(ev.color ?: calendars[ev.calendarID]!!.color))
                                    .height(28.dp)
                                    .clickable { onEventClick(instance) }
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    ev.title.ifEmpty { stringResource(R.string.no_title) },
                                    Modifier.padding(4.dp),
                                    Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyGrid(
    timedByDateHour: Map<LocalDate, Map<Int, List<Instance>>>,
    weekDays: List<LocalDate>,
    verticalState: ScrollState,
    onEventClick: (Instance) -> Unit,
    innerPadding: PaddingValues
) {
    // Each hour row height
    val hourRowHeight = 56.dp
    val minEventHeight = 18.dp
    val minEventWidth = 56.dp

    Row(
        Modifier
            .fillMaxSize()
            .verticalScroll(verticalState)
            .padding(bottom = innerPadding.calculateBottomPadding()).padding(bottom = 4.dp), Arrangement.spacedBy(4.dp)
    ) {
        // create 7 equal columns with weight so all 7 fit on screen
        for (d in weekDays) {
            // collect unique timed events for this day (timedByDateHour groups by hour)
            val eventsForDay = timedByDateHour[d]?.values?.flatten().orEmpty().distinctBy { it.id }

            Box(Modifier.weight(1f)) {
                // background hourly grid — fixed 24 rows
                Column {
                    for (hour in 0..23) {
                        Box(
                            Modifier
                                .height(hourRowHeight)
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF222222))
                                .background(Color(0xFF0F0F0F))
                        )
                    }
                }

                // compute positioned events using the helper that assigns columns for overlaps
                val positioned = computePositionedEventsForDay(eventsForDay, d)

                // overlay event segments positioned by their time within the day and column
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columnWidth = this.maxWidth

                    positioned.forEach { ev ->
                        val instance = eventsForDay.find { it.id == ev.instanceID }!!
                        // compute vertical position and height
                        val startHours = ev.startMinutes.toFloat() / 60f
                        val lengthHours = (ev.endMinutes - ev.startMinutes).toFloat() / 60f

                        val yOffset = hourRowHeight * startHours
                        var heightDp = hourRowHeight * lengthHours
                        if (heightDp < minEventHeight) heightDp = minEventHeight

                        // compute horizontal position and size
                        val widthFraction = 1f / ev.totalColumns.toFloat()
                        val xFraction = ev.columnIndex * widthFraction
                        val xOffsetDp = columnWidth * xFraction
                        val widthDp = (columnWidth * widthFraction).coerceAtLeast(minEventWidth)

                        Box(
                            Modifier
                                .offset(xOffsetDp, yOffset)
                                .size(widthDp, heightDp)
                                .padding(2.dp)
                                .zIndex(1f + ev.columnIndex * 0.01f)
                                .border(1.dp, Color.Black)
                                .background(Color(ev.color))
                                .clickable { onEventClick(instance) }
                        ) {
                            Text(
                                ev.title.ifEmpty { stringResource(R.string.no_title) },
                                Modifier.padding(6.dp),
                                Color.White,
                                maxLines = 2,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
