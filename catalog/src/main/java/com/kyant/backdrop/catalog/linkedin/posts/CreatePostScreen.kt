package com.kyant.backdrop.catalog.linkedin.posts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.shapes.RoundedRectangle

// ==================== UI Styling Constants ====================
private val NeumorphicBackground = Color(0xFFecf0f3)
private val PostButtonGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4B70E2), Color(0xFF3a5bc7))
)

// ==================== Post Type Icons (using drawable resources) ====================
private data class PostTypeConfig(
    val type: PostType,
    val label: String,
    val iconRes: Int,
    val emoji: String // Fallback emoji
)

private val postTypeConfigs = listOf(
    PostTypeConfig(PostType.TEXT, "Text", R.drawable.ic_file_text, "📝"),
    PostTypeConfig(PostType.IMAGE, "Image", R.drawable.ic_image, "🖼️"),
    PostTypeConfig(PostType.VIDEO, "Video", R.drawable.ic_video, "🎥"),
    PostTypeConfig(PostType.LINK, "Link", R.drawable.ic_link, "🔗"),
    PostTypeConfig(PostType.POLL, "Poll", R.drawable.ic_poll, "📊"),
    PostTypeConfig(PostType.ARTICLE, "Article", R.drawable.ic_article, "✨"),
    PostTypeConfig(PostType.CELEBRATION, "Celebration", R.drawable.ic_celebration, "🏆")
)

// ==================== Color Presets ====================
private val colorPresets = listOf(
    "#ef4444" to "Red",
    "#f97316" to "Orange",
    "#eab308" to "Yellow",
    "#22c55e" to "Green",
    "#14b8a6" to "Teal",
    "#3b82f6" to "Blue",
    "#a855f7" to "Purple",
    "#ec4899" to "Pink"
)

// ==================== Poll Duration Options ====================
private val pollDurations = listOf(
    1 to "1 hour",
    6 to "6 hours",
    12 to "12 hours",
    24 to "1 day",
    72 to "3 days",
    168 to "1 week"
)

/**
 * Create Post Screen - Full-featured post creation with all 7 post types
 * Instagram/LinkedIn style with glass theme
 */
