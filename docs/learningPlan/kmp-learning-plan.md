# 20-Minute Daily Kotlin Multiplatform Learning Plan

A comprehensive, hands-on approach to learning Kotlin Multiplatform (KMP) with just 20 minutes of focused learning per day. This plan includes specific code snippets, detailed instructions, and resources to guide you through building a complete cross-platform notes application.

## Project: "KMP Notes" - A Cross-Platform Note-Taking Application

## Week 1: Project Setup & Foundation

### Day 1: Initial Project Setup
**Goal**: Create a new KMP project with Android support
**Actions**:
1. Install the latest IntelliJ IDEA or Android Studio
2. Go to File > New > Project > Kotlin Multiplatform Mobile
3. Name your project "KMPNotes"
4. Select Android and iOS targets
5. Configure Android settings (minimum API level 24)

**Resources**:
- [Official KMP Setup Guide](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)
- [Video walkthrough of KMP project creation](https://www.youtube.com/watch?v=GN0RGbNGNbw)

### Day 2: Project Structure Understanding
**Goal**: Explore and understand the project structure
**Actions**:
1. Navigate through the generated directories:
   - `shared/src/commonMain` - Shared Kotlin code
   - `shared/src/androidMain` - Android-specific code
   - `shared/src/iosMain` - iOS-specific code
   - `androidApp` - Android application module
2. Examine the build.gradle files
3. Look at the sample code to understand the expect/actual pattern

**Resources**:
- [Understanding KMP Project Structure](https://kotlinlang.org/docs/multiplatform-discover-project.html)

### Day 3: Create Data Model
**Goal**: Define the Note data class
**Actions**:
1. In `shared/src/commonMain/kotlin/com/yourusername/kmpnotes/model`, create a new file called `Note.kt`:

```kotlin
package com.yourusername.kmpnotes.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
```

2. Add kotlinx-datetime dependency in `shared/build.gradle.kts`:

```kotlin
commonMain {
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    }
}
```

### Day 4: Create Repository Interface
**Goal**: Define the note repository interface
**Actions**:
1. Create `shared/src/commonMain/kotlin/com/yourusername/kmpnotes/repository/NoteRepository.kt`:

```kotlin
package com.yourusername.kmpnotes.repository

import com.yourusername.kmpnotes.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(id: Long)
}
```

2. Add coroutines dependency in `shared/build.gradle.kts`:

```kotlin
commonMain {
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    }
}
```

### Day 5: Create In-Memory Repository
**Goal**: Implement a simple in-memory repository
**Actions**:
1. Create `shared/src/commonMain/kotlin/com/yourusername/kmpnotes/repository/InMemoryNoteRepository.kt`:

```kotlin
package com.yourusername.kmpnotes.repository

import com.yourusername.kmpnotes.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

class InMemoryNoteRepository : NoteRepository {
    private var lastId = 0L
    private val notes = mutableListOf<Note>()
    private val _notesFlow = MutableStateFlow<List<Note>>(emptyList())
    
    override fun getAllNotes(): Flow<List<Note>> = _notesFlow.asStateFlow()
    
    override suspend fun getNoteById(id: Long): Note? {
        return notes.find { it.id == id }
    }
    
    override suspend fun insertNote(note: Note): Long {
        val id = ++lastId
        val newNote = note.copy(id = id)
        notes.add(newNote)
        _notesFlow.update { notes.toList() }
        return id
    }
    
    override suspend fun updateNote(note: Note) {
        val index = notes.indexOfFirst { it.id == note.id }
        if (index != -1) {
            val updatedNote = note.copy(updatedAt = Clock.System.now())
            notes[index] = updatedNote
            _notesFlow.update { notes.toList() }
        }
    }
    
    override suspend fun deleteNote(id: Long) {
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes.removeAt(index)
            _notesFlow.update { notes.toList() }
        }
    }
}
```

### Day 6: Set Up Android UI - Basic Structure
**Goal**: Create a simple Android UI with Jetpack Compose
**Actions**:
1. Update `androidApp/build.gradle.kts` to include Compose:

```kotlin
dependencies {
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.ui:ui-tooling:1.6.0")
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.compose.material:material:1.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
```

2. Create `androidApp/src/main/java/com/yourusername/kmpnotes/android/MainActivity.kt`:

```kotlin
package com.yourusername.kmpnotes.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    Greeting("KMP Notes")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview
@Composable
fun DefaultPreview() {
    MaterialTheme {
        Greeting("KMP Notes")
    }
}
```

### Day 7: Create Android ViewModel
**Goal**: Create a ViewModel for the Notes screen
**Actions**:
1. Create `androidApp/src/main/java/com/yourusername/kmpnotes/android/NotesViewModel.kt`:

```kotlin
package com.yourusername.kmpnotes.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourusername.kmpnotes.model.Note
import com.yourusername.kmpnotes.repository.InMemoryNoteRepository
import com.yourusername.kmpnotes.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel : ViewModel() {
    private val repository: NoteRepository = InMemoryNoteRepository()
    
    val notes: StateFlow<List<Note>> = repository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun addNote(title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        
        viewModelScope.launch {
            repository.insertNote(Note(title = title, content = content))
        }
    }
    
    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }
}
```

## Week 2: Building Android UI and Note Features

### Day 8: Create Notes List Screen
**Goal**: Build a list view for notes
**Actions**:
1. Create `androidApp/src/main/java/com/yourusername/kmpnotes/android/NotesScreen.kt`:

```kotlin
package com.yourusername.kmpnotes.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourusername.kmpnotes.model.Note

@Composable
fun NotesScreen(viewModel: NotesViewModel) {
    val notes by viewModel.notes.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("KMP Notes") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(notes) { note ->
                NoteItem(note = note, onClick = { /* TODO */ })
            }
            
            if (notes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notes yet. Tap + to add one.")
                    }
                }
            }
        }
    }
}

@Composable
fun NoteItem(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.h6,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.body2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

2. Update `MainActivity.kt` to use the NotesScreen:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        MaterialTheme {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<NotesViewModel>()
            NotesScreen(viewModel)
        }
    }
}
```

### Day 9: Add Note Creation UI
**Goal**: Create a screen to add new notes
**Actions**:
1. Create `androidApp/src/main/java/com/yourusername/kmpnotes/android/AddNoteScreen.kt`:

```kotlin
package com.yourusername.kmpnotes.android

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddNoteScreen(
    onNavigateBack: () -> Unit,
    onSaveNote: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSaveNote(title, content)
                            onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                maxLines = 20
            )
        }
    }
}
```

### Day 10: Navigation Setup
**Goal**: Add navigation between screens
**Actions**:
1. Add Navigation Compose dependency to `androidApp/build.gradle.kts`:

```kotlin
implementation("androidx.navigation:navigation-compose:2.7.5")
```

2. Create `androidApp/src/main/java/com/yourusername/kmpnotes/android/Navigation.kt`:

```kotlin
package com.yourusername.kmpnotes.android

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NotesNavigation() {
    val navController = rememberNavController()
    val viewModel: NotesViewModel = viewModel()
    
    NavHost(navController = navController, startDestination = "notes") {
        composable("notes") {
            NotesScreen(
                viewModel = viewModel,
                onAddNote = { navController.navigate("addNote") }
            )
        }
        composable("addNote") {
            AddNoteScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveNote = { title, content ->
                    viewModel.addNote(title, content)
                }
            )
        }
    }
}
```

3. Update `NotesScreen.kt` to add navigation parameter:

```kotlin
@Composable
fun NotesScreen(viewModel: NotesViewModel, onAddNote: () -> Unit) {
    // ... existing code
    
    Scaffold(
        // ... existing code
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNote) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) {
        // ... existing code
    }
}
```

4. Update `MainActivity.kt`:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        MaterialTheme {
            NotesNavigation()
        }
    }
}
```

### Day 11: Add Test Data & Run App
**Goal**: Add sample notes and test the app
**Actions**:
1. Update `NotesViewModel.kt` to include sample data:

```kotlin
init {
    // Add some sample notes
    viewModelScope.launch {
        repository.insertNote(Note(
            title = "Welcome to KMP Notes",
            content = "This is your first note in this cross-platform app!"
        ))
        repository.insertNote(Note(
            title = "Shopping List",
            content = "- Milk\n- Eggs\n- Bread\n- Cheese"
        ))
        repository.insertNote(Note(
            title = "Learning KMP",
            content = "Kotlin Multiplatform is an exciting technology that lets you share code between platforms."
        ))
    }
}
```

2. Build and run the app on your Android device or emulator

### Day 12: iOS Setup - Part 1
**Goal**: Set up the iOS part of the project
**Actions**:
1. If using Mac, open Xcode and build the iOS app
2. If not using Mac, set up a CI service or Mac virtual machine to build iOS
3. Explore the generated iOS code structure:
   - `iosApp/iosApp/ContentView.swift`
   - `iosApp/iosApp/iOSApp.swift`

**Resources**:
- [KMP iOS Setup Guide](https://kotlinlang.org/docs/multiplatform-mobile-ios-dependencies.html)

### Day 13: iOS Setup - Part 2
**Goal**: Create a basic SwiftUI interface for iOS
**Actions**:
1. Update `iosApp/iosApp/ContentView.swift`:

```swift
import SwiftUI
import shared

