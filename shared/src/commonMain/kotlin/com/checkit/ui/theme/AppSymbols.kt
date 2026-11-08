package com.checkit.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dangerous
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb as OutlinedLightbulb
import androidx.compose.material.icons.outlined.PriorityHigh as OutlinedPriorityHigh
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppSymbolCategory {
    Callout,
    List,
    Task,
    Action,
    Status,
    Media,
    Food,
    Transport,
    Nature,
    Object,
    General
}

/**
 * Definition of a cross-platform symbol mapping.
 */
data class AppSymbolSpec(
    val key: String,
    val sfSymbol: String,
    val materialIconName: String,
    val category: AppSymbolCategory = AppSymbolCategory.General
)

/**
 * Central registry object for application symbols across Android (Compose Material Icons)
 * and macOS (SF Symbols). All keys are normalized to lowercase.
 */
object AppSymbols {

    private val specsList: List<AppSymbolSpec> = listOf(
        // Callout Symbols
        AppSymbolSpec("info", "info.circle", "Info", AppSymbolCategory.Callout),
        AppSymbolSpec("lightbulb", "lightbulb", "Lightbulb", AppSymbolCategory.Callout),
        AppSymbolSpec("idea", "brain.head.profile", "Psychology", AppSymbolCategory.Callout),
        AppSymbolSpec("sparkles", "sparkles", "AutoAwesome", AppSymbolCategory.Callout),
        AppSymbolSpec("summary", "star", "StarOutline", AppSymbolCategory.Callout),
        AppSymbolSpec("important", "exclamationmark.circle", "PriorityHigh", AppSymbolCategory.Callout),
        AppSymbolSpec("warning", "exclamationmark.triangle", "Warning", AppSymbolCategory.Callout),
        AppSymbolSpec("caution", "exclamationmark.octagon", "Dangerous", AppSymbolCategory.Callout),
        AppSymbolSpec("question", "questionmark.circle", "HelpOutline", AppSymbolCategory.Callout),
        AppSymbolSpec("quote", "quote.opening", "FormatQuote", AppSymbolCategory.Callout),

        // List & General Category Icons (Normalized to lowercase keys)
        AppSymbolSpec("inbox", "tray", "Inbox", AppSymbolCategory.List),
        AppSymbolSpec("format_list_bulleted", "list.bullet", "FormatListBulleted", AppSymbolCategory.List),
        AppSymbolSpec("bookmark", "bookmark", "Bookmark", AppSymbolCategory.List),
        AppSymbolSpec("local_offer", "tag", "LocalOffer", AppSymbolCategory.List),
        AppSymbolSpec("card_giftcard", "gift", "CardGiftcard", AppSymbolCategory.List),
        AppSymbolSpec("cake", "birthday.cake", "Cake", AppSymbolCategory.List),
        AppSymbolSpec("school", "graduationcap", "School", AppSymbolCategory.List),
        AppSymbolSpec("backpack", "backpack", "Backpack", AppSymbolCategory.List),
        AppSymbolSpec("edit_note", "square.and.pencil", "EditNote", AppSymbolCategory.List),
        AppSymbolSpec("description", "doc.text", "Description", AppSymbolCategory.List),
        AppSymbolSpec("menu_book", "book", "MenuBook", AppSymbolCategory.List),
        AppSymbolSpec("credit_card", "creditcard", "CreditCard", AppSymbolCategory.List),
        AppSymbolSpec("account_balance_wallet", "wallet.pass", "AccountBalanceWallet", AppSymbolCategory.List),
        AppSymbolSpec("attach_money", "dollarsign", "AttachMoney", AppSymbolCategory.List),
        AppSymbolSpec("fitness_center", "figure.run", "FitnessCenter", AppSymbolCategory.List),
        AppSymbolSpec("directions_run", "figure.run", "DirectionsRun", AppSymbolCategory.List),
        AppSymbolSpec("restaurant", "fork.knife", "Restaurant", AppSymbolCategory.List),
        AppSymbolSpec("wine_bar", "wineglass", "WineBar", AppSymbolCategory.List),
        AppSymbolSpec("medication", "cross.vial", "Medication", AppSymbolCategory.List),
        AppSymbolSpec("home", "house", "Home", AppSymbolCategory.List),
        AppSymbolSpec("apartment", "building.2", "Apartment", AppSymbolCategory.List),
        AppSymbolSpec("account_balance", "building.columns", "AccountBalance", AppSymbolCategory.List),
        AppSymbolSpec("landscape", "mountain.2", "Landscape", AppSymbolCategory.List),
        AppSymbolSpec("desktop_windows", "desktopcomputer", "DesktopWindows", AppSymbolCategory.List),
        AppSymbolSpec("sports_esports", "gamecontroller", "SportsEsports", AppSymbolCategory.List),
        AppSymbolSpec("headphones", "headphones", "Headphones", AppSymbolCategory.List),
        AppSymbolSpec("eco", "leaf", "Eco", AppSymbolCategory.List),
        AppSymbolSpec("person", "person", "Person", AppSymbolCategory.List),
        AppSymbolSpec("diversity_3", "person.3", "Diversity3", AppSymbolCategory.List),
        AppSymbolSpec("family_restroom", "figure.2.and.child.holdinghands", "FamilyRestroom", AppSymbolCategory.List),
        AppSymbolSpec("pets", "pawprint", "Pets", AppSymbolCategory.List),
        AppSymbolSpec("child_care", "stroller", "ChildCare", AppSymbolCategory.List),
        AppSymbolSpec("set_meal", "fish", "SetMeal", AppSymbolCategory.List),
        AppSymbolSpec("shopping_basket", "basket", "ShoppingBasket", AppSymbolCategory.List),
        AppSymbolSpec("work", "briefcase", "Work", AppSymbolCategory.List),
        AppSymbolSpec("local_mall", "bag", "LocalMall", AppSymbolCategory.List),
        AppSymbolSpec("inventory_2", "archivebox", "Inventory2", AppSymbolCategory.List),
        AppSymbolSpec("sports_soccer", "soccerball", "SportsSoccer", AppSymbolCategory.List),
        AppSymbolSpec("sports_baseball", "baseball", "SportsBaseball", AppSymbolCategory.List),
        AppSymbolSpec("sports_basketball", "basketball", "SportsBasketball", AppSymbolCategory.List),
        AppSymbolSpec("sports_football", "football", "SportsFootball", AppSymbolCategory.List),
        AppSymbolSpec("sports_tennis", "tennis.racket", "SportsTennis", AppSymbolCategory.List),
        AppSymbolSpec("train", "train.side.front.car", "Train", AppSymbolCategory.List),
        AppSymbolSpec("folder", "folder", "Folder", AppSymbolCategory.List),
        AppSymbolSpec("directions_boat", "ferry", "DirectionsBoat", AppSymbolCategory.List),
        AppSymbolSpec("directions_car", "car", "DirectionsCar", AppSymbolCategory.List),
        AppSymbolSpec("beach_access", "umbrella", "BeachAccess", AppSymbolCategory.List),
        AppSymbolSpec("wb_sunny", "sun.max", "WbSunny", AppSymbolCategory.List),
        AppSymbolSpec("dark_mode", "moon", "DarkMode", AppSymbolCategory.List),
        AppSymbolSpec("water_drop", "drop", "WaterDrop", AppSymbolCategory.List),
        AppSymbolSpec("ac_unit", "snowflake", "AcUnit", AppSymbolCategory.List),
        AppSymbolSpec("local_fire_department", "flame", "LocalFireDepartment", AppSymbolCategory.List),
        AppSymbolSpec("construction", "wrench.and.screwdriver", "Construction", AppSymbolCategory.List),
        AppSymbolSpec("build", "hammer", "Build", AppSymbolCategory.List),
        AppSymbolSpec("content_cut", "scissors", "ContentCut", AppSymbolCategory.List),
        AppSymbolSpec("architecture", "ruler", "Architecture", AppSymbolCategory.List),
        AppSymbolSpec("data_object", "curlybraces", "DataObject", AppSymbolCategory.List),
        AppSymbolSpec("chat_bubble", "bubble.left", "ChatBubble", AppSymbolCategory.List),
        AppSymbolSpec("priority_high", "exclamationmark", "PriorityHigh", AppSymbolCategory.List),
        AppSymbolSpec("star", "star", "Star", AppSymbolCategory.List),
        AppSymbolSpec("favorite", "heart", "Favorite", AppSymbolCategory.List),
        AppSymbolSpec("crop_square", "square", "CropSquare", AppSymbolCategory.List),
        AppSymbolSpec("circle", "circle", "Circle", AppSymbolCategory.List),
        AppSymbolSpec("change_history", "triangle", "ChangeHistory", AppSymbolCategory.List),
        AppSymbolSpec("diamond", "diamond", "Diamond", AppSymbolCategory.List),
        AppSymbolSpec("task_alt", "checkmark.circle", "TaskAlt", AppSymbolCategory.List),
        AppSymbolSpec("notes", "note.text", "Notes", AppSymbolCategory.List),
        AppSymbolSpec("today", "calendar", "Today", AppSymbolCategory.List),
        AppSymbolSpec("schedule", "clock", "Schedule", AppSymbolCategory.List),
        AppSymbolSpec("shopping_cart", "cart", "ShoppingCart", AppSymbolCategory.List),
        AppSymbolSpec("flight", "airplane", "Flight", AppSymbolCategory.List),
        AppSymbolSpec("music_note", "music.note", "MusicNote", AppSymbolCategory.List),

        // Additional Common Task & App Icons
        AppSymbolSpec("calendar", "calendar", "Today", AppSymbolCategory.Task),
        AppSymbolSpec("clock", "clock", "Schedule", AppSymbolCategory.Task),
        AppSymbolSpec("timer", "timer", "Schedule", AppSymbolCategory.Task),
        AppSymbolSpec("check", "checkmark", "TaskAlt", AppSymbolCategory.Task),
        AppSymbolSpec("delete", "trash", "Delete", AppSymbolCategory.Action),
        AppSymbolSpec("trash", "trash", "Delete", AppSymbolCategory.Action),
        AppSymbolSpec("edit", "pencil", "EditNote", AppSymbolCategory.Action),
        AppSymbolSpec("search", "magnifyingglass", "Code", AppSymbolCategory.Action),
        AppSymbolSpec("settings", "gear", "Build", AppSymbolCategory.Action),
        AppSymbolSpec("gear", "gear", "Build", AppSymbolCategory.Action),
        AppSymbolSpec("bell", "bell", "PriorityHigh", AppSymbolCategory.Status),
        AppSymbolSpec("lock", "lock", "Bookmark", AppSymbolCategory.Status),
        AppSymbolSpec("pin", "pin", "Flag", AppSymbolCategory.Status),
        AppSymbolSpec("tag", "tag", "LocalOffer", AppSymbolCategory.List),
        AppSymbolSpec("cloud", "cloud", "WbSunny", AppSymbolCategory.Nature),
        AppSymbolSpec("link", "link", "AllInclusive", AppSymbolCategory.General),
        AppSymbolSpec("email", "envelope", "Email", AppSymbolCategory.General),
        AppSymbolSpec("all_inclusive", "infinity", "AllInclusive", AppSymbolCategory.General)
    )

