#!/usr/bin/env python3
"""Small dependency-free guard for Supabase SQL migration boundaries.

This does NOT parse PostgreSQL or validate RLS policies; deploy to a disposable
Supabase project and run role-based integration tests before production.
"""
from pathlib import Path
import re
import sys

paths = sorted(Path("backend/supabase").glob("*.sql"))
if not paths:
    sys.exit("No Supabase migrations found")

for path in paths:
    data = path.read_text(encoding="utf-8")
    if not data.lower().strip().startswith("--"):
        sys.exit(f"{path}: expected a comment header")
    if re.search(r"\bas\s+\$(?!\$)", data, flags=re.IGNORECASE):
        sys.exit(f"{path}: malformed AS dollar quote")
    if re.search(r"\bend\s+\$(?!\$)", data, flags=re.IGNORECASE):
        sys.exit(f"{path}: malformed END dollar quote")
    functions = len(re.findall(r"\bcreate\s+or\s+replace\s+function\b", data, re.I))
    pairs = data.count("$$") // 2
    if data.count("$$") % 2 or pairs != functions:
        sys.exit(f"{path}: expected {functions} function dollar-quote pairs; got {pairs}")
    if "commit;" not in data.lower() or "begin;" not in data.lower():
        sys.exit(f"{path}: transaction boundaries missing")
    if re.search(r"grant\s+all\s+on\s+public\.", data, re.I):
        sys.exit(f"{path}: broad privilege grant requires review")
    print(f"OK {path}: {functions} SQL functions and balanced dollar quotes")

print("Static SQL guard passed; live Supabase verification still required.")
