use crate::converter::{convert_database_to_proto, convert_proto_key};
use crate::error::BridgeError;
use crate::proto;
use keepass::db::DatabaseOpenError;
use keepass::Database;

pub fn decode(data: &[u8], key_proto: proto::DatabaseKey) -> Result<proto::Database, BridgeError> {
    let key = convert_proto_key(key_proto)?;
    let db = Database::parse(data, key).map_err(|error| match error {
        DatabaseOpenError::Key(err) => BridgeError::InvalidKey {
            message: err.to_string(),
        },
        _ => BridgeError::ParseError {
            message: error.to_string(),
        },
    })?;

    Ok(convert_database_to_proto(&db))
}
