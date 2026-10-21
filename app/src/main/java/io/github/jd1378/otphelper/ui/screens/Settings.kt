package io.github.jd1378.otphelper.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
  var activeSection by remember { mutableStateOf<SettingsSection?>(null) }

  BackHandler(enabled = activeSection != null) { activeSection = null }

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

  LaunchedEffect(activeSection) { scrollState.scrollTo(0) }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        TitleBar(
            upPress = { if (activeSection == null) upPress() else activeSection = null },
            showBackBtn = activeSection != null,
            text =
                stringResource(
                    if (activeSection == SettingsSection.APPEARANCE) R.string.theme_and_colors
                    else activeSection?.titleRes ?: R.string.settings
                ),
        )
      },
  ) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .padding(horizontal = dimensionResource(R.dimen.padding_page))
                .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_sections)),
    ) {
      if (activeSection == null) {
        SettingGroup(title = stringResource(R.string.setup_and_access)) {
          SettingsNavigationRow(
              title = stringResource(R.string.MODE_ROUTE),
              subtitle = stringResource(R.string.settings_mode_help),
              iconRes = R.drawable.ic_tune_24,
              onClick = { onNavigateToRoute(MainDestinations.MODE_ROUTE, false, true) },
          )
          HorizontalDivider()
          SettingsNavigationRow(
              title = stringResource(R.string.PERMISSION_ROUTE),
              subtitle = stringResource(R.string.settings_permissions_help),
              iconRes = R.drawable.ic_security_24,
              onClick = { onNavigateToRoute(MainDestinations.PERMISSIONS_ROUTE, false, true) },
          )
        }

        SettingsSectionGroup(
            title = stringResource(R.string.settings_personalization),
            sections =
                SettingsSection.entries.filter {
                  it.category == SettingsCategory.PERSONALIZATION
                },
            onSectionSelected = { activeSection = it },
        )
        SettingsSectionGroup(
            title = stringResource(R.string.settings_app_behavior),
            sections =
                SettingsSection.entries.filter { it.category == SettingsCategory.APP_BEHAVIOR },
            onSectionSelected = { activeSection = it },
        )
      }

      if (activeSection == SettingsSection.APPEARANCE) {
      SettingGroup(title = stringResource(R.string.appearance)) {
        Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleSmall)
        ThemeModeSelector(
            currentMode = AppThemeMode.fromStoredValue(userSettings.themeMode),
            onModeSelected = viewModel::setThemeMode,
        )
        HorizontalDivider()
        Text(stringResource(R.string.custom_colors), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AppColorPalette.entries.filter { it != AppColorPalette.MATERIAL_YOU }.forEach { palette ->
            val label =
                when (palette) {
                  AppColorPalette.MATERIAL_YOU -> stringResource(R.string.palette_material_you)
                  AppColorPalette.BLUE -> stringResource(R.string.palette_blue)
                  AppColorPalette.VIOLET -> stringResource(R.string.palette_violet)
                  AppColorPalette.GREEN -> stringResource(R.string.palette_green)
                  AppColorPalette.ROSE -> stringResource(R.string.palette_rose)
                }
            ColorPaletteOption(
                palette = palette,
                label = label,
                selected = userSettings.colorPalette == palette.storedValue,
                onClick = { viewModel.setColorPalette(palette) },
            )
          }
        }
        HorizontalDivider()
        ThemeToggleRow(
            title = stringResource(R.string.pure_black_mode),
            checked = userSettings.themeMode == AppThemeMode.AMOLED.storedValue,
            onCheckedChange = { enabled ->
              viewModel.setThemeMode(if (enabled) AppThemeMode.AMOLED else AppThemeMode.DARK)
            },
            iconRes = R.drawable.ic_amoled_24,
        )
        HorizontalDivider()
        ThemeToggleRow(
            title = stringResource(R.string.enable_dynamic_theme),
            checked = userSettings.colorPalette == AppColorPalette.MATERIAL_YOU.storedValue,
            onCheckedChange = { enabled ->
              viewModel.setColorPalette(
                  if (enabled) AppColorPalette.MATERIAL_YOU else AppColorPalette.BLUE
              )
            },
            iconRes = R.drawable.ic_dynamic_color_24,
        )
      }
      }

      if (activeSection == SettingsSection.GMAIL) {
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
      }

      if (activeSection == SettingsSection.CONTENT) {
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
      }

      if (activeSection == SettingsSection.NOTIFICATIONS) {
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
      }

      if (activeSection == SettingsSection.HISTORY) {
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
      }

      if (activeSection == SettingsSection.ADVANCED) {
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
      }

      if (activeSection == SettingsSection.PRIVACY) {
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
      }

      if (activeSection == SettingsSection.INTEGRATIONS) {
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
    iconRes: Int = R.drawable.baseline_question_mark_24,
    onClick: () -> Unit,
) {
  ListItem(
      modifier = Modifier.clickable(onClick = onClick),
      leadingContent = { Icon(painterResource(iconRes), contentDescription = null) },
      headlineContent = { Text(title) },
      supportingContent = { subtitle?.let { Text(it) } },
      trailingContent = { Icon(painterResource(R.drawable.baseline_navigate_next_24), null) },
      colors =
          ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
  )
}