struct ContentView: View {
    @ObservedObject private(set) var viewModel = NotesViewModel()
    
    var body: some View {
        NavigationView {
            List {
                ForEach(viewModel.notes, id: \.id) { note in
                    VStack(alignment: .leading) {
                        Text(note.title)
                            .font(.headline)
                        Text(note.content)
                            .font(.subheadline)
                            .lineLimit(2)
                    }
                }
            }
            .navigationTitle("KMP Notes")
            .toolbar {
                Button(action: {
                    // TODO: Add note
                }) {
                    Image(systemName: "plus")
                }
            }
        }
    }
}

class NotesViewModel: ObservableObject {
    @Published var notes = [Note]()
    
    init() {
        // TODO: Connect to repository
        // Add sample notes for now
        notes = [
            Note(id: 1, title: "Sample Note", content: "This is a sample note", createdAt: Date(), updatedAt: Date())
        ]
    }
}
```

## Week 3: Shared Database with SQLDelight

### Day 14: SQLDelight Setup
**Goal**: Add SQLDelight for database persistence
**Actions**:
1. Update `shared/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins
    id("app.cash.sqldelight") version "2.0.0"
}

// ... existing content

sqldelight {
    databases {
        create("NotesDatabase") {
            packageName.set("com.yourusername.kmpnotes.db")
        }
    }
}

