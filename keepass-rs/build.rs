use std::{env, path::PathBuf};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let proto_root = PathBuf::from("proto");
    let proto_file = proto_root.join("keepassvault/v1/database.proto");
    let protoc = protoc_bin_vendored::protoc_bin_path()?;

    env::set_var("PROTOC", protoc);
    prost_build::compile_protos(&[proto_file], &[proto_root])?;

    Ok(())
}
