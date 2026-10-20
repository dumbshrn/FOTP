package io.github.jd1378.otphelper.gmail

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

internal interface GmailAccountPreferences {
  fun getAccountsJson(): String?

  fun setAccountsJson(value: String): Boolean

  fun getLegacyEmail(): String?

  fun removeLegacyEmail()
}

class GmailAccountStore internal constructor(private val preferences: GmailAccountPreferences) {
  constructor(context: Context) :
      this(
          SharedPreferencesGmailAccountPreferences(
              context.getSharedPreferences(GmailFetchActivity.PREFS_NAME, Context.MODE_PRIVATE)
          )
      )

  fun getAccounts(): List<String> {
    val savedAccounts = preferences.getAccountsJson()
    if (savedAccounts != null) return decodeAccounts(savedAccounts)

    val legacyEmail =
        preferences.getLegacyEmail()?.trim()?.takeIf(String::isNotEmpty) ?: return emptyList()
    val migratedAccounts = listOf(legacyEmail)
    if (preferences.setAccountsJson(encodeAccounts(migratedAccounts))) {
      preferences.removeLegacyEmail()
    }
    return migratedAccounts
  }

  fun addAccount(email: String): Boolean {
    val normalizedEmail = email.trim()
    if (normalizedEmail.isEmpty()) return false
    val accounts = getAccounts().toMutableList()
    if (accounts.any { it.equals(normalizedEmail, ignoreCase = true) }) return true
    accounts.add(normalizedEmail)
    return preferences.setAccountsJson(encodeAccounts(accounts))
  }

  fun removeAccount(email: String): Boolean {
    val existingAccounts = getAccounts()
    val accounts = existingAccounts.filterNot { it.equals(email.trim(), ignoreCase = true) }
    if (accounts.size == existingAccounts.size) return true
    return preferences.setAccountsJson(encodeAccounts(accounts))
  }

  private fun decodeAccounts(serialized: String): List<String> =
      runCatching {
        val array = JSONArray(serialized)
        buildList {
          for (index in 0 until array.length()) {
            val email = array.optString(index).trim()
            if (email.isNotEmpty() && none { it.equals(email, ignoreCase = true) }) add(email)
          }
        }
      }
          .getOrDefault(emptyList())

  private fun encodeAccounts(accounts: List<String>): String = JSONArray(accounts).toString()

  private class SharedPreferencesGmailAccountPreferences(
    private val preferences: SharedPreferences
  ) : GmailAccountPreferences {
    override fun getAccountsJson(): String? = preferences.getString(PREF_ACCOUNTS, null)

    override fun setAccountsJson(value: String): Boolean =
        preferences.edit().putString(PREF_ACCOUNTS, value).commit()

    override fun getLegacyEmail(): String? =
        preferences.getString(GmailFetchActivity.PREF_EMAIL, null)

    override fun removeLegacyEmail() {
      preferences.edit().remove(GmailFetchActivity.PREF_EMAIL).commit()
    }
  }

  private companion object {
    const val PREF_ACCOUNTS = "connected_emails"
  }
}