// ... platform specific dependencies
androidMain {
    dependencies {
        implementation("app.cash.sqldelight:android-driver:2.0.0")
    }
}
    
iosMain {
    dependencies {
        implementation("app.cash.sqldelight:native-driver:2.0.0")
    }
}
```

### Day 15: Define SQLDelight Schema
**Goal**: Create database schema for notes
**Actions**:
1. Create `shared/src/commonMain/sqldelight/com/yourusername/kmpnotes/db/Notes.sq`:

```sql
CREATE TABLE Note (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

selectAll:
SELECT * FROM Note ORDER BY updated_at DESC;

selectById:
SELECT * FROM Note WHERE id = ?;

insert:
INSERT INTO Note(title, content, created_at, updated_at)
VALUES (?, ?, ?, ?);

update:
UPDATE Note
SET title = ?, content = ?, updated_at = ?
WHERE id = ?;

delete:
DELETE FROM Note WHERE id = ?;
```

### Day 16: Database Driver Setup
**Goal**: Create platform-specific database drivers
**Actions**:
1. Create `shared/src/commonMain/kotlin/com/yourusername/kmpnotes/db/DatabaseDriverFactory.kt`:

```kotlin
package com.yourusername.kmpnotes.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}
```

2. Create `shared/src/androidMain/kotlin/com/yourusername/kmpnotes/db/DatabaseDriverFactory.kt`:

```kotlin
package com.yourusername.kmpnotes.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(NotesDatabase.Schema, context, "notes.db")
    }
}
```

3. Create `shared/src/iosMain/kotlin/com/yourusername/kmpnotes/db/DatabaseDriverFactory.kt`:

```kotlin
package com.yourusername.kmpnotes.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory() {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(NotesDatabase.Schema, "notes.db")
    }
}
```

### Day 17: Create SQLDelight Repository
**Goal**: Implement the repository with SQLDelight
**Actions**:
1. Create `shared/src/commonMain/kotlin/com/yourusername/kmpnotes/repository/SqlDelightNoteRepository.kt`:

```kotlin
package com.yourusername.kmpnotes.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yourusername.kmpnotes.db.DatabaseDriverFactory
import com.yourusername.kmpnotes.db.NotesDatabase
import com.yourusername.kmpnotes.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightNoteRepository(databaseDriverFactory: DatabaseDriverFactory) : NoteRepository {
    private val database = NotesDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.notesQueries
    
    override fun getAllNotes(): Flow<List<Note>> {
        return dbQuery.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { notes ->
                notes.map { it.toNote() }
            }
    }
    
    override suspend fun getNoteById(id: Long): Note? {
        return dbQuery.selectById(id).executeAsOneOrNull()?.toNote()
    }
    
    override suspend fun insertNote(note: Note): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        
        dbQuery.insert(
            title = note.title,
            content = note.content,
            created_at = now,
            updated_at = now
        )
        
        return dbQuery.selectAll().executeAsList().first { 
            it.title == note.title && it.content == note.content 
        }.id
    }
    
    override suspend fun updateNote(note: Note) {
        val now = Clock.System.now().toEpochMilliseconds()
        
        dbQuery.update(
            title = note.title,
            content = note.content,
            updated_at = now,
            id = note.id
        )
    }
    
    override suspend fun deleteNote(id: Long) {
        dbQuery.delete(id)
    }
    
    private fun com.yourusername.kmpnotes.db.Note.toNote(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            createdAt = Instant.fromEpochMilliseconds(created_at),
            updatedAt = Instant.fromEpochMilliseconds(updated_at)
        )
    }
}
```

### Day 18: Connect Repository to Android
**Goal**: Use the SQLDelight repository in Android
**Actions**:
1. Update `androidApp/src/main/java/com/yourusername/kmpnotes/android/NotesViewModel.kt`:

```kotlin
class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository = SqlDelightNoteRepository(
        DatabaseDriverFactory(application)
    )
    
    // Rest of the code stays the same
}
```

2. Make sure to update the imports

### Day 19: Connect Repository to iOS
**Goal**: Use the SQLDelight repository in iOS
**Actions**:
1. Update iOS models and viewmodels to use the repository

### Day 20: Database Testing
**Goal**: Test the database implementation
**Actions**:
1. Run the app and verify that notes are saved correctly
2. Test adding, editing, and deleting notes

## Week 4: Desktop Support

### Day 21: Desktop Module Setup
**Goal**: Add desktop support with Compose Multiplatform
**Actions**:
1. Create a new directory `desktopApp` in project root
2. Create `desktopApp/build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.5.10"
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.runtime)
}

