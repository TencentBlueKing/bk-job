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


RESOURCE_TYPES_PATH = "/api/v1/open/model/systems/{system_id}/resource-types/"
RESOURCE_TYPE_PATH = "/api/v1/open/model/systems/{system_id}/resource-types/{resource_type_id}/"
ACTIONS_PATH = "/api/v1/open/model/systems/{system_id}/actions/"
ACTION_PATH = "/api/v1/open/model/systems/{system_id}/actions/{action_id}/"
ROLES_PATH = "/api/v1/open/model/systems/{system_id}/roles/"
ROLE_PATH = "/api/v1/open/model/systems/{system_id}/roles/{role_id}/"
ROLE_ACTIONS_PATH = "/api/v1/open/model/systems/{system_id}/roles/{role_id}/actions/"
MODEL_PAGE_SIZE = 100


def clean_resource_types(resource_types):
    cleaned = []
    for item in strip_comments(resource_types):
        payload = {"id": item["id"], "name": item["name"]}
        if item.get("ancestors"):
            payload["ancestors"] = item["ancestors"]
        cleaned.append(payload)
    return cleaned


def ancestors_of(item):
    return list(item.get("ancestors") or [])


def diff_resource_types(desired, remote):
    """Compare local resource types with IAM. Update only when ancestors change."""
    remote_by_id = {item["id"]: item for item in remote}
    to_create = [item for item in desired if item["id"] not in remote_by_id]
    to_delete = [item for item in remote if item["id"] not in {d["id"] for d in desired}]
    to_update = []
    unchanged = []
    for item in desired:
        remote_item = remote_by_id.get(item["id"])
        if remote_item is None:
            continue
        if ancestors_of(item) != ancestors_of(remote_item):
            to_update.append(item)
        else:
            unchanged.append(item)
    return to_create, to_update, to_delete, unchanged


def order_resource_types_for_delete(items):
    """Delete descendants before ancestors so parent types are not still referenced."""
    remaining = {item["id"]: item for item in items}
    ordered = []
    while remaining:
        ready_ids = [
            item_id
            for item_id, item in remaining.items()
            if not any(item_id in ancestors_of(other) for other in remaining.values())
        ]
        if not ready_ids:
            ordered.extend(remaining.values())
            break
        for item_id in ready_ids:
            ordered.append(remaining.pop(item_id))
    return ordered


def clean_actions(actions):
    cleaned = []
    for item in strip_comments(actions):
        payload = {"id": item["id"], "name": item["name"]}
        resource_type_id = item.get("resource_type_id")
        if resource_type_id:
            payload["resource_type_id"] = resource_type_id
        cleaned.append(payload)
    return cleaned


def resource_type_id_of(item):
    return item.get("resource_type_id") or ""


def diff_actions(desired, remote):
    """Compare local actions with IAM. Update only when resource_type_id changes."""
    remote_by_id = {item["id"]: item for item in remote}
    to_create = [item for item in desired if item["id"] not in remote_by_id]
    to_delete = [item for item in remote if item["id"] not in {d["id"] for d in desired}]
    to_update = []
    unchanged = []
    for item in desired:
        remote_item = remote_by_id.get(item["id"])
        if remote_item is None:
            continue
        if resource_type_id_of(item) != resource_type_id_of(remote_item):
            to_update.append(item)
        else:
            unchanged.append(item)
    return to_create, to_update, to_delete, unchanged


def clean_roles(roles, actions):
    """Pass through IAM register-role payload. Each action keeps its own resource_type_id.

    model.json `roles` matches POST /roles/ body: `{id, name, description?, tenant_id?,
    actions: [{resource_type_id, id}]}`. Empty resource_type_id is kept as "".
    """
    action_by_id = {item["id"]: item for item in actions}
    cleaned = []
    for role in strip_comments(roles):
        if role.get("action_ids") is not None:
            raise ValueError(
                "role %s still uses action_ids; expected actions[{id, resource_type_id}]"
                % role.get("id")
            )
        role_actions = []
        for action in role.get("actions") or []:
            action_id = action.get("id")
            if not action_id:
                raise ValueError("role %s has an action without id" % role.get("id"))
            resource_type_id = action.get("resource_type_id") or ""
            spec = action_by_id.get(action_id)
            if spec is None:
                raise ValueError("role %s references unknown action %s" % (role.get("id"), action_id))
            expected = spec.get("resource_type_id") or ""
            if resource_type_id != expected:
                raise ValueError(
                    "role %s action %s resource_type_id=%s does not match action definition %s"
                    % (role.get("id"), action_id, resource_type_id, expected)
                )
            role_actions.append({"resource_type_id": resource_type_id, "id": action_id})
        payload = {
            "id": role["id"],
            "name": role["name"],
            "actions": role_actions,
        }
        if role.get("description"):
            payload["description"] = role["description"]
        if role.get("tenant_id"):
            payload["tenant_id"] = role["tenant_id"]
        cleaned.append(payload)
    return cleaned


