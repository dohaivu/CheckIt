package com.checkit.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Cloud
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dangerous
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb as OutlinedLightbulb
import androidx.compose.material.icons.outlined.PriorityHigh as OutlinedPriorityHigh
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/** Places where a symbol may be offered. A symbol can support more than one. */
enum class AppSymbolUsage {
    ListIcon,
    Callout
}

/**
 * Definition of a cross-platform symbol mapping.
 *
 * [key] is the stable, lowercase semantic identifier persisted by the app.
 * [usages] decides which pickers offer the symbol; it is not an exclusive
 * category, so a symbol can be both a list icon and a callout icon.
 */
data class AppSymbolSpec(
    val key: String,
    val sfSymbol: String,
    val materialIconName: String,
    val usages: Set<AppSymbolUsage> = setOf(AppSymbolUsage.Callout)
)

/**
 * Central registry object for application symbols across Android (Compose Material Icons)
 * and macOS (SF Symbols). All keys are normalized to lowercase.
 */
object AppSymbols {

    /** Callout-only symbol. */
    private fun callout(key: String, sfSymbol: String, materialIconName: String): AppSymbolSpec =
        AppSymbolSpec(key, sfSymbol, materialIconName)

    /** Symbol offered by both the list icon and callout pickers. */
    private fun listIcon(key: String, sfSymbol: String, materialIconName: String): AppSymbolSpec =
        AppSymbolSpec(
            key = key,
            sfSymbol = sfSymbol,
            materialIconName = materialIconName,
            usages = setOf(AppSymbolUsage.ListIcon, AppSymbolUsage.Callout)
        )

