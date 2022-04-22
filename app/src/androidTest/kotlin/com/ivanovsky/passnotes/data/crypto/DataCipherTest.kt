package com.ivanovsky.passnotes.data.crypto

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.TestData.PLAIN_TEXT
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants.ANDROID_KEY_STORE
import com.ivanovsky.passnotes.data.crypto.DataCipherConstants.KEY_ALIAS
import com.ivanovsky.passnotes.data.crypto.entity.CipherTransformation.AES_CBC_PKCS5
import com.ivanovsky.passnotes.data.crypto.entity.CipherTransformation.AES_CBC_PKCS7
import com.ivanovsky.passnotes.data.crypto.keyprovider.FileSecretKeyProvider
import com.ivanovsky.passnotes.data.crypto.keyprovider.FileSecretKeyProvider.Companion.KEY_STORE_FILE_NAME
import com.ivanovsky.passnotes.data.crypto.keyprovider.KeyStoreSecretKeyProvider
import com.ivanovsky.passnotes.data.crypto.keyprovider.SecretKeyProvider
import java.io.File
import java.security.KeyStore
import java.security.KeyStoreException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataCipherTest {

    private lateinit var context: Context
    private lateinit var ciphers: List<DataCipherImpl>

    private lateinit var fileKeyProvider: SecretKeyProvider
    private lateinit var fileCipher: DataCipherImpl

    private lateinit var keyStoreKeyProvider: SecretKeyProvider
    private lateinit var keyStoreCipher: DataCipherImpl

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        keyStoreKeyProvider = KeyStoreSecretKeyProvider()
        keyStoreCipher = DataCipherImpl(keyStoreKeyProvider, AES_CBC_PKCS7)

        fileKeyProvider = FileSecretKeyProvider(context)
        fileCipher = DataCipherImpl(fileKeyProvider, AES_CBC_PKCS5)

        ciphers = listOf(keyStoreCipher, fileCipher)

        keyStoreKeyProvider.removeKeyIfNeed()
        fileKeyProvider.removeKeyIfNeed()
    }

    @After
    fun tearDown() {
        keyStoreKeyProvider.removeKeyIfNeed()
        fileKeyProvider.removeKeyIfNeed()
    }

    @Test
    fun keyShouldBeGenerated() {
        // Arrange
        assertThat(keyStoreKeyProvider.isKeyExist()).isFalse()
        assertThat(fileKeyProvider.isKeyExist()).isFalse()

        // Act
        keyStoreCipher.encode(PLAIN_TEXT)
        fileCipher.encode(PLAIN_TEXT)

        // Assert
        assertThat(keyStoreKeyProvider.isKeyExist()).isTrue()
        assertThat(fileKeyProvider.isKeyExist()).isTrue()
    }

    @Test
    fun keyShouldBeLoaded() {
        // Arrange
        val storePreEncoded = DataCipherImpl(KeyStoreSecretKeyProvider(), AES_CBC_PKCS7).encode(PLAIN_TEXT)
        val filePreEncoded = DataCipherImpl(FileSecretKeyProvider(context), AES_CBC_PKCS5).encode(PLAIN_TEXT)
        assertThat(keyStoreKeyProvider.isKeyExist()).isTrue()
        assertThat(fileKeyProvider.isKeyExist()).isTrue()

        // Act
        requireNotNull(storePreEncoded)
        requireNotNull(filePreEncoded)
        val keyStoreCipherResult = keyStoreCipher.decode(storePreEncoded)
        val fileCipherResult = fileCipher.decode(filePreEncoded)

        // Assert
        assertThat(keyStoreCipherResult).isEqualTo(PLAIN_TEXT)
        assertThat(fileCipherResult).isEqualTo(PLAIN_TEXT)
    }

    @Test
    fun encode_shouldReturnEncryptedResult() {
        // Arrange
        assertThat(keyStoreKeyProvider.isKeyExist()).isFalse()
        assertThat(fileKeyProvider.isKeyExist()).isFalse()

        // Act
        val keyStoreCipherResult = keyStoreCipher.encode(PLAIN_TEXT)
        val fileCipherResult = fileCipher.encode(PLAIN_TEXT)

        // Assert
        assertThat(keyStoreCipherResult).isNotEmpty()
        assertThat(keyStoreCipherResult).isNotEqualTo(PLAIN_TEXT)
        assertThat(fileCipherResult).isNotEmpty()
        assertThat(fileCipherResult).isNotEqualTo(PLAIN_TEXT)
    }

    @Test
    fun encodingAndDecodingShouldWork() {
        // Arrange
        assertThat(keyStoreKeyProvider.isKeyExist()).isFalse()
        assertThat(fileKeyProvider.isKeyExist()).isFalse()

        // Act
        val keyStoreCipherResult = keyStoreCipher.encodeAndDecode(PLAIN_TEXT)
        val fileCipherResult = fileCipher.encodeAndDecode(PLAIN_TEXT)

        // Assert
        assertThat(keyStoreCipherResult).isEqualTo(PLAIN_TEXT)
        assertThat(fileCipherResult).isEqualTo(PLAIN_TEXT)
    }

    private fun SecretKeyProvider.isKeyExist(): Boolean {
        return when (this) {
            is FileSecretKeyProvider -> {
                getKeyFile().exists()
            }
            is KeyStoreSecretKeyProvider -> {
                var result = false

                try {
                    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
                    keyStore.load(null)
                    result = keyStore.getEntry(KEY_ALIAS, null) != null
                } catch (e: KeyStoreException) {
                    e.printStackTrace()
                }

                result
            }
            else -> throw IllegalStateException()
        }
    }

    private fun SecretKeyProvider.removeKeyIfNeed() {
        when (this) {
            is FileSecretKeyProvider -> {
                if (isKeyExist()) {
                    getKeyFile().delete()
                }
            }
            is KeyStoreSecretKeyProvider -> {
                if (isKeyExist()) {
                    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
                    keyStore.load(null)
                    keyStore.deleteEntry(KEY_ALIAS)
                }
            }
            else -> throw IllegalStateException()
        }
    }

    private fun DataCipher.encodeAndDecode(text: String): String? {
        val encoded = encode(text) ?: return null
        return decode(encoded)
    }

    private fun getKeyFile(): File {
        return File(context.filesDir, KEY_STORE_FILE_NAME)
    }
}