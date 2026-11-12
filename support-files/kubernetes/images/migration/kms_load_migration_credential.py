#!/usr/bin/env python3
"""Decrypt the migration envelope and write its credentials to stdout.

Writes admin username, admin password, app code and app secret in that order,
UTF-8, each terminated by NUL. The app credentials live under
bkapp_id_secret.default so that apigw-manager can share the same envelope.
Errors go to stderr and never include the private key, envelope, or plaintext.
"""

import json
import os
import sys
from pathlib import Path

from bk_kms import decrypt

ENVELOPE_PATH = Path("/etc/kms/migration/envelope")
PRIVATE_KEY_ENV = "BK_JOB_KMS_PRIVATE_KEY_MIGRATION"
APP_ENTRY = "default"


def main():
    try:
        plaintext = decrypt(
            envelope=ENVELOPE_PATH.read_text(encoding="utf-8").strip(),
            private_key=os.environ[PRIVATE_KEY_ENV].strip(),
        )
        credentials = json.loads(plaintext)
    except Exception as exc:
        sys.stderr.write("failed to decrypt migration envelope: %s\n" % type(exc).__name__)
        return 1
    if not isinstance(credentials, dict):
        sys.stderr.write("migration envelope is not a JSON object\n")
        return 1
    apps = credentials.get("bkapp_id_secret")
    app = apps.get(APP_ENTRY) if isinstance(apps, dict) else None
    if not isinstance(app, dict):
        sys.stderr.write("migration envelope field bkapp_id_secret.%s is missing\n" % APP_ENTRY)
        return 1
    fields = (
        ("admin-username", credentials.get("admin-username")),
        ("admin-password", credentials.get("admin-password")),
        ("bkapp_id_secret.%s.app_code" % APP_ENTRY, app.get("app_code")),
        ("bkapp_id_secret.%s.app_secret" % APP_ENTRY, app.get("app_secret")),
    )
    values = []
    for name, value in fields:
        if not isinstance(value, str) or not value or "\0" in value:
            sys.stderr.write("migration envelope field %s is missing or not a string\n" % name)
            return 1
        values.append(value.encode("utf-8"))
    for value in values:
        sys.stdout.buffer.write(value + b"\0")
    sys.stdout.buffer.flush()
    return 0


if __name__ == "__main__":
    sys.exit(main())