compose.desktop {
    application {
        mainClass = "com.yourusername.kmpnotes.desktop.MainKt"
    }
}
```

### Day 22: Desktop Main Class
**Goal**: Create desktop entry point
**Actions**:
1. Create `desktopApp/src/main/kotlin/com/yourusername/kmpnotes/desktop/Main.kt`:

```kotlin
package com.yourusername.kmpnotes.desktop

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yourusername.kmpnotes.db.DatabaseDriverFactory

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "KMP Notes") {
        MaterialTheme {
            NotesApp(DatabaseDriverFactory())
        }
    }
}
```

### Day 23: Desktop Database Driver
**Goal**: Create database driver for desktop
**Actions**:
1. Create `shared/src/jvmMain/kotlin/com/yourusername/kmpnotes/db/DatabaseDriverFactory.kt`:

```kotlin
package com.yourusername.kmpnotes.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), "kmpnotes.db")
        val driver = JdbcSqliteDriver(url = "jdbc:sqlite:${databasePath.absolutePath}")
        
        if (!databasePath.exists()) {
            NotesDatabase.Schema.create(driver)
        }
        
        return driver
    }
}
```

### Day 24: Desktop UI Implementation
**Goal**: Create UI for desktop app
**Actions**:
1. Create `desktopApp/src/main/kotlin/com/yourusername/kmpnotes/desktop/NotesApp.kt`:

```kotlin
package com.yourusername.kmpnotes.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourusername.kmpnotes.db.DatabaseDriverFactory
import com.yourusername.kmpnotes.repository.NoteRepository
import com.yourusername.kmpnotes.repository.SqlDelightNoteRepository

