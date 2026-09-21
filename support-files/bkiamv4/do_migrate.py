# coding:utf-8
"""
Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.

Copyright (C) 2021 Tencent.  All rights reserved.

BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.

License for BK-JOB蓝鲸智云作业平台:
--------------------------------------------------------------------
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of
the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
"""

from __future__ import print_function

import argparse
import json
import os
import sys

import requests
import urllib3

urllib3.disable_warnings()

MODEL_DIR_ENABLE = "tenant_enable"
MODEL_DIR_DISABLE = "tenant_disable"
BK_TENANT_ID = "system"


def load_json(path):
    with open(path) as f:
        return json.load(f)


def strip_comments(value):
    """Recursively drop `_comment` keys so they are not sent to IAM."""
    if isinstance(value, dict):
        return {k: strip_comments(v) for k, v in value.items() if k != "_comment"}
    if isinstance(value, list):
        return [strip_comments(item) for item in value]
    return value


def parse_managers(raw):
    if not raw:
        return []
    return [item.strip() for item in raw.split(",") if item.strip()]


def render_system(system, app_code, job_host, managers):
    rendered = strip_comments(system)
    clients = []
    for client in rendered.get("clients") or []:
        clients.append(app_code if client == "{{APP_CODE}}" else client)
    rendered["clients"] = clients
    rendered["managers"] = managers
    callback_url = rendered.get("callback_url") or ""
    rendered["callback_url"] = callback_url.replace("{{JOB_HOST}}", job_host.rstrip("/"))
    return rendered


def clean_resource_types(resource_types):
    cleaned = []
    for item in strip_comments(resource_types):
        payload = {"id": item["id"], "name": item["name"]}
        if item.get("ancestors"):
            payload["ancestors"] = item["ancestors"]
        cleaned.append(payload)
    return cleaned


def clean_actions(actions):
    cleaned = []
    for item in strip_comments(actions):
        payload = {"id": item["id"], "name": item["name"]}
        resource_type_id = item.get("resource_type_id")
        if resource_type_id:
            payload["resource_type_id"] = resource_type_id
        cleaned.append(payload)
    return cleaned


def expand_roles(roles, actions):
    """Turn `action_ids` into IAM `actions[{id, resource_type_id}]`.

    Authorization dimension is the role's own resource_type_id, unless the
    action itself is resource-independent (empty resource_type_id).
    """
    action_by_id = {item["id"]: item for item in actions}
    expanded = []
    for role in strip_comments(roles):
        role_dimension = role.get("resource_type_id") or ""
        role_actions = []
        for action_id in role.get("action_ids") or []:
            action = action_by_id.get(action_id)
            if action is None:
                raise ValueError("role %s references unknown action %s" % (role.get("id"), action_id))
            if action.get("resource_type_id"):
                dimension = role_dimension
            else:
                dimension = ""
            role_actions.append({"id": action_id, "resource_type_id": dimension})
        payload = {
            "id": role["id"],
            "name": role["name"],
            "actions": role_actions,
        }
        if role.get("description"):
            payload["description"] = role["description"]
        expanded.append(payload)
    return expanded


def clean_instance_views(instance_views):
    cleaned = {}
    for resource_type_id, views in strip_comments(instance_views or {}).items():
        if not isinstance(views, list):
            continue
        cleaned[resource_type_id] = [
            {"id": view["id"], "name": view["name"], "chain": view["chain"]}
            for view in views
        ]
    return cleaned


class IamV4Client(object):
    def __init__(self, app_code, app_secret, gateway_url):
        self.app_code = app_code
        self.app_secret = app_secret
        self.gateway_url = gateway_url.rstrip("/")

    def _headers(self):
        return {
            "Content-Type": "application/json",
            "X-Bk-Tenant-Id": BK_TENANT_ID,
            "X-Bkapi-Authorization": json.dumps(
                {"bk_app_code": self.app_code, "bk_app_secret": self.app_secret}
            ),
        }

    def post(self, path, data):
        url = "{host}{path}".format(host=self.gateway_url, path=path)
        print("POST %s" % path)
        try:
            resp = requests.post(url, headers=self._headers(), json=data, timeout=30, verify=False)
        except requests.exceptions.RequestException as exc:
            print("http request error: %s" % exc)
            return False, str(exc)
        try:
            body = resp.json()
        except ValueError:
            print("invalid json response: status=%s body=%s" % (resp.status_code, resp.text[:200]))
            return False, "invalid json response"
        if resp.status_code != 200:
            print("http status %s: %s" % (resp.status_code, body))
            return False, "status_code is %s" % resp.status_code
        error = body.get("error")
        if error:
            message = error.get("message") if isinstance(error, dict) else str(error)
            print("iam api error: %s" % message)
            return False, message
        if "code" in body and body.get("code") not in (0, "0", None):
            message = body.get("message") or "iam api fail"
            print("iam api error: %s" % message)
            return False, message
        return True, "ok"


