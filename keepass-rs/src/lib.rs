pub mod converter;
pub mod decode;
pub mod encode;
pub mod error;

use crate::decode::decode;
use crate::encode::encode;
use crate::error::BridgeError;

use crate::proto::{DatabaseError, DecodeDatabaseResult, EncodeDatabaseResult};
use jni::{
    objects::{JByteArray, JObject},
    sys::jbyteArray,
    JNIEnv,
};
use prost::Message;
use std::ptr;

pub mod proto {
    #![allow(dead_code)]

    include!(concat!(env!("OUT_DIR"), "/keepassvault.v1.rs"));
}

fn get_jni_byte_array(env: &JNIEnv<'_>, value: JByteArray<'_>) -> Result<Vec<u8>, BridgeError> {
    Ok(env.convert_byte_array(value)?)
}

fn new_jni_byte_array(env: &JNIEnv<'_>, value: &[u8]) -> jbyteArray {
    env.byte_array_from_slice(value)
        .map(|array| array.into_raw())
        .unwrap_or_else(|_| ptr::null_mut())
}

fn native_decode(
    env: &JNIEnv<'_>,
    database_bytes: JByteArray<'_>,
    key_proto: JByteArray<'_>,
) -> Result<proto::Database, BridgeError> {
    let bytes = get_jni_byte_array(env, database_bytes)?;
    let key = get_jni_byte_array(env, key_proto)
        .and_then(|bytes| Ok(proto::DatabaseKey::decode(bytes.as_slice())?))?;

    Ok(decode(&bytes, key)?)
}

fn native_encode(
    env: &JNIEnv<'_>,
    database_proto: JByteArray<'_>,
    key_proto: JByteArray<'_>,
) -> Result<Vec<u8>, BridgeError> {
    let key = get_jni_byte_array(env, key_proto)
        .and_then(|bytes| Ok(proto::DatabaseKey::decode(bytes.as_slice())?))?;

    let database = get_jni_byte_array(env, database_proto)
        .and_then(|bytes| Ok(proto::Database::decode(bytes.as_slice())?))?;

    Ok(encode(database, key)?)
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_keepassrs_KeepassRsAndroid_nativeDecode(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
    database_bytes: JByteArray<'_>,
    key_proto: JByteArray<'_>,
) -> jbyteArray {
    let result = match native_decode(&env, database_bytes, key_proto) {
        Ok(db) => DecodeDatabaseResult {
            database: Some(db),
            error: None,
        },

        Err(error) => DecodeDatabaseResult {
            database: None,
            error: Some(DatabaseError::from(error)),
        },
    };

    new_jni_byte_array(&env, &result.encode_to_vec())
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_keepassrs_KeepassRsAndroid_nativeEncode(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
    database_proto: JByteArray<'_>,
    key_proto: JByteArray<'_>,
) -> jbyteArray {
    let result = match native_encode(&env, database_proto, key_proto) {
        Ok(bytes) => EncodeDatabaseResult {
            database: Some(bytes),
            error: None,
        },

        Err(error) => EncodeDatabaseResult {
            database: None,
            error: Some(DatabaseError::from(error)),
        },
    };

    new_jni_byte_array(&env, &result.encode_to_vec())
}
