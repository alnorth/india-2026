# India 2026 Cycle Tour

This is a monorepo containing the India 2026 cycle tour project.

## Project Structure

```
india-2026/
├── website/          # Astro static site for the web presence
│   ├── src/         # Website source code
│   ├── content/     # Markdown content and media
│   ├── public/      # Static assets
│   └── README.md    # Website-specific documentation
└── (future: android app will go here)
```

## Projects

### Website

A statically generated site built with Astro that documents the cycle tour from Kanyakumari to Kashmir.

**Documentation**: See [website/README.md](./website/README.md)

**Quick Start**:
```bash
cd website
npm install
npm run dev
```

**Deployment**: Automatically deploys to AWS Amplify on push to main branch.

## Future Additions

This structure is prepared for future expansion with an Android app project that will share content and assets with the website.
