# NCI-META Angular 20 Frontend

This workspace is the NM-313 Angular 20 migration scaffold. It runs beside the
legacy AngularJS UI in `src/main/webapp`.

## Local Servers

- Legacy MEME backend and AngularJS UI: `http://localhost:8080/umls-server-rest`
- Angular 20 dev server: `http://localhost:4200`

The default Angular proxy forwards `/umls-server-rest/**` to
`http://localhost:8080`. The dev proxy forwards the same path to
`http://localhost:18080` for integration-test style backend runs.

## Commands

The workspace pins a local Node 24 dev dependency for Angular CLI commands
because Angular 20 supports Node `^20.19.0 || ^22.12.0 || ^24.0.0`. The local
`node` package keeps `npm start` and `npm run build` stable even if the shell's
default Node is newer.

```bash
npm install
npm start
npm run start:dev
npm run build
npm test
npm run e2e
```

The Cypress scripts unset `ELECTRON_RUN_AS_NODE` because Electron-based Cypress
runners fail to start when launched from shells that inherit that VS Code/Codex
environment variable.

The root `Makefile` wraps the same commands with `make frontend-*` targets.
