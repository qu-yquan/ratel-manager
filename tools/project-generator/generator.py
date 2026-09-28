#!/usr/bin/env python3
"""ratel-manager 项目生成器。"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import sys
import tempfile
import xml.etree.ElementTree as ElementTree
from pathlib import Path


GENERATOR_DIR = Path(__file__).resolve().parent
SOURCE_ROOT = GENERATOR_DIR.parent.parent
TEMPLATE_ROOT = GENERATOR_DIR / "templates"
DEFAULT_GROUP_PREFIX = "org.quyq"
DEFAULT_VERSION = "1.0-SNAPSHOT"
PROJECT_ID_PATTERN = re.compile(r"^[a-z][a-z0-9]*$")
GROUP_PREFIX_PATTERN = re.compile(r"^[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)*$")
IGNORED_NAMES = {
    ".DS_Store",
    ".git",
    ".idea",
    ".umi",
    "dist",
    "node_modules",
    "target",
}
TEXT_SUFFIXES = {
    ".css",
    ".html",
    ".java",
    ".json",
    ".less",
    ".md",
    ".properties",
    ".sql",
    ".ts",
    ".tsx",
    ".txt",
    ".xml",
    ".yaml",
    ".yml",
}


class GenerationError(RuntimeError):
    """项目生成失败。"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="基于 ratel-manager 创建新的 ratel 项目",
    )
    parser.add_argument("project_id", help="项目标识，仅允许小写字母和数字，并以字母开头")
    parser.add_argument(
        "--group-prefix",
        default=DEFAULT_GROUP_PREFIX,
        help=f"groupId 和 Java 包名前缀，默认 {DEFAULT_GROUP_PREFIX}",
    )
    parser.add_argument(
        "--output",
        type=Path,
        help="输出目录，默认在 ratel-manager 上一级生成 ratel-{项目标识}",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="只展示生成参数和目标目录，不写入文件",
    )
    return parser.parse_args()


def validate_args(project_id: str, group_prefix: str) -> None:
    if not PROJECT_ID_PATTERN.fullmatch(project_id):
        raise GenerationError(
            "项目标识格式不正确：仅允许小写字母和数字，并且必须以字母开头，例如 demo、project2"
        )
    if not GROUP_PREFIX_PATTERN.fullmatch(group_prefix):
        raise GenerationError(
            "groupId 前缀格式不正确：必须是小写 Java 包名，例如 org.quyq"
        )


def read_platform_version() -> str:
    pom = SOURCE_ROOT / "root-pom" / "pom.xml"
    root = ElementTree.parse(pom).getroot()
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    artifact_id = root.findtext("m:artifactId", namespaces=namespace)
    version = root.findtext("m:version", namespaces=namespace)
    if artifact_id != "root-pom" or not version:
        raise GenerationError(f"无法从 {pom} 获取公共平台版本")
    return version.strip()


def template_context(
    project_id: str,
    group_prefix: str,
    output_dir: Path,
    platform_version: str,
) -> dict[str, str]:
    group_id = f"{group_prefix}.{project_id}"
    package_path = group_id.replace(".", "/")
    project_class_name = "".join(part.capitalize() for part in re.split(r"[^a-z0-9]+", project_id))
    core_path = SOURCE_ROOT / "web" / "gwsu-core"
    workspace_file = output_dir / "web" / "pnpm-workspace.yaml"
    core_relative_path = os.path.relpath(core_path, workspace_file.parent).replace(os.sep, "/")
    return {
        "PROJECT_ID": project_id,
        "PROJECT_NAME": f"ratel-{project_id}",
        "PROJECT_CLASS_NAME": project_class_name,
        "GROUP_PREFIX": group_prefix,
        "GROUP_ID": group_id,
        "PACKAGE_PATH": package_path,
        "PROJECT_VERSION": DEFAULT_VERSION,
        "PLATFORM_VERSION": platform_version,
        "GWSU_CORE_RELATIVE_PATH": core_relative_path,
    }


def render_text(content: str, context: dict[str, str]) -> str:
    rendered = content
    for key, value in context.items():
        rendered = rendered.replace(f"@@{key}@@", value)
    unresolved = sorted(set(re.findall(r"@@[A-Z0-9_]+@@", rendered)))
    if unresolved:
        raise GenerationError(f"模板中存在未解析变量：{', '.join(unresolved)}")
    return rendered


