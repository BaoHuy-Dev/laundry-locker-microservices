# Models

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

This folder contains business logic models and data structures.

## Structure

- **User.model.ts** - User business logic
- **Locker.model.ts** - Locker business logic
- **Order.model.ts** - Order business logic

## Examples

```typescript
// User.model.ts
export class UserModel {
  constructor(data: User) { ... }

  getFullName(): string { ... }
  isVerified(): boolean { ... }
  canRentLocker(): boolean { ... }
}
```
