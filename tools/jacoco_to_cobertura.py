#!/usr/bin/env python3
"""Convert a JaCoCo XML report to the Cobertura XML shape GitHub accepts."""

from __future__ import annotations

import argparse
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


@dataclass
class LineCoverage:
    number: int
    covered_instructions: int
    missed_branches: int
    covered_branches: int

    @property
    def is_covered(self) -> bool:
        return self.covered_instructions > 0

    @property
    def branch_total(self) -> int:
        return self.missed_branches + self.covered_branches


@dataclass
class FileCoverage:
    package_name: str
    filename: str
    path: str
    lines: list[LineCoverage] = field(default_factory=list)

    @property
    def class_name(self) -> str:
        stem = Path(self.filename).stem
        dotted_package = self.package_name.replace("/", ".")
        return f"{dotted_package}.{stem}" if dotted_package else stem


@dataclass
class CoverageStats:
    lines_valid: int = 0
    lines_covered: int = 0
    branches_valid: int = 0
    branches_covered: int = 0

    def add_file(self, file_coverage: FileCoverage) -> None:
        self.lines_valid += len(file_coverage.lines)
        self.lines_covered += sum(1 for line in file_coverage.lines if line.is_covered)
        self.branches_valid += sum(line.branch_total for line in file_coverage.lines)
        self.branches_covered += sum(line.covered_branches for line in file_coverage.lines)

    @property
    def line_rate(self) -> float:
        return rate(self.lines_covered, self.lines_valid)

    @property
    def branch_rate(self) -> float:
        return rate(self.branches_covered, self.branches_valid)


def rate(covered: int, total: int) -> float:
    return covered / total if total else 0.0


def fmt_rate(value: float) -> str:
    return f"{value:.6f}"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jacoco_xml", type=Path, help="Path to the JaCoCo XML input.")
    parser.add_argument("cobertura_xml", type=Path, help="Path to write Cobertura XML.")
    parser.add_argument(
        "--source-root",
        type=Path,
        required=True,
        help="Main source root used to resolve package/sourcefile paths.",
    )
    parser.add_argument(
        "--project-root",
        type=Path,
        default=Path.cwd(),
        help="Repository root used to make Cobertura filenames repository-relative.",
    )
    return parser.parse_args()


def parse_jacoco_report(report_path: Path, source_root: Path, project_root: Path) -> list[FileCoverage]:
    if not report_path.is_file():
        raise ValueError(f"JaCoCo XML report not found: {report_path}")

    try:
        root = ET.parse(report_path).getroot()
    except ET.ParseError as exc:
        raise ValueError(f"JaCoCo XML report is malformed: {exc}") from exc

    files: list[FileCoverage] = []
    for package in root.findall("package"):
        package_name = package.attrib.get("name", "")
        for sourcefile in package.findall("sourcefile"):
            lines = [
                LineCoverage(
                    number=int(line.attrib["nr"]),
                    covered_instructions=int(line.attrib.get("ci", "0")),
                    missed_branches=int(line.attrib.get("mb", "0")),
                    covered_branches=int(line.attrib.get("cb", "0")),
                )
                for line in sourcefile.findall("line")
            ]
            if not lines:
                continue

            source_name = sourcefile.attrib.get("name")
            if not source_name:
                raise ValueError(f"JaCoCo sourcefile without a name in package {package_name!r}.")

            source_path = source_root.joinpath(*package_name.split("/"), source_name)
            files.append(
                FileCoverage(
                    package_name=package_name,
                    filename=source_name,
                    path=repo_relative_path(source_path, project_root),
                    lines=lines,
                )
            )

    if not files:
        raise ValueError("JaCoCo XML report contains no coverable source lines.")

    return files


def repo_relative_path(path: Path, project_root: Path) -> str:
    resolved_path = path.resolve()
    resolved_root = project_root.resolve()
    try:
        relative = resolved_path.relative_to(resolved_root)
    except ValueError:
        relative = path
    return relative.as_posix()


def grouped_by_package(files: Iterable[FileCoverage]) -> dict[str, list[FileCoverage]]:
    packages: dict[str, list[FileCoverage]] = {}
    for file_coverage in files:
        packages.setdefault(file_coverage.package_name, []).append(file_coverage)
    return packages


def build_cobertura_xml(files: list[FileCoverage], source_root: Path, project_root: Path) -> ET.ElementTree:
    total_stats = CoverageStats()
    for file_coverage in files:
        total_stats.add_file(file_coverage)

    coverage = ET.Element(
        "coverage",
        {
            "line-rate": fmt_rate(total_stats.line_rate),
            "branch-rate": fmt_rate(total_stats.branch_rate),
            "lines-covered": str(total_stats.lines_covered),
            "lines-valid": str(total_stats.lines_valid),
            "branches-covered": str(total_stats.branches_covered),
            "branches-valid": str(total_stats.branches_valid),
            "complexity": "0",
            "version": "jacoco-to-cobertura",
            "timestamp": str(int(time.time())),
        },
    )

    sources = ET.SubElement(coverage, "sources")
    ET.SubElement(sources, "source").text = "."

    packages_element = ET.SubElement(coverage, "packages")
    for package_name, package_files in sorted(grouped_by_package(files).items()):
        package_stats = CoverageStats()
        for file_coverage in package_files:
            package_stats.add_file(file_coverage)

        package_element = ET.SubElement(
            packages_element,
            "package",
            {
                "name": package_name.replace("/", "."),
                "line-rate": fmt_rate(package_stats.line_rate),
                "branch-rate": fmt_rate(package_stats.branch_rate),
                "complexity": "0",
            },
        )
        classes_element = ET.SubElement(package_element, "classes")

        for file_coverage in sorted(package_files, key=lambda item: item.path):
            file_stats = CoverageStats()
            file_stats.add_file(file_coverage)
            class_element = ET.SubElement(
                classes_element,
                "class",
                {
                    "name": file_coverage.class_name,
                    "filename": file_coverage.path,
                    "line-rate": fmt_rate(file_stats.line_rate),
                    "branch-rate": fmt_rate(file_stats.branch_rate),
                    "complexity": "0",
                },
            )
            ET.SubElement(class_element, "methods")
            lines_element = ET.SubElement(class_element, "lines")

            for line in sorted(file_coverage.lines, key=lambda item: item.number):
                line_attributes = {
                    "number": str(line.number),
                    "hits": "1" if line.is_covered else "0",
                    "branch": "true" if line.branch_total else "false",
                }
                if line.branch_total:
                    line_attributes["condition-coverage"] = condition_coverage(line)
                line_element = ET.SubElement(lines_element, "line", line_attributes)
                if line.branch_total:
                    conditions = ET.SubElement(line_element, "conditions")
                    ET.SubElement(
                        conditions,
                        "condition",
                        {
                            "number": "0",
                            "type": "jump",
                            "coverage": condition_coverage(line),
                        },
                    )

    return ET.ElementTree(coverage)


def condition_coverage(line: LineCoverage) -> str:
    percentage = rate(line.covered_branches, line.branch_total) * 100
    return f"{percentage:.0f}% ({line.covered_branches}/{line.branch_total})"


def main() -> int:
    args = parse_args()
    try:
        files = parse_jacoco_report(args.jacoco_xml, args.source_root, args.project_root)
        cobertura = build_cobertura_xml(files, args.source_root, args.project_root)
        args.cobertura_xml.parent.mkdir(parents=True, exist_ok=True)
        ET.indent(cobertura, space="  ")
        cobertura.write(args.cobertura_xml, encoding="utf-8", xml_declaration=True)
    except ValueError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
