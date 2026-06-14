package com.magisk.next.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Иконки из material-icons-extended, не входящие в material-icons-core.
 * После замены зависимости на core — использовать эти объекты вместо Icons.Default.*
 *
 * Замены:
 *   Icons.Default.AutoAwesome                  → AppIcons.AutoAwesome
 *   Icons.Default.Code                         → AppIcons.Code
 *   Icons.Default.DeleteSweep                  → AppIcons.DeleteSweep
 *   Icons.Default.Filter                       → AppIcons.Filter
 *   Icons.Default.FilterList                   → AppIcons.FilterList
 *   Icons.Default.Folder                       → AppIcons.Folder
 *   Icons.Default.FolderOpen                   → AppIcons.FolderOpen
 *   Icons.AutoMirrored.Filled.InsertDriveFile  → AppIcons.InsertDriveFile
 *   Icons.Default.ListAlt                      → AppIcons.ListAlt
 *   Icons.Default.Palette                      → AppIcons.Palette
 *   Icons.Default.Save                         → AppIcons.Save
 *   Icons.Default.UploadFile                   → AppIcons.UploadFile
 *   Icons.Default.ChevronRight                 → AppIcons.ChevronRight
 *   Icons.Default.Visibility                   → AppIcons.Visibility
 *
 * Иконки, остающиеся в Icons.Default.* (есть в core):
 *   Add, ArrowBack, AutoMirrored.ArrowBack,
 *   Delete, Edit, Info, KeyboardArrowDown, KeyboardArrowUp,
 *   MoreVert, Search, Settings
 */
object AppIcons {

    val AutoAwesome: ImageVector by lazy {
        icon(
            "AutoAwesome",
            "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9z" +
            "m-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5z" +
            "M19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z"
        )
    }

    val Code: ImageVector by lazy {
        icon(
            "Code",
            "M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4z" +
            "m5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"
        )
    }

    val DeleteSweep: ImageVector by lazy {
        icon(
            "DeleteSweep",
            "M15 16h4v2h-4zm0-8h7v2h-7zm0 4h6v2h-6z" +
            "M3 18c0 1.1.9 2 2 2h6c1.1 0 2-.9 2-2V8H3v10z" +
            "M14 5h-3l-1-1H6L5 5H2v2h12z"
        )
    }

    val Filter: ImageVector by lazy {
        icon(
            "Filter",
            "M4.25 5.61C6.27 8.2 10 13 10 13v6c0 .55.45 1 1 1h2c.55 0 1-.45 1-1v-6" +
            "s3.72-4.8 5.74-7.39c.51-.66.04-1.61-.79-1.61H5.04c-.83 0-1.3.95-.79 1.61z"
        )
    }

    val FilterList: ImageVector by lazy {
        icon(
            "FilterList",
            "M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z"
        )
    }

    val Folder: ImageVector by lazy {
        icon(
            "Folder",
            "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2" +
            "V8c0-1.1-.9-2-2-2h-8l-2-2z"
        )
    }

    val FolderOpen: ImageVector by lazy {
        icon(
            "FolderOpen",
            "M20 6h-8l-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2" +
            "V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z"
        )
    }

    /** Замена Icons.AutoMirrored.Filled.InsertDriveFile */
    val InsertDriveFile: ImageVector by lazy {
        icon(
            "InsertDriveFile",
            "M6 2c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2" +
            "V8l-6-6H6zm7 7V3.5L18.5 9H13z"
        )
    }

    val ListAlt: ImageVector by lazy {
        icon(
            "ListAlt",
            "M19 5v14H5V5h14m1.1-2H3.9c-.5 0-.9.4-.9.9v16.2c0 .4.4.9.9.9h16.2" +
            "c.4 0 .9-.5.9-.9V3.9c0-.5-.5-.9-.9-.9z" +
            "M11 7h6v2h-6V7zm0 4h6v2h-6v-2zm0 4h6v2h-6v-2z" +
            "M7 7h2v2H7V7zm0 4h2v2H7v-2zm0 4h2v2H7v-2z"
        )
    }

    val Palette: ImageVector by lazy {
        icon(
            "Palette",
            "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5" +
            "0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16" +
            "c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8z" +
            "m-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12z" +
            "m3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8z" +
            "m5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8z" +
            "m3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"
        )
    }

    val Save: ImageVector by lazy {
        icon(
            "Save",
            "M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4z" +
            "m-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z" +
            "m3-10H5V5h10v4z"
        )
    }

    val UploadFile: ImageVector by lazy {
        icon(
            "UploadFile",
            "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2" +
            "V8l-4-4zm4 18H6V4h7v5h5v11z" +
            "M8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 11 8 15.01z"
        )
    }

    val ChevronRight: ImageVector by lazy {
        icon(
            "ChevronRight",
            "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"
        )
    }

    val Visibility: ImageVector by lazy {
        icon(
            "Visibility",
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5" +
            "c-1.73-4.39-6-7.5-11-7.5z" +
            "M12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z" +
            "m0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"
        )
    }

    // Приватный строитель — одна строка SVG-пути → ImageVector
    private fun icon(name: String, pathStr: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(pathStr).toNodes(),
            fill = SolidColor(Color.Black)
        ).build()
}