@Composable
fun NotesApp(databaseDriverFactory: DatabaseDriverFactory) {
    val repository: NoteRepository = remember { 
        SqlDelightNoteRepository(databaseDriverFactory) 
    }
    val viewModel = remember { NotesViewModel(repository) }
    
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.NotesList) }
    
    MaterialTheme {
        when (val screen = selectedScreen) {
            is Screen.NotesList -> {
                NotesListScreen(
                    viewModel = viewModel,
                    onAddNote = { selectedScreen = Screen.AddNote },
                    onNoteClick = { selectedScreen = Screen.EditNote(it) }
                )
            }
            is Screen.AddNote -> {
                AddNoteScreen(
                    onNavigateBack = { selectedScreen = Screen.NotesList },
                    onSaveNote = { title, content ->
                        viewModel.addNote(title, content)
                        selectedScreen = Screen.NotesList
                    }
                )
            }
            is Screen.EditNote -> {
                // TODO: Implement edit screen
                selectedScreen = Screen.NotesList
            }
        }
    }
}

sealed class Screen {
    object NotesList : Screen()
    object AddNote : Screen()
    data class EditNote(val noteId: Long) : Screen()
}
```

### Day 25: Desktop ViewModel
**Goal**: Create ViewModel for desktop
**Actions**:
1. Create `desktopApp/src/main/kotlin/com/yourusername/kmpnotes/desktop/NotesViewModel.kt`:

```kotlin
package com.yourusername.kmpnotes.desktop

import com.yourusername.kmpnotes.model.Note
import com.yourusername.kmpnotes.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NotesViewModel(private val repository: NoteRepository) {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    
    init {
        repository.getAllNotes()
            .onEach { notes ->
                _notes.value = notes
            }
            .launchIn(scope)
    }
    
    fun addNote(title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        
        scope.launch {
            repository.insertNote(Note(title = title, content = content))
        }
    }
    
    fun updateNote(note: Note) {
        scope.launch {
            repository.updateNote(note)
        }
    }
    
    fun deleteNote(id: Long) {
        scope.launch {
            repository.deleteNote(id)
        }
    }
    
    suspend fun getNoteById(id: Long): Note? {
        return repository.getNoteById(id)
    }
}
```

### Day 26: Desktop Notes List Screen
**Goal**: Create the Notes List screen for desktop
**Actions**:
1. Create `desktopApp/src/main/kotlin/com/yourusername/kmpnotes/desktop/NotesListScreen.kt`:

```kotlin
package com.yourusername.kmpnotes.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourusername.kmpnotes.model.Note