def role_action_payload(action):
    return {"id": action["id"], "resource_type_id": resource_type_id_of(action)}


def diff_roles(desired, remote):
    remote_by_id = {item["id"]: item for item in remote}
    to_create = [item for item in desired if item["id"] not in remote_by_id]
    to_delete = [item for item in remote if item["id"] not in {d["id"] for d in desired}]
    existing = []
    for item in desired:
        remote_item = remote_by_id.get(item["id"])
        if remote_item is not None:
            existing.append((item, remote_item))
    return to_create, existing, to_delete


def diff_role_actions(desired_actions, remote_actions):
    """Match role actions by id; recreate when the same action has a different resource_type_id."""
    remote_by_id = {item["id"]: item for item in remote_actions}
    to_create = [item for item in desired_actions if item["id"] not in remote_by_id]
    to_delete = [item for item in remote_actions if item["id"] not in {d["id"] for d in desired_actions}]
    to_recreate = []
    for item in desired_actions:
        remote_item = remote_by_id.get(item["id"])
        if remote_item is None:
            continue
        if resource_type_id_of(item) != resource_type_id_of(remote_item):
            to_recreate.append(item)
    return to_create, to_recreate, to_delete


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
    def __init__(self, app_code, app_secret, gateway_url, dry_run=False, delete_cascade=False):
        self.app_code = app_code
        self.app_secret = app_secret
        self.gateway_url = gateway_url.rstrip("/")
        self.dry_run = dry_run
        self.delete_cascade = delete_cascade

    def _headers(self):
        return {
            "Content-Type": "application/json",
            "X-Bk-Tenant-Id": BK_TENANT_ID,
            "X-Bkapi-Authorization": json.dumps(
                {"bk_app_code": self.app_code, "bk_app_secret": self.app_secret}
            ),
        }

    def _log_api_failure(self, method, url, params, data, resp=None, error=None):
        print("iam api fail")
        print("request: %s %s" % (method.upper(), url))
        if params:
            print("request params: %s" % json.dumps(params, ensure_ascii=False))
        if data is not None:
            print("request body: %s" % json.dumps(data, ensure_ascii=False))
        if error:
            print("request error: %s" % error)
        if resp is not None:
            print("response status: %s" % resp.status_code)
            print("response body: %s" % (resp.text if resp.text else "<empty>"))

    def _call(self, method, path, data=None, params=None):
        url = "{host}{path}".format(host=self.gateway_url, path=path)
        if self.dry_run and method.upper() in ("POST", "PUT", "DELETE"):
            return True, "ok", None
        kwargs = {"headers": self._headers(), "timeout": 30, "verify": False}
        if params:
            kwargs["params"] = params
        if data is not None:
            kwargs["json"] = data
        try:
            resp = requests.request(method.upper(), url, **kwargs)
        except requests.exceptions.RequestException as exc:
            self._log_api_failure(method, url, params, data, error=str(exc))
            return False, str(exc), None
        request_url = resp.url or url
        if not (200 <= resp.status_code < 300):
            self._log_api_failure(method, request_url, params, data, resp=resp)
            return False, "status_code is %s" % resp.status_code, None
        if not resp.content:
            return True, "ok", None
        try:
            body = resp.json()
        except ValueError:
            self._log_api_failure(method, request_url, params, data, resp=resp)
            return False, "invalid json response", None
        error = body.get("error")
        if error:
            self._log_api_failure(method, request_url, params, data, resp=resp)
            message = error.get("message") if isinstance(error, dict) else str(error)
            return False, message, None
        if "code" in body and body.get("code") not in (0, "0", None):
            self._log_api_failure(method, request_url, params, data, resp=resp)
            return False, body.get("message") or "iam api fail", None
        return True, "ok", body

    def post(self, path, data):
        ok, message, _body = self._call("post", path, data=data)
        return ok, message

    def register_system(self, system):
        return self.post("/api/v1/open/rbac/model/systems/", system)

    def _list_all(self, path):
        page = 1
        results = []
        while page <= 100:
            ok, message, body = self._call(
                "get", path, params={"page": page, "page_size": MODEL_PAGE_SIZE}
            )
            if not ok:
                return False, message
            data = (body or {}).get("data") or {}
            page_results = data.get("results") or []
            results.extend(page_results)
            count = data.get("count")
            if not page_results:
                break
            if count is not None and len(results) >= count:
                break
            if len(page_results) < MODEL_PAGE_SIZE:
                break
            page += 1
        return True, results

    def list_all_resource_types(self, system_id):
        return self._list_all(RESOURCE_TYPES_PATH.format(system_id=system_id))

    def register_resource_types(self, system_id, resource_types):
        return self.post(RESOURCE_TYPES_PATH.format(system_id=system_id), resource_types)

    def update_resource_type(self, system_id, resource_type_id, data):
        path = RESOURCE_TYPE_PATH.format(system_id=system_id, resource_type_id=resource_type_id)
        ok, message, _body = self._call("put", path, data=data)
        return ok, message

    def delete_resource_type(self, system_id, resource_type_id):
        path = RESOURCE_TYPE_PATH.format(system_id=system_id, resource_type_id=resource_type_id)
        ok, message, _body = self._call("delete", path)
        return ok, message

    def list_all_actions(self, system_id):
        return self._list_all(ACTIONS_PATH.format(system_id=system_id))

    def register_actions(self, system_id, actions):
        return self.post(ACTIONS_PATH.format(system_id=system_id), actions)

    def delete_action(self, system_id, action_id):
        path = ACTION_PATH.format(system_id=system_id, action_id=action_id)
        ok, message, _body = self._call("delete", path)
        return ok, message

    def list_all_roles(self, system_id):
        return self._list_all(ROLES_PATH.format(system_id=system_id))

    def register_roles(self, system_id, roles):
        return self.post(ROLES_PATH.format(system_id=system_id), roles)

    def delete_role(self, system_id, role_id):
        path = ROLE_PATH.format(system_id=system_id, role_id=role_id)
        ok, message, _body = self._call("delete", path)
        return ok, message

    def create_role_actions(self, system_id, role_id, actions):
        path = ROLE_ACTIONS_PATH.format(system_id=system_id, role_id=role_id)
        return self.post(path, actions)

    def delete_role_actions(self, system_id, role_id, action_ids):
        path = ROLE_ACTIONS_PATH.format(system_id=system_id, role_id=role_id)
        params = {"ids": ",".join(action_ids)}
        if self.delete_cascade:
            params["cascade"] = "rbac_authorization"
        ok, message, _body = self._call("delete", path, params=params)
        return ok, message

    def register_instance_selections(self, system_id, resource_type_id, views):
        path = "/api/v1/open/mgmt/systems/{system_id}/resource-types/{resource_type_id}/instance-selections/".format(
            system_id=system_id, resource_type_id=resource_type_id
        )
        return self.post(path, views)