    private val normalizedKeyMap: Map<String, AppSymbolSpec> = buildMap {
        specsList.forEach { spec ->
            if (spec.materialIconName.isNotBlank()) {
                put(spec.materialIconName.lowercase(), spec)
            }
        }
        specsList.forEach { spec ->
            put(spec.key.lowercase(), spec)
        }
    }

    val allSymbols: List<AppSymbolSpec> get() = specsList

    val listIconKeys: List<String> = specsList
        .filter { it.category == AppSymbolCategory.List }
        .map { it.key }
        .distinct()

    fun findSpec(key: String): AppSymbolSpec? {
        val normalized = key.lowercase().trim()
        return normalizedKeyMap[normalized]
    }

    /**
     * Resolves the SF Symbol string for a symbol key on macOS / iOS.
     * Falls back to "info.circle".
     */
    fun sfSymbol(key: String): String = findSpec(key)?.sfSymbol ?: "info.circle"

    /**
     * Resolves the Compose Material [ImageVector] for a symbol key on Android.
     */
    fun materialIcon(key: String): ImageVector = when (key.lowercase().trim()) {
        "info" -> Icons.Outlined.Info
        "lightbulb" -> Icons.Outlined.OutlinedLightbulb
        "idea" -> Icons.Outlined.Psychology
        "sparkles" -> Icons.Outlined.AutoAwesome
        "summary" -> Icons.Outlined.StarOutline
        "important" -> Icons.Outlined.OutlinedPriorityHigh
        "warning" -> Icons.Outlined.Warning
        "caution" -> Icons.Outlined.Dangerous
        "question" -> Icons.AutoMirrored.Outlined.HelpOutline
        "quote" -> Icons.Outlined.FormatQuote
        else -> findSpec(key)?.let { resolveMaterialVector(it.materialIconName) } ?: Icons.Default.LocalOffer
    }

