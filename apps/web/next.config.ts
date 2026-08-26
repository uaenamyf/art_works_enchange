import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Docker runtime copies standalone output；本机 Windows 构建不启用，避免 pnpm 软链接因权限失败。
  output: process.env.NEXT_OUTPUT === "standalone" ? "standalone" : undefined,
  experimental: {
    typedRoutes: false
  },
  images: {
    // SEC-WEB-003: tighten before production. Currently permissive to allow
    // mock Unsplash URLs during Week 1-2. Add production CDN domain(s) and
    // remove the wildcard before launch.
    remotePatterns: [
      { protocol: "https", hostname: "images.unsplash.com" },
      { protocol: "http", hostname: "localhost" }
    ]
  },
  // SEC-WEB-001: baseline security headers applied to every response.
  // HSTS is intentionally omitted (per OpenAI security spec: avoid without
  // understanding the lasting impact; we are not yet on managed HTTPS).
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "X-Frame-Options", value: "DENY" },
          {
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=(), interest-cohort=()"
          }
          // CSP: defer until we have a clear inventory of inline scripts and
          // image hosts. Track as follow-up alongside the first production
          // deployment of Media (Week 4) so we can write a tight policy.
        ]
      }
    ];
  },
  poweredByHeader: false
};

export default nextConfig;
