// Connexion à la base Postgres du serveur de jeu, en lecture seule (le site ne modifie jamais rien).
// DATABASE_URL (sans mot de passe) + PGPASSWORD, comme dans docker-compose.yml.
import postgres from "postgres";

type Sql = ReturnType<typeof postgres>;

const globalForDb = globalThis as unknown as { __petSoccerSql?: Sql };

const options = {
  max: 5,
  idle_timeout: 30,
  connect_timeout: 5,
  query_timeout: 10,
  onnotice: () => {},
  connection: { application_name: "pet-soccer-web", default_transaction_read_only: true },
};

export function db(): Sql {
  if (!globalForDb.__petSoccerSql) {
    const url = process.env.DATABASE_URL;
    globalForDb.__petSoccerSql = url ? postgres(url, options) : postgres(options);
  }
  return globalForDb.__petSoccerSql;
}
