use std::ptr;

use jni::{
    objects::{JByteArray, JObject, JString},
    sys::{jbyteArray, jint},
    JNIEnv,
};
use keepass::{
    config::{
        CompressionConfig, DatabaseConfig, DatabaseVersion, InnerCipherConfig, KdfConfig,
        OuterCipherConfig, VariantDictionaryValue,
    },
    db::{
        AttachmentRef, AutoType, Color, CustomDataItem, CustomDataValue, CustomIconRef, EntryRef,
        GroupRef, Icon, Meta, Times,
    },
    Database, DatabaseKey,
};
use prost::Message;

mod proto {
    #![allow(dead_code)]

    include!(concat!(env!("OUT_DIR"), "/passnotes.keepass.v1.rs"));
}

fn add(left: i32, right: i32) -> i32 {
    left + right
}

fn read_database_with_password(database_data: &[u8], password: &str) -> Option<Vec<u8>> {
    let key = DatabaseKey::new().with_password(password);
    let database = Database::parse(database_data, key).ok()?;
    let response = proto::ReadDatabaseResponse {
        database: Some(convert_database(&database)),
    };

    Some(response.encode_to_vec())
}

fn convert_database(database: &Database) -> proto::Database {
    let mut attachments = database.iter_all_attachments().collect::<Vec<_>>();
    attachments.sort_by_key(|attachment| attachment.id().id());

    let mut custom_icons = database.iter_all_custom_icons().collect::<Vec<_>>();
    custom_icons.sort_by_key(|icon| icon.id().uuid().as_u128());

    let mut deleted_objects = database
        .deleted_objects
        .iter()
        .map(|(uuid, deletion_time)| proto::DeletedObject {
            uuid: uuid_to_bytes(uuid),
            deletion_time_epoch_ms: optional_time_to_epoch_ms(*deletion_time),
        })
        .collect::<Vec<_>>();
    deleted_objects.sort_by(|left, right| left.uuid.cmp(&right.uuid));

    proto::Database {
        config: Some(convert_config(&database.config)),
        meta: Some(convert_meta(&database.meta)),
        root_group: Some(convert_group(database.root())),
        attachments: attachments.into_iter().map(convert_attachment).collect(),
        custom_icons: custom_icons.into_iter().map(convert_custom_icon).collect(),
        deleted_objects,
    }
}

fn convert_config(config: &DatabaseConfig) -> proto::DatabaseConfig {
    proto::DatabaseConfig {
        version: Some(convert_version(&config.version)),
        outer_cipher: convert_outer_cipher(&config.outer_cipher_config) as i32,
        compression: convert_compression(&config.compression_config) as i32,
        inner_cipher: convert_inner_cipher(&config.inner_cipher_config) as i32,
        kdf: Some(convert_kdf(&config.kdf_config)),
        public_custom_data: config
            .public_custom_data
            .as_ref()
            .map(|dictionary| {
                let mut items = dictionary
                    .iter()
                    .map(|(key, value)| proto::VariantDictionaryItem {
                        key: key.clone(),
                        value: Some(convert_variant_dictionary_value(value)),
                    })
                    .collect::<Vec<_>>();
                items.sort_by(|left, right| left.key.cmp(&right.key));
                items
            })
            .unwrap_or_default(),
    }
}

fn convert_version(version: &DatabaseVersion) -> proto::DatabaseVersion {
    let (format, major, minor) = match version {
        DatabaseVersion::KDB(minor) => (proto::DatabaseFormat::Kdb, 1, *minor),
        DatabaseVersion::KDB2(minor) => (proto::DatabaseFormat::Kdbx2, 2, *minor),
        DatabaseVersion::KDB3(minor) => (proto::DatabaseFormat::Kdbx3, 3, *minor),
        DatabaseVersion::KDB4(minor) => (proto::DatabaseFormat::Kdbx4, 4, *minor),
    };

    proto::DatabaseVersion {
        format: format as i32,
        major: Some(major),
        minor: Some(u32::from(minor)),
    }
}

fn convert_outer_cipher(cipher: &OuterCipherConfig) -> proto::OuterCipher {
    match cipher {
        OuterCipherConfig::AES256 => proto::OuterCipher::Aes256,
        OuterCipherConfig::Twofish => proto::OuterCipher::Twofish,
        OuterCipherConfig::ChaCha20 => proto::OuterCipher::Chacha20,
        _ => proto::OuterCipher::Unspecified,
    }
}

fn convert_compression(compression: &CompressionConfig) -> proto::Compression {
    match compression {
        CompressionConfig::None => proto::Compression::None,
        CompressionConfig::GZip => proto::Compression::Gzip,
        _ => proto::Compression::Unspecified,
    }
}

