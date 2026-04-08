import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  workers: 1,
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'on-first-retry'
  },
  projects: [
    {
      name: 'msedge',
      use: { ...devices['Desktop Edge'], channel: 'msedge' }
    }
  ],
  webServer: [
    {
      command: 'powershell -NoProfile -ExecutionPolicy Bypass -File ../scripts/run-backend-e2e.ps1',
      url: 'http://127.0.0.1:8083/swagger-ui.html',
      reuseExistingServer: true,
      timeout: 180_000
    },
    {
      command: 'npm run dev -- --host 127.0.0.1 --port 5173',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: true,
      timeout: 120_000
    }
  ]
})
