use std::{collections::HashMap, ptr};

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
        AttachmentRef, AutoType, Color, CustomDataItem, CustomDataValue, CustomIconRef, EntryId,
        EntryRef, GroupId, GroupRef, Icon, Meta, Times, Value,
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

fn write_database_with_password(
    original_database_data: &[u8],
    request_data: &[u8],
    password: &str,
) -> Option<Vec<u8>> {
    write_database_with_passwords(original_database_data, request_data, password, password)
}

fn write_database_with_passwords(
    original_database_data: &[u8],
    request_data: &[u8],
    old_password: &str,
    new_password: &str,
) -> Option<Vec<u8>> {
    let old_key = DatabaseKey::new().with_password(old_password);
    let new_key = DatabaseKey::new().with_password(new_password);
    let mut database = Database::parse(original_database_data, old_key).ok()?;
    let request = proto::WriteDatabaseRequest::decode(request_data).ok()?;
    let proto_database = request.database?;

    apply_database(&mut database, &proto_database).ok()?;

    let mut output = Vec::new();
    database.save(&mut output, new_key).ok()?;
    Some(output)
}

fn apply_database(database: &mut Database, proto_database: &proto::Database) -> Result<(), String> {
    if let Some(meta) = &proto_database.meta {
        apply_meta(&mut database.meta, meta);
    }

    let root = proto_database
        .root_group
        .as_ref()
        .ok_or_else(|| "missing root group".to_string())?;

    let entry_ids = database
        .root()
        .entries()
        .map(|entry| entry.id())
        .collect::<Vec<_>>();
    for entry_id in entry_ids {
        if let Some(entry) = database.entry_mut(entry_id) {
            entry.remove();
        }
    }

    let group_ids = database
        .root()
        .groups()
        .map(|group| group.id())
        .collect::<Vec<_>>();
    for group_id in group_ids {
        if let Some(group) = database.group_mut(group_id) {
            group.remove();
        }
    }

    {
        let mut root_group = database.root_mut();
        apply_group_fields(&mut root_group, root);
    }

    let attachment_by_id = proto_database
        .attachments
        .iter()
        .map(|attachment| (attachment.id, attachment))
        .collect::<HashMap<_, _>>();

    for child in &root.groups {
        add_group_recursive(database, database.root().id(), child, &attachment_by_id)?;
    }
    for entry in &root.entries {
        add_entry(database, database.root().id(), entry, &attachment_by_id)?;
    }

    Ok(())
}

fn apply_meta(meta: &mut Meta, proto: &proto::DatabaseMeta) {
    meta.generator = proto.generator.clone();
    meta.database_name = proto.database_name.clone();
    meta.database_name_changed = proto
        .database_name_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    meta.database_description = proto.database_description.clone();
    meta.database_description_changed = proto
        .database_description_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    meta.default_username = proto.default_username.clone();
    meta.default_username_changed = proto
        .default_username_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    meta.maintenance_history_days = proto.maintenance_history_days.map(|value| value as usize);
    meta.recyclebin_enabled = proto.recycle_bin_enabled;
    meta.recyclebin_uuid = proto
        .recycle_bin_uuid
        .as_ref()
        .and_then(|bytes| uuid_from_bytes(bytes));
    meta.recyclebin_changed = proto
        .recycle_bin_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    meta.entry_templates_group = proto
        .entry_templates_group_uuid
        .as_ref()
        .and_then(|bytes| uuid_from_bytes(bytes));
    meta.entry_templates_group_changed = proto
        .entry_templates_group_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    meta.last_selected_group = proto
        .last_selected_group_uuid
        .as_ref()
        .and_then(|bytes| uuid_from_bytes(bytes));
    meta.last_top_visible_group = proto
        .last_top_visible_group_uuid
        .as_ref()
        .and_then(|bytes| uuid_from_bytes(bytes));
    meta.history_max_items = proto.history_max_items.map(|value| value as isize);
    meta.history_max_size = proto.history_max_size.map(|value| value as isize);
    meta.settings_changed = proto.settings_changed_epoch_ms.and_then(epoch_ms_to_time);
}

fn add_group_recursive(
    database: &mut Database,
    parent_id: GroupId,
    proto_group: &proto::Group,
    attachments: &HashMap<u32, &proto::Attachment>,
) -> Result<(), String> {
    let group_id = GroupId::from(uuid_from_bytes(&proto_group.uuid).ok_or("invalid group uuid")?);
    {
        let mut parent = database
            .group_mut(parent_id)
            .ok_or_else(|| "missing parent group".to_string())?;
        let mut group = parent
            .add_group_with_id(group_id)
            .map_err(|error| error.to_string())?;
        apply_group_fields(&mut group, proto_group);
    }

    for child in &proto_group.groups {
        add_group_recursive(database, group_id, child, attachments)?;
    }
    for entry in &proto_group.entries {
        add_entry(database, group_id, entry, attachments)?;
    }

    Ok(())
}

fn apply_group_fields(group: &mut keepass::db::GroupMut<'_>, proto_group: &proto::Group) {
    group.name = proto_group.name.clone();
    group.notes = proto_group.notes.clone();
    group.tags = proto_group.tags.clone();
    group.times = proto_group
        .times
        .as_ref()
        .map(convert_proto_times)
        .unwrap_or_default();
    group.is_expanded = proto_group.is_expanded;
    group.default_autotype_sequence = proto_group.default_autotype_sequence.clone();
    group.enable_autotype = proto_group.enable_autotype;
    group.enable_searching = proto_group.enable_searching;
}

