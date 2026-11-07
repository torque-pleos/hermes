package com.zigae.hermes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zigae.hermes.ui.theme.Ivi
import com.zigae.hermes.ui.theme.IviSpace
import com.zigae.hermes.ui.theme.IviType

enum class Tab { BRIEFING, ROUTE, FLEET, DIAGNOSTICS }

/** 왼쪽 내비게이션 레일(운전석 쪽, 큰 아이콘+라벨) + 본문 */
@Composable
fun AppShell(tab: Tab, onTab: (Tab) -> Unit, fleetWet: Int, content: @Composable () -> Unit) {
    val c = Ivi.colors
    Row(Modifier.fillMaxSize().background(c.ivi_background_app)) {
        Column(
            Modifier.width(IviSpace.railWidth).fillMaxHeight().padding(vertical = IviSpace.l, horizontal = IviSpace.s),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(IviSpace.s),
        ) {
            // 앱 이름 (로고 대신 글자)
            Text("Hermes", style = IviType.title, color = c.ivi_accent_primary, modifier = Modifier.padding(vertical = IviSpace.s))
            Spacer(Modifier.height(IviSpace.s))
            RailItem(HermesIcons.Dashboard, "브리핑", tab == Tab.BRIEFING, { onTab(Tab.BRIEFING) })
            RailItem(HermesIcons.Route, "경로", tab == Tab.ROUTE, { onTab(Tab.ROUTE) })
            RailItem(HermesIcons.Groups, "차량 공유", tab == Tab.FLEET, { onTab(Tab.FLEET) }, badge = fleetWet.takeIf { it > 0 }?.toString())
            Spacer(Modifier.weight(1f))
            RailItem(HermesIcons.Tune, "진단", tab == Tab.DIAGNOSTICS, { onTab(Tab.DIAGNOSTICS) })
        }
        VDivider()
        Box(Modifier.weight(1f).fillMaxHeight()) { content() }
    }
}
