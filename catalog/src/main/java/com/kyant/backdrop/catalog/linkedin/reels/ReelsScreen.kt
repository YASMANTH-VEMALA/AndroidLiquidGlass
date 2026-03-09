package com.kyant.backdrop.catalog.linkedin.reels

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reels Preview Section for Home Feed
 * Horizontal scrollable row of reel thumbnails with preview on hover/long-press
 */
@Composable
fun ReelsPreviewSection(
    reels: List<Reel>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean = false,
    onReelClick: (Int) -> Unit = {},
    onSeeAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Reels icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "▶",
                        style = TextStyle(Color.White, 12.sp, FontWeight.Bold)
                    )
                }
                Spacer(Modifier.width(8.dp))
                BasicText(
                    "Reels",
                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BasicText(
                    "See all →",
                    style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                )
            }
        }
        
        // Loading state
        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    ReelThumbnailSkeleton()
                }
            }
        } else if (reels.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "No reels yet",
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                )
            }
        } else {
            // Reels thumbnails
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(reels, key = { _, reel -> reel.id }) { index, reel ->
                    ReelThumbnailCard(
                        reel = reel,
                        backdrop = backdrop,
                        onClick = { onReelClick(index) }
                    )
                }
                
                // "Explore More" card at the end
                item {
                    ExploreMoreCard(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        onClick = onSeeAllClick
                    )
                }
            }
        }
    }
}

/**
 * Individual reel thumbnail card for the preview section
 */
@Composable
private fun ReelThumbnailCard(
    reel: Reel,
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .width(120.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(reel.thumbnailUrl ?: reel.videoUrl)
                .crossfade(true)
                .build(),
            contentDescription = reel.title ?: "Reel",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )
        
        // Stats overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            // Views count
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText("▶", style = TextStyle(Color.White, 10.sp))
                Spacer(Modifier.width(4.dp))
                BasicText(
                    formatCount(reel.viewsCount),
                    style = TextStyle(Color.White, 11.sp, FontWeight.Medium)
                )
            }
            
            Spacer(Modifier.height(2.dp))
            
            // Author
            BasicText(
                "@${reel.author.username ?: reel.author.name}",
                style = TextStyle(Color.White.copy(alpha = 0.9f), 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Duration badge
        reel.durationSeconds.takeIf { it > 0 }?.let { duration ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                BasicText(
                    formatDuration(duration),
                    style = TextStyle(Color.White, 9.sp, FontWeight.Medium)
                )
            }
        }
    }
}

/**
 * Skeleton loader for reel thumbnail
 */
@Composable
private fun ReelThumbnailSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(120.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Gray.copy(alpha = 0.2f))
    )
}

/**
 * "Explore More" card at the end of reels row
 */
@Composable
private fun ExploreMoreCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(120.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText("▶", style = TextStyle(Color.White, 18.sp, FontWeight.Bold))
            }
            Spacer(Modifier.height(8.dp))
            BasicText(
                "Explore\nReels",
                style = TextStyle(Color.White, 12.sp, FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// ==================== Full Screen Reels Feed ====================

/**
 * Full-screen reels feed with vertical paging (Instagram-like)
 * Features:
 * - Vertical snap-to-item paging
 * - Video preloading for next 2-3 reels
 * - Smooth playback transitions
 * - Double-tap to like
 */
@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsFeedScreen(
    reels: List<Reel>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onLike: (String) -> Unit = {},
    onSave: (String) -> Unit = {},
    onComment: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onTrackView: (String, Long, Boolean) -> Unit = { _, _, _ -> },
    onLoadMore: () -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { reels.size }
    )
    
    // Track current visible reel for autoplay
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    
    // Preload next reels
    LaunchedEffect(currentPage) {
        // Load more when near the end
        if (currentPage >= reels.size - 3) {
            onLoadMore()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 2 // Preload 2 pages ahead
            ) { page ->
                val reel = reels[page]
                val isActive = page == currentPage
                
                ReelCard(
                    reel = reel,
                    isActive = isActive,
                    onLike = { onLike(reel.id) },
                    onSave = { onSave(reel.id) },
                    onComment = { onComment(reel.id) },
                    onShare = { onShare(reel.id) },
                    onProfileClick = { onProfileClick(reel.author.id) },
                    onTrackView = { watchTime, completed -> 
                        onTrackView(reel.id, watchTime, completed) 
                    }
                )
            }
            
            // Close button
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDismiss() }
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "✕",
                    style = TextStyle(Color.White, 18.sp, FontWeight.Bold)
                )
            }
            
            // Progress indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                BasicText(
                    "${currentPage + 1}/${reels.size}",
                    style = TextStyle(Color.White.copy(alpha = 0.7f), 12.sp)
                )
            }
        }
    }
}

/**
 * Individual Reel Card with video player
 */
