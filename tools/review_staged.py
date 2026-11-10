"""Review every indexed file before committing. Emits finding locations, never secret values.

This is a bounded baseline check, not a replacement for manual security review.
Optional --original-auth compares the old credential privately without printing it.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import subprocess
import sys

parser = argparse.ArgumentParser()
parser.add_argument('--original-auth', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]


def git(*arguments):
    return subprocess.check_output(['git', '-C', str(root), *arguments])


paths = [p.decode('utf-8') for p in git('ls-files', '-z').split(b'\0') if p]
findings = []
known_secret = None
if args.original_auth:
    original = args.original_auth.read_text(encoding='utf-8-sig')
    match = re.search(r'\bPASS\s*=\s*"([^"]+)"', original)
    if not match:
        sys.exit('Unable to privately identify the historical credential; review stopped.')
    known_secret = match.group(1).encode('utf-8')

binary_assets = json.loads(git('show', ':docs/resource-provenance.json'))
schema_manifest = json.loads(git('show', ':docs/schema-provenance.json'))
schema_path = schema_manifest['resource']
if schema_path != 'docs/reference/mysql/V001__historical_schema.sql.txt':
    sys.exit('Unexpected sanitized schema path; review stopped.')
postgres_migrations = json.loads(git('show', ':docs/postgresql-migrations.json'))
if set(postgres_migrations) != {'src/test/resources/db/postgresql/V1__reference_data.sql'}:
    sys.exit('Unexpected PostgreSQL migration paths; review stopped.')
patterns = [
    ('private key', re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----')),
    ('GitHub token', re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})')),
    ('embedded JDBC credentials', re.compile(rb'jdbc:(?:mysql|postgresql):[^\r\n"<>]*[?&](?:user|password)=', re.I)),
    ('private absolute user path', re.compile(rb'[A-Za-z]:[\\/]+Users[\\/]+(?!<)[A-Za-z0-9_. -]+[\\/]', re.I)),
    ('literal password assignment', re.compile(rb'\b(?:PASS|PASSWORD|TOKEN|SECRET)\s*=\s*"[^"\r\n]+"', re.I)),
    ('SQL sample inserts', re.compile(rb'^\s*INSERT\s+INTO\s+`', re.I | re.M)),
]
for path in paths:
    entry = git('ls-files', '-s', '--', path).decode('utf-8').split()[0]
    if entry not in {'100644', '100755'}:
        findings.append((path, 'unexpected link/submodule/file mode'))
        continue
    data = git('show', ':' + path)
    lower = path.lower()
    if (any(part in lower.split('/') for part in ['target', 'bin', 'backups', 'private', 'dumps', 'uploads', '.metadata'])
            or (lower.endswith('.sql') and path not in postgres_migrations)
            or lower.endswith(('.dump', '.zip', '.jar', '.class', '.key', '.pem'))
            or (Path(path).name.startswith('.env') and not path.endswith('.example'))
            or (lower.startswith('config/') and not path.endswith('.example'))):
        findings.append((path, 'private/generated material is staged'))
    if path == schema_path:
        if hashlib.sha256(data).hexdigest() != schema_manifest['sha256']:
            findings.append((path, 'schema differs from reviewed provenance'))
        ddl = re.sub(rb'(?m)^--[^\r\n]*', b'', data)
        statements = [s.strip() for s in ddl.split(b';') if s.strip()]
        if (len(statements) != 14 or any(not re.match(rb'CREATE TABLE `sobol_dev`\.`[a-z_]+` \(', s) for s in statements)
                or re.search(rb'\b(?:INSERT|TRUNCATE|DROP|USE|GRANT|REVOKE|CALL|LOAD)\b', ddl, re.I)
                or re.search(rb'\baccounts\b', ddl, re.I)):
            findings.append((path, 'only the 14 sanitized development CREATE TABLE statements are allowed'))
    if path in postgres_migrations:
        if hashlib.sha256(data).hexdigest() != postgres_migrations[path]:
            findings.append((path, 'PostgreSQL migration differs from reviewed digest'))
        ddl = re.sub(rb'(?m)^--[^\r\n]*', b'', data)
        statements = [s.strip() for s in ddl.split(b';') if s.strip()]
        names = [re.match(rb'CREATE TABLE sobol\.([a-z_]+) \(', s) for s in statements]
        if (len(names) != 3 or any(m is None for m in names)
                or {m.group(1) for m in names if m} != {b'category', b'supplier', b'customer'}
                or re.search(rb'\b(?:INSERT|UPDATE|DELETE|TRUNCATE|DROP|GRANT|REVOKE|COPY|accounts|AUTO_INCREMENT)\b', ddl, re.I)):
            findings.append((path, 'only the reviewed PostgreSQL master-data CREATE TABLE statements are allowed'))
    if known_secret and known_secret in data:
        findings.append((path, 'historical database credential is present'))
    if b'\0' in data or lower.endswith(('.png', '.jpg', '.jpeg', '.gif', '.ico')):
        expected = binary_assets.get(path)
        if not expected or hashlib.sha256(data).hexdigest() != expected['sha256']:
            findings.append((path, 'unapproved binary/personal-image risk'))
        continue
    for label, pattern in patterns:
        if pattern.search(data):
            findings.append((path, label))

if findings:
    for path, label in findings:
        print(f'BLOCKED: {path}: {label}')
    sys.exit(1)
print(f'PASS: reviewed all {len(paths)} indexed files; no prohibited content found by baseline checks.')