    private val specsList: List<AppSymbolSpec> = listOf(
        // Callout symbols
        callout("info", "info.circle", "OutlinedInfo"),
        callout("lightbulb", "lightbulb", "OutlinedLightbulb"),
        callout("idea", "brain.head.profile", "OutlinedPsychology"),
        callout("sparkles", "sparkles", "OutlinedAutoAwesome"),
        callout("summary", "list.bullet.rectangle", "Summarize"),
        callout("important", "exclamationmark.circle", "OutlinedPriorityHigh"),
        callout("warning", "exclamationmark.triangle", "OutlinedWarning"),
        callout("caution", "exclamationmark.octagon", "OutlinedDangerous"),
        callout("question", "questionmark.circle", "OutlinedHelpOutline"),
        callout("quote", "quote.opening", "OutlinedFormatQuote"),
        callout("success", "checkmark.seal", "Verified"),
        callout("shield", "checkmark.shield", "VerifiedUser"),
        callout("target", "target", "GpsFixed"),
        callout("trophy", "trophy", "EmojiEvents"),
        callout("key", "key", "Key"),
        callout("paperclip", "paperclip", "AttachFile"),
        callout("image", "photo", "Image"),
        callout("file", "doc", "InsertDriveFile"),
        callout("code", "chevron.left.forwardslash.chevron.right", "Code"),
        callout("formula", "function", "Functions"),
        callout("calculate", "plus.forwardslash.minus", "Calculate"),
        callout("chart", "chart.bar", "BarChart"),
        callout("trending", "chart.line.uptrend.xyaxis", "TrendingUp"),
        callout("bolt", "bolt", "Bolt"),
        callout("thumb_up", "hand.thumbsup", "ThumbUp"),
        callout("thumb_down", "hand.thumbsdown", "ThumbDown"),
        callout("announcement", "megaphone", "Campaign"),
        callout("science", "flask", "Science"),
        callout("place", "mappin.and.ellipse", "Place"),
        callout("map", "map", "Map"),
        callout("language", "globe", "Language"),
        callout("translate", "character.bubble", "Translate"),
        callout("bug", "ladybug", "BugReport"),
        callout("database", "cylinder.split.1x2", "Storage"),
        callout("upload", "arrow.up.circle", "FileUpload"),
        callout("download", "arrow.down.circle", "FileDownload"),
        callout("save", "square.and.arrow.down", "Save"),
        callout("sync", "arrow.triangle.2.circlepath", "Sync"),
        callout("add", "plus.circle", "AddCircle"),
        callout("remove", "minus.circle", "RemoveCircle"),
        callout("close", "xmark.circle", "Cancel"),
        callout("stop", "hand.raised", "PanTool"),
        callout("checklist", "checklist", "Checklist"),

        // List icons (also available as callouts)
        listIcon("inbox", "tray", "Inbox"),
        listIcon("list", "list.bullet", "FormatListBulleted"),
        listIcon("bookmark", "bookmark", "Bookmark"),
        listIcon("tag", "tag", "LocalOffer"),
        listIcon("gift", "gift", "CardGiftcard"),
        listIcon("cake", "birthday.cake", "Cake"),
        listIcon("school", "graduationcap", "School"),
        listIcon("backpack", "backpack", "Backpack"),
        listIcon("edit_note", "square.and.pencil", "EditNote"),
        listIcon("document", "doc.text", "Description"),
        listIcon("book", "book", "MenuBook"),
        listIcon("credit_card", "creditcard", "CreditCard"),
        listIcon("wallet", "wallet.pass", "AccountBalanceWallet"),
        listIcon("money", "dollarsign", "AttachMoney"),
        listIcon("fitness", "figure.strengthtraining.traditional", "FitnessCenter"),
        listIcon("run", "figure.run", "DirectionsRun"),
        listIcon("restaurant", "fork.knife", "Restaurant"),
        listIcon("wine", "wineglass", "WineBar"),
        listIcon("medication", "cross.vial", "Medication"),
        listIcon("home", "house", "Home"),
        listIcon("apartment", "building.2", "Apartment"),
        listIcon("bank", "building.columns", "AccountBalance"),
        listIcon("landscape", "mountain.2", "Landscape"),
        listIcon("desktop", "desktopcomputer", "DesktopWindows"),
        listIcon("gaming", "gamecontroller", "SportsEsports"),
        listIcon("headphones", "headphones", "Headphones"),
        listIcon("eco", "leaf", "Eco"),
        listIcon("person", "person", "Person"),
        listIcon("people", "person.3", "Diversity3"),
        listIcon("family", "figure.2.and.child.holdinghands", "FamilyRestroom"),
        listIcon("pets", "pawprint", "Pets"),
        listIcon("childcare", "stroller", "ChildCare"),
        listIcon("fish", "fish", "SetMeal"),
        listIcon("basket", "basket", "ShoppingBasket"),
        listIcon("work", "briefcase", "Work"),
        listIcon("shopping_bag", "bag", "LocalMall"),
        listIcon("archive", "archivebox", "Inventory2"),
        listIcon("soccer", "soccerball", "SportsSoccer"),
        listIcon("baseball", "baseball", "SportsBaseball"),
        listIcon("basketball", "basketball", "SportsBasketball"),
        listIcon("football", "football", "SportsFootball"),
        listIcon("tennis", "tennis.racket", "SportsTennis"),
        listIcon("train", "train.side.front.car", "Train"),
        listIcon("folder", "folder", "Folder"),
        listIcon("ferry", "ferry", "DirectionsBoat"),
        listIcon("car", "car", "DirectionsCar"),
        listIcon("beach", "umbrella", "BeachAccess"),
        listIcon("sun", "sun.max", "WbSunny"),
        listIcon("moon", "moon", "DarkMode"),
        listIcon("water", "drop", "WaterDrop"),
        listIcon("snow", "snowflake", "AcUnit"),
        listIcon("fire", "flame", "LocalFireDepartment"),
        listIcon("tools", "wrench.and.screwdriver", "Construction"),
        listIcon("hammer", "hammer", "Build"),
        listIcon("scissors", "scissors", "ContentCut"),
        listIcon("ruler", "ruler", "Architecture"),
        listIcon("braces", "curlybraces", "DataObject"),
        listIcon("chat", "bubble.left", "ChatBubble"),
        listIcon("priority", "exclamationmark", "PriorityHigh"),
        listIcon("star", "star", "Star"),
        listIcon("heart", "heart", "Favorite"),
        listIcon("square", "square", "CropSquare"),
        listIcon("circle", "circle", "Circle"),
        listIcon("triangle", "triangle", "ChangeHistory"),
        listIcon("diamond", "diamond", "Diamond"),
        listIcon("check_circle", "checkmark.circle", "TaskAlt"),
        listIcon("note", "note.text", "Notes"),
        listIcon("today", "calendar", "Today"),
        listIcon("schedule", "clock", "Schedule"),
        listIcon("cart", "cart", "ShoppingCart"),
        listIcon("flight", "airplane", "Flight"),
        listIcon("music", "music.note", "MusicNote"),

        // General app symbols
        callout("timer", "timer", "Timer"),
        callout("check", "checkmark", "Check"),
        callout("trash", "trash", "Delete"),
        callout("edit", "pencil", "Edit"),
        callout("search", "magnifyingglass", "Search"),
        callout("settings", "gear", "Settings"),
        callout("bell", "bell", "Notifications"),
        callout("lock", "lock", "Lock"),
        callout("pin", "pin", "PushPin"),
        callout("cloud", "cloud", "Cloud"),
        callout("link", "link", "Link"),
        callout("email", "envelope", "Email"),
        callout("infinity", "infinity", "AllInclusive")
    )

