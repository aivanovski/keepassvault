// pub mod test_data;

pub struct TestDatabaseKey {
    pub password: Option<&'static str>,
    pub key_content: Option<&'static str>,
}
pub struct TestDatabase {
    pub path: &'static str,
    pub key: TestDatabaseKey,
}

pub const TEST_DATABASES: &[TestDatabase] = &[
    TestDatabase {
        path: "tests/resources/test-password.kdbx",
        key: TestDatabaseKey {
            password: Some("abc123"),
            key_content: None,
        },
    },
    TestDatabase {
        path: "tests/resources/test-key-file.kdbx",
        key: TestDatabaseKey {
            password: None,
            key_content: Some("def456"),
        },
    },
    TestDatabase {
        path: "tests/resources/test-password-key-file.kdbx",
        key: TestDatabaseKey {
            password: Some("abc123"),
            key_content: Some("def456"),
        },
    },
];