@Composable
fun CreatePostScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isCreating: Boolean,
    error: String?,
    userName: String,
    userAvatar: String?,
    mentionSearchResults: List<MentionUser>,
    isSearchingMentions: Boolean,
    onCreateTextPost: (content: String, visibility: String, mentions: List<String>) -> Unit,
    onCreateImagePost: (content: String?, visibility: String, images: List<Pair<ByteArray, String>>, mentions: List<String>) -> Unit,
    onCreateVideoPost: (content: String?, visibility: String, videoBytes: ByteArray, videoFilename: String, mentions: List<String>) -> Unit,
    onCreateLinkPost: (linkUrl: String, content: String?, visibility: String, mentions: List<String>) -> Unit,
    onCreatePollPost: (pollOptions: List<String>, pollDurationHours: Int, content: String?, visibility: String, showResultsBeforeVote: Boolean, mentions: List<String>) -> Unit,
    onCreateArticlePost: (articleTitle: String, content: String?, visibility: String, coverImage: Pair<ByteArray, String>?, articleTags: List<String>, mentions: List<String>) -> Unit,
    onCreateCelebrationPost: (celebrationType: String, content: String?, visibility: String, mentions: List<String>) -> Unit,
    onSearchMentions: (String) -> Unit,
    onClearMentionSearch: () -> Unit,
    onClearError: () -> Unit,
    onPostCreated: () -> Unit
) {
    val context = LocalContext.current
    
    // State
    var selectedPostType by remember { mutableStateOf(PostType.TEXT) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var showVisibilityDropdown by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    // Image state
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageBytes by remember { mutableStateOf<List<Pair<ByteArray, String>>>(emptyList()) }
    
    // Video state
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    
    // Link state
    var linkUrl by remember { mutableStateOf("") }
    
    // Poll state
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var pollDurationHours by remember { mutableIntStateOf(24) }
    var showResultsBeforeVote by remember { mutableStateOf(false) }
    
    // Article state
    var articleTitle by remember { mutableStateOf("") }
    var articleTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var articleTagInput by remember { mutableStateOf("") }
    var articleCoverUri by remember { mutableStateOf<Uri?>(null) }
    var articleCoverBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    
    // Celebration state
    var selectedCelebrationType by remember { mutableStateOf<CelebrationType?>(null) }
    
    // Mention state
    var selectedMentions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showMentionDropdown by remember { mutableStateOf(false) }
    var mentionStartIndex by remember { mutableIntStateOf(-1) }
    
    // File pickers
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newBytes = uris.mapNotNull { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val filename = uri.lastPathSegment ?: "image.jpg"
                        bytes to filename
                    }
                } catch (e: Exception) {
                    null
                }
            }
            imageUris = uris
            imageBytes = newBytes.take(10) // Max 10 images
        }
    }
    
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    val filename = it.lastPathSegment ?: "video.mp4"
                    videoUri = it
                    videoBytes = bytes to filename
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    val articleCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    val filename = it.lastPathSegment ?: "cover.jpg"
                    articleCoverUri = it
                    articleCoverBytes = bytes to filename
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Detect @mention in content
    LaunchedEffect(textFieldValue.text) {
        val text = textFieldValue.text
        val cursorPos = textFieldValue.selection.start
        
        // Find @ before cursor
        val lastAtIndex = text.lastIndexOf('@', cursorPos - 1)
        if (lastAtIndex >= 0) {
            val textAfterAt = text.substring(lastAtIndex + 1, cursorPos)
            if (textAfterAt.isNotEmpty() && !textAfterAt.contains(' ')) {
                mentionStartIndex = lastAtIndex
                showMentionDropdown = true
                onSearchMentions(textAfterAt)
            } else {
                showMentionDropdown = false
                onClearMentionSearch()
            }
        } else {
            showMentionDropdown = false
            onClearMentionSearch()
        }
    }
    
    // Handle create post
    val handleCreatePost: () -> Unit = {
        val content = textFieldValue.text
        when (selectedPostType) {
            PostType.TEXT -> {
                if (content.isNotBlank()) {
                    onCreateTextPost(content, visibility, selectedMentions)
                }
            }
            PostType.IMAGE -> {
                if (imageBytes.isNotEmpty()) {
                    onCreateImagePost(content.ifBlank { null }, visibility, imageBytes, selectedMentions)
                }
            }
            PostType.VIDEO -> {
                videoBytes?.let { (bytes, filename) ->
                    onCreateVideoPost(content.ifBlank { null }, visibility, bytes, filename, selectedMentions)
                }
            }
            PostType.LINK -> {
                if (linkUrl.isNotBlank()) {
                    onCreateLinkPost(linkUrl, content.ifBlank { null }, visibility, selectedMentions)
                }
            }
            PostType.POLL -> {
                val validOptions = pollOptions.filter { it.isNotBlank() }
                if (validOptions.size >= 2) {
                    onCreatePollPost(validOptions, pollDurationHours, content.ifBlank { null }, visibility, showResultsBeforeVote, selectedMentions)
                }
            }
            PostType.ARTICLE -> {
                if (articleTitle.isNotBlank()) {
                    onCreateArticlePost(articleTitle, content.ifBlank { null }, visibility, articleCoverBytes, articleTags, selectedMentions)
                }
            }
            PostType.CELEBRATION -> {
                selectedCelebrationType?.let { type ->
                    onCreateCelebrationPost(type.name, content.ifBlank { null }, visibility, selectedMentions)
                }
            }
            else -> {}
        }
    }
    
    // Validation
    val canPost = when (selectedPostType) {
        PostType.TEXT -> textFieldValue.text.isNotBlank()
        PostType.IMAGE -> imageBytes.isNotEmpty()
        PostType.VIDEO -> videoBytes != null
        PostType.LINK -> linkUrl.isNotBlank()
        PostType.POLL -> pollOptions.filter { it.isNotBlank() }.size >= 2
        PostType.ARTICLE -> articleTitle.isNotBlank()
        PostType.CELEBRATION -> selectedCelebrationType != null
        else -> false
    }
    
    // Clear form after successful post
    LaunchedEffect(isCreating) {
        if (!isCreating && error == null) {
            // Check if we just finished creating (would need proper state tracking)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 8.dp)
            .padding(top = 8.dp, bottom = 80.dp)
    ) {
        // Main card container - Full page
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(28f.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                        lens(16f.dp.toPx(), 32f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.12f)
                                )
                            )
                        )
                    }
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Post button
                CreatePostHeader(
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isCreating = isCreating,
                    canPost = canPost,
                    onPost = handleCreatePost
                )
                
                // Post type tabs
                PostTypeTabs(
                    selectedType = selectedPostType,
                    onTypeSelected = { selectedPostType = it },
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                
                // User info with visibility selector
                UserInfoRow(
                    userName = userName,
                    userAvatar = userAvatar,
                    visibility = visibility,
                    showVisibilityDropdown = showVisibilityDropdown,
                    onVisibilityDropdownToggle = { showVisibilityDropdown = it },
                    onVisibilityChange = { visibility = it },
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                
                // Rich text toolbar
                RichTextToolbar(
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = { textFieldValue = it },
                    showColorPicker = showColorPicker,
                    onColorPickerToggle = { showColorPicker = it },
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                
                // Content text area with mention dropdown
                ContentTextArea(
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = { textFieldValue = it },
                    placeholder = getPlaceholder(selectedPostType),
                    contentColor = contentColor,
                    mentionSearchResults = mentionSearchResults,
                    isSearchingMentions = isSearchingMentions,
                    showMentionDropdown = showMentionDropdown,
                    onMentionSelected = { user ->
                        // Replace @query with @username
                        val text = textFieldValue.text
                        val username = user.username ?: user.name ?: "user"
                        val beforeMention = text.substring(0, mentionStartIndex)
                        val afterCursor = text.substring(textFieldValue.selection.start)
                        val newText = "$beforeMention@$username $afterCursor"
                        textFieldValue = TextFieldValue(
                            text = newText,
                            selection = TextRange(mentionStartIndex + username.length + 2)
                        )
                        selectedMentions = selectedMentions + user.id
                        showMentionDropdown = false
                        onClearMentionSearch()
                    },
                    onDismissMentionDropdown = {
                        showMentionDropdown = false
                        onClearMentionSearch()
                    },
                    backdrop = backdrop
                )
                
                // Color picker
                AnimatedVisibility(visible = showColorPicker) {
                    ColorPickerRow(
                        onColorSelected = { hex ->
                            val selection = textFieldValue.selection
                            val text = textFieldValue.text
                            if (selection.length > 0) {
                                val selectedText = text.substring(selection.start, selection.end)
                                val newText = text.substring(0, selection.start) + 
                                    "[color:$hex]$selectedText[/color]" + 
                                    text.substring(selection.end)
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(selection.start + hex.length + 8 + selectedText.length)
                                )
                            } else {
                                val newText = text.substring(0, selection.start) + 
                                    "[color:$hex][/color]" + 
                                    text.substring(selection.start)
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(selection.start + hex.length + 8)
                                )
                            }
                            showColorPicker = false
                        },
                        contentColor = contentColor
                    )
                }
                
                // Type-specific content
                when (selectedPostType) {
                    PostType.IMAGE -> ImagePicker(
                        imageUris = imageUris,
                        onPickImages = { imagePicker.launch("image/*") },
                        onRemoveImage = { index ->
                            imageUris = imageUris.filterIndexed { i, _ -> i != index }
                            imageBytes = imageBytes.filterIndexed { i, _ -> i != index }
                        },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    PostType.VIDEO -> VideoPicker(
                        videoUri = videoUri,
                        onPickVideo = { videoPicker.launch("video/*") },
                        onRemoveVideo = {
                            videoUri = null
                            videoBytes = null
                        },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    PostType.LINK -> LinkInput(
                        linkUrl = linkUrl,
                        onLinkUrlChange = { linkUrl = it },
                        contentColor = contentColor
                    )
                    PostType.POLL -> PollEditor(
                        options = pollOptions,
                        onOptionsChange = { pollOptions = it },
                        durationHours = pollDurationHours,
                        onDurationChange = { pollDurationHours = it },
                        showResultsBeforeVote = showResultsBeforeVote,
                        onShowResultsChange = { showResultsBeforeVote = it },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    PostType.ARTICLE -> ArticleEditor(
                        title = articleTitle,
                        onTitleChange = { articleTitle = it },
                        coverUri = articleCoverUri,
                        onPickCover = { articleCoverPicker.launch("image/*") },
                        onRemoveCover = {
                            articleCoverUri = null
                            articleCoverBytes = null
                        },
                        tags = articleTags,
                        tagInput = articleTagInput,
                        onTagInputChange = { articleTagInput = it },
                        onAddTag = {
                            if (articleTagInput.isNotBlank() && articleTags.size < 5) {
                                articleTags = articleTags + articleTagInput.trim()
                                articleTagInput = ""
                            }
                        },
                        onRemoveTag = { tag -> articleTags = articleTags - tag },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    PostType.CELEBRATION -> CelebrationPicker(
                        selectedType = selectedCelebrationType,
                        onTypeSelected = { selectedCelebrationType = it },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    else -> {} // TEXT type has no additional UI
                }
                
                // Error display
                AnimatedVisibility(visible = error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE53935).copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = error ?: "",
                            style = TextStyle(Color(0xFFE53935), 14.sp),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onClearError() },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("×", style = TextStyle(Color(0xFFE53935), 18.sp))
                        }
                    }
                }
            }
        }
    }
}

