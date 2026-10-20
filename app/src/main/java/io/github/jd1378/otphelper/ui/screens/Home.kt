package io.github.jd1378.otphelper.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jd1378.otphelper.R
import io.github.jd1378.otphelper.data.local.entity.DetectedCode
import io.github.jd1378.otphelper.ui.components.SettingGroup
import io.github.jd1378.otphelper.ui.navigation.MainDestinations
import io.github.jd1378.otphelper.utils.Clipboard
import io.github.jd1378.otphelper.utils.CodeExtractor
import java.util.Date

@Composable
fun Home(
    onNavigateToRoute: (String, Boolean, Boolean) -> Unit,
    modifier: Modifier,
    viewModel: HomeViewModel,
) {
  val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
  val latestCodes by viewModel.latestCodes.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val codeExtractor =
      remember(userSettings.sensitivePhrasesList, userSettings.ignoredPhrasesList, userSettings.cleanupPhrasesList) {
        CodeExtractor(
            userSettings.sensitivePhrasesList,
            userSettings.ignoredPhrasesList,
            userSettings.cleanupPhrasesList,
        )
      }

  androidx.compose.runtime.LaunchedEffect(userSettings) {
    if (!userSettings.isSetupFinished) {
      onNavigateToRoute(MainDestinations.MODE_ROUTE + "?setup=true", true, true)
    }
  }

  LazyColumn(
      modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.codes),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.codes_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      val latest = latestCodes.firstOrNull()
      val latestCode =
          if (latest == null || userSettings.shouldReplaceCodeInHistory) null
          else codeExtractor.getCode(latest.text)
      LatestCodeCard(
          detectedCode = latest,
          code = latestCode,
          onCopy = {
            latestCode?.let {
              Clipboard.copyCodeToClipboard(
                  context,
                  it,
                  userSettings.isShowCopyConfirmationEnabled,
                  !userSettings.isCopyAsNotSensitiveEnabled,
              )
            }
          },
      )
    }

    item {
      Button(
          onClick = { viewModel.fetchGmail(context) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
      ) {
        Icon(Icons.Outlined.Email, contentDescription = null)
        Text(stringResource(R.string.gmail_fetch_otp), modifier = Modifier.padding(start = 8.dp))
      }
    }

    item {
      Text(
          text = stringResource(R.string.recent_codes),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
      )
    }

    if (latestCodes.isEmpty()) {
      item {
        SettingGroup(title = stringResource(R.string.no_codes_yet)) {
          Text(
              text =
                  stringResource(
                      if (userSettings.isHistoryDisabled) R.string.codes_history_disabled
                      else R.string.codes_empty_help
                  ),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedButton(
              onClick = { onNavigateToRoute(MainDestinations.PERMISSIONS_ROUTE, false, true) },
          ) {
            Text(stringResource(R.string.PERMISSION_ROUTE))
          }
        }
      }
    } else {
      items(latestCodes.drop(1), key = { it.id }) { detected ->
        RecentCodeRow(
            detectedCode = detected,
            code = if (userSettings.shouldReplaceCodeInHistory) null else codeExtractor.getCode(detected.text),
        )
      }
    }

    item {
      OutlinedButton(
          onClick = { onNavigateToRoute(MainDestinations.HISTORY_ROUTE, false, true) },
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.view_history))
      }
    }
  }
}

@Composable
private fun LatestCodeCard(
    detectedCode: DetectedCode?,
    code: String?,
    onCopy: () -> Unit,
) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(32.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
  ) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
          text = stringResource(R.string.latest_code),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Text(
          text =
              code
                  ?: stringResource(
                      if (detectedCode == null) R.string.no_code_available
                      else R.string.code_hidden_for_privacy
                  ),
          style = MaterialTheme.typography.displaySmall,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Text(
          text = detectedCode?.let { sourceLabel(it) + " · " + relativeTime(it.createdAt) }
              ?: stringResource(R.string.waiting_for_codes),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      OutlinedButton(onClick = onCopy, enabled = code != null) {
        Icon(painterResource(R.drawable.baseline_content_copy_24), contentDescription = null)
        Text(stringResource(R.string.copy), modifier = Modifier.padding(start = 8.dp))
      }
    }
  }
}

@Composable
private fun RecentCodeRow(detectedCode: DetectedCode, code: String?) {
  Surface(
      color = MaterialTheme.colorScheme.surfaceContainer,
      shape = RoundedCornerShape(24.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = code ?: stringResource(R.string.code_hidden_for_privacy),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = sourceLabel(detectedCode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
          text = relativeTime(detectedCode.createdAt),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun sourceLabel(detectedCode: DetectedCode): String =
    when {
      detectedCode.notificationTag == "gmail" -> "Gmail"
      detectedCode.smsOrigin.isNotBlank() -> detectedCode.smsOrigin
      else -> detectedCode.packageName
    }

private fun relativeTime(date: Date): String =
    DateUtils.getRelativeTimeSpanString(date.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        .toString()
