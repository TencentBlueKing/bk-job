#!/usr/bin/env python3
"""Decrypt the migration envelope for sync-bk-aidev.

Writes app code, app secret and access token in that order, UTF-8, each
terminated by NUL. App credentials are bkapp_id_secret.default, shared with
job-migration and sync-api-gateway. Access token is the optional key
bkai-access-token; an absent key is written as an empty field. When
BK_AIDEV_REQUIRE_ACCESS_TOKEN=true the key must be a non-empty string.
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
    app_code = app.get("app_code")
    app_secret = app.get("app_secret")
    if not isinstance(app_code, str) or not app_code or "\0" in app_code:
        sys.stderr.write("migration envelope field bkapp_id_secret.%s.app_code is missing\n" % APP_ENTRY)
        return 1
    if not isinstance(app_secret, str) or not app_secret or "\0" in app_secret:
        sys.stderr.write("migration envelope field bkapp_id_secret.%s.app_secret is missing\n" % APP_ENTRY)
        return 1
    token = credentials.get("bkai-access-token", "")
    require_token = os.environ.get("BK_AIDEV_REQUIRE_ACCESS_TOKEN") == "true"
    if token is None:
        token = ""
    if not isinstance(token, str) or "\0" in token or (require_token and not token):
        sys.stderr.write("migration envelope field bkai-access-token is missing or not a string\n")
        return 1
    for value in (app_code, app_secret, token):
        sys.stdout.buffer.write(value.encode("utf-8") + b"\0")
    sys.stdout.buffer.flush()
    return 0


if __name__ == "__main__":
    sys.exit(main())
