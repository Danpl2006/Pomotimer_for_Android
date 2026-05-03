package com.example.pomotimer

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    // Переопрделение метода род. класса на то что данные входящие могут быть пустыми
    override fun onCreate(savedInstanceState: Bundle?) {

        // Вызов род. метода для правильной инициализации Activity
        super.onCreate(savedInstanceState)

        // Метод, который устанавливает Compose-интерфейс для этой Activity
        setContent {
            // Установка цветовой темной темы
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                PomodoroApp()
            }
        }
    }
}

// Аннотация для компилятора
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroApp() {

    // Переменная хранит номер текущей вкладки
    var selectedTab by remember { mutableStateOf(0) }
    // Настройки здесь, чтобы делиться между экранами
    var workMinutes by remember { mutableStateOf(25) }
    var breakMinutes by remember { mutableStateOf(5) }
    var cyclesTarget by remember { mutableStateOf(4) }

    //  Готовая структура экрана
    Scaffold(
        // Нижняя панель
        bottomBar = {
            NavigationBar {
                // Кнопка таймера
                NavigationBarItem(
                    // Иконка пункта
                    icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    // Текст под иконкой
                    label = { Text("Таймер") },
                    // Подсвечивать ли пункт как активный
                    selected = selectedTab == 0,
                    // Что делать при нажатии
                    onClick = { selectedTab = 0 }
                )
                // Кнопка настроек
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                // Кнопка задач
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Задачи") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )

            }
        }
        // paddingValues - отступы, которые Scaffold передаёт, чтобы контент не залезал под bottomBar
    ) { paddingValues ->
        // Применение отступов к контейнеру
        Box(modifier = Modifier.padding(paddingValues)) {
            // Выбор экрана по номеру вкладки
            when (selectedTab) {
                0 -> TimerScreen(
                    workMinutes = workMinutes,
                    breakMinutes = breakMinutes,
                    cyclesTarget = cyclesTarget
                )
                1 -> SettingsScreen(
                    workMinutes = workMinutes,
                    breakMinutes = breakMinutes,
                    cyclesTarget = cyclesTarget,
                    onWorkMinutesChange = { workMinutes = it },
                    onBreakMinutesChange = { breakMinutes = it },
                    onCyclesTargetChange = { cyclesTarget = it } )
                2 -> TasksScreen()

            }
            }
        }
    }