def skip_if_failed(ok, message, step_name):
    """Always ignore. Used for create-only steps where already-exists is expected on rerun."""
    if ok:
        print("%s success" % step_name)
        return True
    print("%s fail: %s，忽略" % (step_name, message))
    return True


def accept_step(ok, message, step_name, ignore_fail):
    if ok:
        return True
    print("%s fail: %s" % (step_name, message))
    if ignore_fail:
        print("ignoreFail=true, skip %s and continue" % step_name)
        return True
    return False


def load_model_payloads(model_dir, app_code, job_host, managers):
    model_path = os.path.join(model_dir, "model.json")
    views_path = os.path.join(model_dir, "instance-views.json")
    if not os.path.isfile(model_path):
        raise ValueError("model file not found: %s" % model_path)
    if not os.path.isfile(views_path):
        raise ValueError("instance-views file not found: %s" % views_path)

    model = load_json(model_path)
    views_file = load_json(views_path)
    system_id = model.get("system_id") or (model.get("system") or {}).get("id")
    if not system_id:
        raise ValueError("system_id is required")

    return {
        "system_id": system_id,
        "system": render_system(model["system"], app_code, job_host, managers),
        "resource_types": clean_resource_types(model.get("resource_types") or []),
        "actions": clean_actions(model.get("actions") or []),
        "roles": clean_roles(model.get("roles") or [], model.get("actions") or []),
        "instance_views": clean_instance_views(views_file.get("instance_views") or {}),
    }