// ==================== Subcomponents ====================

@Composable
private fun CreatePostHeader(
    contentColor: Color,
    accentColor: Color,
    isCreating: Boolean,
    canPost: Boolean,
    onPost: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = "Create Post",
            style = TextStyle(contentColor, 24.sp, FontWeight.Bold)
        )
        
        // Post button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (canPost && !isCreating) PostButtonGradient
                    else Brush.linearGradient(
                        colors = listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f))
                    )
                )
                .clickable(enabled = canPost && !isCreating, onClick = onPost)
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    text = "Post",
                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun PostTypeTabs(
    selectedType: PostType,
    onTypeSelected: (PostType) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        postTypeConfigs.forEach { config ->
            val isSelected = config.type == selectedType
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.2f)
                        else contentColor.copy(alpha = 0.05f)
                    )
                    .clickable { onTypeSelected(config.type) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use icon drawable
                    androidx.compose.foundation.Image(
                        painter = painterResource(config.iconRes),
                        contentDescription = config.label,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(
                            if (isSelected) accentColor else contentColor.copy(alpha = 0.7f)
                        )
                    )
                    BasicText(
                        text = config.label,
                        style = TextStyle(
                            color = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun UserInfoRow(
    userName: String,
    userAvatar: String?,
    visibility: String,
    showVisibilityDropdown: Boolean,
    onVisibilityDropdownToggle: (Boolean) -> Unit,
    onVisibilityChange: (String) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (!userAvatar.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(userAvatar)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                BasicText(
                    text = userName.firstOrNull()?.uppercase() ?: "U",
                    style = TextStyle(contentColor, 16.sp, FontWeight.Bold)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = userName,
                style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
            )
            
            // Visibility selector
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .clickable { onVisibilityDropdownToggle(true) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, label) = when (visibility) {
                        "PUBLIC" -> "🌐" to "Anyone"
                        "CONNECTIONS" -> "👥" to "Connections"
                        "PRIVATE" -> "🔒" to "Only me"
                        else -> "🌐" to "Anyone"
                    }
                    BasicText(
                        text = "$icon $label ▼",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 11.sp)
                    )
                }
                
                DropdownMenu(
                    expanded = showVisibilityDropdown,
                    onDismissRequest = { onVisibilityDropdownToggle(false) }
                ) {
                    DropdownMenuItem(
                        text = { BasicText("🌐 Anyone", style = TextStyle(contentColor, 14.sp)) },
                        onClick = {
                            onVisibilityChange("PUBLIC")
                            onVisibilityDropdownToggle(false)
                        }
                    )
                    DropdownMenuItem(
                        text = { BasicText("👥 Connections only", style = TextStyle(contentColor, 14.sp)) },
                        onClick = {
                            onVisibilityChange("CONNECTIONS")
                            onVisibilityDropdownToggle(false)
                        }
                    )
                    DropdownMenuItem(
                        text = { BasicText("🔒 Only me", style = TextStyle(contentColor, 14.sp)) },
                        onClick = {
                            onVisibilityChange("PRIVATE")
                            onVisibilityDropdownToggle(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RichTextToolbar(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    showColorPicker: Boolean,
    onColorPickerToggle: (Boolean) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Bold
        ToolbarButton(
            icon = "B",
            label = "Bold",
            contentColor = contentColor
        ) {
            wrapSelection(textFieldValue, onTextFieldValueChange, "**", "**")
        }
        
        // Italic
        ToolbarButton(
            icon = "I",
            label = "Italic",
            contentColor = contentColor,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        ) {
            wrapSelection(textFieldValue, onTextFieldValueChange, "*", "*")
        }
        
        // List
        ToolbarButton(
            icon = "•",
            label = "List",
            contentColor = contentColor
        ) {
            val text = textFieldValue.text
            val cursor = textFieldValue.selection.start
            val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
            val newText = text.substring(0, lineStart) + "- " + text.substring(lineStart)
            onTextFieldValueChange(TextFieldValue(
                text = newText,
                selection = TextRange(cursor + 2)
            ))
        }
        
        // Code
        ToolbarButton(
            icon = "</>",
            label = "Code",
            contentColor = contentColor
        ) {
            wrapSelection(textFieldValue, onTextFieldValueChange, "`", "`")
        }
        
        // @Mention
        ToolbarButton(
            icon = "@",
            label = "Mention",
            contentColor = contentColor
        ) {
            val text = textFieldValue.text
            val cursor = textFieldValue.selection.start
            val newText = text.substring(0, cursor) + "@" + text.substring(cursor)
            onTextFieldValueChange(TextFieldValue(
                text = newText,
                selection = TextRange(cursor + 1)
            ))
        }
        
        // Color
        ToolbarButton(
            icon = "🎨",
            label = "Color",
            contentColor = contentColor,
            isActive = showColorPicker
        ) {
            onColorPickerToggle(!showColorPicker)
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: String,
    label: String,
    contentColor: Color,
    fontStyle: androidx.compose.ui.text.font.FontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) contentColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = icon,
            style = TextStyle(
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (icon == "B") FontWeight.Bold else FontWeight.Normal,
                fontStyle = fontStyle
            )
        )
    }
}

private fun wrapSelection(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    prefix: String,
    suffix: String
) {
    val selection = textFieldValue.selection
    val text = textFieldValue.text
    
    if (selection.length > 0) {
        val selectedText = text.substring(selection.start, selection.end)
        val newText = text.substring(0, selection.start) + 
            prefix + selectedText + suffix + 
            text.substring(selection.end)
        onTextFieldValueChange(TextFieldValue(
            text = newText,
            selection = TextRange(selection.start + prefix.length, selection.end + prefix.length)
        ))
    } else {
        val newText = text.substring(0, selection.start) + 
            prefix + suffix + 
            text.substring(selection.start)
        onTextFieldValueChange(TextFieldValue(
            text = newText,
            selection = TextRange(selection.start + prefix.length)
        ))
    }
}

@Composable
private fun ColorPickerRow(
    onColorSelected: (String) -> Unit,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        colorPresets.forEach { (hex, name) ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(hex)))
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}

@Composable
private fun ContentTextArea(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    contentColor: Color,
    mentionSearchResults: List<MentionUser>,
    isSearchingMentions: Boolean,
    showMentionDropdown: Boolean,
    onMentionSelected: (MentionUser) -> Unit,
    onDismissMentionDropdown: () -> Unit,
    backdrop: LayerBackdrop? = null
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Glass-themed mention suggestions (inline, above text field)
        AnimatedVisibility(
            visible = showMentionDropdown && (mentionSearchResults.isNotEmpty() || isSearchingMentions),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(20f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(24f.dp.toPx())
                                },
                                onDrawSurface = {
                                    // Glass shine gradient
                                    drawRect(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.25f),
                                                Color.White.copy(alpha = 0.12f),
                                                Color.White.copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                }
                            )
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.2f),
                                            Color.White.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                        }
                    )
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header
                    BasicText(
                        text = "Tag someone",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    
                    if (isSearchingMentions) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = contentColor.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        mentionSearchResults.take(5).forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .then(
                                        if (backdrop != null) {
                                            Modifier.drawBackdrop(
                                                backdrop = backdrop,
                                                shape = { RoundedRectangle(14f.dp) },
                                                effects = {
                                                    vibrancy()
                                                    blur(16f.dp.toPx())
                                                },
                                                onDrawSurface = {
                                                    drawRect(
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                Color.White.copy(alpha = 0.18f),
                                                                Color.White.copy(alpha = 0.1f)
                                                            )
                                                        )
                                                    )
                                                }
                                            )
                                        } else {
                                            Modifier.background(Color.White.copy(alpha = 0.12f))
                                        }
                                    )
                                    .clickable { onMentionSelected(user) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Profile image with glass ring
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.3f),
                                                    Color.White.copy(alpha = 0.1f)
                                                )
                                            )
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(contentColor.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!user.profileImage.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(user.profileImage)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            BasicText(
                                                text = user.name?.firstOrNull()?.uppercase() ?: "U",
                                                style = TextStyle(contentColor, 16.sp, FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                                
                                // User info
                                Column(modifier = Modifier.weight(1f)) {
                                    BasicText(
                                        text = user.name ?: user.username ?: "User",
                                        style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                                    )
                                    BasicText(
                                        text = "@${user.username ?: ""}",
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                                    )
                                }
                                
                                // Tap indicator
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        text = "+",
                                        style = TextStyle(contentColor.copy(alpha = 0.7f), 16.sp, FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Text field (full height)
        BasicTextField(
            value = textFieldValue,
            onValueChange = onTextFieldValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(contentColor.copy(alpha = 0.04f))
                .padding(16.dp),
            textStyle = TextStyle(contentColor, 16.sp, lineHeight = 24.sp),
            cursorBrush = SolidColor(contentColor),
            decorationBox = { innerTextField ->
                Box {
                    if (textFieldValue.text.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = TextStyle(contentColor.copy(alpha = 0.4f), 16.sp)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

private fun getPlaceholder(postType: PostType): String {
    return when (postType) {
        PostType.TEXT -> "What do you want to share? Use **bold**, *italic*, [color:#22c55e]colored text[/color]"
        PostType.IMAGE -> "Add a caption for your images..."
        PostType.VIDEO -> "Describe your video..."
        PostType.LINK -> "Add a comment about this link..."
        PostType.POLL -> "Ask a question..."
        PostType.ARTICLE -> "Write your article content..."
        PostType.CELEBRATION -> "Share more about your achievement!"
        else -> "What's on your mind?"
    }
}

// ==================== Type-Specific Components ====================

@Composable
private fun ImagePicker(
    imageUris: List<Uri>,
    onPickImages: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (imageUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.06f))
                    .clickable(onClick = onPickImages),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText("📷", style = TextStyle(fontSize = 32.sp))
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicText(
                        text = "Add images (max 10)",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imageUris.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(uri).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onRemoveImage(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("×", style = TextStyle(Color.White, 14.sp))
                        }
                    }
                }
                
                if (imageUris.size < 10) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.06f))
                            .clickable(onClick = onPickImages),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("+", style = TextStyle(contentColor, 24.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPicker(
    videoUri: Uri?,
    onPickVideo: () -> Unit,
    onRemoveVideo: () -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    if (videoUri == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.06f))
                .clickable(onClick = onPickVideo),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText("🎥", style = TextStyle(fontSize = 32.sp))
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = "Add video (max 500MB)",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText("🎬", style = TextStyle(fontSize = 24.sp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = "Video attached",
                        style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                    )
                    BasicText(
                        text = videoUri.lastPathSegment ?: "video.mp4",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f))
                        .clickable(onClick = onRemoveVideo),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("×", style = TextStyle(Color.Red, 18.sp))
                }
            }
        }
    }
}

@Composable
private fun LinkInput(
    linkUrl: String,
    onLinkUrlChange: (String) -> Unit,
    contentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = "Link URL",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
        )
        BasicTextField(
            value = linkUrl,
            onValueChange = onLinkUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.06f))
                .padding(16.dp),
            textStyle = TextStyle(contentColor, 14.sp),
            cursorBrush = SolidColor(contentColor),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (linkUrl.isEmpty()) {
                        BasicText(
                            text = "https://example.com",
                            style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PollEditor(
    options: List<String>,
    onOptionsChange: (List<String>) -> Unit,
    durationHours: Int,
    onDurationChange: (Int) -> Unit,
    showResultsBeforeVote: Boolean,
    onShowResultsChange: (Boolean) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    var showDurationDropdown by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            text = "Poll Options (2-6)",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
        )
        
        options.forEachIndexed { index, option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = option,
                    onValueChange = { newValue ->
                        onOptionsChange(options.toMutableList().apply { set(index, newValue) })
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (option.isEmpty()) {
                                BasicText(
                                    text = "Option ${index + 1}",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                if (options.size > 2) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.1f))
                            .clickable {
                                onOptionsChange(options.filterIndexed { i, _ -> i != index })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("×", style = TextStyle(Color.Red, 16.sp))
                    }
                }
            }
        }
        
        if (options.size < 6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .clickable { onOptionsChange(options + "") }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "+ Add option",
                    style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                )
            }
        }
        
        // Duration selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "Duration:",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .clickable { showDurationDropdown = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        text = pollDurations.find { it.first == durationHours }?.second ?: "1 day",
                        style = TextStyle(contentColor, 14.sp)
                    )
                }
                
                DropdownMenu(
                    expanded = showDurationDropdown,
                    onDismissRequest = { showDurationDropdown = false }
                ) {
                    pollDurations.forEach { (hours, label) ->
                        DropdownMenuItem(
                            text = { BasicText(label, style = TextStyle(contentColor, 14.sp)) },
                            onClick = {
                                onDurationChange(hours)
                                showDurationDropdown = false
                            }
                        )
                    }
                }
            }
        }
        
        // Show results before vote toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowResultsChange(!showResultsBeforeVote) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showResultsBeforeVote,
                onCheckedChange = onShowResultsChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicText(
                text = "Show results before voting",
                style = TextStyle(contentColor, 14.sp)
            )
        }
    }
}

