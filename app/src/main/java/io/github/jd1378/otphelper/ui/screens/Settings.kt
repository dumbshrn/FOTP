package io.github.jd1378.otphelper.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.jd1378.otphelper.gmail.GmailAccountStore
import io.github.jd1378.otphelper.gmail.GmailFetchActivity
import io.github.jd1378.otphelper.gmail.GmailFetchWorker
import io.github.jd1378.otphelper.ModeOfOperation
import io.github.jd1378.otphelper.R
import io.github.jd1378.otphelper.repository.UserSettingsRepositoryMock
import io.github.jd1378.otphelper.ui.components.CodeBlock
import io.github.jd1378.otphelper.ui.components.SettingHelp
import io.github.jd1378.otphelper.ui.components.SettingLabel
import io.github.jd1378.otphelper.ui.components.TitleBar
import io.github.jd1378.otphelper.ui.navigation.MainDestinations
import io.github.jd1378.otphelper.ui.theme.OtpHelperTheme
import io.github.jd1378.otphelper.ui.theme.AppColorPalette
import io.github.jd1378.otphelper.ui.theme.AppThemeMode
import io.github.jd1378.otphelper.ui.components.SettingGroup
import io.github.jd1378.otphelper.ui.utils.getCurrentLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun Settings(
    onNavigateToRoute: (String, Boolean, Boolean) -> Unit,
    upPress: () -> Unit,
    viewModel: SettingsViewModel,
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()
  val scrollState = rememberScrollState()
  val focusManager = LocalFocusManager.current
  var isInitialLoad by remember { mutableStateOf(true) }
  var targetPackageName by remember { mutableStateOf("") }
  var connectedGmails by remember { mutableStateOf(emptyList<String>()) }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    connectedGmails = GmailAccountStore(context).getAccounts()
  }

  LaunchedEffect(Unit) {
    scope.launch {
      targetPackageName = viewModel.getBroadcastTargetPackageName()
      isInitialLoad = false
    }
  }

  LaunchedEffect(targetPackageName) {
    if (isInitialLoad) return@LaunchedEffect
    // effectively debounce
    delay(100)
    viewModel.setBroadcastTargetPackageName(targetPackageName)
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = { TitleBar(upPress = upPress, text = stringResource(R.string.settings)) },
  ) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .padding(horizontal = dimensionResource(R.dimen.padding_page))
                .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_sections)),
    ) {
      SettingGroup(title = stringResource(R.string.setup_and_access)) {
        SettingsNavigationRow(
            title = stringResource(R.string.MODE_ROUTE),
            subtitle = stringResource(R.string.settings_mode_help),
            onClick = { onNavigateToRoute(MainDestinations.MODE_ROUTE, false, true) },
        )
        HorizontalDivider()
        SettingsNavigationRow(
            title = stringResource(R.string.PERMISSION_ROUTE),
            subtitle = stringResource(R.string.settings_permissions_help),
            onClick = { onNavigateToRoute(MainDestinations.PERMISSIONS_ROUTE, false, true) },
        )
      }

      SettingGroup(title = stringResource(R.string.appearance)) {
        Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          AppThemeMode.entries.forEach { themeMode ->
            val label =
                when (themeMode) {
                  AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                  AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                  AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                  AppThemeMode.AMOLED -> stringResource(R.string.theme_amoled)
                }
            FilterChip(
                selected = userSettings.themeMode == themeMode.storedValue,
                onClick = { viewModel.setThemeMode(themeMode) },
                label = { Text(label) },
            )
          }
        }
        Text(stringResource(R.string.custom_colors), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          AppColorPalette.entries.forEach { palette ->
            val label =
                when (palette) {
                  AppColorPalette.MATERIAL_YOU -> stringResource(R.string.palette_material_you)
                  AppColorPalette.BLUE -> stringResource(R.string.palette_blue)
                  AppColorPalette.VIOLET -> stringResource(R.string.palette_violet)
                  AppColorPalette.GREEN -> stringResource(R.string.palette_green)
                  AppColorPalette.ROSE -> stringResource(R.string.palette_rose)
                }
            FilterChip(
                selected = userSettings.colorPalette == palette.storedValue,
                onClick = { viewModel.setColorPalette(palette) },
                label = { Text(label) },
            )
          }
        }
        Text(
            text =
                stringResource(
                    if (userSettings.colorPalette == AppColorPalette.MATERIAL_YOU.storedValue)
                        R.string.palette_material_you_desc
                    else R.string.custom_colors_desc
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      SettingGroup(title = stringResource(R.string.gmail_otp_title)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_settings)),
        ) {
          Text(stringResource(R.string.gmail_connected_accounts))
          if (connectedGmails.isEmpty()) {
            Text(stringResource(R.string.gmail_no_connected_accounts))
          }
          connectedGmails.forEachIndexed { index, emailAddress ->
            if (index > 0) HorizontalDivider()
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Email, contentDescription = null) },
                headlineContent = { Text(emailAddress) },
                supportingContent = { Text(stringResource(R.string.gmail_account_connected)) },
                trailingContent = {
                  androidx.compose.material3.TextButton(
                      onClick = {
                        context.startActivity(
                            Intent(context, GmailFetchActivity::class.java)
                                .setAction(GmailFetchActivity.ACTION_DISCONNECT)
                                .putExtra(GmailFetchActivity.EXTRA_EMAIL_ADDRESS, emailAddress)
                        )
                      },
                  ) {
                    Text(stringResource(R.string.gmail_disconnect))
                  }
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
            )
          }
          androidx.compose.material3.OutlinedButton(
              modifier = Modifier.fillMaxWidth(),
              onClick = {
                context.startActivity(
                    Intent(context, GmailFetchActivity::class.java)
                        .setAction(GmailFetchActivity.ACTION_CONNECT)
                )
              },
          ) {
            Text(stringResource(R.string.gmail_add_account))
          }
          Button(
              modifier = Modifier.fillMaxWidth(),
              onClick = { GmailFetchWorker.enqueue(context) },
          ) {
            Text(stringResource(R.string.gmail_fetch_otp))
          }
          SettingHelp(stringResource(R.string.gmail_fetch_help))
        }
      }

      SettingGroup(title = stringResource(R.string.content_and_detection)) {
        SettingsNavigationRow(
            title = stringResource(R.string.language),
            subtitle = getCurrentLocale()?.displayLanguage ?: stringResource(R.string.error),
            onClick = { onNavigateToRoute(MainDestinations.LANGUAGE_SELECTION_ROUTE, false, true) },
        )
        HorizontalDivider()
        SettingsNavigationRow(
            title = stringResource(R.string.sensitive_phrases),
            onClick = { onNavigateToRoute(MainDestinations.SENSITIVE_PHRASES_ROUTE, false, true) },
        )
        HorizontalDivider()
        SettingsNavigationRow(
            title = stringResource(R.string.ignored_phrases),
            onClick = { onNavigateToRoute(MainDestinations.IGNORED_PHRASES_ROUTE, false, true) },
        )
        HorizontalDivider()
        SettingsNavigationRow(
            title = stringResource(R.string.cleanup_phrases),
            onClick = { onNavigateToRoute(MainDestinations.CLEANUP_PHRASES_ROUTE, false, true) },
        )
        HorizontalDivider()
        SettingsNavigationRow(
            title = stringResource(R.string.detection_test),
            onClick = { onNavigateToRoute(MainDestinations.DETECTION_TEST_ROUTE, false, true) },
        )
      }

      SettingGroup(title = stringResource(R.string.notifications)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_settings)),
            modifier =
                Modifier.padding(
                    horizontal = dimensionResource(R.dimen.padding_li_h),
                    vertical = dimensionResource(R.dimen.padding_li_v),
                ),
        ) {
          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.auto_copy))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.isAutoCopyEnabled,
                  onCheckedChange = { viewModel.onAutoCopyToggle() },
              )
            }

            SettingHelp(stringResource(R.string.auto_copy_help))
          }

          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.send_detected_notif))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.isPostNotifEnabled,
                  onCheckedChange = { viewModel.onPostNotifToggle() },
              )
            }
            SettingHelp(stringResource(R.string.send_detected_notif_help))
          }

          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.show_copy_confirmation))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.isShowCopyConfirmationEnabled,
                  onCheckedChange = { viewModel.onShowCopyConfirmationToggle() },
              )
            }
            SettingHelp(
                stringResource(
                    R.string.show_confirmation_help,
                    stringResource(R.string.code_copied_to_clipboard),
                )
            )
          }

          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.show_toast))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.isShowToastEnabled,
                  onCheckedChange = { viewModel.onShowToastToggle() },
              )
            }
            SettingHelp(stringResource(R.string.show_toast_help))
          }

          if (userSettings.modeOfOperation == ModeOfOperation.Notification) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                elevation =
                    ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 1.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp,
                    ),
                onClick = { viewModel.onSendTestNotifPressed(context) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            ) {
              Text(stringResource(R.string.send_test_notification), fontWeight = FontWeight.Medium)
            }
          }
        }
      }

      SettingGroup(title = stringResource(R.string.history)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_settings)),
            modifier =
                Modifier.padding(
                    horizontal = dimensionResource(R.dimen.padding_li_h),
                    vertical = dimensionResource(R.dimen.padding_li_v),
                ),
        ) {
          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.history))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = !userSettings.isHistoryDisabled,
                  onCheckedChange = { viewModel.onHistoryToggle(context) },
              )
            }
            SettingHelp(stringResource(R.string.history_help))
          }

          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.replace_code_in_history))
              Switch(
                  enabled = !userSettings.isHistoryDisabled,
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.shouldReplaceCodeInHistory,
                  onCheckedChange = { viewModel.onShouldReplaceCodeInHistoryToggle() },
              )
            }

            SettingHelp(stringResource(R.string.replace_code_in_history_help))
          }
        }
      }

      SettingGroup(title = stringResource(R.string.advanced_settings)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_settings)),
            modifier =
                Modifier.padding(
                    horizontal = dimensionResource(R.dimen.padding_li_h),
                    vertical = dimensionResource(R.dimen.padding_li_v),
                ),
        ) {
          SettingHelp(stringResource(R.string.experimental_features_warning))
          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.auto_dismiss))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.isAutoDismissEnabled,
                  onCheckedChange = { viewModel.onAutoDismissToggle() },
              )
            }
            SettingHelp(stringResource(R.string.auto_dismiss_desc))
          }

          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.auto_mark_as_read))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.isAutoMarkAsReadEnabled,
                  onCheckedChange = { viewModel.onAutoMarkAsReadToggle() },
              )
            }

            SettingHelp(stringResource(R.string.auto_mark_as_read_help))
          }
        }
      }

      SettingGroup(title = stringResource(R.string.privacy)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_settings)),
            modifier =
                Modifier.padding(
                    horizontal = dimensionResource(R.dimen.padding_li_h),
                    vertical = dimensionResource(R.dimen.padding_li_v),
                ),
        ) {
          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.copy_as_not_sensitive))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.isCopyAsNotSensitiveEnabled,
                  onCheckedChange = { viewModel.onIsCopyAsNotSensitiveToggle() },
              )
            }
            SettingHelp(stringResource(R.string.copy_as_not_sensitive_desc))
          }
        }
      }

      SettingGroup(title = stringResource(R.string.integrations)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_settings)),
            modifier =
                Modifier.padding(
                    horizontal = dimensionResource(R.dimen.padding_li_h),
                    vertical = dimensionResource(R.dimen.padding_li_v),
                ),
        ) {
          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              SettingLabel(stringResource(R.string.broadcast_code))
              Switch(
                  modifier = Modifier.height(32.dp),
                  checked = userSettings.isBroadcastCodeEnabled,
                  onCheckedChange = { viewModel.onBroadcastCodeToggle() },
              )
            }
            SettingHelp(stringResource(R.string.broadcast_code_help))
          }

          TextField(
              modifier = Modifier.fillMaxWidth(),
              label = { Text(stringResource(R.string.target_package_name)) },
              placeholder = { Text("e.g. com.llamalab.automate") },
              value = targetPackageName,
              onValueChange = { targetPackageName = it },
              supportingText = { SettingHelp(stringResource(R.string.broadcast_target_help)) },
              keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
          )

          Column(
              modifier = Modifier.semantics(mergeDescendants = true) {},
              verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
          ) {
            Text(stringResource(R.string.intent_action))
            CodeBlock(stringResource(R.string.intent_action_code_detected))
            SettingHelp(stringResource(R.string.intent_action_code_detected_help))
          }
        }
      }

      Surface(
          color = MaterialTheme.colorScheme.surfaceContainer,
          shape = MaterialTheme.shapes.large,
          onClick = { onNavigateToRoute(MainDestinations.ABOUT_ROUTE, false, true) },
          modifier = Modifier.semantics(mergeDescendants = true) {},
      ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
              modifier =
                  Modifier.padding(
                      horizontal = dimensionResource(R.dimen.padding_settings),
                      vertical = dimensionResource(R.dimen.padding_medium),
                  ),
              horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_xs)),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.about),
            )
            Text(stringResource(R.string.about))
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
  ListItem(
      modifier = Modifier.clickable(onClick = onClick),
      leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
      headlineContent = { Text(title) },
      supportingContent = { subtitle?.let { Text(it) } },
      trailingContent = { Icon(painterResource(R.drawable.baseline_navigate_next_24), null) },
      colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
  )
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun SettingsPreview() {
  OtpHelperTheme {
    Settings(
        onNavigateToRoute = { _, _, _ -> {} },
        upPress = {},
        viewModel = SettingsViewModel(SavedStateHandle(), UserSettingsRepositoryMock()),
    )
  }
}