@OptIn(UnstableApi::class)
@Composable
private fun ReelCard(
    reel: Reel,
    isActive: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onProfileClick: () -> Unit,
    onTrackView: (Long, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var showHeart by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(reel.isLiked) }
    var likesCount by remember { mutableIntStateOf(reel.likesCount) }
    var isSaved by remember { mutableStateOf(reel.isSaved) }
    
    // Watch time tracking
    var watchStartTime by remember { mutableLongStateOf(0L) }
    var viewTracked by remember { mutableStateOf(false) }
    
    // Create ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = if (!reel.hlsUrl.isNullOrEmpty()) {
                MediaItem.fromUri(reel.hlsUrl)
            } else {
                MediaItem.fromUri(reel.videoUrl)
            }
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING
                    
                    if (playbackState == Player.STATE_ENDED) {
                        // Track completed view
                        val watchTime = System.currentTimeMillis() - watchStartTime
                        onTrackView(watchTime, true)
                    }
                }
                
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }
    
    // Handle active state changes
    LaunchedEffect(isActive) {
        if (isActive) {
            watchStartTime = System.currentTimeMillis()
            viewTracked = false
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            
            // Track view after 3 seconds
            delay(3000)
            if (!viewTracked && isActive) {
                viewTracked = true
                val watchTime = System.currentTimeMillis() - watchStartTime
                onTrackView(watchTime, false)
            }
        } else {
            exoPlayer.pause()
            exoPlayer.seekTo(0)
        }
    }
    
    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (isActive) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
    
    // Double-tap handler
    var lastTapTime by remember { mutableLongStateOf(0L) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Single tap: toggle play/pause
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    onDoubleTap = {
                        // Double tap: like
                        if (!isLiked) {
                            isLiked = true
                            likesCount++
                            showHeart = true
                            onLike()
                            
                            scope.launch {
                                delay(800)
                                showHeart = false
                            }
                        }
                    }
                )
            }
    ) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading indicator
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Show thumbnail while loading
                reel.thumbnailUrl?.let { thumb ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumb)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Loading spinner overlay
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "●",
                        style = TextStyle(Color.White, 24.sp)
                    )
                }
            }
        }
        
        // Double-tap heart animation
        AnimatedVisibility(
            visible = showHeart,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            BasicText(
                "❤",
                style = TextStyle(Color.Red, 80.sp)
            )
        }
        
        // Bottom gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        
        // Reel info (bottom left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 100.dp, end = 80.dp)
                .navigationBarsPadding()
        ) {
            // Author info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                ) {
                    reel.author.profileImage?.let { img ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(img)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                reel.author.name?.firstOrNull()?.uppercase() ?: "U",
                                style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.width(10.dp))
                
                Column {
                    BasicText(
                        reel.author.name ?: "User",
                        style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        "@${reel.author.username ?: ""}",
                        style = TextStyle(Color.White.copy(alpha = 0.7f), 12.sp)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                // Follow button (if not following)
                if (!reel.author.isFollowing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .clickable { /* Follow action */ }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        BasicText(
                            "Follow",
                            style = TextStyle(Color.Black, 12.sp, FontWeight.SemiBold)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Caption
            reel.caption?.let { caption ->
                BasicText(
                    caption,
                    style = TextStyle(Color.White, 13.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Hashtags
            if (reel.hashtags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                BasicText(
                    reel.hashtags.take(3).joinToString(" ") { "#$it" },
                    style = TextStyle(Color.White.copy(alpha = 0.9f), 12.sp, FontWeight.Medium)
                )
            }
            
            // Audio (if any)
            reel.audio?.let { audio ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText("♪", style = TextStyle(Color.White, 12.sp))
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        "${audio.title} • ${audio.artist ?: "Original"}",
                        style = TextStyle(Color.White.copy(alpha = 0.8f), 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Action buttons (right side)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 100.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Like button
            ActionButton(
                icon = if (isLiked) "❤" else "♡",
                label = formatCount(likesCount),
                isActive = isLiked,
                activeColor = Color.Red,
                onClick = {
                    isLiked = !isLiked
                    likesCount += if (isLiked) 1 else -1
                    onLike()
                }
            )
            
            // Comment button
            ActionButton(
                icon = "💬",
                label = formatCount(reel.commentsCount),
                onClick = onComment
            )
            
            // Save button
            ActionButton(
                icon = if (isSaved) "🔖" else "📑",
                label = formatCount(reel.savesCount),
                isActive = isSaved,
                activeColor = Color.Yellow,
                onClick = {
                    isSaved = !isSaved
                    onSave()
                }
            )
            
            // Share button
            ActionButton(
                icon = "↗",
                label = formatCount(reel.sharesCount),
                onClick = onShare
            )
        }
    }
}

/**
 * Action button for reels (like, comment, share, etc.)
 */
@Composable
private fun ActionButton(
    icon: String,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        BasicText(
            icon,
            style = TextStyle(
                color = if (isActive) activeColor else Color.White,
                fontSize = 28.sp
            )
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            label,
            style = TextStyle(Color.White, 11.sp, FontWeight.Medium)
        )
    }
}

// ==================== Utility Functions ====================

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${(count / 1_000_000.0).format(1)}M"
        count >= 1_000 -> "${(count / 1_000.0).format(1)}K"
        else -> count.toString()
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}:${secs.toString().padStart(2, '0')}" else "0:${secs.toString().padStart(2, '0')}"
}

private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this).trimEnd('0').trimEnd('.')
}