fn convert_inner_cipher(cipher: &InnerCipherConfig) -> proto::InnerCipher {
    match cipher {
        InnerCipherConfig::Plain => proto::InnerCipher::Plain,
        InnerCipherConfig::Salsa20 => proto::InnerCipher::Salsa20,
        InnerCipherConfig::ChaCha20 => proto::InnerCipher::Chacha20,
        _ => proto::InnerCipher::Unspecified,
    }
}

fn convert_kdf(kdf: &KdfConfig) -> proto::KdfConfig {
    let value = match kdf {
        KdfConfig::Aes { rounds } => {
            proto::kdf_config::Value::Aes(proto::AesKdf { rounds: *rounds })
        }
        KdfConfig::Argon2 {
            iterations,
            memory,
            parallelism,
            version,
        } => proto::kdf_config::Value::Argon2(proto::Argon2Kdf {
            iterations: *iterations,
            memory_kib: *memory,
            parallelism: *parallelism,
            version: version.as_u32(),
        }),
        KdfConfig::Argon2id {
            iterations,
            memory,
            parallelism,
            version,
        } => proto::kdf_config::Value::Argon2id(proto::Argon2Kdf {
            iterations: *iterations,
            memory_kib: *memory,
            parallelism: *parallelism,
            version: version.as_u32(),
        }),
        _ => {
            return proto::KdfConfig { value: None };
        }
    };

    proto::KdfConfig { value: Some(value) }
}

fn convert_meta(meta: &Meta) -> proto::DatabaseMeta {
    proto::DatabaseMeta {
        generator: meta.generator.clone(),
        database_name: meta.database_name.clone(),
        database_name_changed_epoch_ms: optional_time_to_epoch_ms(meta.database_name_changed),
        database_description: meta.database_description.clone(),
        database_description_changed_epoch_ms: optional_time_to_epoch_ms(
            meta.database_description_changed,
        ),
        default_username: meta.default_username.clone(),
        default_username_changed_epoch_ms: optional_time_to_epoch_ms(meta.default_username_changed),
        maintenance_history_days: meta.maintenance_history_days.map(|value| value as u32),
        color: meta.color.as_ref().map(convert_color),
        master_key_changed_epoch_ms: optional_time_to_epoch_ms(meta.master_key_changed),
        master_key_change_rec_days: meta.master_key_change_rec.map(|value| value as i32),
        master_key_change_force_days: meta.master_key_change_force.map(|value| value as i32),
        memory_protection: meta.memory_protection.as_ref().map(|protection| {
            proto::MemoryProtection {
                protect_title: protection.protect_title,
                protect_username: protection.protect_username,
                protect_password: protection.protect_password,
                protect_url: protection.protect_url,
                protect_notes: protection.protect_notes,
            }
        }),
        recycle_bin_enabled: meta.recyclebin_enabled,
        recycle_bin_uuid: meta.recyclebin_uuid.as_ref().map(uuid_to_bytes),
        recycle_bin_changed_epoch_ms: optional_time_to_epoch_ms(meta.recyclebin_changed),
        entry_templates_group_uuid: meta.entry_templates_group.as_ref().map(uuid_to_bytes),
        entry_templates_group_changed_epoch_ms: optional_time_to_epoch_ms(
            meta.entry_templates_group_changed,
        ),
        last_selected_group_uuid: meta.last_selected_group.as_ref().map(uuid_to_bytes),
        last_top_visible_group_uuid: meta.last_top_visible_group.as_ref().map(uuid_to_bytes),
        history_max_items: meta.history_max_items.map(|value| value as i32),
        history_max_size: meta.history_max_size.map(|value| value as i32),
        settings_changed_epoch_ms: optional_time_to_epoch_ms(meta.settings_changed),
        custom_data: convert_custom_data(&meta.custom_data),
    }
}

fn convert_group(group: GroupRef<'_>) -> proto::Group {
    let mut groups = group.groups().collect::<Vec<_>>();
    groups.sort_by_key(|group| group.id().uuid().as_u128());

    let mut entries = group.entries().collect::<Vec<_>>();
    entries.sort_by_key(|entry| entry.id().uuid().as_u128());

    proto::Group {
        uuid: uuid_to_bytes(&group.id().uuid()),
        parent_uuid: group
            .parent()
            .map(|parent| uuid_to_bytes(&parent.id().uuid())),
        previous_parent_group_uuid: group
            .previous_parent()
            .map(|parent| uuid_to_bytes(&parent.id().uuid())),
        name: group.name.clone(),
        notes: group.notes.clone(),
        tags: group.tags.clone(),
        icon: group.icon().map(convert_icon),
        times: Some(convert_times(&group.times)),
        custom_data: convert_custom_data(&group.custom_data),
        is_expanded: group.is_expanded,
        default_autotype_sequence: group.default_autotype_sequence.clone(),
        enable_autotype: group.enable_autotype,
        enable_searching: group.enable_searching,
        last_top_visible_entry_uuid: None,
        groups: groups.into_iter().map(convert_group).collect(),
        entries: entries
            .into_iter()
            .map(|entry| convert_entry(entry, true))
            .collect(),
    }
}