@Composable
private fun ArticleEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    coverUri: Uri?,
    onPickCover: () -> Unit,
    onRemoveCover: () -> Unit,
    tags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Title input
        Column {
            BasicText(
                text = "Article Title *",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.06f))
                    .padding(16.dp),
                textStyle = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
                cursorBrush = SolidColor(contentColor),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            BasicText(
                                text = "Enter article title...",
                                style = TextStyle(contentColor.copy(alpha = 0.4f), 16.sp)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        
        // Cover image
        Column {
            BasicText(
                text = "Cover Image (optional)",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (coverUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .clickable(onClick = onPickCover),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "+ Add cover image",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(coverUri).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable(onClick = onRemoveCover),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("×", style = TextStyle(Color.White, 16.sp))
                    }
                }
            }
        }
        
        // Tags
        Column {
            BasicText(
                text = "Tags (max 5)",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = tagInput,
                    onValueChange = onTagInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (tagInput.isEmpty()) {
                                BasicText(
                                    text = "Add a tag",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                if (tags.size < 5 && tagInput.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor)
                            .clickable(onClick = onAddTag)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        BasicText("+", style = TextStyle(Color.White, 14.sp))
                    }
                }
            }
            
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = tag,
                                style = TextStyle(accentColor, 12.sp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { onRemoveTag(tag) },
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("×", style = TextStyle(accentColor, 12.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationPicker(
    selectedType: CelebrationType?,
    onTypeSelected: (CelebrationType) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            text = "What are you celebrating?",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
        )
        
        // Grid of celebration types (2 columns)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CelebrationType.entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { type ->
                        val isSelected = type == selectedType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.2f)
                                    else contentColor.copy(alpha = 0.06f)
                                )
                                .clickable { onTypeSelected(type) }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                BasicText(
                                    text = type.emoji,
                                    style = TextStyle(fontSize = 28.sp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                BasicText(
                                    text = type.label,
                                    style = TextStyle(
                                        color = if (isSelected) accentColor else contentColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // Fill empty space if odd number of items
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
