import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  async redirects() {
    return [
      // l'ancien site avait le classement (et sa recherche) sur « / »
      { source: "/", has: [{ type: "query", key: "q" }], destination: "/classement", permanent: false },
    ];
  },
};

export default nextConfig;