def log_skip(kind, item_id):
    print("%s %s 一样，跳过" % (kind, item_id))


def dryrun_flag(dry_run):
    return "true" if dry_run else "false"


def log_change(verb, kind, item_id, dry_run):
    print("%s %s %s dryrun=%s" % (verb, kind, item_id, dryrun_flag(dry_run)))


def log_mutation(message, dry_run):
    print("%s dryrun=%s" % (message, dryrun_flag(dry_run)))


def sync_resource_types(client, system_id, desired, ignore_fail):
    ok, remote = client.list_all_resource_types(system_id)
    if not ok:
        return accept_step(False, remote, "list resource types", ignore_fail)

    to_create, to_update, to_delete, unchanged = diff_resource_types(desired, remote)

    for item in unchanged:
        log_skip("资源类型", item["id"])

    if to_create:
        ok, message = client.register_resource_types(system_id, to_create)
        if ok:
            for item in to_create:
                log_change("新增", "资源类型", item["id"], client.dry_run)
        else:
            return accept_step(False, message, "新增资源类型", ignore_fail)

    for item in to_update:
        payload = {"name": item["name"], "ancestors": ancestors_of(item)}
        ok, message = client.update_resource_type(system_id, item["id"], payload)
        if ok:
            log_change("更新", "资源类型", item["id"], client.dry_run)
        else:
            return accept_step(False, message, "更新 资源类型 %s" % item["id"], ignore_fail)

    for item in order_resource_types_for_delete(to_delete):
        ok, message = client.delete_resource_type(system_id, item["id"])
        if ok:
            log_change("删除", "资源类型", item["id"], client.dry_run)
        else:
            return accept_step(False, message, "删除 资源类型 %s" % item["id"], ignore_fail)
    return True


def sync_actions(client, system_id, desired, ignore_fail):
    ok, remote = client.list_all_actions(system_id)
    if not ok:
        return accept_step(False, remote, "list actions", ignore_fail)

    to_create, to_update, to_delete, unchanged = diff_actions(desired, remote)

    for item in unchanged:
        log_skip("操作", item["id"])

    if to_create:
        ok, message = client.register_actions(system_id, to_create)
        if ok:
            for item in to_create:
                log_change("新增", "操作", item["id"], client.dry_run)
        else:
            return accept_step(False, message, "新增操作", ignore_fail)

    # IAM update-action has no resource_type_id; recreate when the binding changes.
    for item in to_update:
        step = "操作 %s 关联资源类型改变，删除后重新创建" % item["id"]
        ok, message = client.delete_action(system_id, item["id"])
        if not ok:
            if not accept_step(False, message, step, ignore_fail):
                return False
            continue
        ok, message = client.register_actions(system_id, [item])
        if ok:
            log_mutation(step, client.dry_run)
        elif not accept_step(False, message, step, ignore_fail):
            return False

    for item in to_delete:
        ok, message = client.delete_action(system_id, item["id"])
        if ok:
            log_change("删除", "操作", item["id"], client.dry_run)
        else:
            return accept_step(False, message, "删除 操作 %s" % item["id"], ignore_fail)
    return True


def sync_existing_role(client, system_id, desired, remote, ignore_fail):
    role_id = desired["id"]
    to_create, to_recreate, to_delete = diff_role_actions(
        desired.get("actions") or [], remote.get("actions") or []
    )
    if not to_create and not to_recreate and not to_delete:
        log_skip("角色", role_id)
        return True

    for action in to_recreate:
        step = "角色 %s 操作 %s 关联资源类型改变，删除后重新创建" % (role_id, action["id"])
        ok, message = client.delete_role_actions(system_id, role_id, [action["id"]])
        if not ok:
            if not accept_step(False, message, step, ignore_fail):
                return False
            continue
        ok, message = client.create_role_actions(system_id, role_id, [role_action_payload(action)])
        if ok:
            log_mutation(step, client.dry_run)
        elif not accept_step(False, message, step, ignore_fail):
            return False

    if to_delete:
        action_ids = [action["id"] for action in to_delete]
        ok, message = client.delete_role_actions(system_id, role_id, action_ids)
        if ok:
            for action_id in action_ids:
                log_mutation("角色 %s 删除 操作 %s" % (role_id, action_id), client.dry_run)
        elif not accept_step(False, message, "角色 %s 删除操作" % role_id, ignore_fail):
            return False

    if to_create:
        ok, message = client.create_role_actions(
            system_id, role_id, [role_action_payload(action) for action in to_create]
        )
        if ok:
            for action in to_create:
                log_mutation("角色 %s 新增 操作 %s" % (role_id, action["id"]), client.dry_run)
        elif not accept_step(False, message, "角色 %s 新增操作" % role_id, ignore_fail):
            return False
    return True