fn add_entry(
    database: &mut Database,
    parent_id: GroupId,
    proto_entry: &proto::Entry,
    attachments: &HashMap<u32, &proto::Attachment>,
) -> Result<(), String> {
    let entry_id = EntryId::from(uuid_from_bytes(&proto_entry.uuid).ok_or("invalid entry uuid")?);
    let mut parent = database
        .group_mut(parent_id)
        .ok_or_else(|| "missing parent group".to_string())?;
    let mut entry = parent
        .add_entry_with_id(entry_id)
        .map_err(|error| error.to_string())?;

    entry.fields.clear();
    for field in &proto_entry.fields {
        let value = if field.is_protected {
            Value::protected(field.value.clone())
        } else {
            Value::unprotected(field.value.clone())
        };
        entry.set(field.name.clone(), value);
    }

    entry.tags = proto_entry.tags.clone();
    entry.times = proto_entry
        .times
        .as_ref()
        .map(convert_proto_times)
        .unwrap_or_default();
    entry.foreground_color = proto_entry
        .foreground_color
        .as_ref()
        .map(convert_proto_color);
    entry.background_color = proto_entry
        .background_color
        .as_ref()
        .map(convert_proto_color);
    entry.override_url = proto_entry.override_url.clone();
    entry.quality_check = proto_entry.quality_check;

    for attachment_ref in &proto_entry.attachments {
        if let Some(attachment) = attachments.get(&attachment_ref.attachment_id) {
            let data = if attachment.is_protected {
                Value::protected(attachment.data.clone())
            } else {
                Value::unprotected(attachment.data.clone())
            };
            entry.add_attachment(attachment_ref.name.clone(), data);
        }
    }

    Ok(())
}

fn convert_proto_times(times: &proto::Times) -> Times {
    let mut result = Times::default();
    result.creation = times.creation_epoch_ms.and_then(epoch_ms_to_time);
    result.last_modification = times.last_modification_epoch_ms.and_then(epoch_ms_to_time);
    result.last_access = times.last_access_epoch_ms.and_then(epoch_ms_to_time);
    result.expiry = times.expiry_epoch_ms.and_then(epoch_ms_to_time);
    result.location_changed = times.location_changed_epoch_ms.and_then(epoch_ms_to_time);
    result.expires = times.expires;
    result.usage_count = times.usage_count.map(|value| value as usize);
    result
}

fn convert_proto_color(color: &proto::Color) -> Color {
    Color {
        r: color.red as u8,
        g: color.green as u8,
        b: color.blue as u8,
    }
}

fn uuid_from_bytes(bytes: &[u8]) -> Option<uuid::Uuid> {
    uuid::Uuid::from_slice(bytes).ok()
}

fn epoch_ms_to_time(epoch_ms: i64) -> Option<chrono::NaiveDateTime> {
    chrono::DateTime::from_timestamp_millis(epoch_ms).map(|time| time.naive_utc())
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

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_domain_rust_RustBridge_nativeWriteDatabaseWithPassword(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    original_database_data: JByteArray<'_>,
    write_request_data: JByteArray<'_>,
    password: JString<'_>,
) -> jbyteArray {
    let original_bytes = get_jni_byte_array(&env, original_database_data);
    let request_bytes = get_jni_byte_array(&env, write_request_data);
    let password = get_jni_string(&mut env, password);

    match (original_bytes, request_bytes, password) {
        (Some(original_bytes), Some(request_bytes), Some(password)) => {
            write_database_with_password(&original_bytes, &request_bytes, &password)
                .map(|response| new_jni_byte_array(&env, &response))
                .unwrap_or_else(ptr::null_mut)
        }
        _ => ptr::null_mut(),
    }
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_ivanovsky_passnotes_domain_rust_RustBridge_nativeWriteDatabaseWithPasswords(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    original_database_data: JByteArray<'_>,
    write_request_data: JByteArray<'_>,
    old_password: JString<'_>,
    new_password: JString<'_>,
) -> jbyteArray {
    let original_bytes = get_jni_byte_array(&env, original_database_data);
    let request_bytes = get_jni_byte_array(&env, write_request_data);
    let old_password = get_jni_string(&mut env, old_password);
    let new_password = get_jni_string(&mut env, new_password);

    match (original_bytes, request_bytes, old_password, new_password) {
        (Some(original_bytes), Some(request_bytes), Some(old_password), Some(new_password)) => {
            write_database_with_passwords(
                &original_bytes,
                &request_bytes,
                &old_password,
                &new_password,
            )
            .map(|response| new_jni_byte_array(&env, &response))
            .unwrap_or_else(ptr::null_mut)
        }
        _ => ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::{add, proto, read_database_with_password, write_database_with_password};
    use keepass::{Database, DatabaseKey};
    use prost::Message;

    #[test]
    fn should_add_numbers() {
        assert_eq!(add(20, 22), 42)
    }

    #[test]
    fn should_not_decode_invalid_database() {
        assert!(read_database_with_password(b"not-a-database", "abc123").is_none());
    }

    #[test]
    fn should_write_database() {
        let password = "testing";
        let mut original = Vec::new();
        Database::new()
            .save(&mut original, DatabaseKey::new().with_password(password))
            .unwrap();

        let response = read_database_with_password(&original, password).unwrap();
        let database = proto::ReadDatabaseResponse::decode(response.as_slice())
            .unwrap()
            .database
            .unwrap();
        let request = proto::WriteDatabaseRequest {
            database: Some(database),
        };

        let updated = write_database_with_password(&original, &request.encode_to_vec(), password)
            .expect("database should be written");

        assert!(read_database_with_password(&updated, password).is_some());
    }
}
