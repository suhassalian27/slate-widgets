package com.altusix.slate.widgets.applauncher

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class VectorIconItem(
    val name: String,
    val imageVector: ImageVector
)

object AppLauncherVectorIcons {
    val icons: List<VectorIconItem> = listOf(
        // Essentials & Navigation
        VectorIconItem("star", Icons.Default.Star),
        VectorIconItem("favorite", Icons.Default.Favorite),
        VectorIconItem("favorite_border", Icons.Default.FavoriteBorder),
        VectorIconItem("home", Icons.Default.Home),
        VectorIconItem("person", Icons.Default.Person),
        VectorIconItem("account_box", Icons.Default.AccountBox),
        VectorIconItem("account_circle", Icons.Default.AccountCircle),
        VectorIconItem("face", Icons.Default.Face),
        VectorIconItem("settings", Icons.Default.Settings),
        VectorIconItem("build", Icons.Default.Build),

        // Communication & Social
        VectorIconItem("phone", Icons.Default.Phone),
        VectorIconItem("call", Icons.Default.Call),
        VectorIconItem("email", Icons.Default.Email),
        VectorIconItem("send", Icons.Default.Send),
        VectorIconItem("share", Icons.Default.Share),
        VectorIconItem("thumb_up", Icons.Default.ThumbUp),
        VectorIconItem("notifications", Icons.Default.Notifications),

        // Search & Location
        VectorIconItem("search", Icons.Default.Search),
        VectorIconItem("location", Icons.Default.LocationOn),
        VectorIconItem("place", Icons.Default.Place),

        // Actions & Controls
        VectorIconItem("add", Icons.Default.Add),
        VectorIconItem("add_circle", Icons.Default.AddCircle),
        VectorIconItem("edit", Icons.Default.Edit),
        VectorIconItem("create", Icons.Default.Create),
        VectorIconItem("delete", Icons.Default.Delete),
        VectorIconItem("check", Icons.Default.Check),
        VectorIconItem("check_circle", Icons.Default.CheckCircle),
        VectorIconItem("done", Icons.Default.Done),
        VectorIconItem("close", Icons.Default.Close),
        VectorIconItem("clear", Icons.Default.Clear),
        VectorIconItem("refresh", Icons.Default.Refresh),

        // Media & Utility
        VectorIconItem("play_arrow", Icons.Default.PlayArrow),
        VectorIconItem("shopping_cart", Icons.Default.ShoppingCart),
        VectorIconItem("lock", Icons.Default.Lock),
        VectorIconItem("date_range", Icons.Default.DateRange),
        VectorIconItem("list", Icons.Default.List),
        VectorIconItem("menu", Icons.Default.Menu),
        VectorIconItem("more_vert", Icons.Default.MoreVert),
        VectorIconItem("info", Icons.Default.Info),
        VectorIconItem("warning", Icons.Default.Warning),

        // Arrows & Navigation Controls
        VectorIconItem("arrow_forward", Icons.Default.ArrowForward),
        VectorIconItem("arrow_back", Icons.Default.ArrowBack),
        VectorIconItem("arrow_drop_down", Icons.Default.ArrowDropDown),
        VectorIconItem("keyboard_arrow_down", Icons.Default.KeyboardArrowDown),
        VectorIconItem("keyboard_arrow_up", Icons.Default.KeyboardArrowUp),
        VectorIconItem("keyboard_arrow_left", Icons.Default.KeyboardArrowLeft),
        VectorIconItem("keyboard_arrow_right", Icons.Default.KeyboardArrowRight)
    )

    fun findIcon(name: String): ImageVector? {
        return icons.find { it.name.equals(name, ignoreCase = true) }?.imageVector
    }
}