def do_migrate(model_dir, gateway_url, app_code, app_secret, job_host, managers):
    model_path = os.path.join(model_dir, "model.json")
    views_path = os.path.join(model_dir, "instance-views.json")
    if not os.path.isfile(model_path):
        print("model file not found: %s" % model_path)
        return False
    if not os.path.isfile(views_path):
        print("instance-views file not found: %s" % views_path)
        return False

    model = load_json(model_path)
    views_file = load_json(views_path)
    system_id = model.get("system_id") or (model.get("system") or {}).get("id")
    if not system_id:
        print("system_id is required")
        return False

    try:
        system = render_system(model["system"], app_code, job_host, managers)
        resource_types = clean_resource_types(model.get("resource_types") or [])
        actions = clean_actions(model.get("actions") or [])
        roles = expand_roles(model.get("roles") or [], model.get("actions") or [])
        instance_views = clean_instance_views(views_file.get("instance_views") or {})
    except (KeyError, ValueError) as exc:
        print("invalid model json: %s" % exc)
        return False

    client = IamV4Client(app_code, app_secret, gateway_url)

    ok, message = client.post("/api/v1/open/rbac/model/systems/", system)
    if not ok:
        print("register system fail: %s" % message)
        return False
    print("register system [%s] success" % system_id)

    ok, message = client.post(
        "/api/v1/open/rbac/model/systems/{system_id}/resource-types/".format(system_id=system_id),
        resource_types,
    )
    if not ok:
        print("register resource types fail: %s" % message)
        return False
    print("register resource types success, count=%s" % len(resource_types))

    ok, message = client.post(
        "/api/v1/open/rbac/model/systems/{system_id}/actions/".format(system_id=system_id),
        actions,
    )
    if not ok:
        print("register actions fail: %s" % message)
        return False
    print("register actions success, count=%s" % len(actions))

    ok, message = client.post(
        "/api/v1/open/rbac/model/systems/{system_id}/roles/".format(system_id=system_id),
        roles,
    )
    if not ok:
        print("register roles fail: %s" % message)
        return False
    print("register roles success, count=%s" % len(roles))

    for resource_type_id, views in instance_views.items():
        if not views:
            continue
        path = "/api/v1/open/systems/{system_id}/resource-types/{resource_type_id}/instance-views".format(
            system_id=system_id, resource_type_id=resource_type_id
        )
        ok, message = client.post(path, views)
        if not ok:
            print("register instance views fail: type=%s error=%s" % (resource_type_id, message))
            return False
        print("register instance views success: type=%s count=%s" % (resource_type_id, len(views)))
    return True


def resolve_model_dir(base_dir, tenant_enabled):
    sub_dir = MODEL_DIR_ENABLE if tenant_enabled == "true" else MODEL_DIR_DISABLE
    return os.path.join(base_dir, sub_dir)


def main():
    parser = argparse.ArgumentParser(description="Register IAM v4 permission model for bk-job")
    parser.add_argument("-t", dest="gateway_url", required=True, help="IAM v4 API gateway url")
    parser.add_argument("-a", dest="app_code", required=True, help="bk app code")
    parser.add_argument("-s", dest="app_secret", required=True, help="bk app secret")
    parser.add_argument("--job-host", dest="job_host", required=True, help="Job API url used as callback host")
    parser.add_argument(
        "--tenant-enabled",
        dest="tenant_enabled",
        default="false",
        help="true to use tenant_enable model, otherwise tenant_disable",
    )
    parser.add_argument("--managers", dest="managers", default="", help="comma-separated system managers")
    parser.add_argument(
        "--model-dir",
        dest="model_dir",
        default="",
        help="override model directory; default is ./tenant_enable or ./tenant_disable next to this script",
    )
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    model_dir = args.model_dir or resolve_model_dir(script_dir, args.tenant_enabled)
    print("start migrate iam v4 model, dir=%s" % model_dir)
    ok = do_migrate(
        model_dir=model_dir,
        gateway_url=args.gateway_url,
        app_code=args.app_code,
        app_secret=args.app_secret,
        job_host=args.job_host,
        managers=parse_managers(args.managers),
    )
    if not ok:
        print("do migrate iam v4 fail")
        sys.exit(1)
    print("do migrate iam v4 success")


if __name__ == "__main__":
    main()
