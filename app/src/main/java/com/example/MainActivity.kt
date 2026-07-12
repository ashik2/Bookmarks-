package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.data.Bookmark
import com.example.data.BookmarkDatabase
import com.example.ui.BookmarkUtils
import com.example.ui.BookmarkViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

class MainActivity : ComponentActivity() {

    private val viewModel: BookmarkViewModel by viewModels {
        BookmarkViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming share intents
        if (handleShareIntent(intent)) {
            return
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE) }
            val systemInDark = isSystemInDarkTheme()
            var isDarkTheme by remember {
                mutableStateOf(if (prefs.contains("is_dark_theme")) prefs.getBoolean("is_dark_theme", false) else systemInDark)
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                ) { innerPadding ->
                    BookmarkScreen(
                        viewModel = viewModel,
                        onToggleTheme = {
                            isDarkTheme = !isDarkTheme
                            prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?): Boolean {
        val action = intent?.action
        val type = intent?.type
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    val parsed = BookmarkUtils.extractUrlAndTitle(sharedText)
                    if (parsed != null) {
                        val (url, title) = parsed
                        val db = BookmarkDatabase.getDatabase(this)
                        lifecycleScope.launch {
                            val formattedUrl = BookmarkUtils.formatUrl(url)
                            val existing = db.bookmarkDao().getBookmarkByUrl(formattedUrl)
                            if (existing != null) {
                                Toast.makeText(this@MainActivity, "Already bookmarked: ${existing.title}", Toast.LENGTH_SHORT).show()
                            } else {
                                val cleanTitle = title.ifEmpty { BookmarkUtils.getCleanName(formattedUrl) }
                                db.bookmarkDao().insertBookmark(Bookmark(title = cleanTitle, url = formattedUrl))
                                Toast.makeText(this@MainActivity, "Saved to Bookmarks: $cleanTitle", Toast.LENGTH_SHORT).show()
                            }
                            finish()
                        }
                        return true
                    } else {
                        Toast.makeText(this, "No valid link found to save", Toast.LENGTH_SHORT).show()
                        finish()
                        return true
                    }
                }
            }
        }
        return false
    }
}

