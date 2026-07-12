use crate::error::BridgeError;
use crate::proto;
use keepass::config::{
    CompressionConfig, DatabaseConfig, DatabaseVersion, InnerCipherConfig, KdfConfig,
    OuterCipherConfig, VariantDictionary, VariantDictionaryValue,
};
use keepass::db::{
    AttachmentRef, AutoType, AutoTypeAssociation, Color, CustomDataItem, CustomDataValue,
    CustomIconId, CustomIconRef, DataTransferObfuscation, Entry, EntryId, EntryMut, EntryRef,
    GroupId, GroupMut, GroupRef, History, Icon, MemoryProtection, Meta, Times, Value,
};
use keepass::{Database, DatabaseKey};
use std::collections::HashMap;
use std::io::Cursor;

pub fn convert_database_to_proto(database: &Database) -> proto::Database {
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

pub fn convert_proto_key(key: proto::DatabaseKey) -> Result<DatabaseKey, BridgeError> {
    let mut database_key = DatabaseKey::new();

    if let Some(password) = key.password {
        database_key = database_key.with_password(&password);
    }
    if let Some(key_bytes) = key.key_bytes {
        database_key = database_key.with_keyfile(&mut Cursor::new(key_bytes))?;
    }

    Ok(database_key)
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
            DataTransferObfuscation::None => proto::DataTransferObfuscation::None as i32,
            DataTransferObfuscation::UseClipboard => {
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

fn epoch_ms_to_time(epoch_ms: i64) -> Option<chrono::NaiveDateTime> {
    chrono::DateTime::from_timestamp_millis(epoch_ms).map(|time| time.naive_utc())
}

fn optional_time_to_epoch_ms(time: Option<chrono::NaiveDateTime>) -> Option<i64> {
    time.map(|value| value.and_utc().timestamp_millis())
}

fn uuid_to_bytes(uuid: &uuid::Uuid) -> Vec<u8> {
    uuid.as_bytes().to_vec()
}

pub fn convert_proto_database(proto: proto::Database) -> Result<Database, BridgeError> {
    let config = proto
        .config
        .as_ref()
        .map(convert_proto_config)
        .transpose()?
        .unwrap_or_default();

    let root_group = proto
        .root_group
        .as_ref()
        .ok_or_else(|| protobuf_error("missing root group"))?;
    let root_id = uuid_from_bytes_required(&root_group.uuid)?;

    let mut database = Database::new_with_root_id(GroupId::from_uuid(root_id));
    database.config = config;

    if let Some(meta) = proto.meta.as_ref() {
        apply_meta(&mut database, meta)?;
    }

    for deleted_object in &proto.deleted_objects {
        database.deleted_objects.insert(
            uuid_from_bytes_required(&deleted_object.uuid)?,
            deleted_object
                .deletion_time_epoch_ms
                .and_then(epoch_ms_to_time),
        );
    }

    let attachment_by_id = proto
        .attachments
        .iter()
        .map(|attachment| (attachment.id, attachment))
        .collect::<HashMap<_, _>>();
    let custom_icon_by_uuid = proto
        .custom_icons
        .iter()
        .map(|icon| (icon.uuid.clone(), icon))
        .collect::<HashMap<_, _>>();
    let mut custom_icon_id_by_uuid = HashMap::new();

    {
        let mut root = database.root_mut();
        apply_group_fields(
            &mut root,
            root_group,
            &custom_icon_by_uuid,
            &mut custom_icon_id_by_uuid,
        )?;
    }

    let root_id = database.root().id();
    for child in &root_group.groups {
        add_group(
            &mut database,
            root_id,
            child,
            &attachment_by_id,
            &custom_icon_by_uuid,
            &mut custom_icon_id_by_uuid,
        )?;
    }
    for entry in &root_group.entries {
        add_entry(
            &mut database,
            root_id,
            entry,
            &attachment_by_id,
            &custom_icon_by_uuid,
            &mut custom_icon_id_by_uuid,
        )?;
    }

    Ok(database)
}

fn convert_proto_config(proto: &proto::DatabaseConfig) -> Result<DatabaseConfig, BridgeError> {
    let mut config = DatabaseConfig::default();

    if let Some(version) = proto.version.as_ref() {
        config.version = convert_proto_version(version)?;
    }
    config.outer_cipher_config = match proto::OuterCipher::try_from(proto.outer_cipher).ok() {
        Some(proto::OuterCipher::Aes256) => OuterCipherConfig::AES256,
        Some(proto::OuterCipher::Twofish) => OuterCipherConfig::Twofish,
        Some(proto::OuterCipher::Chacha20) => OuterCipherConfig::ChaCha20,
        _ => config.outer_cipher_config,
    };
    config.compression_config = match proto::Compression::try_from(proto.compression).ok() {
        Some(proto::Compression::None) => CompressionConfig::None,
        Some(proto::Compression::Gzip) => CompressionConfig::GZip,
        _ => config.compression_config,
    };
    config.inner_cipher_config = match proto::InnerCipher::try_from(proto.inner_cipher).ok() {
        Some(proto::InnerCipher::Plain) => InnerCipherConfig::Plain,
        Some(proto::InnerCipher::Salsa20) => InnerCipherConfig::Salsa20,
        Some(proto::InnerCipher::Chacha20) => InnerCipherConfig::ChaCha20,
        _ => config.inner_cipher_config,
    };

    if let Some(kdf) = proto
        .kdf
        .as_ref()
        .map(convert_proto_kdf)
        .transpose()?
        .flatten()
    {
        config.kdf_config = kdf;
    }

    if !proto.public_custom_data.is_empty() {
        let mut public_custom_data = VariantDictionary::new();
        for item in &proto.public_custom_data {
            if let Some(value) = item.value.as_ref().and_then(convert_proto_variant_value) {
                public_custom_data.insert(item.key.clone(), value);
            }
        }
        config.public_custom_data = Some(public_custom_data);
    }

    Ok(config)
}

fn convert_proto_version(proto: &proto::DatabaseVersion) -> Result<DatabaseVersion, BridgeError> {
    let minor = u16::try_from(proto.minor.unwrap_or_default())
        .map_err(|_| protobuf_error("database version minor value is out of range"))?;

    let version = match proto::DatabaseFormat::try_from(proto.format).ok() {
        Some(proto::DatabaseFormat::Kdb) => DatabaseVersion::KDB(minor),
        Some(proto::DatabaseFormat::Kdbx2) => DatabaseVersion::KDB2(minor),
        Some(proto::DatabaseFormat::Kdbx3) => DatabaseVersion::KDB3(minor),
        Some(proto::DatabaseFormat::Kdbx4) => DatabaseVersion::KDB4(minor),
        _ => DatabaseConfig::default().version,
    };

    Ok(version)
}

fn convert_proto_kdf(proto: &proto::KdfConfig) -> Result<Option<KdfConfig>, BridgeError> {
    let Some(value) = proto.value.as_ref() else {
        return Ok(None);
    };
    let result = match value {
        proto::kdf_config::Value::Aes(value) => KdfConfig::Aes {
            rounds: value.rounds,
        },
        proto::kdf_config::Value::Argon2(value) => KdfConfig::Argon2 {
            iterations: value.iterations,
            memory: value.memory_kib,
            parallelism: value.parallelism,
            version: convert_argon2_version(value.version)?,
        },
        proto::kdf_config::Value::Argon2id(value) => KdfConfig::Argon2id {
            iterations: value.iterations,
            memory: value.memory_kib,
            parallelism: value.parallelism,
            version: convert_argon2_version(value.version)?,
        },
    };

    Ok(Some(result))
}

fn convert_argon2_version(version: u32) -> Result<argon2::Version, BridgeError> {
    match version {
        0x10 => Ok(argon2::Version::Version10),
        0x13 => Ok(argon2::Version::Version13),
        _ => Err(protobuf_error("invalid argon2 version")),
    }
}

fn convert_proto_variant_value(
    proto: &proto::VariantDictionaryValue,
) -> Option<VariantDictionaryValue> {
    match proto.value.as_ref()? {
        proto::variant_dictionary_value::Value::Uint32Value(value) => {
            Some(VariantDictionaryValue::UInt32(*value))
        }
        proto::variant_dictionary_value::Value::Uint64Value(value) => {
            Some(VariantDictionaryValue::UInt64(*value))
        }
        proto::variant_dictionary_value::Value::Int32Value(value) => {
            Some(VariantDictionaryValue::Int32(*value))
        }
        proto::variant_dictionary_value::Value::Int64Value(value) => {
            Some(VariantDictionaryValue::Int64(*value))
        }
        proto::variant_dictionary_value::Value::BoolValue(value) => {
            Some(VariantDictionaryValue::Bool(*value))
        }
        proto::variant_dictionary_value::Value::StringValue(value) => {
            Some(VariantDictionaryValue::String(value.clone()))
        }
        proto::variant_dictionary_value::Value::BytesValue(value) => {
            Some(VariantDictionaryValue::ByteArray(value.clone()))
        }
    }
}

fn apply_meta(database: &mut Database, proto: &proto::DatabaseMeta) -> Result<(), BridgeError> {
    database.meta.generator = proto.generator.clone();
    database.meta.database_name = proto.database_name.clone();
    database.meta.database_name_changed = proto
        .database_name_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    database.meta.database_description = proto.database_description.clone();
    database.meta.database_description_changed = proto
        .database_description_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    database.meta.default_username = proto.default_username.clone();
    database.meta.default_username_changed = proto
        .default_username_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    database.meta.maintenance_history_days =
        proto.maintenance_history_days.map(|value| value as usize);
    database.meta.color = proto.color.as_ref().map(convert_proto_color);
    database.meta.master_key_changed = proto.master_key_changed_epoch_ms.and_then(epoch_ms_to_time);
    database.meta.master_key_change_rec =
        proto.master_key_change_rec_days.map(|value| value as isize);
    database.meta.master_key_change_force = proto
        .master_key_change_force_days
        .map(|value| value as isize);
    database.meta.memory_protection =
        proto
            .memory_protection
            .as_ref()
            .map(|protection| MemoryProtection {
                protect_title: protection.protect_title,
                protect_username: protection.protect_username,
                protect_password: protection.protect_password,
                protect_url: protection.protect_url,
                protect_notes: protection.protect_notes,
            });
    database.meta.recyclebin_enabled = proto.recycle_bin_enabled;
    database.meta.recyclebin_uuid = optional_uuid_from_bytes(proto.recycle_bin_uuid.as_deref())?;
    database.meta.recyclebin_changed = proto
        .recycle_bin_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    database.meta.entry_templates_group =
        optional_uuid_from_bytes(proto.entry_templates_group_uuid.as_deref())?;
    database.meta.entry_templates_group_changed = proto
        .entry_templates_group_changed_epoch_ms
        .and_then(epoch_ms_to_time);
    database.meta.last_selected_group =
        optional_uuid_from_bytes(proto.last_selected_group_uuid.as_deref())?;
    database.meta.last_top_visible_group =
        optional_uuid_from_bytes(proto.last_top_visible_group_uuid.as_deref())?;
    database.meta.history_max_items = proto.history_max_items.map(|value| value as isize);
    database.meta.history_max_size = proto.history_max_size.map(|value| value as isize);
    database.meta.settings_changed = proto.settings_changed_epoch_ms.and_then(epoch_ms_to_time);
    database.meta.custom_data = convert_proto_custom_data(&proto.custom_data);

    Ok(())
}

fn add_group(
    database: &mut Database,
    parent_id: GroupId,
    proto: &proto::Group,
    attachment_by_id: &HashMap<u32, &proto::Attachment>,
    custom_icon_by_uuid: &HashMap<Vec<u8>, &proto::CustomIcon>,
    custom_icon_id_by_uuid: &mut HashMap<Vec<u8>, CustomIconId>,
) -> Result<(), BridgeError> {
    let group_id = GroupId::from(uuid_from_bytes_required(&proto.uuid)?);

    {
        let mut parent = database
            .group_mut(parent_id)
            .ok_or_else(|| protobuf_error("missing parent group"))?;
        let mut group =
            parent
                .add_group_with_id(group_id)
                .map_err(|error| BridgeError::ProtobufFormat {
                    message: error.to_string(),
                })?;
        apply_group_fields(
            &mut group,
            proto,
            custom_icon_by_uuid,
            custom_icon_id_by_uuid,
        )?;
    }

    for child in &proto.groups {
        add_group(
            database,
            group_id,
            child,
            attachment_by_id,
            custom_icon_by_uuid,
            custom_icon_id_by_uuid,
        )?;
    }
    for entry in &proto.entries {
        add_entry(
            database,
            group_id,
            entry,
            attachment_by_id,
            custom_icon_by_uuid,
            custom_icon_id_by_uuid,
        )?;
    }

    Ok(())
}

fn apply_group_fields(
    group: &mut GroupMut<'_>,
    proto: &proto::Group,
    custom_icon_by_uuid: &HashMap<Vec<u8>, &proto::CustomIcon>,
    custom_icon_id_by_uuid: &mut HashMap<Vec<u8>, CustomIconId>,
) -> Result<(), BridgeError> {
    group.name = proto.name.clone();
    group.notes = proto.notes.clone();
    group.tags = proto.tags.clone();
    group.times = proto
        .times
        .as_ref()
        .map(convert_proto_times)
        .unwrap_or_default();
    group.custom_data = convert_proto_custom_data(&proto.custom_data);
    group.is_expanded = proto.is_expanded;
    group.default_autotype_sequence = proto.default_autotype_sequence.clone();
    group.enable_autotype = proto.enable_autotype;
    group.enable_searching = proto.enable_searching;
    apply_group_icon(
        group,
        proto.icon.as_ref(),
        custom_icon_by_uuid,
        custom_icon_id_by_uuid,
    )?;

    Ok(())
}

fn add_entry(
    database: &mut Database,
    parent_id: GroupId,
    proto: &proto::Entry,
    attachment_by_id: &HashMap<u32, &proto::Attachment>,
    custom_icon_by_uuid: &HashMap<Vec<u8>, &proto::CustomIcon>,
    custom_icon_id_by_uuid: &mut HashMap<Vec<u8>, CustomIconId>,
) -> Result<(), BridgeError> {
    let entry_id = EntryId::from(uuid_from_bytes_required(&proto.uuid)?);
    let mut parent = database
        .group_mut(parent_id)
        .ok_or_else(|| protobuf_error("missing parent group"))?;
    let mut entry =
        parent
            .add_entry_with_id(entry_id)
            .map_err(|error| BridgeError::ProtobufFormat {
                message: error.to_string(),
            })?;

    apply_entry_fields(
        &mut entry,
        proto,
        attachment_by_id,
        custom_icon_by_uuid,
        custom_icon_id_by_uuid,
    )?;

    Ok(())
}

fn apply_entry_fields(
    entry: &mut EntryMut<'_>,
    proto: &proto::Entry,
    attachment_by_id: &HashMap<u32, &proto::Attachment>,
    custom_icon_by_uuid: &HashMap<Vec<u8>, &proto::CustomIcon>,
    custom_icon_id_by_uuid: &mut HashMap<Vec<u8>, CustomIconId>,
) -> Result<(), BridgeError> {
    apply_entry_public_fields(entry, proto);
    apply_entry_icon(
        entry,
        proto.icon.as_ref(),
        custom_icon_by_uuid,
        custom_icon_id_by_uuid,
    )?;

    if !proto.history.is_empty() {
        let mut historical_entries = Vec::new();
        for historical in proto.history.iter().rev() {
            let entry_ref = entry.as_ref();
            let mut historical_entry = (*entry_ref).clone();
            historical_entry.history = None;
            apply_historical_entry_public_fields(&mut historical_entry, historical);
            historical_entries.push(historical_entry);
        }

        let history = entry.history.get_or_insert_with(History::default);
        for historical_entry in historical_entries {
            history.add_entry(historical_entry);
        }
    }

    for attachment_ref in &proto.attachments {
        if let Some(attachment) = attachment_by_id.get(&attachment_ref.attachment_id) {
            entry.add_attachment(
                attachment_ref.name.clone(),
                convert_proto_bytes_value(&attachment.data, attachment.is_protected),
            );
        }
    }

    Ok(())
}

fn apply_entry_public_fields(entry: &mut Entry, proto: &proto::Entry) {
    entry.fields.clear();
    for field in &proto.fields {
        entry.set(
            field.name.clone(),
            convert_proto_string_value(&field.value, field.is_protected),
        );
    }

    entry.autotype = proto.autotype.as_ref().map(convert_proto_autotype);
    entry.tags = proto.tags.clone();
    entry.times = proto
        .times
        .as_ref()
        .map(convert_proto_times)
        .unwrap_or_default();
    entry.custom_data = convert_proto_custom_data(&proto.custom_data);
    entry.foreground_color = proto.foreground_color.as_ref().map(convert_proto_color);
    entry.background_color = proto.background_color.as_ref().map(convert_proto_color);
    entry.override_url = proto.override_url.clone();
    entry.quality_check = proto.quality_check;
}

fn apply_historical_entry_public_fields(entry: &mut Entry, proto: &proto::Entry) {
    apply_entry_public_fields(entry, proto);
}

fn apply_group_icon(
    group: &mut GroupMut<'_>,
    icon: Option<&proto::Icon>,
    custom_icon_by_uuid: &HashMap<Vec<u8>, &proto::CustomIcon>,
    custom_icon_id_by_uuid: &mut HashMap<Vec<u8>, CustomIconId>,
) -> Result<(), BridgeError> {
    match icon.and_then(|icon| icon.value.as_ref()) {
        Some(proto::icon::Value::BuiltinId(id)) => group.set_icon_builtin(*id as usize),
        Some(proto::icon::Value::CustomIconUuid(uuid)) => {
            let Some(custom_icon_id) = get_or_create_group_custom_icon(
                group,
                uuid,
                custom_icon_by_uuid,
                custom_icon_id_by_uuid,
            ) else {
                return Ok(());
            };
            group
                .set_icon_custom(custom_icon_id)
                .map_err(|error| BridgeError::ProtobufFormat {
                    message: error.to_string(),
                })?;
        }
        None => group.set_icon_none(),
    }

    Ok(())
}

fn get_or_create_group_custom_icon(
    group: &mut GroupMut<'_>,
    uuid: &[u8],
    custom_icon_by_uuid: &HashMap<Vec<u8>, &proto::CustomIcon>,
    custom_icon_id_by_uuid: &mut HashMap<Vec<u8>, CustomIconId>,
) -> Option<CustomIconId> {
    if let Some(id) = custom_icon_id_by_uuid.get(uuid) {
        return Some(*id);
    }

    let proto_icon = custom_icon_by_uuid.get(uuid)?;
    let mut icon = group.set_icon_custom_new(proto_icon.data.clone());
    icon.name = proto_icon.name.clone();
    icon.last_modification_time = proto_icon
        .last_modification_time_epoch_ms
        .and_then(epoch_ms_to_time);
    let id = icon.id();
    custom_icon_id_by_uuid.insert(uuid.to_vec(), id);
    Some(id)
}

fn apply_entry_icon(
    entry: &mut EntryMut<'_>,
    icon: Option<&proto::Icon>,
    custom_icon_by_uuid: &HashMap<Vec<u8>, &proto::CustomIcon>,
    custom_icon_id_by_uuid: &mut HashMap<Vec<u8>, CustomIconId>,
) -> Result<(), BridgeError> {
    match icon.and_then(|icon| icon.value.as_ref()) {
        Some(proto::icon::Value::BuiltinId(id)) => entry.set_icon_builtin(*id as usize),
        Some(proto::icon::Value::CustomIconUuid(uuid)) => {
            let Some(custom_icon_id) = get_or_create_entry_custom_icon(
                entry,
                uuid,
                custom_icon_by_uuid,
                custom_icon_id_by_uuid,
            ) else {
                return Ok(());
            };
            entry
                .set_icon_custom(custom_icon_id)
                .map_err(|error| BridgeError::ProtobufFormat {
                    message: error.to_string(),
                })?;
        }
        None => entry.set_icon_none(),
    }

    Ok(())
}

fn get_or_create_entry_custom_icon(
    entry: &mut EntryMut<'_>,
    uuid: &[u8],
    custom_icon_by_uuid: &HashMap<Vec<u8>, &proto::CustomIcon>,
    custom_icon_id_by_uuid: &mut HashMap<Vec<u8>, CustomIconId>,
) -> Option<CustomIconId> {
    if let Some(id) = custom_icon_id_by_uuid.get(uuid) {
        return Some(*id);
    }

    let proto_icon = custom_icon_by_uuid.get(uuid)?;
    let mut icon = entry.set_icon_custom_new(proto_icon.data.clone());
    icon.name = proto_icon.name.clone();
    icon.last_modification_time = proto_icon
        .last_modification_time_epoch_ms
        .and_then(epoch_ms_to_time);
    let id = icon.id();
    custom_icon_id_by_uuid.insert(uuid.to_vec(), id);
    Some(id)
}

fn convert_proto_autotype(proto: &proto::AutoType) -> AutoType {
    AutoType {
        enabled: proto.enabled,
        default_sequence: proto.default_sequence.clone(),
        data_transfer_obfuscation: match proto::DataTransferObfuscation::try_from(
            proto.data_transfer_obfuscation,
        )
        .ok()
        {
            Some(proto::DataTransferObfuscation::UseClipboard) => {
                DataTransferObfuscation::UseClipboard
            }
            _ => DataTransferObfuscation::None,
        },
        associations: proto
            .associations
            .iter()
            .map(|association| AutoTypeAssociation {
                window: association.window.clone(),
                sequence: association.sequence.clone(),
            })
            .collect(),
    }
}

fn convert_proto_custom_data(items: &[proto::CustomDataItem]) -> HashMap<String, CustomDataItem> {
    items
        .iter()
        .map(|item| {
            (
                item.key.clone(),
                CustomDataItem {
                    value: item
                        .value
                        .as_ref()
                        .and_then(convert_proto_custom_data_value),
                    last_modification_time: item
                        .last_modification_time_epoch_ms
                        .and_then(epoch_ms_to_time),
                },
            )
        })
        .collect()
}

fn convert_proto_custom_data_value(proto: &proto::CustomDataValue) -> Option<CustomDataValue> {
    match proto.value.as_ref()? {
        proto::custom_data_value::Value::StringValue(value) => {
            Some(CustomDataValue::String(value.clone()))
        }
        proto::custom_data_value::Value::BinaryValue(value) => {
            Some(CustomDataValue::Binary(value.clone()))
        }
    }
}

fn convert_proto_string_value(value: &str, is_protected: bool) -> Value<String> {
    if is_protected {
        Value::protected(value.to_owned())
    } else {
        Value::unprotected(value.to_owned())
    }
}

fn convert_proto_bytes_value(value: &[u8], is_protected: bool) -> Value<Vec<u8>> {
    if is_protected {
        Value::protected(value.to_vec())
    } else {
        Value::unprotected(value.to_vec())
    }
}

fn optional_uuid_from_bytes(bytes: Option<&[u8]>) -> Result<Option<uuid::Uuid>, BridgeError> {
    bytes.map(uuid_from_bytes_required).transpose()
}

fn uuid_from_bytes_required(bytes: &[u8]) -> Result<uuid::Uuid, BridgeError> {
    uuid::Uuid::from_slice(bytes).map_err(|_| protobuf_error("invalid uuid bytes"))
}

fn protobuf_error(message: &str) -> BridgeError {
    BridgeError::ProtobufFormat {
        message: message.to_string(),
    }
}
