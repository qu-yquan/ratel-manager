{
  "private": true,
  "name": "@@PROJECT_ID@@-web-workspace",
  "version": "1.0.0",
  "scripts": {
    "dev:system": "pnpm --filter sub-system dev",
    "build:core": "pnpm --filter @gwsu/core build",
    "build:all": "pnpm build:core && pnpm --filter sub-system build",
    "clean": "pnpm -r exec rm -rf node_modules dist .umi"
  },
  "devDependencies": {
    "@types/react": "^19.2.0",
    "@types/react-dom": "^19.2.0",
    "typescript": "^4.9.0"
  },
  "pnpm": {
    "overrides": {
      "react": "^19.2.0",
      "react-dom": "^19.2.0"
    }
  }
}