@Composable
fun BookmarkScreen(
    viewModel: BookmarkViewModel,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bookmarks by viewModel.allBookmarks.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBookmarkForOptions by remember { mutableStateOf<Bookmark?>(null) }
    var selectedBookmarkForEdit by remember { mutableStateOf<Bookmark?>(null) }
    var selectedBookmarkForDelete by remember { mutableStateOf<Bookmark?>(null) }
    var urlToOpenInPicker by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Grid layout strictly matching the minimalist pattern, adaptive columns
        Box(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 52.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(bookmarks) { _, bookmark ->
                    GridItem(
                        title = bookmark.title,
                        isAddButton = false,
                        onClick = {
                            val formatted = BookmarkUtils.formatUrl(bookmark.url)
                            if (BookmarkUtils.isGoogleMapsUrl(formatted) || BookmarkUtils.isWazeUrl(formatted)) {
                                val opened = BookmarkUtils.openSpecificMapApp(context, bookmark.url)
                                if (!opened) {
                                    urlToOpenInPicker = bookmark.url
                                }
                            } else {
                                urlToOpenInPicker = bookmark.url
                            }
                        },
                        onLongClick = {
                            selectedBookmarkForOptions = bookmark
                        },
                        url = bookmark.url
                    )
                }

                // Add button directly after the last bookmark inside the grid
                item {
                    GridItem(
                        title = "Add",
                        isAddButton = true,
                        onClick = {
                            showAddDialog = true
                        },
                        onLongClick = onToggleTheme
                    )
                }
            }
        }
    }

    // Add manual bookmark dialog
    if (showAddDialog) {
        BookmarkFormDialog(
            dialogTitle = "New Bookmark",
            existingUrls = bookmarks.map { it.url },
            onDismiss = { showAddDialog = false },
            onConfirm = { title, url ->
                viewModel.addBookmark(title, url)
                showAddDialog = false
            }
        )
    }

    // Options dialog showing Edit and Delete on long press
    selectedBookmarkForOptions?.let { bookmark ->
        BookmarkOptionsDialog(
            bookmark = bookmark,
            onDismiss = { selectedBookmarkForOptions = null },
            onEdit = {
                selectedBookmarkForEdit = bookmark
            },
            onDelete = {
                selectedBookmarkForDelete = bookmark
            }
        )
    }

    // Edit bookmark dialog
    selectedBookmarkForEdit?.let { bookmark ->
        BookmarkFormDialog(
            initialTitle = bookmark.title,
            initialUrl = bookmark.url,
            dialogTitle = "Edit Bookmark",
            existingUrls = bookmarks.map { it.url },
            onDismiss = { selectedBookmarkForEdit = null },
            onConfirm = { title, url ->
                viewModel.updateBookmark(bookmark.id, title, url)
                selectedBookmarkForEdit = null
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Custom Browser Picker Dialog
    urlToOpenInPicker?.let { url ->
        BrowserPickerDialog(
            url = url,
            onDismiss = { urlToOpenInPicker = null },
            context = context
        )
    }

    // Delete confirmation dialog
    selectedBookmarkForDelete?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { selectedBookmarkForDelete = null },
            title = {
                Text(
                    text = "Delete Bookmark?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${bookmark.title}\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBookmark(bookmark.id)
                        selectedBookmarkForDelete = null
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedBookmarkForDelete = null }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridItem(
    title: String,
    isAddButton: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    url: String? = null
) {
    val displayLetter = if (title.isNotEmpty()) title.first().uppercase() else "?"
    val circleColor = if (isAddButton) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        BookmarkUtils.getColorForTitle(title)
    }
    val contentColor = if (isAddButton) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color.White
    }

    var isImageLoaded by remember { mutableStateOf(false) }

    val finalBgColor = if (isAddButton) {
        MaterialTheme.colorScheme.surfaceVariant
    } else if (isImageLoaded) {
        Color.Transparent
    } else {
        circleColor
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isAddButton) "add_bookmark_grid_button" else "bookmark_item_${title}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic, responsive ripple container with combined clicks
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(finalBgColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isAddButton) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add manually",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                if (!url.isNullOrEmpty()) {
                    val faviconUrl = remember(url) { BookmarkUtils.getFaviconUrl(url) }
                    AsyncImage(
                        model = faviconUrl,
                        contentDescription = title,
                        onState = { state ->
                            isImageLoaded = state is coil.compose.AsyncImagePainter.State.Success
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                if (!isImageLoaded) {
                    Text(
                        text = displayLetter,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        ),
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle/Title label
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = 12.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun BookmarkOptionsDialog(
    bookmark: Bookmark,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Edit option
                TextButton(
                    onClick = {
                        onEdit()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("option_edit_${bookmark.id}"),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Bookmark",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Edit Bookmark",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Delete option
                TextButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("option_delete_${bookmark.id}"),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Bookmark",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Delete Bookmark",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", textAlign = TextAlign.Center)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkFormDialog(
    initialTitle: String = "",
    initialUrl: String = "",
    dialogTitle: String = "New Bookmark",
    existingUrls: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    var isUrlError by remember { mutableStateOf(false) }
    var urlErrorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (isUrlError && it.trim().isNotEmpty()) {
                            isUrlError = false
                        }
                    },
                    label = { Text("Link / URL") },
                    placeholder = { Text("example.com") },
                    singleLine = true,
                    isError = isUrlError,
                    supportingText = {
                        if (isUrlError) {
                            Text(urlErrorMessage, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Auto-detects title if left blank", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Optional)") },
                    placeholder = { Text("e.g. Google") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUrl = url.trim()
                    val formatted = BookmarkUtils.formatUrl(trimmedUrl)
                    if (trimmedUrl.isEmpty()) {
                        isUrlError = true
                        urlErrorMessage = "URL cannot be empty"
                    } else if (existingUrls.any {
                        val existingFormatted = BookmarkUtils.formatUrl(it)
                        existingFormatted.equals(formatted, ignoreCase = true) && 
                        !BookmarkUtils.formatUrl(initialUrl).equals(formatted, ignoreCase = true)
                    }) {
                        isUrlError = true
                        urlErrorMessage = "This URL is already bookmarked"
                    } else {
                        onConfirm(title, trimmedUrl)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun BrowserPickerDialog(
    url: String,
    onDismiss: () -> Unit,
    context: Context
) {
    val pm = context.packageManager
    val browsers = remember {
        val list = mutableListOf<android.content.pm.ResolveInfo>()
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PackageManager.MATCH_ALL
        } else {
            0
        }

        // Method 1: Query with generic https intent
        try {
            val httpsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            list.addAll(pm.queryIntentActivities(httpsIntent, flags))
        } catch (e: Exception) {
            // silent fallback
        }

        // Method 2: Query using Browser App selector
        try {
            val browserCategoryIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_BROWSER)
            }
            list.addAll(pm.queryIntentActivities(browserCategoryIntent, flags))
        } catch (e: Exception) {
            // silent fallback
        }

        list.distinctBy { it.activityInfo.packageName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Open with",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (browsers.isEmpty()) {
                    Text(
                        text = "No browsers detected. Attempting to open with system chooser...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(browsers) { info ->
                            val appLabel = remember { info.loadLabel(pm).toString() }
                            val appIcon = remember {
                                try {
                                    info.loadIcon(pm).toBitmap().asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val formattedUrl = BookmarkUtils.formatUrl(url)
                                        val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                                            setPackage(info.activityInfo.packageName)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            context.startActivity(launchIntent)
                                        } catch (e: Exception) {
                                            openUrlInSystemDefault(context, url)
                                        }
                                        onDismiss()
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (appIcon != null) {
                                        Image(
                                            bitmap = appIcon,
                                            contentDescription = appLabel,
                                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = appLabel,
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = appLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

private fun openUrlInSystemDefault(context: Context, url: String) {
    val formattedUrl = BookmarkUtils.formatUrl(url)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
    val chooser = Intent.createChooser(intent, "Open with")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open link", Toast.LENGTH_SHORT).show()
    }
}