fn convert_entry(entry: EntryRef<'_>, include_history: bool) -> proto::Entry {
    let mut fields = entry
        .fields
        .iter()
        .map(|(name, value)| proto::Field {
            name: name.clone(),
            value: value.get().clone(),
            is_protected: value.is_protected(),
        })
        .collect::<Vec<_>>();
    fields.sort_by(|left, right| left.name.cmp(&right.name));

    let mut attachments = entry
        .attachments_named()
        .map(|(name, attachment)| proto::EntryAttachment {
            name: name.to_owned(),
            attachment_id: attachment.id().id() as u32,
        })
        .collect::<Vec<_>>();
    attachments.sort_by(|left, right| left.name.cmp(&right.name));

    let history = if include_history {
        entry
            .history
            .as_ref()
            .map(|history| {
                (0..history.get_entries().len())
                    .filter_map(|index| entry.historical(index))
                    .map(|entry| convert_entry(entry, false))
                    .collect()
            })
            .unwrap_or_default()
    } else {
        Vec::new()
    };

    proto::Entry {
        uuid: uuid_to_bytes(&entry.id().uuid()),
        parent_group_uuid: uuid_to_bytes(&entry.parent().id().uuid()),
        previous_parent_group_uuid: entry
            .previous_parent()
            .map(|parent| uuid_to_bytes(&parent.id().uuid())),
        fields,
        autotype: entry.autotype.as_ref().map(convert_autotype),
        tags: entry.tags.clone(),
        times: Some(convert_times(&entry.times)),
        custom_data: convert_custom_data(&entry.custom_data),
        icon: entry.icon().map(convert_icon),
        foreground_color: entry.foreground_color.as_ref().map(convert_color),
        background_color: entry.background_color.as_ref().map(convert_color),
        override_url: entry.override_url.clone(),
        quality_check: entry.quality_check,
        attachments,
        history,
    }
}

fn convert_attachment(attachment: AttachmentRef<'_>) -> proto::Attachment {
    proto::Attachment {
        id: attachment.id().id() as u32,
        data: attachment.data.get().clone(),
        is_protected: attachment.data.is_protected(),
    }
}

fn convert_custom_icon(icon: CustomIconRef<'_>) -> proto::CustomIcon {
    proto::CustomIcon {
        uuid: uuid_to_bytes(&icon.id().uuid()),
        name: icon.name.clone(),
        last_modification_time_epoch_ms: optional_time_to_epoch_ms(icon.last_modification_time),
        data: icon.data.clone(),
    }
}

fn convert_icon(icon: &Icon) -> proto::Icon {
    let value = match icon {
        Icon::BuiltIn(id) => proto::icon::Value::BuiltinId(*id as u32),
        Icon::Custom(id) => proto::icon::Value::CustomIconUuid(uuid_to_bytes(&id.uuid())),
    };

    proto::Icon { value: Some(value) }
}

fn convert_autotype(autotype: &AutoType) -> proto::AutoType {
    proto::AutoType {
        enabled: autotype.enabled,
        default_sequence: autotype.default_sequence.clone(),
        data_transfer_obfuscation: match autotype.data_transfer_obfuscation {
            keepass::db::DataTransferObfuscation::None => {
                proto::DataTransferObfuscation::None as i32
            }
            keepass::db::DataTransferObfuscation::UseClipboard => {
                proto::DataTransferObfuscation::UseClipboard as i32
            }
        },
        associations: autotype
            .associations
            .iter()
            .map(|association| proto::AutoTypeAssociation {
                window: association.window.clone(),
                sequence: association.sequence.clone(),
            })
            .collect(),
    }
}

fn convert_times(times: &Times) -> proto::Times {
    proto::Times {
        creation_epoch_ms: optional_time_to_epoch_ms(times.creation),
        last_modification_epoch_ms: optional_time_to_epoch_ms(times.last_modification),
        last_access_epoch_ms: optional_time_to_epoch_ms(times.last_access),
        expiry_epoch_ms: optional_time_to_epoch_ms(times.expiry),
        location_changed_epoch_ms: optional_time_to_epoch_ms(times.location_changed),
        expires: times.expires,
        usage_count: times.usage_count.map(|value| value as u32),
    }
}