@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onAddNote: () -> Unit,
    onNoteClick: (Long) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KMP Notes") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNote) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(notes) { note ->
                NoteItem(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onDelete = { viewModel.deleteNote(note.id) }
                )
            }
            
            if (notes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notes yet. Click + to add one.")
                    }
                }
            }
        }
    }
}

@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
```

### Day 27: Desktop Add Note Screen
**Goal**: Create the Add Note screen for desktop
**Actions**:
1. Create `desktopApp/src/main/kotlin/com/yourusername/kmpnotes/desktop/AddNoteScreen.kt`:

```kotlin
package com.yourusername.kmpnotes.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddNoteScreen(
    onNavigateBack: () -> Unit,
    onSaveNote: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (title.isNotBlank() || content.isNotBlank()) {
                                onSaveNote(title, content)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = 20
            )
        }
    }
}
```

### Day 28: Run Desktop App
**Goal**: Complete the desktop app and test it
**Actions**:
1. Update `settings.gradle.kts` to include the desktop module:

```kotlin
include(":androidApp", ":shared", ":desktopApp")
```

2. Build and run the desktop app:
```
./gradlew :desktopApp:run
```

## Week 5: Adding Web Support

### Day 29: Web Module Setup
**Goal**: Add web support with Kotlin/JS
**Actions**:
1. Create a new directory `webApp` in project root
2. Create `webApp/build.gradle.kts`:

```kotlin
plugins {
    kotlin("js")
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react:18.2.0-pre.346")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:18.2.0-pre.346")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion:11.10.4-pre.346")
    implementation("io.ktor:ktor-client-js:2.3.2")
}
```

### Day 30: Add Web Source Setup
**Goal**: Set up web module source directories
**Actions**:
1. Create `webApp/src/main/kotlin/com/yourusername/kmpnotes/web/Main.kt`:

```kotlin
package com.yourusername.kmpnotes.web

import kotlinx.browser.document
import react.create
import react.dom.client.createRoot

fun main() {
    val container = document.getElementById("root") ?: error("Root container not found")
    createRoot(container).render(App.create())
}
```

2. Create `webApp/src/main/resources/index.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KMP Notes</title>
</head>
<body>
    <div id="root"></div>
    <script src="webApp.js"></script>
</body>
</html>
```

### Day 31: Web Database Driver
**Goal**: Create a database driver for web
**Actions**:
1. Create `shared/src/jsMain/kotlin/com/yourusername/kmpnotes/db/DatabaseDriverFactory.kt`:

```kotlin
package com.yourusername.kmpnotes.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import kotlinx.browser.window

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            NotesDatabase.Schema,
            "notes.db"
        )
    }
}
```

### Day 32: Create Web App Component
**Goal**: Create the main App component
**Actions**:
1. Create `webApp/src/main/kotlin/com/yourusername/kmpnotes/web/App.kt`:

```kotlin
package com.yourusername.kmpnotes.web

import com.yourusername.kmpnotes.db.DatabaseDriverFactory
import com.yourusername.kmpnotes.repository.SqlDelightNoteRepository
import kotlinx.coroutines.MainScope
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1

private val scope = MainScope()

val App = FC<Props> {
    val repository = SqlDelightNoteRepository(DatabaseDriverFactory())
    val (notes, setNotes) = useState(emptyList<com.yourusername.kmpnotes.model.Note>())
    val (currentScreen, setCurrentScreen) = useState<Screen>(Screen.NotesList)
    
    useEffectOnce {
        scope.collectNotes(repository) { setNotes(it) }
    }
    
    div {
        className = "app-container"
        
        h1 {
            +"KMP Notes"
        }
        
        when (val screen = currentScreen) {
            is Screen.NotesList -> {
                NotesList {
                    this.notes = notes
                    this.onAddNote = { setCurrentScreen(Screen.AddNote) }
                    this.onNoteClick = { setCurrentScreen(Screen.EditNote(it)) }
                    this.onDeleteNote = { noteId ->
                        scope.deleteNote(repository, noteId)
                    }
                }
            }
            is Screen.AddNote -> {
                AddNoteForm {
                    this.onCancel = { setCurrentScreen(Screen.NotesList) }
                    this.onSave = { title, content ->
                        scope.addNote(repository, title, content)
                        setCurrentScreen(Screen.NotesList)
                    }
                }
            }
            is Screen.EditNote -> {
                // TODO: Implement edit screen
                setCurrentScreen(Screen.NotesList)
            }
        }
    }
}