@Composable
fun TimerScreen(
    workMinutes: Int,
    breakMinutes: Int,
    cyclesTarget: Int
) {
    val context = LocalContext.current

    // Состояние таймера
    var timeLeftSeconds by remember { mutableStateOf(workMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var isWorkPhase by remember { mutableStateOf(true) }  // true = работа, false = перерыв
    var cyclesCompleted by remember { mutableStateOf(0) }

    // Воспроизведение звука
    fun playSound() {
        try {
            val mediaPlayer = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { mp -> mp.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Логика таймера

    LaunchedEffect(isRunning, timeLeftSeconds) {
        if (isRunning && timeLeftSeconds > 0) {
            delay(1000)
            timeLeftSeconds--
        } else if (isRunning && timeLeftSeconds == 0) {
            playSound()  // звук при завершении фазы

            if (isWorkPhase) {
                // Работа закончилась -> идём на перерыв (цикл не увеличиваем)
                timeLeftSeconds = breakMinutes * 60
                isWorkPhase = false
                isRunning = true
            } else {
                // Перерыв закончился -> завершён полный цикл
                val newCycles = cyclesCompleted + 1

                if (newCycles >= cyclesTarget) {
                    // Все циклы выполнены -> сброс
                    timeLeftSeconds = workMinutes * 60
                    isWorkPhase = true
                    cyclesCompleted = 0
                    isRunning = false
                } else {
                    // Начинаем новый рабочий цикл
                    timeLeftSeconds = workMinutes * 60
                    isWorkPhase = true
                    cyclesCompleted = newCycles
                    isRunning = true
                }
            }
        }
    }

    // Сброс таймера при изменении настроек
    LaunchedEffect(workMinutes, breakMinutes, cyclesTarget) {
        if (!isRunning) {
            timeLeftSeconds = workMinutes * 60
            isWorkPhase = true
            cyclesCompleted = 0
        }
    }

    // Форматирование времени (секунды -> MM:SS)
    fun formatTime(seconds: Int): String {
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
        val secs = seconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, secs)
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Фаза работа/перерыв
        Text(
            text = if (isWorkPhase) "🍅 РАБОТА" else "☕ ПЕРЕРЫВ",
            fontSize = 20.sp,
            color = if (isWorkPhase) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Таймер (крупный текст)
        Text(
            text = formatTime(timeLeftSeconds),
            fontSize = 68.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопки управления
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRunning) "Пауза" else "Начать")
            }

            // Кнопка сброса (показывается только когда таймер не запущен)
            if (!isRunning) {
                OutlinedButton(
                    onClick = {
                        timeLeftSeconds = workMinutes * 60
                        isWorkPhase = true
                        cyclesCompleted = 0
                    }
                ) {
                    Text("Сброс")
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Циклы
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = "Циклов пройдено: $cyclesCompleted из $cyclesTarget",
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SettingsScreen(
    workMinutes: Int,
    breakMinutes: Int,
    cyclesTarget: Int,
    onWorkMinutesChange: (Int) -> Unit,
    onBreakMinutesChange: (Int) -> Unit,
    onCyclesTargetChange: (Int) -> Unit
) {
    var workTimeStr by remember { mutableStateOf(workMinutes.toString()) }
    var breakTimeStr by remember { mutableStateOf(breakMinutes.toString()) }
    var cyclesStr by remember { mutableStateOf(cyclesTarget.toString()) }

    // Синхронизация полей с внешними настройками
    LaunchedEffect(workMinutes) { workTimeStr = workMinutes.toString() }
    LaunchedEffect(breakMinutes) { breakTimeStr = breakMinutes.toString() }
    LaunchedEffect(cyclesTarget) { cyclesStr = cyclesTarget.toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Настройки таймера",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = workTimeStr,  // было workTime
            onValueChange = { workTimeStr = it },  // было workTime = it
            label = { Text("Время таймера (мин)") },
            placeholder = { Text("25") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = breakTimeStr,  // было breakTime
            onValueChange = { breakTimeStr = it },  // было breakTime = it
            label = { Text("Время перерыва (мин)") },
            placeholder = { Text("5") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cyclesStr,  // было cycles
            onValueChange = { cyclesStr = it },  // было cycles = it
            label = { Text("Количество циклов") },
            placeholder = { Text("4") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onWorkMinutesChange(workTimeStr.toIntOrNull() ?: 25)
                onBreakMinutesChange(breakTimeStr.toIntOrNull() ?: 5)
                onCyclesTargetChange(cyclesStr.toIntOrNull() ?: 4)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить настройки")
        }

        OutlinedButton(
            onClick = {
                workTimeStr = "25"
                breakTimeStr = "5"
                cyclesStr = "4"
                onWorkMinutesChange(25)
                onBreakMinutesChange(5)
                onCyclesTargetChange(4)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Стандартные")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("О программе:", fontWeight = FontWeight.Bold)
                Text("Приложение Pomodoro Timer реализует метод тайм-менеджмента, основанный на чередовании интервалов работы и отдыха.")
                Text("Выполнили: Распопов Д. Ануфриев А.", modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun TasksScreen() {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(loadTasks(context)) }
    var newTaskText by remember { mutableStateOf("") }

    LaunchedEffect(tasks) {
        saveTasks(context, tasks)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Мои задачи:",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Новая задача...") },
                singleLine = true
            )
            Button(
                onClick = {
                    if (newTaskText.isNotBlank()) {
                        tasks = tasks + Task(text = newTaskText, isDone = false)
                        newTaskText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                TaskItem(
                    task = task,
                    onToggleDone = {
                        tasks = tasks.map {
                            if (it.id == task.id) it.copy(isDone = !it.isDone)
                            else it
                        }
                    },
                    onDelete = {
                        tasks = tasks.filter { it.id != task.id }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { saveTasks(context, tasks) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Сохранить задачи")
            }
            OutlinedButton(
                onClick = { tasks = emptyList() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Удалить задачи")
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleDone() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.text,
                fontSize = 16.sp,
                style = if (task.isDone)
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                else
                    MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}


data class Task(
    val id: Int = (System.currentTimeMillis() % 100000).toInt(),
    val text: String,
    val isDone: Boolean = false
)


fun saveTasks(context: Context, tasks: List<Task>) {
    try {
        val file = File(context.filesDir, "tasks.txt")
        BufferedWriter(FileWriter(file)).use { writer ->
            tasks.forEach { task ->
                writer.write("${task.id}|${task.text}|${task.isDone}\n")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadTasks(context: Context): List<Task> {
    val tasks = mutableListOf<Task>()
    try {
        val file = File(context.filesDir, "tasks.txt")
        if (!file.exists()) return emptyList()

        BufferedReader(FileReader(file)).use { reader ->
            reader.forEachLine { line ->
                val parts = line.split("|")
                if (parts.size == 3) {
                    tasks.add(Task(
                        id = parts[0].toInt(),
                        text = parts[1],
                        isDone = parts[2].toBoolean()
                    ))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return tasks
}