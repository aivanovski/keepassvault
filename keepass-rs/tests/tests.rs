pub mod test_data;

#[cfg(test)]
mod tests {
    use crate::test_data::{TestDatabaseKey, TEST_DATABASES};
    use keepass_rs::decode::decode;
    use keepass_rs::encode::encode;
    use keepass_rs::error::BridgeError;
    use keepass_rs::proto;
    use std::fs;
    use std::io::Error;
    use std::path::PathBuf;

    #[test]
    fn should_decode_database() {
        for test_database in TEST_DATABASES {
            // arrange
            let db_bytes = read_file(test_database.path).unwrap();

            // act
            let database = decode(&db_bytes, convert_key(&test_database.key));

            // assert
            assert!(database.is_ok());
        }
    }

    #[test]
    fn should_return_error_when_key_is_invalid() {
        // arrange
        let test_database = TEST_DATABASES.first().unwrap();
        let db_bytes = read_file(test_database.path).unwrap();
        let invalid_key = proto::DatabaseKey {
            password: Some("aaa".to_string()),
            key_bytes: None,
        };

        // act
        let database = decode(&db_bytes, invalid_key);

        // assert
        assert!(database.is_err());
        match database.unwrap_err() {
            BridgeError::InvalidKey { message } => {
                assert_eq!(message, "Incorrect key");
            }
            other => panic!("Invalid error value {other:?}"),
        }
    }

    #[test]
    fn should_encode_database() {
        for test_database in TEST_DATABASES {
            // arrange
            let db_bytes = read_file(test_database.path).unwrap();
            let key = convert_key(&test_database.key);
            let database = decode(&db_bytes, key.clone()).unwrap();
            let expected = get_expected_database(&database);

            // act
            let encoded = encode(database, key.clone());

            // assert
            let actual = decode(&encoded.unwrap(), key.clone()).unwrap();
            assert_eq!(actual.config, expected.config);
            assert_eq!(actual.meta, expected.meta);
            assert_eq!(actual.root_group, expected.root_group);
            assert_eq!(actual.attachments, expected.attachments);
            assert_eq!(actual.custom_icons, expected.custom_icons);
            assert_eq!(actual.deleted_objects, expected.deleted_objects);
        }
    }

    fn get_expected_database(source: &proto::Database) -> proto::Database {
        proto::Database {
            config: source.config.clone().map(|config| proto::DatabaseConfig {
                version: Some(proto::DatabaseVersion {
                    format: proto::DatabaseFormat::Kdbx4 as i32,
                    major: Some(4),
                    minor: Some(1),
                }),
                outer_cipher: config.outer_cipher,
                compression: config.compression,
                inner_cipher: config.inner_cipher,
                kdf: config.kdf.clone(),
                public_custom_data: config.public_custom_data.clone(),
            }),
            meta: source.meta.clone(),
            root_group: source.root_group.clone(),
            attachments: source.attachments.clone(),
            custom_icons: source.custom_icons.clone(),
            deleted_objects: source.deleted_objects.clone(),
        }
    }

    fn read_file(path: &str) -> Result<Vec<u8>, Error> {
        let path = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join(path);
        fs::read(path)
    }

    fn convert_key(key: &TestDatabaseKey) -> proto::DatabaseKey {
        proto::DatabaseKey {
            password: key.password.map(|p| p.to_string()),
            key_bytes: key.key_content.map(|k| k.to_string().into_bytes()),
        }
    }
}