def render_template(template_name: str, destination: Path, context: dict[str, str]) -> None:
    template = TEMPLATE_ROOT / template_name
    if not template.is_file():
        raise GenerationError(f"模板不存在：{template}")
    content = template.read_text(encoding="utf-8")
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(render_text(content, context), encoding="utf-8")


def ignore_copy_names(_: str, names: list[str]) -> set[str]:
    return {name for name in names if name in IGNORED_NAMES}


def copy_tree(source: Path, destination: Path) -> None:
    if not source.is_dir():
        raise GenerationError(f"源目录不存在：{source}")
    shutil.copytree(source, destination, ignore=ignore_copy_names)


def is_text_file(path: Path) -> bool:
    return path.suffix.lower() in TEXT_SUFFIXES or path.name in {
        ".env",
        ".gitignore",
    }


def replace_in_tree(root: Path, replacements: dict[str, str]) -> None:
    for path in root.rglob("*"):
        if not path.is_file() or not is_text_file(path):
            continue
        content = path.read_text(encoding="utf-8")
        updated = content
        for old, new in replacements.items():
            updated = updated.replace(old, new)
        if updated != content:
            path.write_text(updated, encoding="utf-8")


def move_java_package(module: Path, old_package: str, new_package: str) -> None:
    for source_set in ("main", "test"):
        java_root = module / "src" / source_set / "java"
        old_dir = java_root / old_package.replace(".", "/")
        if not old_dir.exists():
            continue
        new_dir = java_root / new_package.replace(".", "/")
        new_dir.parent.mkdir(parents=True, exist_ok=True)
        if new_dir.exists():
            raise GenerationError(f"Java 包目录已存在，无法移动：{new_dir}")
        shutil.move(str(old_dir), str(new_dir))
        remove_empty_parents(old_dir.parent, java_root)


def remove_empty_parents(path: Path, stop: Path) -> None:
    current = path
    while current != stop and current.exists():
        try:
            current.rmdir()
        except OSError:
            break
        current = current.parent


def copy_system_module(stage: Path, context: dict[str, str]) -> None:
    destination = stage / "business" / "business-system"
    copy_tree(SOURCE_ROOT / "business" / "business-system", destination)
    old_package = "org.quyq.gwsu.system"
    new_package = f"{context['GROUP_ID']}.system"
    replace_in_tree(destination, {old_package: new_package})
    move_java_package(destination / "business-system-api", old_package, new_package)
    move_java_package(destination / "business-system-server", old_package, new_package)
    render_template("backend/business-system-pom.xml.tpl", destination / "pom.xml", context)
    render_template(
        "backend/business-system-api-pom.xml.tpl",
        destination / "business-system-api" / "pom.xml",
        context,
    )
    render_template(
        "backend/business-system-server-pom.xml.tpl",
        destination / "business-system-server" / "pom.xml",
        context,
    )


def copy_application_resources(stage: Path, context: dict[str, str]) -> None:
    single_source = SOURCE_ROOT / "business" / "application" / "single" / "gwsu" / "src" / "main" / "resources"
    single_destination = (
        stage
        / "business"
        / "application"
        / "single"
        / f"{context['PROJECT_ID']}-application"
        / "src"
        / "main"
        / "resources"
    )
    copy_tree(single_source, single_destination)
    replace_in_tree(
        single_destination,
        {
            "name: gwsu-application": f"name: {context['PROJECT_ID']}-application",
            "typeAliasesPackage: org.quyq.gwsu.**.domain,org.quyq.gwsu.**.vo": (
                f"typeAliasesPackage: {context['GROUP_ID']}.**.domain,{context['GROUP_ID']}.**.vo,"
                "org.quyq.gwsu.**.domain,org.quyq.gwsu.**.vo"
            ),
        },
    )

    distributed_source = (
        SOURCE_ROOT
        / "business"
        / "application"
        / "distributed"
        / "gwsu-system"
        / "src"
        / "main"
        / "resources"
    )
    distributed_destination = (
        stage
        / "business"
        / "application"
        / "distributed"
        / f"{context['PROJECT_ID']}-system"
        / "src"
        / "main"
        / "resources"
    )
    copy_tree(distributed_source, distributed_destination)
    replace_in_tree(
        distributed_destination,
        {"name: gwsu-system": f"name: {context['PROJECT_ID']}-system"},
    )