fn convert_color(color: &Color) -> proto::Color {
    proto::Color {
        red: u32::from(color.r),
        green: u32::from(color.g),
        blue: u32::from(color.b),
    }
}

fn convert_custom_data(
    custom_data: &std::collections::HashMap<String, CustomDataItem>,
) -> Vec<proto::CustomDataItem> {
    let mut items = custom_data
        .iter()
        .map(|(key, item)| proto::CustomDataItem {
            key: key.clone(),
            value: item.value.as_ref().map(convert_custom_data_value),
            last_modification_time_epoch_ms: optional_time_to_epoch_ms(item.last_modification_time),
        })
        .collect::<Vec<_>>();
    items.sort_by(|left, right| left.key.cmp(&right.key));
    items
}

fn convert_custom_data_value(value: &CustomDataValue) -> proto::CustomDataValue {
    let value = match value {
        CustomDataValue::String(value) => {
            proto::custom_data_value::Value::StringValue(value.clone())
        }
        CustomDataValue::Binary(value) => {
            proto::custom_data_value::Value::BinaryValue(value.clone())
        }
    };

    proto::CustomDataValue { value: Some(value) }
}

fn convert_variant_dictionary_value(
    value: &VariantDictionaryValue,
) -> proto::VariantDictionaryValue {
    let value = match value {
        VariantDictionaryValue::UInt32(value) => {
            proto::variant_dictionary_value::Value::Uint32Value(*value)
        }
        VariantDictionaryValue::UInt64(value) => {
            proto::variant_dictionary_value::Value::Uint64Value(*value)
        }
        VariantDictionaryValue::Int32(value) => {
            proto::variant_dictionary_value::Value::Int32Value(*value)
        }
        VariantDictionaryValue::Int64(value) => {
            proto::variant_dictionary_value::Value::Int64Value(*value)
        }
        VariantDictionaryValue::Bool(value) => {
            proto::variant_dictionary_value::Value::BoolValue(*value)
        }
        VariantDictionaryValue::String(value) => {
            proto::variant_dictionary_value::Value::StringValue(value.clone())
        }
        VariantDictionaryValue::ByteArray(value) => {
            proto::variant_dictionary_value::Value::BytesValue(value.clone())
        }
        _ => {
            return proto::VariantDictionaryValue { value: None };
        }
    };

    proto::VariantDictionaryValue { value: Some(value) }
}

fn optional_time_to_epoch_ms(time: Option<chrono::NaiveDateTime>) -> Option<i64> {
    time.map(|value| value.and_utc().timestamp_millis())
}

fn uuid_to_bytes(uuid: &uuid::Uuid) -> Vec<u8> {
    uuid.as_bytes().to_vec()
}

fn get_jni_byte_array(env: &JNIEnv<'_>, value: JByteArray<'_>) -> Option<Vec<u8>> {
    env.convert_byte_array(value).ok()
}

fn get_jni_string(env: &mut JNIEnv<'_>, value: JString<'_>) -> Option<String> {
    env.get_string(&value).ok().map(|value| value.into())
}

fn new_jni_byte_array(env: &JNIEnv<'_>, value: &[u8]) -> jbyteArray {
    env.byte_array_from_slice(value)
        .map(|array| array.into_raw())
        .unwrap_or_else(|_| ptr::null_mut())
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_domain_rust_RustBridge_nativeAdd(
    _env: JNIEnv<'_>,
    _this: JObject<'_>,
    left: jint,
    right: jint,
) -> jint {
    add(left, right)
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_domain_rust_RustBridge_nativeCanDecodeWithPassword(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    database_data: JByteArray<'_>,
    password: JString<'_>,
) -> jbyteArray {
    let bytes = get_jni_byte_array(&env, database_data);
    let password = get_jni_string(&mut env, password);

    match (bytes, password) {
        (Some(bytes), Some(password)) => read_database_with_password(&bytes, &password)
            .map(|response| new_jni_byte_array(&env, &response))
            .unwrap_or_else(ptr::null_mut),
        _ => ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::{add, read_database_with_password};

    #[test]
    fn should_add_numbers() {
        assert_eq!(add(20, 22), 42)
    }

    #[test]
    fn should_not_decode_invalid_database() {
        assert!(read_database_with_password(b"not-a-database", "abc123").is_none());
    }
}
