/*
 * Copyright 2015 Jo Rabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.linguafranca.pwdb.kdbx.stream_3_1;

import org.linguafranca.pwdb.kdbx.StreamEncryptor;
import org.linguafranca.pwdb.security.Encryption;
import org.spongycastle.crypto.engines.Salsa20Engine;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.util.encoders.Hex;

import java.security.MessageDigest;

/**
 * A helper class for Salsa20 encryption.
 *
 * <p>Salsa20 doesn't quite fit the memory model
 * supposed by SerializableDatabase.StreamEncryptor - all encrypted
 * items have to be en/decrypted in order of encryption,
 * i.e. in document order and at the same time.
 *
 * <p>The encrypt and decrypt methods
 * actually do the same thing. They are here
 * only to fulfill the interface contract.
 *
 * @author jo
 */
public class Salsa20StreamEncryptor implements StreamEncryptor {

    private final Salsa20Engine salsa20;
    private final byte[] key;

    // Android compatibility
    private static final byte[] SALSA20_IV = Hex.decode("E830094B97205D2A".getBytes());

    /**
     * Creates a Salsa20 engine
     *
     * @param key the key to use
     * @return an initialized Salsa20 engine
     */
    @SuppressWarnings("WeakerAccess")
    public static Salsa20Engine createSalsa20(byte[] key) {
        MessageDigest md = Encryption.getMessageDigestInstance();
        KeyParameter keyParameter = new KeyParameter(md.digest(key));
        ParametersWithIV ivParameter = new ParametersWithIV(keyParameter, SALSA20_IV);
        Salsa20Engine engine = new Salsa20Engine();
        engine.init(true, ivParameter);
        return engine;
    }

    /**
     * Constructor creates engine used for both encryption and decryption
     *
     * @param key the key to use
     */
    public Salsa20StreamEncryptor(byte[] key) {
        this.key = key;
        salsa20 = createSalsa20(key);
    }

    @Override
    public byte[] getKey() {
        return key;
    }

    @Override
    public byte[] decrypt(byte[] encryptedText) {
        byte[] output = new byte[encryptedText.length];
        salsa20.processBytes(encryptedText, 0, encryptedText.length, output, 0);
        return output;
    }

    @Override
    public byte[] encrypt(byte[] decryptedText) {
        byte[] output = new byte[decryptedText.length];
        salsa20.processBytes(decryptedText, 0, decryptedText.length, output, 0);
        return output;
    }
}