def create_backend(stage: Path, context: dict[str, str]) -> None:
    destinations = {
        "backend/root-pom.xml.tpl": stage / "pom.xml",
        "backend/core-pom.xml.tpl": stage / f"{context['PROJECT_ID']}-core" / "pom.xml",
        "backend/core-package-info.java.tpl": (
            stage
            / f"{context['PROJECT_ID']}-core"
            / "src"
            / "main"
            / "java"
            / context["PACKAGE_PATH"]
            / "core"
            / "package-info.java"
        ),
        "backend/business-pom.xml.tpl": stage / "business" / "pom.xml",
        "backend/application-pom.xml.tpl": stage / "business" / "application" / "pom.xml",
        "backend/distributed-pom.xml.tpl": stage / "business" / "application" / "distributed" / "pom.xml",
        "backend/system-app-pom.xml.tpl": (
            stage
            / "business"
            / "application"
            / "distributed"
            / f"{context['PROJECT_ID']}-system"
            / "pom.xml"
        ),
        "backend/system-application.java.tpl": (
            stage
            / "business"
            / "application"
            / "distributed"
            / f"{context['PROJECT_ID']}-system"
            / "src"
            / "main"
            / "java"
            / context["PACKAGE_PATH"]
            / "system"
            / f"{context['PROJECT_CLASS_NAME']}SystemApplication.java"
        ),
        "backend/single-pom.xml.tpl": stage / "business" / "application" / "single" / "pom.xml",
        "backend/single-app-pom.xml.tpl": (
            stage
            / "business"
            / "application"
            / "single"
            / f"{context['PROJECT_ID']}-application"
            / "pom.xml"
        ),
        "backend/single-application.java.tpl": (
            stage
            / "business"
            / "application"
            / "single"
            / f"{context['PROJECT_ID']}-application"
            / "src"
            / "main"
            / "java"
            / context["PACKAGE_PATH"]
            / f"{context['PROJECT_CLASS_NAME']}Application.java"
        ),
        "backend/aot-configuration.java.tpl": (
            stage
            / "business"
            / "application"
            / "single"
            / f"{context['PROJECT_ID']}-application"
            / "src"
            / "main"
            / "java"
            / context["PACKAGE_PATH"]
            / "config"
            / "AOTConfiguration.java"
        ),
        "backend/yaml-runtime-hints.java.tpl": (
            stage
            / "business"
            / "application"
            / "single"
            / f"{context['PROJECT_ID']}-application"
            / "src"
            / "main"
            / "java"
            / context["PACKAGE_PATH"]
            / "config"
            / "YamlRuntimeHints.java"
        ),
    }
    for template_name, destination in destinations.items():
        render_template(template_name, destination, context)
    copy_system_module(stage, context)
    copy_application_resources(stage, context)


def create_database_files(stage: Path) -> None:
    files = (
        "ddl/mysql/system.sql",
        "ddl/postgre/system.sql",
        "dml/system.sql",
    )
    for relative in files:
        source = SOURCE_ROOT / "docker" / "initdb" / relative
        destination = stage / "docker" / "initdb" / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)


def create_frontend(stage: Path, context: dict[str, str]) -> None:
    app_destination = stage / "web" / "apps" / "sub-system"
    copy_tree(SOURCE_ROOT / "web" / "apps" / "sub-system", app_destination)
    render_template("frontend/package.json.tpl", stage / "web" / "package.json", context)
    render_template(
        "frontend/pnpm-workspace.yaml.tpl",
        stage / "web" / "pnpm-workspace.yaml",
        context,
    )


