# Context

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

This folder contains React Context providers for global state management.

## Structure

- **AuthContext.tsx** - Authentication state
- **LockerContext.tsx** - Locker state management
- **ThemeContext.tsx** - Theme and appearance

## Examples

```typescript
// AuthContext.tsx
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  // ... authentication logic
  return (
    <AuthContext.Provider value={{ user, setUser }}>...</AuthContext.Provider>
  );
};
```