    private fun resolveMaterialVector(name: String): ImageVector = when (name) {
        "AccountBalance" -> Icons.Default.AccountBalance
        "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
        "AcUnit" -> Icons.Default.AcUnit
        "AllInclusive" -> Icons.Default.AllInclusive
        "Apartment" -> Icons.Default.Apartment
        "Architecture" -> Icons.Default.Architecture
        "AttachMoney" -> Icons.Default.AttachMoney
        "Backpack" -> Icons.Default.Backpack
        "BeachAccess" -> Icons.Default.BeachAccess
        "Bookmark" -> Icons.Default.Bookmark
        "Build" -> Icons.Default.Build
        "Cake" -> Icons.Default.Cake
        "CardGiftcard" -> Icons.Default.CardGiftcard
        "ChangeHistory" -> Icons.Default.ChangeHistory
        "ChatBubble" -> Icons.Default.ChatBubble
        "ChildCare" -> Icons.Default.ChildCare
        "Circle" -> Icons.Default.Circle
        "Code" -> Icons.Default.Code
        "Construction" -> Icons.Default.Construction
        "ContentCut" -> Icons.Default.ContentCut
        "CreditCard" -> Icons.Default.CreditCard
        "CropSquare" -> Icons.Default.CropSquare
        "DarkMode" -> Icons.Default.DarkMode
        "DataObject" -> Icons.Default.DataObject
        "Delete" -> Icons.Default.Delete
        "Description" -> Icons.Default.Description
        "Diamond" -> Icons.Default.Diamond
        "DirectionsBoat" -> Icons.Default.DirectionsBoat
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "DirectionsRun" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Diversity3" -> Icons.Default.Diversity3
        "DesktopWindows" -> Icons.Default.DesktopWindows
        "Eco" -> Icons.Default.Eco
        "EditNote" -> Icons.Default.EditNote
        "Email" -> Icons.Default.Email
        "FamilyRestroom" -> Icons.Default.FamilyRestroom
        "Favorite" -> Icons.Default.Favorite
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Flag" -> Icons.Default.Flag
        "Flight" -> Icons.Default.Flight
        "Folder" -> Icons.Default.Folder
        "FormatListBulleted" -> Icons.AutoMirrored.Filled.FormatListBulleted
        "Headphones" -> Icons.Default.Headphones
        "Home" -> Icons.Default.Home
        "Inbox" -> Icons.Default.Inbox
        "Inventory2" -> Icons.Default.Inventory2
        "Landscape" -> Icons.Default.Landscape
        "Lightbulb" -> Icons.Default.Lightbulb
        "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
        "LocalMall" -> Icons.Default.LocalMall
        "LocalOffer" -> Icons.Default.LocalOffer
        "Medication" -> Icons.Default.Medication
        "MenuBook" -> Icons.AutoMirrored.Filled.MenuBook
        "MusicNote" -> Icons.Default.MusicNote
        "Notes" -> Icons.AutoMirrored.Filled.Notes
        "Person" -> Icons.Default.Person
        "Pets" -> Icons.Default.Pets
        "PriorityHigh" -> Icons.Default.PriorityHigh
        "Restaurant" -> Icons.Default.Restaurant
        "Schedule" -> Icons.Default.Schedule
        "School" -> Icons.Default.School
        "SetMeal" -> Icons.Default.SetMeal
        "ShoppingBasket" -> Icons.Default.ShoppingBasket
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "SportsBaseball" -> Icons.Default.SportsBaseball
        "SportsBasketball" -> Icons.Default.SportsBasketball
        "SportsEsports" -> Icons.Default.SportsEsports
        "SportsFootball" -> Icons.Default.SportsFootball
        "SportsSoccer" -> Icons.Default.SportsSoccer
        "SportsTennis" -> Icons.Default.SportsTennis
        "Star" -> Icons.Default.Star
        "TaskAlt" -> Icons.Default.TaskAlt
        "Today" -> Icons.Default.Today
        "Train" -> Icons.Default.Train
        "ViewAgenda" -> Icons.Default.ViewAgenda
        "ViewList" -> Icons.AutoMirrored.Filled.ViewList
        "WaterDrop" -> Icons.Default.WaterDrop
        "WbSunny" -> Icons.Default.WbSunny
        "WineBar" -> Icons.Default.WineBar
        "Work" -> Icons.Default.Work
        "Info" -> Icons.Outlined.Info
        "Psychology" -> Icons.Outlined.Psychology
        "AutoAwesome" -> Icons.Outlined.AutoAwesome
        "StarOutline" -> Icons.Outlined.StarOutline
        "Warning" -> Icons.Outlined.Warning
        "Dangerous" -> Icons.Outlined.Dangerous
        "HelpOutline" -> Icons.AutoMirrored.Outlined.HelpOutline
        "FormatQuote" -> Icons.Outlined.FormatQuote
        else -> Icons.Default.LocalOffer
    }
}
