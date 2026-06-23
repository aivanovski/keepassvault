use core::ffi::{c_char, c_void};
use core::ptr;
use std::ffi::CStr;

use keepass::{Database, DatabaseKey};

type JArray = JObject;
type JBoolean = u8;
type JByte = i8;
type JByteArray = JObject;
type JNIEnv = *const JNINativeInterface;
type JObject = *mut c_void;
type JSize = i32;
type JString = JObject;

#[repr(C)]
struct JNINativeInterface {
    // Skip the JNI vtable entries we do not need for this proof of concept.
    _prefix: [*const c_void; 168],
    get_string_utf_length: unsafe extern "system" fn(env: *mut JNIEnv, string: JString) -> JSize,
    get_string_utf_chars: unsafe extern "system" fn(
        env: *mut JNIEnv,
        string: JString,
        is_copy: *mut JBoolean,
    ) -> *const c_char,
    release_string_utf_chars:
        unsafe extern "system" fn(env: *mut JNIEnv, string: JString, chars: *const c_char),
    get_array_length: unsafe extern "system" fn(env: *mut JNIEnv, array: JArray) -> JSize,
    _new_object_array:
        unsafe extern "system" fn(env: *mut JNIEnv, length: JSize, element_class: JObject, initial_element: JObject) -> JObject,
    _get_object_array_element:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, index: JSize) -> JObject,
    _set_object_array_element:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, index: JSize, value: JObject),
    _new_boolean_array: unsafe extern "system" fn(env: *mut JNIEnv, length: JSize) -> JObject,
    _new_byte_array: unsafe extern "system" fn(env: *mut JNIEnv, length: JSize) -> JByteArray,
    _new_char_array: unsafe extern "system" fn(env: *mut JNIEnv, length: JSize) -> JObject,
    _new_short_array: unsafe extern "system" fn(env: *mut JNIEnv, length: JSize) -> JObject,
    _new_int_array: unsafe extern "system" fn(env: *mut JNIEnv, length: JSize) -> JObject,
    _new_long_array: unsafe extern "system" fn(env: *mut JNIEnv, length: JSize) -> JObject,
    _new_float_array: unsafe extern "system" fn(env: *mut JNIEnv, length: JSize) -> JObject,
    _new_double_array: unsafe extern "system" fn(env: *mut JNIEnv, length: JSize) -> JObject,
    _get_boolean_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, is_copy: *mut JBoolean) -> *mut JBoolean,
    _get_byte_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JByteArray, is_copy: *mut JBoolean) -> *mut JByte,
    _get_char_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, is_copy: *mut JBoolean) -> *mut u16,
    _get_short_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, is_copy: *mut JBoolean) -> *mut i16,
    _get_int_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, is_copy: *mut JBoolean) -> *mut i32,
    _get_long_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, is_copy: *mut JBoolean) -> *mut i64,
    _get_float_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, is_copy: *mut JBoolean) -> *mut f32,
    _get_double_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, is_copy: *mut JBoolean) -> *mut f64,
    _release_boolean_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, elems: *mut JBoolean, mode: i32),
    _release_byte_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JByteArray, elems: *mut JByte, mode: i32),
    _release_char_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, elems: *mut u16, mode: i32),
    _release_short_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, elems: *mut i16, mode: i32),
    _release_int_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, elems: *mut i32, mode: i32),
    _release_long_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, elems: *mut i64, mode: i32),
    _release_float_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, elems: *mut f32, mode: i32),
    _release_double_array_elements:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, elems: *mut f64, mode: i32),
    _get_boolean_array_region:
        unsafe extern "system" fn(env: *mut JNIEnv, array: JObject, start: JSize, len: JSize, buf: *mut JBoolean),
    get_byte_array_region: unsafe extern "system" fn(
        env: *mut JNIEnv,
        array: JByteArray,
        start: JSize,
        len: JSize,
        buf: *mut JByte,
    ),
}

fn add(left: i32, right: i32) -> i32 {
    left + right
}

fn can_decode_with_password(database_data: &[u8], password: &str) -> bool {
    let key = DatabaseKey::new().with_password(password);

    Database::parse(database_data, key).is_ok()
}

unsafe fn get_jni_functions(env: *mut JNIEnv) -> &'static JNINativeInterface {
    unsafe { &**env }
}

unsafe fn get_string(env: *mut JNIEnv, value: JString) -> Option<String> {
    if env.is_null() || value.is_null() {
        return None;
    }

    let functions = unsafe { get_jni_functions(env) };
    let chars = unsafe { (functions.get_string_utf_chars)(env, value, ptr::null_mut()) };
    if chars.is_null() {
        return None;
    }

    let password = unsafe { CStr::from_ptr(chars) }.to_str().ok().map(str::to_owned);
    unsafe { (functions.release_string_utf_chars)(env, value, chars) };

    password
}

unsafe fn get_byte_array(env: *mut JNIEnv, value: JByteArray) -> Option<Vec<u8>> {
    if env.is_null() || value.is_null() {
        return None;
    }

    let functions = unsafe { get_jni_functions(env) };
    let len = unsafe { (functions.get_array_length)(env, value) };
    if len < 0 {
        return None;
    }

    let mut bytes = vec![0_u8; len as usize];
    unsafe {
        (functions.get_byte_array_region)(env, value, 0, len, bytes.as_mut_ptr().cast::<JByte>())
    };

    Some(bytes)
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_domain_rust_RustBridge_nativeAdd(
    _env: *mut c_void,
    _this: JObject,
    left: i32,
    right: i32,
) -> i32 {
    add(left, right)
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_domain_rust_RustBridge_nativeCanDecodeWithPassword(
    env: *mut c_void,
    _this: JObject,
    database_data: JByteArray,
    password: JString,
) -> JBoolean {
    let env = env.cast::<JNIEnv>();
    let bytes = unsafe { get_byte_array(env, database_data) };
    let password = unsafe { get_string(env, password) };

    match (bytes, password) {
        (Some(bytes), Some(password)) if can_decode_with_password(&bytes, &password) => 1,
        _ => 0,
    }
}

#[cfg(test)]
mod tests {
    use super::{add, can_decode_with_password};

    #[test]
    fn should_add_numbers() {
        assert_eq!(add(20, 22), 42)
    }

    #[test]
    fn should_not_decode_invalid_database() {
        assert!(!can_decode_with_password(b"not-a-database", "abc123"));
    }
}