sealed class Screen {
    object NotesList : Screen()
    object AddNote : Screen()
    data class EditNote(val noteId: Long) : Screen()
}
```

### Day 33: Create Web Utilities
**Goal**: Create utilities for the web app
**Actions**:
1. Create `webApp/src/main/kotlin/com/yourusername/kmpnotes/web/Utils.kt`:

```kotlin
package com.yourusername.kmpnotes.web

import com.yourusername.kmpnotes.model.Note
import com.yourusername.kmpnotes.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

fun CoroutineScope.collectNotes(
    repository: NoteRepository,
    onCollect: (List<Note>) -> Unit
) {
    repository.getAllNotes()
        .onEach { notes ->
            onCollect(notes)
        }
        .launchIn(this)
}

fun CoroutineScope.addNote(
    repository: NoteRepository,
    title: String,
    content: String
) {
    if (title.isBlank() && content.isBlank()) return
    
    launch {
        repository.insertNote(Note(title = title, content = content))
    }
}

fun CoroutineScope.deleteNote(
    repository: NoteRepository,
    id: Long
) {
    launch {
        repository.deleteNote(id)
    }
}
```

### Day 34: Create Notes List Component
**Goal**: Create the Notes List component for web
**Actions**:
1. Create `webApp/src/main/kotlin/com/yourusername/kmpnotes/web/NotesList.kt`:

```kotlin
package com.yourusername.kmpnotes.web

import com.yourusername.kmpnotes.model.Note
import emotion.react.css
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.p

external interface NotesListProps : Props {
    var notes: List<Note>
    var onAddNote: () -> Unit
    var onNoteClick: (Long) -> Unit
    var onDeleteNote: (Long) -> Unit
}

val NotesList = FC<NotesListProps> { props ->
    div {
        css {
            padding = 16.px
        }
        
        button {
            onClick = { props.onAddNote() }
            +"Add Note"
        }
        
        div {
            css {
                marginTop = 16.px
                display = "flex"
                flexDirection = "column"
                gap = 12.px
            }
            
            if (props.notes.isEmpty()) {
                div {
                    +"No notes yet. Click 'Add Note' to create one."
                }
            } else {
                props.notes.forEach { note ->
                    NoteItem {
                        this.note = note
                        this.onClick = { props.onNoteClick(note.id) }
                        this.onDelete = { props.onDeleteNote(note.id) }
                    }
                }
            }
        }
    }
}

external interface NoteItemProps : Props {
    var note: Note
    var onClick: () -> Unit
    var onDelete: () -> Unit
}

val NoteItem = FC<NoteItemProps> { props ->
    div {
        css {
            border = "1px solid #ddd"
            borderRadius = 4.px
            padding = 16.px
            cursor = "pointer"
            position = "relative"
        }
        onClick = { props.onClick() }
        
        h3 {
            +props.note.title
        }
        
        p {
            +props.note.content
        }
        
        button {
            css {
                position = "absolute"
                top = 8.px
                right = 8.px
            }
            onClick = { 
                it.stopPropagation()
                props.onDelete()
            }
            +"Delete"
        }
    }
}
```

### Day 35: Create Add Note Form
**Goal**: Create the Add Note form for web
**Actions**:
1. Create `webApp/src/main/kotlin/com/yourusername/kmpnotes/web/AddNoteForm.kt`:

```kotlin
package com.yourusername.kmpnotes.web

import emotion.react.css
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.textarea

external interface AddNoteFormProps : Props {
    var onCancel: () -> Unit
    var onSave: (title: String, content: String) -> Unit
}

