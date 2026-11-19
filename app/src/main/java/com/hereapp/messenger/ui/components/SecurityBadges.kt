package com.hereapp.messenger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereapp.messenger.core.security.BadgeType
import com.hereapp.messenger.core.security.UserBadge
import com.hereapp.messenger.ui.theme.ChampagneGold
import com.hereapp.messenger.ui.theme.MutedBlue
import com.hereapp.messenger.ui.theme.SterlingSilver

val VerifiedBlueTickColor = Color(0xFF1D9BF0)

@Composable
fun VerifiedBlueTick(
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified Account",
        tint = VerifiedBlueTickColor,
        modifier = modifier.size(size)
    )
}

@Composable
fun FounderOwnerBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = ChampagneGold.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = "Founder & Owner",
                    tint = ChampagneGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Founder & Owner",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChampagneGold
                )
            }
        }
    }
}

@Composable
fun HereBadgePill(
    badge: UserBadge,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (badge.type) {
        BadgeType.FOUNDER_OWNER -> Icons.Outlined.WorkspacePremium to ChampagneGold
        BadgeType.VERIFIED -> Icons.Filled.Verified to VerifiedBlueTickColor
        BadgeType.MODERATOR -> Icons.Filled.Shield to MutedBlue
        BadgeType.CUSTOM -> Icons.Filled.Star to SterlingSilver
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = badge.displayLabel,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = badge.displayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}
