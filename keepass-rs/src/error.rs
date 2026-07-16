use crate::proto;
use prost::DecodeError;
use std::io::Error;

#[derive(Debug)]
pub enum BridgeError {
    IoError { message: String },
    ParseError { message: String },
    ProtobufFormat { message: String },
    JniError { error: jni::errors::Error },
    InvalidKey { message: String },
}

impl BridgeError {
    pub fn format_error(&self) -> String {
        match self {
            BridgeError::IoError { message } => message.to_string(),
            BridgeError::ParseError { message } => message.to_string(),
            BridgeError::ProtobufFormat { message } => message.to_string(),
            BridgeError::JniError { error } => error.to_string(),
            BridgeError::InvalidKey { message } => message.to_string(),
        }
    }
}

impl From<std::io::Error> for BridgeError {
    fn from(value: Error) -> Self {
        BridgeError::IoError {
            message: value.to_string(),
        }
    }
}

impl From<jni::errors::Error> for BridgeError {
    fn from(value: jni::errors::Error) -> Self {
        BridgeError::JniError { error: value }
    }
}

impl From<DecodeError> for BridgeError {
    fn from(value: DecodeError) -> Self {
        BridgeError::ProtobufFormat {
            message: value.to_string(),
        }
    }
}

impl From<BridgeError> for proto::DatabaseError {
    fn from(value: BridgeError) -> Self {
        match value {
            BridgeError::InvalidKey { message } => proto::DatabaseError {
                error_type: proto::DatabaseErrorType::InvalidKey.into(),
                message,
            },
            _ => proto::DatabaseError {
                error_type: proto::DatabaseErrorType::GenericError.into(),
                message: value.format_error().to_string(),
            },
        }
    }
}
