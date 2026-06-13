package com.checkit.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AcUnit
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
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListBulleted
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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
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
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object AppIconColorDefaults {
    const val FallbackColorHex: String = "#64748B"
    val FallbackColor: Color = Color(0xFF64748B)
    val DailyPlanCardColor = FallbackColor

    val ListColors: List<String> = listOf(
        "#2563EB", // Blue
        "#7C3AED", // Violet
        "#C026D3", // Fuchsia
        "#DB2777", // Pink
        "#DC2626", // Red
        "#EA580C", // Orange
        "#CA8A04", // Amber
        "#65A30D", // Lime
        "#16A34A", // Green
        "#0D9488", // Teal
        "#0891B2", // Cyan
        "#7C2D12", // Brown
        "#64748B" // Slate
    )

    val ListIcons: List<String> = listOf(
        "Inbox",
        "FormatListBulleted",
        "Bookmark",
        "LocalOffer",
        "CardGiftcard",
        "Cake",
        "School",
        "Backpack",
        "EditNote",
        "Description",
        "MenuBook",
        "CreditCard",
        "AccountBalanceWallet",
        "AttachMoney",
        "FitnessCenter",
        "DirectionsRun",
        "Restaurant",
        "WineBar",
        "Medication",
        "Home",
        "Apartment",
        "AccountBalance",
        "Landscape",
        "DesktopWindows",
        "SportsEsports",
        "Headphones",
        "Eco",
        "Person",
        "Diversity3",
        "FamilyRestroom",
        "Pets",
        "ChildCare",
        "SetMeal",
        "ShoppingBasket",
        "Work",
        "LocalMall",
        "Inventory2",
        "SportsSoccer",
        "SportsBaseball",
        "SportsBasketball",
        "SportsFootball",
        "SportsTennis",
        "Train",
        "Folder",
        "DirectionsBoat",
        "DirectionsCar",
        "BeachAccess",
        "WbSunny",
        "DarkMode",
        "WaterDrop",
        "AcUnit",
        "LocalFireDepartment",
        "Construction",
        "Build",
        "ContentCut",
        "Architecture",
        "DataObject",
        "ChatBubble",
        "PriorityHigh",
        "Star",
        "Favorite",
        "CropSquare",
        "Circle",
        "ChangeHistory",
        "Diamond",
        "TaskAlt",
        "Notes",
        "Today",
        "Schedule",
        "ShoppingCart",
        "Flight",
        "MusicNote",
        "Lightbulb"
    )

    fun icon(name: String): ImageVector = when (name) {
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
        "DirectionsRun" -> Icons.Default.DirectionsRun
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
        "FormatListBulleted" -> Icons.Default.FormatListBulleted
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
        "MenuBook" -> Icons.Default.MenuBook
        "MusicNote" -> Icons.Default.MusicNote
        "Notes" -> Icons.Default.Notes
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
        "ViewList" -> Icons.Default.ViewList
        "WaterDrop" -> Icons.Default.WaterDrop
        "WbSunny" -> Icons.Default.WbSunny
        "WineBar" -> Icons.Default.WineBar
        "Work" -> Icons.Default.Work
        else -> Icons.Default.LocalOffer
    }

    fun colorFromHexOrNull(hex: String): Color? {
        val rgb = hex.removePrefix("#").toIntOrNull(16) ?: return null
        return Color(
            red = ((rgb shr 16) and 0xFF) / 255f,
            green = ((rgb shr 8) and 0xFF) / 255f,
            blue = (rgb and 0xFF) / 255f
        )
    }

    fun colorFromHex(hex: String): Color = colorFromHexOrNull(hex) ?: FallbackColor
}

fun materialIcon(name: String): ImageVector = AppIconColorDefaults.icon(name)

fun String.toColor(): Color = AppIconColorDefaults.colorFromHex(this)

fun String.parseHexColorOrNull(): Color? = AppIconColorDefaults.colorFromHexOrNull(this)
