import Image from "next/image";

/**
 * Logo du jeu (détouré par mods/art/make_site_logo.py) avec « Online » posé dessus en bas à droite,
 * penché comme le « pet » : texte SVG (net à toutes les tailles), rouge dégradé et gros contour blanc.
 */
export default function LogoOnline({ width = 360, priority = false }: { width?: number; priority?: boolean }) {
  const height = Math.round((width * 132) / 391);
  return (
    <span className="relative inline-block select-none" style={{ width, height: height * 1.18 }}>
      <Image
        src="/img/logo-petsoccer.png"
        alt="Pet Soccer"
        width={391}
        height={132}
        priority={priority}
        style={{ width, height }}
        className="drop-shadow-[0_6px_14px_rgba(0,0,0,0.45)]"
      />
      <svg
        viewBox="0 0 230 74"
        aria-label="Online"
        role="img"
        className="absolute"
        style={{
          width: width * 0.5,
          right: -width * 0.04,
          bottom: 0,
          transform: "rotate(-7deg)",
          filter: "drop-shadow(0 3px 0 rgba(70,0,0,0.55)) drop-shadow(0 6px 10px rgba(0,0,0,0.4))",
          overflow: "visible",
        }}
      >
        <defs>
          <linearGradient id="online-fill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#ff7a5c" />
            <stop offset="0.45" stopColor="#ff2a1f" />
            <stop offset="1" stopColor="#a80a10" />
          </linearGradient>
        </defs>
        <text
          x="115"
          y="58"
          textAnchor="middle"
          fontSize="62"
          style={{ fontFamily: "var(--font-lilita)" }}
          fill="url(#online-fill)"
          stroke="#fff"
          strokeWidth="11"
          strokeLinejoin="round"
          paintOrder="stroke"
        >
          Online
        </text>
      </svg>
    </span>
  );
}