@Composable
private fun SettingsSectionGroup(
    title: String,
    sections: List<SettingsSection>,
    onSectionSelected: (SettingsSection) -> Unit,
) {
  SettingGroup(title = title) {
    sections.forEachIndexed { index, section ->
      if (index > 0) HorizontalDivider()
      SettingsNavigationRow(
          title = stringResource(section.titleRes),
          subtitle = stringResource(section.summaryRes),
          iconRes = section.iconRes,
          onClick = { onSectionSelected(section) },
      )
    }
  }
}

private enum class SettingsCategory {
  PERSONALIZATION,
  APP_BEHAVIOR,
}

private enum class SettingsSection(
    val titleRes: Int,
    val summaryRes: Int,
    val iconRes: Int,
    val category: SettingsCategory,
) {
  APPEARANCE(
      R.string.appearance,
      R.string.appearance_summary,
      R.drawable.ic_palette_24,
      SettingsCategory.PERSONALIZATION,
  ),
  GMAIL(
      R.string.gmail_otp_title,
      R.string.gmail_settings_summary,
      R.drawable.ic_email_24,
      SettingsCategory.PERSONALIZATION,
  ),
  CONTENT(
      R.string.content_and_detection,
      R.string.content_settings_summary,
      R.drawable.ic_language_24,
      SettingsCategory.PERSONALIZATION,
  ),
  NOTIFICATIONS(
      R.string.notifications,
      R.string.notifications_settings_summary,
      R.drawable.ic_notifications_24,
      SettingsCategory.APP_BEHAVIOR,
  ),
  HISTORY(
      R.string.history,
      R.string.history_settings_summary,
      R.drawable.ic_history_24,
      SettingsCategory.APP_BEHAVIOR,
  ),
  PRIVACY(
      R.string.privacy,
      R.string.privacy_settings_summary,
      R.drawable.ic_security_24,
      SettingsCategory.APP_BEHAVIOR,
  ),
  ADVANCED(
      R.string.advanced_settings,
      R.string.advanced_settings_summary,
      R.drawable.ic_tune_24,
      SettingsCategory.APP_BEHAVIOR,
  ),
  INTEGRATIONS(
      R.string.integrations,
      R.string.integrations_settings_summary,
      R.drawable.ic_extension_24,
      SettingsCategory.APP_BEHAVIOR,
  ),
}

@Composable
private fun ColorPaletteOption(
    palette: AppColorPalette,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val swatches =
      when (palette) {
        AppColorPalette.MATERIAL_YOU ->
            listOf(scheme.primary, scheme.secondaryContainer, scheme.tertiaryContainer)
        AppColorPalette.BLUE ->
            listOf(Color(0xFFA8C7FA), Color(0xFFBEC8DB), Color(0xFF00528C))
        AppColorPalette.VIOLET ->
            listOf(Color(0xFF54205C), Color(0xFFD8B8DE), Color(0xFFF3AAA9))
        AppColorPalette.GREEN ->
            listOf(Color(0xFF235D47), Color(0xFFC7D8B5), Color(0xFFE5C78C))
        AppColorPalette.ROSE ->
            listOf(Color(0xFF681F2D), Color(0xFFE4B9BF), Color(0xFFEFC28D))
      }

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Card(
        modifier =
            Modifier.size(76.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .semantics {
                  contentDescription = label
                  this.selected = selected
                },
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(2.dp, scheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
    ) {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(52.dp)) {
          val diameter = size.minDimension
          val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
          val bounds = Size(diameter, diameter)
          val radius = diameter / 2
          drawCircle(swatches[2], radius)
          drawArc(swatches[0], 180f, 180f, true, topLeft, bounds)
          drawArc(swatches[1], 90f, 90f, true, topLeft, bounds)
          drawArc(swatches[2], 0f, 90f, true, topLeft, bounds)
          drawCircle(scheme.outlineVariant, radius, style = Stroke(1.dp.toPx()))
        }
        if (selected) {
          androidx.compose.material3.Surface(shape = CircleShape, color = scheme.primary) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.padding(6.dp).size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ThemeModeSelector(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  Row(
      modifier = Modifier.fillMaxWidth().clip(CircleShape).background(scheme.surfaceVariant),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT, AppThemeMode.DARK).forEach { mode ->
      val selected =
          currentMode == mode || (mode == AppThemeMode.DARK && currentMode == AppThemeMode.AMOLED)
      val label =
          when (mode) {
            AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
            AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
            else -> stringResource(R.string.theme_dark)
          }
      Surface(
          modifier = Modifier.weight(1f).clickable { onModeSelected(mode) },
          shape = CircleShape,
          color = if (selected) scheme.primaryContainer else Color.Transparent,
      ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
          Text(
              text = label,
              color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun ThemeToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconRes: Int,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Icon(painterResource(iconRes), contentDescription = null)
    Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
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
