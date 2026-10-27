#!/usr/bin/env node
/**
 * Chess Problem — relative link checker for Markdown.
 *
 * Usage:
 *   node tools/check-docs.mjs
 *
 * Checks that every relative link/image target exists. External URLs,
 * mailto/tel, pure anchors and code fences are ignored. Anchors inside a file
 * are not validated (see GETTING_STARTED troubleshooting).
 */

import { readdir, readFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const IGNORED_DIRS = new Set([
  ".git",
  ".gradle",
  ".idea",
  ".kotlin",
  ".vscode",
  "build",
  "node_modules",
  "out",
  "tmp",
]);
const LINK_RE = /!?\[[^\]]*\]\(\s*(?:<([^>]+)>|([^\s)]+))(?:\s+["'][^"']*["'])?\s*\)/g;
const REF_RE = /^\s{0,3}\[[^\]]+\]:\s*(\S+)/gm;

async function walk(dir) {
  const files = [];
  const entries = await readdir(dir, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.isDirectory()) {
      if (!IGNORED_DIRS.has(entry.name)) {
        files.push(...(await walk(path.join(dir, entry.name))));
      }
    } else if (entry.isFile() && entry.name.endsWith(".md")) {
      files.push(path.join(dir, entry.name));
    }
  }
  return files;
}

function stripCodeFences(markdown) {
  const lines = markdown.split(/\r?\n/);
  let inFence = false;
  return lines
    .map((line) => {
      if (/^\s*(```|~~~)/.test(line)) {
        inFence = !inFence;
        return "";
      }
      return inFence ? "" : line;
    })
    .join("\n");
}

function collectLinks(markdown) {
  const links = [];
  const text = stripCodeFences(markdown);
  const lines = text.split(/\r?\n/);

  lines.forEach((line, index) => {
    for (const match of line.matchAll(LINK_RE)) {
      links.push({ target: match[1] ?? match[2], line: index + 1 });
    }
  });

  for (const match of text.matchAll(REF_RE)) {
    links.push({ target: match[1], line: null });
  }

  return links;
}

function isExternal(target) {
  return (
    /^(https?:|mailto:|tel:|data:|ftp:)/i.test(target) ||
    target.startsWith("//")
  );
}

function normalizeTarget(target) {
  let value = target.trim();
  if (value.startsWith("<") && value.endsWith(">")) {
    value = value.slice(1, -1).trim();
  }
  value = value.split("#")[0].split("?")[0];
  try {
    value = decodeURIComponent(value);
  } catch {
    // Keep the raw value; the existence check will report it.
  }
  return value;
}

async function main() {
  const files = await walk(ROOT);
  const broken = [];
  let checked = 0;

  for (const file of files) {
    const markdown = await readFile(file, "utf8");
    const links = collectLinks(markdown);
    for (const link of links) {
      const target = (link.target ?? "").trim();
      if (!target || target.startsWith("#") || isExternal(target)) continue;
      const normalized = normalizeTarget(target);
      if (!normalized) continue;

      const resolved = normalized.startsWith("/")
        ? path.join(ROOT, normalized)
        : path.resolve(path.dirname(file), normalized);
      checked += 1;
      if (!existsSync(resolved)) {
        broken.push({
          file: path.relative(ROOT, file).replaceAll("\\", "/"),
          line: link.line,
          target,
        });
      }
    }
  }

  if (broken.length > 0) {
    for (const item of broken) {
      const where = item.line ? `${item.file}:${item.line}` : item.file;
      console.error(`BROKEN ${where} -> ${item.target}`);
    }
    console.error(`\n${broken.length} broken relative link(s) in ${files.length} file(s).`);
    process.exitCode = 1;
    return;
  }

  console.log(`OK  ${files.length} Markdown file(s), ${checked} relative link(s) checked.`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
