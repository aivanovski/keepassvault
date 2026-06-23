use core::ffi::c_void;

fn add(left: i32, right: i32) -> i32 {
    left + right
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_domain_rust_RustBridge_nativeAdd(
    _env: *mut c_void,
    _this: *mut c_void,
    left: i32,
    right: i32,
) -> i32 {
    add(left, right)
}

#[cfg(test)]
mod tests {
    use super::add;

    #[test]
    fn should_add_numbers() {
        assert_eq!(add(20, 22), 42)
    }
}
