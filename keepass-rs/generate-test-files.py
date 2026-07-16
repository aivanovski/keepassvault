#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.10"
# dependencies = [
#   "pykeepass>=4.1,<5",
# ]
# ///

"""
Generate KeePass test databases with human-readable fixture data.

Usage:
  keepass-rs/generate-test-files.py

"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from io import BytesIO
from pathlib import Path

from construct import Container
from pykeepass.kdbx_parsing.kdbx4 import kdf_uuids
from pykeepass.pykeepass import (
    BLANK_DATABASE_LOCATION,
    BLANK_DATABASE_PASSWORD,
    PyKeePass,
)

DEFAULT_OUTPUT_DIR = Path(__file__).resolve().parent / "tests/resources"
DATABASE_PASSWORD = "abc123"
DATABASE_KEY_CONTENT = "def456"
FIXTURE_TIME = datetime(2024, 1, 1, 12, 0, 0, tzinfo=timezone.utc)
EXPIRY_TIME = datetime(2024, 6, 1, 12, 0, 0, tzinfo=timezone.utc)
AES_KDF_ROUNDS = 6000
AES_KDF_SEED = bytes.fromhex(
    "7839e86543e0449b3d6cc46b95b4bbab"
    "7d519c7eb01d9cf59d64d82f6ab1a15f"
)


@dataclass(frozen=True)
class CustomFieldSpec:
    key: str
    value: str
    protect: bool = False


@dataclass(frozen=True)
class AttachmentSpec:
    name: str
    content: bytes
    protect: bool = True


@dataclass(frozen=True)
class EntryHistorySpec:
    title: str
    username: str
    password: str
    url: str
    notes: str


@dataclass(frozen=True)
class EntrySpec:
    title: str
    username: str
    password: str
    url: str
    notes: str
    custom_fields: tuple[CustomFieldSpec, ...] = ()
    attachments: tuple[AttachmentSpec, ...] = ()
    history: tuple[EntryHistorySpec, ...] = ()
    expiry_time: datetime | None = None


@dataclass(frozen=True)
class GroupSpec:
    name: str
    entries: tuple[EntrySpec, ...]


def main() -> None:
    output_dir = DEFAULT_OUTPUT_DIR

    output_dir.mkdir(parents=True, exist_ok=True)
    groups = generate_database_content()

    write_database(
        output=output_dir / "test-password.kdbx",
        password=DATABASE_PASSWORD,
        key_content=None,
        groups=groups,
    )
    write_database(
        output=output_dir / "test-key-file.kdbx",
        password=None,
        key_content=DATABASE_KEY_CONTENT,
        groups=groups,
    )
    write_database(
        output=output_dir / "test-password-key-file.kdbx",
        password=DATABASE_PASSWORD,
        key_content=DATABASE_KEY_CONTENT,
        groups=groups,
    )

    print(f"Generated KeePass test databases in {output_dir.resolve()}")
    print(f"Password: {DATABASE_PASSWORD}")
    print(f"Key content: {DATABASE_KEY_CONTENT}")


def write_database(
    output: Path,
    password: str | None,
    key_content: str | None,
    groups: tuple[GroupSpec, ...],
) -> None:
    if output.exists():
        output.unlink()

    database = create_aes_kdf_database(output, password, key_content)
    database.root_group.name = "Test Database"
    set_fixed_times(database.root_group)

    for group_spec in groups:
        group = database.add_group(database.root_group, group_spec.name)
        set_fixed_times(group)

        for entry_spec in group_spec.entries:
            entry = database.add_entry(
                group,
                entry_spec.title,
                entry_spec.username,
                entry_spec.password,
                entry_spec.url,
                entry_spec.notes,
            )
            set_fixed_times(entry)

            for history_spec in entry_spec.history:
                apply_history_spec(entry, history_spec)
                entry.save_history()

            apply_entry_spec(entry, entry_spec)

            for custom_field in entry_spec.custom_fields:
                entry.set_custom_property(
                    custom_field.key,
                    custom_field.value,
                    protect=custom_field.protect,
                )

            for attachment in entry_spec.attachments:
                binary_id = database.add_binary(
                    attachment.content,
                    protected=attachment.protect,
                )
                entry.add_attachment(binary_id, attachment.name)

    database.save()


def create_aes_kdf_database(
    output: Path,
    password: str | None,
    key_content: str | None,
) -> PyKeePass:
    database = PyKeePass(BLANK_DATABASE_LOCATION, BLANK_DATABASE_PASSWORD)
    database.filename = str(output)
    database.password = password
    database.keyfile = key_content_to_keyfile(key_content)
    configure_aes_kdf(database)

    return database


def key_content_to_keyfile(key_content: str | None) -> BytesIO | None:
    if key_content is None:
        return None

    return BytesIO(key_content.encode("utf-8"))


def configure_aes_kdf(database: PyKeePass) -> None:
    kdf_parameters = database.kdbx.header.value.dynamic_header.kdf_parameters.data
    kdf_parameters.dict = Container(
        {
            "$UUID": Container(
                type=0x42,
                key="$UUID",
                value=kdf_uuids["aeskdf"],
                next_byte=0x05,
            ),
            "R": Container(
                type=0x05,
                key="R",
                value=AES_KDF_ROUNDS,
                next_byte=0x42,
            ),
            "S": Container(
                type=0x42,
                key="S",
                value=AES_KDF_SEED,
                next_byte=0x00,
            ),
        }
    )
    del database.kdbx.header["data"]


def set_fixed_times(element) -> None:
    element.ctime = FIXTURE_TIME
    element.mtime = FIXTURE_TIME
    element.atime = FIXTURE_TIME


def apply_history_spec(entry, history: EntryHistorySpec) -> None:
    entry.title = history.title
    entry.username = history.username
    entry.password = history.password
    entry.url = history.url
    entry.notes = history.notes
    set_fixed_times(entry)


def apply_entry_spec(entry, spec: EntrySpec) -> None:
    entry.title = spec.title
    entry.username = spec.username
    entry.password = spec.password
    entry.url = spec.url
    entry.notes = spec.notes
    set_fixed_times(entry)
    if spec.expiry_time is not None:
        entry.expires = True
        entry.expiry_time = spec.expiry_time


def generate_database_content() -> tuple[GroupSpec, ...]:
    return (
        GroupSpec(
            name="Work",
            entries=(
                entry(
                    title="GitHub",
                    username="john.doe.dev@example.test",
                    password="abc123",
                    url="https://github.example.test/login",
                    notes="Fake source control account.",
                    custom_fields=(
                        CustomFieldSpec(
                            key="RecoveryEmail",
                            value="john.doe.recovery@example.test",
                        ),
                        CustomFieldSpec(
                            key="ApiToken",
                            value="ghp_fake_test_token",
                            protect=True,
                        ),
                    ),
                    attachments=(
                        AttachmentSpec(
                            name="github-recovery-codes.txt",
                            content=b"1111-2222\n3333-4444\n5555-6666\n",
                            protect=True,
                        ),
                    ),
                    history=(
                        EntryHistorySpec(
                            title="GitHub",
                            username="john.doe.dev@example.test",
                            password="old-password-001",
                            url="https://github.example.test/login",
                            notes="Previous fake source control account password.",
                        ),
                    ),
                ),
                entry(
                    title="Jira",
                    username="john.doe.jira@example.test",
                    password="abc123",
                    url="https://jira.example.test/login.jsp",
                    notes="Fake issue tracker account.",
                ),
                entry(
                    title="VPN",
                    username="john.doe.vpn",
                    password="abc123",
                    url="https://vpn.example.test",
                    notes="Fake corporate VPN credentials.",
                    expiry_time=EXPIRY_TIME,
                ),
            ),
        ),
        GroupSpec(
            name="Home",
            entries=(
                entry(
                    title="Home NAS",
                    username="john.doe@example.test",
                    password="abc123",
                    url="http://localhost:8000/nas",
                    notes="Fake NAS login.",
                ),
                entry(
                    title="Rewards Credit Card",
                    username="john.doe.card@example.test",
                    password="abc123",
                    url="https://cards.example.test/sign-in",
                    notes="Fake credit card portal login.",
                ),
                entry(
                    title="Brokerage",
                    username="john.doe.invest@example.test",
                    password="abc123",
                    url="https://brokerage.example.test/login",
                    notes="Fake brokerage account login.",
                ),
            ),
        ),
    )


def entry(
    title: str,
    username: str,
    password: str,
    url: str,
    notes: str,
    custom_fields: tuple[CustomFieldSpec, ...] = (),
    attachments: tuple[AttachmentSpec, ...] = (),
    history: tuple[EntryHistorySpec, ...] = (),
    expiry_time: datetime | None = None,
) -> EntrySpec:
    return EntrySpec(
        title=title,
        username=username,
        password=password,
        url=url,
        notes=notes,
        custom_fields=custom_fields,
        attachments=attachments,
        history=history,
        expiry_time=expiry_time,
    )


if __name__ == "__main__":
    main()
