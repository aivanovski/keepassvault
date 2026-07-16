use crate::converter::{convert_proto_database, convert_proto_key};
use crate::error::BridgeError;
use crate::proto;
use keepass::config::DatabaseVersion;
use keepass::Database;

pub fn encode(
    database_proto: proto::Database,
    key_proto: proto::DatabaseKey,
) -> Result<Vec<u8>, BridgeError> {
    let key = convert_proto_key(key_proto)?;
    let mut database = convert_proto_database(database_proto)?;
    convert_to_supported_version(&mut database);

    let mut output = Vec::new();
    database
        .save(&mut output, key)
        .map_err(|error| BridgeError::IoError {
            message: error.to_string(),
        })?;

    Ok(output)
}

fn convert_to_supported_version(database: &mut Database) {
    if matches!(
        database.config.version,
        DatabaseVersion::KDB(_)
            | DatabaseVersion::KDB2(_)
            | DatabaseVersion::KDB3(_)
            | DatabaseVersion::KDB4(0)
    ) {
        database.config.version = DatabaseVersion::KDB4(1);
    }
}
