# Types

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

This folder contains TypeScript type definitions and interfaces.

## Structure

- **user.ts** - User-related types
- **locker.ts** - Locker-related types
- **order.ts** - Order-related types
- **api.ts** - API request/response types

## Examples

```typescript
export interface User {
  id: string;
  name: string;
  email: string;
  phone: string;
}

export interface Locker {
  id: string;
  number: string;
  size: "small" | "medium" | "large";
  status: "available" | "occupied" | "maintenance";
}
```