def sync_roles(client, system_id, desired, ignore_fail):
    ok, remote = client.list_all_roles(system_id)
    if not ok:
        return accept_step(False, remote, "list roles", ignore_fail)

    to_create, existing, to_delete = diff_roles(desired, remote)

    if to_create:
        ok, message = client.register_roles(system_id, to_create)
        if ok:
            for item in to_create:
                log_change("新增", "角色", item["id"], client.dry_run)
        elif not accept_step(False, message, "新增角色", ignore_fail):
            return False

    for desired_role, remote_role in existing:
        if not sync_existing_role(client, system_id, desired_role, remote_role, ignore_fail):
            return False

    for item in to_delete:
        ok, message = client.delete_role(system_id, item["id"])
        if ok:
            log_change("删除", "角色", item["id"], client.dry_run)
        else:
            # Role delete fails when authorizations still exist; always ignore.
            print("删除 角色 %s fail: %s，忽略" % (item["id"], message))
    return True


def do_migrate(
    model_dir, gateway_url, app_code, app_secret, job_host, managers, ignore_fail, dry_run=False, delete_cascade=False
):
    try:
        payloads = load_model_payloads(model_dir, app_code, job_host, managers)
    except (KeyError, ValueError) as exc:
        print("invalid model json: %s" % exc)
        return False

    client = IamV4Client(
        app_code, app_secret, gateway_url, dry_run=dry_run, delete_cascade=delete_cascade
    )
    system_id = payloads["system_id"]

    # System and instance views are still create-only; already-exists is expected on rerun.
    ok, message = client.register_system(payloads["system"])
    skip_if_failed(ok, message, "register system [%s]" % system_id)

    if not sync_resource_types(client, system_id, payloads["resource_types"], ignore_fail):
        return False

    if not sync_actions(client, system_id, payloads["actions"], ignore_fail):
        return False

    if not sync_roles(client, system_id, payloads["roles"], ignore_fail):
        return False

    for resource_type_id, views in payloads["instance_views"].items():
        if not views:
            continue
        ok, message = client.register_instance_selections(system_id, resource_type_id, views)
        skip_if_failed(
            ok, message, "register instance selections type=%s count=%s" % (resource_type_id, len(views))
        )
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
        "--ignore-fail",
        dest="ignore_fail",
        default="true",
        help="true: write API failure skips the step and does not fail the job",
    )
    parser.add_argument(
        "--model-dir",
        dest="model_dir",
        default="",
        help="override model directory; default is ./tenant_enable or ./tenant_disable next to this script",
    )
    parser.add_argument(
        "--dry-run",
        dest="dry_run",
        default="false",
        help="true: compare and log mutations but do not call IAM write APIs; unset/false disables dry-run",
    )
    parser.add_argument(
        "--delete-cascade",
        dest="delete_cascade",
        default="false",
        help="true: DELETE role actions with cascade=rbac_authorization; unset/false omits it",
    )
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    model_dir = args.model_dir or resolve_model_dir(script_dir, args.tenant_enabled)
    ignore_fail = str(args.ignore_fail).lower() == "true"
    dry_run = str(args.dry_run).lower() == "true"
    delete_cascade = str(args.delete_cascade).lower() == "true"
    print(
        "start migrate iam v4 model, dir=%s ignoreFail=%s dryrun=%s deleteCascade=%s"
        % (model_dir, ignore_fail, dryrun_flag(dry_run), dryrun_flag(delete_cascade))
    )
    ok = do_migrate(
        model_dir=model_dir,
        gateway_url=args.gateway_url,
        app_code=args.app_code,
        app_secret=args.app_secret,
        job_host=args.job_host,
        managers=parse_managers(args.managers),
        ignore_fail=ignore_fail,
        dry_run=dry_run,
        delete_cascade=delete_cascade,
    )
    if not ok:
        print("do migrate iam v4 fail")
        sys.exit(1)
    print("do migrate iam v4 success")


if __name__ == "__main__":
    main()
