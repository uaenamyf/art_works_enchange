import type { Config } from "tailwindcss";

/**
 * Tailwind 配置：把 Design Token 暴露为 Tailwind 颜色 / 圆角 / 阴影 token，
 * 业务组件优先使用 utility class；只在跨 utility 表达不清时写自定义 CSS。
 */
const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // Neutrals
        ink: {
          900: "#1A1A1A",
          700: "#4A4A4A",
          500: "#777777",
          300: "#D8D8D8",
          100: "#F5F5F3",
          50: "#FAFAF8"
        },
        // Brand
        brand: {
          500: "#C97B4B"
        },
        // Semantic
        success: "#2E7D5B",
        error: "#D64545",
        warning: "#D89B32"
      },
      borderRadius: {
        token: "8px"
      },
      maxWidth: {
        gallery: "1440px"
      },
      fontFamily: {
        sans: ["var(--font-sans)", "system-ui", "sans-serif"],
        serif: ["var(--font-serif)", "Georgia", "serif"]
      }
    }
  },
  plugins: []
};

export default config;
