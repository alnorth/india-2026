/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}',
  ],
  theme: {
    extend: {
      colors: {
        // Terracotta - earthy red-orange (primary)
        terracotta: {
          50: '#fef3f0',
          100: '#fdd8cc',
          200: '#f9a885',
          300: '#e57347',
          400: '#c94d1f',
          500: '#a83f18',
          600: '#8b3419',
          700: '#6e2a14',
          800: '#4f1e0e',
          900: '#311309',
        },
        // Forest green - deep greens for nature
        forest: {
          50: '#f0f7f4',
          100: '#d4ebe0',
          200: '#a8d7c0',
          300: '#6fb897',
          400: '#4a9d6f',
          500: '#38824f',
          600: '#2d6e47',
          700: '#24593a',
          800: '#1a4229',
          900: '#102918',
        },
        // Saffron - warm yellow-orange (cultural)
        saffron: {
          50: '#fef9f0',
          100: '#fdefd4',
          200: '#fbd488',
          300: '#f7b547',
          400: '#e69a1f',
          500: '#c5821a',
          600: '#a86f16',
          700: '#855912',
          800: '#5e3f0d',
          900: '#3a2708',
        },
        // Earth - rich brown tones
        earth: {
          50: '#faf7f4',
          100: '#ebe3da',
          200: '#d6c7b5',
          300: '#b89f84',
          400: '#9d7e5f',
          500: '#826647',
          600: '#6b5638',
          700: '#56452d',
          800: '#3f3221',
          900: '#2a2116',
        },
        // Sand - warm beige neutrals (replacing gray)
        sand: {
          50: '#faf8f5',
          100: '#f0ebe3',
          200: '#e3dbd0',
          300: '#d0c5b5',
          400: '#b5a794',
          500: '#988d7d',
          600: '#7d7366',
          700: '#635c52',
          800: '#4a453e',
          900: '#312e2a',
        },
        // Teal - ocean/peacock accent
        teal: {
          50: '#f0f8f9',
          100: '#d4eef2',
          200: '#a9dde5',
          300: '#6fc3d1',
          400: '#46a3b3',
          500: '#358794',
          600: '#2d7d8b',
          700: '#24636e',
          800: '#1a474f',
          900: '#102c31',
        },
      },
    },
  },
  plugins: [],
}
