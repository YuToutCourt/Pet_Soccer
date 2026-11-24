"""Base Postgres des tests : jamais celle des vrais joueurs.

    python server\\testdb.py          # vide la base de test (la crée si besoin) et affiche son adresse

Adresse : variable TEST_DATABASE_URL, sinon la base pet_soccer_test du Postgres de docker-compose.yml lancé sur ce
PC (docker compose up -d postgres). Utilisé par test_server.py, test_store.py et mods\\online-test.ps1.
"""
import os
import sys

import psycopg

DEFAULT_URL = "postgresql://pet-soccer:{password}@127.0.0.1:54320/pet_soccer_test"


def test_url():
    url = os.environ.get("TEST_DATABASE_URL")
    if url:
        return url
    return DEFAULT_URL.format(password=os.environ.get("POSTGRES_PASSWORD", "toor"))


def fresh_test_db(url=None, keep=False):
    """Crée la base de test si elle n'existe pas, puis la vide entièrement (sauf keep). Renvoie son adresse."""
    url = url or test_url()
    info = psycopg.conninfo.conninfo_to_dict(url)
    name = info.get("dbname")
    if not name or "test" not in name:
        raise SystemExit("refusé : la base de test doit avoir « test » dans son nom (%s)" % name)
    admin = dict(info, dbname="postgres")
    with psycopg.connect(psycopg.conninfo.make_conninfo(**admin), autocommit=True) as conn:
        if conn.execute("SELECT 1 FROM pg_database WHERE datname = %s", (name,)).fetchone() is None:
            conn.execute('CREATE DATABASE "%s"' % name.replace('"', ""))
    if keep:
        return url
    with psycopg.connect(url, autocommit=True) as conn:
        conn.execute("DROP SCHEMA public CASCADE")
        conn.execute("CREATE SCHEMA public")
    return url


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if a != "--keep"]
    print(fresh_test_db(args[0] if args else None, keep="--keep" in sys.argv[1:]))