    private val specsByKey: Map<String, AppSymbolSpec> = specsList.associateBy { it.key }

    /** Every registered symbol, in picker order. */
    val allSymbols: List<AppSymbolSpec> = specsList

    /** Keys offered by the callout icon picker. */
    val calloutIconKeys: List<String> = specsList
        .filter { AppSymbolUsage.Callout in it.usages }
        .map { it.key }

    /** Keys offered by the list icon picker. */
    val listIconKeys: List<String> = specsList
        .filter { AppSymbolUsage.ListIcon in it.usages }
        .map { it.key }

    /** Resolves a symbol key (case-insensitively) to its spec. */
    fun findSpec(key: String): AppSymbolSpec? = specsByKey[key.lowercase().trim()]

    /** Resolves the SF Symbol string for a symbol key on macOS / iOS. */
    fun sfSymbol(key: String): String = findSpec(key)?.sfSymbol ?: "info.circle"

    /** Resolves the Compose Material [ImageVector] for a symbol key on Android. */
    fun materialIcon(key: String): ImageVector =
        findSpec(key)?.let { materialVectors[it.materialIconName] } ?: Icons.Default.LocalOffer

    private val materialVectors: Map<String, ImageVector> = mapOf(
        "AccountBalance" to Icons.Default.AccountBalance,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "AcUnit" to Icons.Default.AcUnit,
        "AddCircle" to Icons.Default.AddCircle,
        "AllInclusive" to Icons.Default.AllInclusive,
        "Apartment" to Icons.Default.Apartment,
        "Architecture" to Icons.Default.Architecture,
        "AttachFile" to Icons.Default.AttachFile,
        "AttachMoney" to Icons.Default.AttachMoney,
        "Backpack" to Icons.Default.Backpack,
        "BarChart" to Icons.Default.BarChart,
        "BeachAccess" to Icons.Default.BeachAccess,
        "Bolt" to Icons.Default.Bolt,
        "Bookmark" to Icons.Default.Bookmark,
        "BugReport" to Icons.Default.BugReport,
        "Build" to Icons.Default.Build,
        "Cake" to Icons.Default.Cake,
        "Calculate" to Icons.Default.Calculate,
        "Campaign" to Icons.Default.Campaign,
        "Cancel" to Icons.Default.Cancel,
        "CardGiftcard" to Icons.Default.CardGiftcard,
        "ChangeHistory" to Icons.Default.ChangeHistory,
        "ChatBubble" to Icons.Default.ChatBubble,
        "Check" to Icons.Default.Check,
        "CheckCircle" to Icons.Default.CheckCircle,
        "Checklist" to Icons.Default.Checklist,
        "ChildCare" to Icons.Default.ChildCare,
        "Circle" to Icons.Default.Circle,
        "Cloud" to Icons.Default.Cloud,
        "Code" to Icons.Default.Code,
        "Construction" to Icons.Default.Construction,
        "ContentCut" to Icons.Default.ContentCut,
        "CreditCard" to Icons.Default.CreditCard,
        "CropSquare" to Icons.Default.CropSquare,
        "DarkMode" to Icons.Default.DarkMode,
        "DataObject" to Icons.Default.DataObject,
        "Delete" to Icons.Default.Delete,
        "Description" to Icons.Default.Description,
        "DesktopWindows" to Icons.Default.DesktopWindows,
        "Diamond" to Icons.Default.Diamond,
        "DirectionsBoat" to Icons.Default.DirectionsBoat,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "DirectionsRun" to Icons.AutoMirrored.Filled.DirectionsRun,
        "Diversity3" to Icons.Default.Diversity3,
        "Eco" to Icons.Default.Eco,
        "Edit" to Icons.Default.Edit,
        "EditNote" to Icons.Default.EditNote,
        "Email" to Icons.Default.Email,
        "EmojiEvents" to Icons.Default.EmojiEvents,
        "FamilyRestroom" to Icons.Default.FamilyRestroom,
        "Favorite" to Icons.Default.Favorite,
        "FileDownload" to Icons.Default.FileDownload,
        "FileUpload" to Icons.Default.FileUpload,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "Flight" to Icons.Default.Flight,
        "Folder" to Icons.Default.Folder,
        "FormatListBulleted" to Icons.AutoMirrored.Filled.FormatListBulleted,
        "Functions" to Icons.Default.Functions,
        "GpsFixed" to Icons.Default.GpsFixed,
        "Headphones" to Icons.Default.Headphones,
        "Home" to Icons.Default.Home,
        "Image" to Icons.Default.Image,
        "Inbox" to Icons.Default.Inbox,
        "InsertDriveFile" to Icons.AutoMirrored.Filled.InsertDriveFile,
        "Inventory2" to Icons.Default.Inventory2,
        "Key" to Icons.Default.Key,
        "Landscape" to Icons.Default.Landscape,
        "Language" to Icons.Default.Language,
        "Lightbulb" to Icons.Default.Lightbulb,
        "Link" to Icons.Default.Link,
        "LocalFireDepartment" to Icons.Default.LocalFireDepartment,
        "LocalMall" to Icons.Default.LocalMall,
        "LocalOffer" to Icons.Default.LocalOffer,
        "Lock" to Icons.Default.Lock,
        "Map" to Icons.Default.Map,
        "Medication" to Icons.Default.Medication,
        "MenuBook" to Icons.AutoMirrored.Filled.MenuBook,
        "MusicNote" to Icons.Default.MusicNote,
        "Notes" to Icons.AutoMirrored.Filled.Notes,
        "Notifications" to Icons.Default.Notifications,
        "OutlinedAutoAwesome" to Icons.Outlined.AutoAwesome,
        "OutlinedDangerous" to Icons.Outlined.Dangerous,
        "OutlinedFormatQuote" to Icons.Outlined.FormatQuote,
        "OutlinedHelpOutline" to Icons.AutoMirrored.Outlined.HelpOutline,
        "OutlinedInfo" to Icons.Outlined.Info,
        "OutlinedLightbulb" to Icons.Outlined.OutlinedLightbulb,
        "OutlinedPriorityHigh" to Icons.Outlined.OutlinedPriorityHigh,
        "OutlinedPsychology" to Icons.Outlined.Psychology,
        "OutlinedWarning" to Icons.Outlined.Warning,
        "PanTool" to Icons.Default.PanTool,
        "Person" to Icons.Default.Person,
        "Pets" to Icons.Default.Pets,
        "Place" to Icons.Default.Place,
        "PriorityHigh" to Icons.Default.PriorityHigh,
        "PushPin" to Icons.Default.PushPin,
        "RemoveCircle" to Icons.Default.RemoveCircle,
        "Restaurant" to Icons.Default.Restaurant,
        "Save" to Icons.Default.Save,
        "Schedule" to Icons.Default.Schedule,
        "School" to Icons.Default.School,
        "Science" to Icons.Default.Science,
        "Search" to Icons.Default.Search,
        "SetMeal" to Icons.Default.SetMeal,
        "Settings" to Icons.Default.Settings,
        "ShoppingBasket" to Icons.Default.ShoppingBasket,
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "SportsBaseball" to Icons.Default.SportsBaseball,
        "SportsBasketball" to Icons.Default.SportsBasketball,
        "SportsEsports" to Icons.Default.SportsEsports,
        "SportsFootball" to Icons.Default.SportsFootball,
        "SportsSoccer" to Icons.Default.SportsSoccer,
        "SportsTennis" to Icons.Default.SportsTennis,
        "Star" to Icons.Default.Star,
        "Storage" to Icons.Default.Storage,
        "Summarize" to Icons.Default.Summarize,
        "Sync" to Icons.Default.Sync,
        "TaskAlt" to Icons.Default.TaskAlt,
        "ThumbDown" to Icons.Default.ThumbDown,
        "ThumbUp" to Icons.Default.ThumbUp,
        "Timer" to Icons.Default.Timer,
        "Today" to Icons.Default.Today,
        "Train" to Icons.Default.Train,
        "Translate" to Icons.Default.Translate,
        "TrendingUp" to Icons.AutoMirrored.Filled.TrendingUp,
        "Verified" to Icons.Default.Verified,
        "VerifiedUser" to Icons.Default.VerifiedUser,
        "WaterDrop" to Icons.Default.WaterDrop,
        "WbSunny" to Icons.Default.WbSunny,
        "WineBar" to Icons.Default.WineBar,
        "Work" to Icons.Default.Work
    )

    /** Material icon names that have an explicit Compose vector mapping. */
    internal val mappedMaterialIconNames: Set<String> = materialVectors.keys
}
