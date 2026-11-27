"use client";

// Recharge les données de la page (composants serveur) toutes les `every` ms, tant que l'onglet est visible.
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function AutoRefresh({ every = 4000 }: { every?: number }) {
  const router = useRouter();
  useEffect(() => {
    const tick = () => {
      if (document.visibilityState === "visible") router.refresh();
    };
    const id = setInterval(tick, every);
    document.addEventListener("visibilitychange", tick);
    return () => {
      clearInterval(id);
      document.removeEventListener("visibilitychange", tick);
    };
  }, [router, every]);
  return null;
}
