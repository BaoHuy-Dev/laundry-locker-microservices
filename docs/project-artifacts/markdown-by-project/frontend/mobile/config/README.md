# Config

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

This folder contains configuration files and constants.

## Structure

- **env.ts** - Environment variables
- **api.config.ts** - API endpoints configuration
- **app.config.ts** - App-wide configuration

## Examples

```typescript
// env.ts
export const API_URL =
  process.env.EXPO_PUBLIC_API_URL || "http://localhost:3000";
export const PAYMENT_KEY = process.env.EXPO_PUBLIC_PAYMENT_KEY;

// api.config.ts
export const API_ENDPOINTS = {
  AUTH: "/auth",
  LOCKERS: "/lockers",
  ORDERS: "/orders",
  USERS: "/users",
};
```