def validate_generated_project(stage: Path, context: dict[str, str]) -> None:
    required = (
        stage / "pom.xml",
        stage / f"{context['PROJECT_ID']}-core" / "pom.xml",
        stage / "business" / "business-system" / "business-system-server" / "pom.xml",
        stage / "business" / "application" / "distributed" / f"{context['PROJECT_ID']}-system" / "pom.xml",
        stage / "business" / "application" / "single" / f"{context['PROJECT_ID']}-application" / "pom.xml",
        stage / "web" / "apps" / "sub-system" / "package.json",
        stage / "web" / "pnpm-workspace.yaml",
    )
    missing = [str(path.relative_to(stage)) for path in required if not path.is_file()]
    if missing:
        raise GenerationError(f"生成结果缺少必要文件：{', '.join(missing)}")

    unresolved: list[str] = []
    stale_packages: list[str] = []
    for path in stage.rglob("*"):
        if not path.is_file() or not is_text_file(path):
            continue
        content = path.read_text(encoding="utf-8")
        if re.search(r"@@[A-Z0-9_]+@@", content):
            unresolved.append(str(path.relative_to(stage)))
        if "org.quyq.gwsu.system" in content:
            stale_packages.append(str(path.relative_to(stage)))
    if unresolved:
        raise GenerationError(f"生成结果存在未解析模板变量：{', '.join(unresolved)}")
    if stale_packages:
        raise GenerationError(f"生成结果存在未转换的 system 包名：{', '.join(stale_packages)}")

    for pom in stage.rglob("pom.xml"):
        try:
            ElementTree.parse(pom)
        except ElementTree.ParseError as error:
            raise GenerationError(f"POM 不是有效 XML：{pom.relative_to(stage)}：{error}") from error

    package_json = json.loads(
        (stage / "web" / "apps" / "sub-system" / "package.json").read_text(
            encoding="utf-8"
        )
    )
    if package_json.get("name") != "sub-system":
        raise GenerationError("system 子应用名称必须为 sub-system")
    if package_json.get("dependencies", {}).get("@gwsu/core") != "workspace:*":
        raise GenerationError("system 子应用必须使用 workspace:* 引用 @gwsu/core")


def create_metadata(stage: Path, context: dict[str, str]) -> None:
    metadata = {
        "generatorVersion": 1,
        "projectId": context["PROJECT_ID"],
        "groupPrefix": context["GROUP_PREFIX"],
        "groupId": context["GROUP_ID"],
        "platformVersion": context["PLATFORM_VERSION"],
        "gwsuCore": context["GWSU_CORE_RELATIVE_PATH"],
    }
    (stage / ".project-generator.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    render_template("README.md.tpl", stage / "README.md", context)
    render_template("gitignore.tpl", stage / ".gitignore", context)


def generate(output_dir: Path, context: dict[str, str]) -> None:
    if output_dir.exists():
        raise GenerationError(f"目标目录已存在，不会覆盖：{output_dir}")
    output_dir.parent.mkdir(parents=True, exist_ok=True)
    temp_dir = Path(tempfile.mkdtemp(prefix=f".{output_dir.name}-", dir=output_dir.parent))
    try:
        create_backend(temp_dir, context)
        create_database_files(temp_dir)
        create_frontend(temp_dir, context)
        create_metadata(temp_dir, context)
        validate_generated_project(temp_dir, context)
        temp_dir.replace(output_dir)
    except Exception:
        shutil.rmtree(temp_dir, ignore_errors=True)
        raise


def main() -> int:
    args = parse_args()
    try:
        validate_args(args.project_id, args.group_prefix)
        output_dir = (
            args.output.expanduser().resolve()
            if args.output
            else (SOURCE_ROOT.parent / f"ratel-{args.project_id}").resolve()
        )
        platform_version = read_platform_version()
        context = template_context(
            args.project_id,
            args.group_prefix,
            output_dir,
            platform_version,
        )
        print(f"项目标识：{args.project_id}")
        print(f"项目 groupId：{context['GROUP_ID']}")
        print(f"公共平台版本：{platform_version}")
        print(f"输出目录：{output_dir}")
        print(f"@gwsu/core：{context['GWSU_CORE_RELATIVE_PATH']}")
        if args.dry_run:
            print("dry-run 完成，未写入文件。")
            return 0
        generate(output_dir, context)
        print(f"项目创建完成：{output_dir}")
        print(f"后端验证：cd {output_dir} && mvn validate")
        print(f"前端安装：cd {output_dir / 'web'} && pnpm install")
        return 0
    except GenerationError as error:
        print(f"创建失败：{error}", file=sys.stderr)
        return 2
    except Exception as error:
        print(f"创建失败：{error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
