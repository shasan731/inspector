package com.shahriarhasan.usedphoneinspector.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: Int, val body: Int, val icon: ImageVector)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(R.string.onboarding_title_one, R.string.onboarding_body_one, Icons.Default.Devices),
        OnboardingPage(R.string.onboarding_title_two, R.string.onboarding_body_two, Icons.Default.CheckCircle),
        OnboardingPage(R.string.onboarding_title_three, R.string.onboarding_body_three, Icons.Default.PrivacyTip),
        OnboardingPage(R.string.onboarding_title_four, R.string.onboarding_body_four, Icons.Default.VerifiedUser),
    )
    val pager = rememberPagerState(pageCount = pages::size)
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val item = pages[page]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(item.title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(stringResource(item.body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    if (page == 2) {
                        Text(
                            stringResource(R.string.privacy_summary),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                Text(
                    text = if (index == pager.currentPage) "●" else "○",
                    color = if (index == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        if (pager.currentPage == pages.lastIndex) {
            Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.get_started))
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onComplete) { Text(stringResource(R.string.skip)) }
                Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                    Text(stringResource(R.string.next))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