val AddNoteForm = FC<AddNoteFormProps> { props ->
    var title by useState("")
    var content by useState("")
    
    div {
        css {
            padding = 16.px
        }
        
        h2 {
            +"Add New Note"
        }
        
        div {
            css {
                marginBottom = 16.px
            }
            
            label {
                css {
                    display = "block"
                    marginBottom = 4.px
                }
                htmlFor = "title"
                +"Title"
            }
            
            input {
                css {
                    width = 100.pct
                    padding = 8.px
                    fontSize = 16.px
                }
                id = "title"
                value = title
                onChange = { event ->
                    title = event.target.value
                }
            }
        }
        
        div {
            css {
                marginBottom = 16.px
            }
            
            label {
                css {
                    display = "block"
                    marginBottom = 4.px
                }
                htmlFor = "content"
                +"Content"
            }
            
            textarea {
                css {
                    width = 100.pct
                    height = 200.px
                    padding = 8.px
                    fontSize = 16.px
                }
                id = "content"
                value = content
                onChange = { event ->
                    content = event.target.value
                }
            }
        }
        
        div {
            css {
                display = "flex"
                gap = 8.px
            }
            
            button {
                onClick = { props.onCancel() }
                +"Cancel"
            }
            
            button {
                onClick = { props.onSave(title, content) }
                +"Save"
            }
        }
    }
}
```

### Day 36: Update Settings and Run Web App
**Goal**: Update settings and run the web app
**Actions**:
1. Update `settings.gradle.kts` to include the web module:

```kotlin
include(":androidApp", ":shared", ":desktopApp", ":webApp")
```

2. Build and run the web app:
```
./gradlew :webApp:browserDevelopmentRun
```

## Week 6: Refining and Enhancing

### Day 37: Add Note Categories
**Goal**: Add categories to notes
**Actions**:
1. Update Note model in `shared/src/commonMain/kotlin/com/yourusername/kmpnotes/model/Note.kt`:

```kotlin
data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
```

2. Update SQLDelight schema in `shared/src/commonMain/sqldelight/com/yourusername/kmpnotes/db/Notes.sq`:

```sql
-- Update the table with the category column
ALTER TABLE Note ADD COLUMN category TEXT NOT NULL DEFAULT 'General';

-- Update existing queries and add category-specific ones
selectByCategory:
SELECT * FROM Note WHERE category = ? ORDER BY updated_at DESC;
```

### Day 38: Implement Category Filtering
**Goal**: Add UI for filtering notes by category
**Actions**:
1. Update repository to include category filtering
2. Update UI in each platform to show and filter by categories

### Day 39: Add Note Search
**Goal**: Implement note search functionality
**Actions**:
1. Add search query to repository
2. Implement search UI in each platform

### Day 40: Final Polishing
**Goal**: Polish the application across all platforms
**Actions**:
1. Add app icons for each platform
2. Improve UI theming and consistency
3. Add final touches and test on all platforms

## Supplementary Resources

### Sample Project Repositories
- [KaMP Kit](https://github.com/touchlab/KaMPKit) - A complete KMP starter kit
- [KMM-ViewModel](https://github.com/rickclephas/KMM-ViewModel) - For state management patterns
- [PeopleInSpace](https://github.com/joreilly/PeopleInSpace) - A more complex KMP example

### Learning Resources
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [Compose Multiplatform Documentation](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin/JS Documentation](https://kotlinlang.org/docs/js-overview.html)

### Tips for 20-Minute Sessions
1. **Prepare your environment**: Have IDE ready to go before your 20-minute session starts
2. **Set clear breakpoints**: Know exactly where to stop and where to pick up next time
3. **Use TODO comments**: Mark your current progress and what needs to be done next
4. **Keep a learning journal**: Document challenges and solutions for quick reference
5. **Test incrementally**: Run and test after each logical chunk of work
6. **Focus on one platform at a time**: Complete functionality on one platform before moving to the next