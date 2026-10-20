package io.github.jd1378.otphelper.gmail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GmailAccountStoreTest {
  @Test
  fun returnsEmptyListWhenNoAccountsAreStored() {
    val store = GmailAccountStore(FakeGmailAccountPreferences())

    assertEquals(emptyList<String>(), store.getAccounts())
  }

  @Test
  fun migratesLegacyAccountOnFirstRead() {
    val preferences = FakeGmailAccountPreferences(legacyEmail = "one@gmail.com")
    val store = GmailAccountStore(preferences)

    assertEquals(listOf("one@gmail.com"), store.getAccounts())
    assertNull(preferences.legacyEmail)
    assertEquals(listOf("one@gmail.com"), store.getAccounts())
  }

  @Test
  fun keepsLegacyAccountWhenMigrationWriteFails() {
    val preferences =
        FakeGmailAccountPreferences(legacyEmail = "one@gmail.com", writeSucceeds = false)
    val store = GmailAccountStore(preferences)

    assertEquals(listOf("one@gmail.com"), store.getAccounts())
    assertEquals("one@gmail.com", preferences.legacyEmail)
  }

  @Test
  fun preservesAccountOrderAndDoesNotAddCaseInsensitiveDuplicates() {
    val store = GmailAccountStore(FakeGmailAccountPreferences())

    store.addAccount("one@gmail.com")
    store.addAccount("two@gmail.com")
    store.addAccount("ONE@gmail.com")

    assertEquals(listOf("one@gmail.com", "two@gmail.com"), store.getAccounts())
  }

  @Test
  fun reportsWhenAddingAccountCannotBePersisted() {
    val store = GmailAccountStore(FakeGmailAccountPreferences(writeSucceeds = false))

    assertEquals(false, store.addAccount("one@gmail.com"))
    assertEquals(emptyList<String>(), store.getAccounts())
  }

  @Test
  fun removesOnlyRequestedAccount() {
    val store = GmailAccountStore(FakeGmailAccountPreferences())
    store.addAccount("one@gmail.com")
    store.addAccount("two@gmail.com")

    store.removeAccount("ONE@gmail.com")

    assertEquals(listOf("two@gmail.com"), store.getAccounts())
  }

  @Test
  fun reportsWhenAccountRemovalCannotBePersisted() {
    val preferences =
        FakeGmailAccountPreferences(
            accountsJson = "[\"one@gmail.com\"]",
            writeSucceeds = false,
        )
    val store = GmailAccountStore(preferences)

    assertEquals(false, store.removeAccount("one@gmail.com"))
  }

  private class FakeGmailAccountPreferences(
    var accountsJson: String? = null,
    var legacyEmail: String? = null,
    private val writeSucceeds: Boolean = true,
  ) : GmailAccountPreferences {
    override fun getAccountsJson(): String? = accountsJson

    override fun setAccountsJson(value: String): Boolean {
      if (!writeSucceeds) return false
      accountsJson = value
      return true
    }

    override fun getLegacyEmail(): String? = legacyEmail

    override fun removeLegacyEmail() {
      legacyEmail = null
    }
  }
}
