# Admin API Implementation with Zod & RTK Query

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

This directory contains the complete Admin API implementation using RTK Query with Zod validation.

## Structure

```
admin/
├── index.ts          # Re-exports all admin API modules
├── dashboard.ts      # Dashboard overview API
├── users.ts          # User management API with Zod validation
├── stores.ts         # Store management API with Zod validation
├── services.ts       # Service management API with Zod validation
├── lockers.ts        # Locker & Box management API with Zod validation
├── orders.ts         # Order management API with Zod validation
├── payments.ts       # Payment management API with Zod validation
├── scheduler.ts      # Scheduler management API
├── loyalty.ts        # Loyalty management API with Zod validation
└── partners.ts       # Partner management API with Zod validation
```

## Features

### 1. Zod Schema Validation
All request bodies are validated using Zod schemas before sending to the API:

- **CreateUserRequest** - Validates email, password, names, phone, roles
- **UpdateUserRequest** - Validates name, email, imageUrl
- **CreateStoreRequest** - Validates store data
- **CreateServiceRequest** - Validates service data with price validation
- **CreateLockerRequest** - Validates locker code, name, storeId
- **CreateBoxRequest** - Validates box number
- **AdjustUserPointsRequest** - Validates points adjustment
- **RejectPartnerRequest** - Validates rejection reason

### 2. RTK Query Implementation
All APIs follow RTK Query best practices:

- **Tags** for cache invalidation
- **Optimistic updates** support
- **Automatic caching**
- **TypeScript** support throughout

### 3. API Modules

#### User Management (`users.ts`)
- `useGetAllUsersQuery` - Get paginated users
- `useGetUserByIdQuery` - Get user by ID
- `useCreateUserMutation` - Create new user (with Zod validation)
- `useUpdateUserMutation` - Update user (with Zod validation)
- `useUpdateUserStatusMutation` - Update user status (with Zod validation)
- `useUpdateUserRolesMutation` - Update user roles (with Zod validation)
- `useDeleteUserMutation` - Delete user

#### Store Management (`stores.ts`)
- `useGetAllStoresQuery` - Get paginated stores
- `useGetStoreByIdQuery` - Get store by ID
- `useCreateStoreMutation` - Create new store (with Zod validation)
- `useUpdateStoreMutation` - Update store (with Zod validation)
- `useUpdateStoreStatusMutation` - Update store status (with Zod validation)
- `useDeleteStoreMutation` - Delete store

#### Service Management (`services.ts`)
- `useGetAllServicesQuery` - Get paginated services
- `useGetServiceByIdQuery` - Get service by ID
- `useCreateServiceMutation` - Create new service (with Zod validation)
- `useUpdateServiceMutation` - Update service (with Zod validation)
- `useUpdateServicePriceMutation` - Update service price (with Zod validation)
- `useUpdateServiceStatusMutation` - Update service status (with Zod validation)
- `useDeleteServiceMutation` - Delete service

#### Locker & Box Management (`lockers.ts`)
- `useGetAllLockersQuery` - Get paginated lockers
- `useGetLockersByStoreQuery` - Get lockers by store
- `useGetLockerByIdQuery` - Get locker by ID
- `useCreateLockerMutation` - Create new locker (with Zod validation)
- `useUpdateLockerMutation` - Update locker (with Zod validation)
- `useSetLockerMaintenanceMutation` - Set maintenance mode (with Zod validation)
- `useAddBoxToLockerMutation` - Add box to locker (with Zod validation)
- `useUpdateBoxStatusMutation` - Update box status (with Zod validation)
- `useDeleteLockerMutation` - Delete locker

#### Order Management (`orders.ts`)
- `useGetAllOrdersQuery` - Get paginated orders with status filter
- `useGetOrderByIdQuery` - Get order by ID
- `useUpdateOrderStatusMutation` - Update order status (with Zod validation)
- `useGetOrderStatisticsQuery` - Get order statistics
- `useGetRevenueReportQuery` - Get revenue report

#### Payment Management (`payments.ts`)
- `useGetAllPaymentsQuery` - Get paginated payments with status filter
- `useGetPaymentByIdQuery` - Get payment by ID
- `useUpdatePaymentStatusMutation` - Update payment status (with Zod validation)

#### Scheduler Management (`scheduler.ts`)
- `useTriggerAutoCancelMutation` - Trigger auto-cancel job
- `useTriggerBoxReleaseMutation` - Trigger box release job
- `useTriggerPickupRemindersMutation` - Trigger pickup reminders
- `useGetSchedulerStatusQuery` - Get scheduler status

#### Loyalty Management (`loyalty.ts`) - NEW!
- `useGetUserLoyaltySummaryQuery` - Get user loyalty summary
- `useAdjustUserPointsMutation` - Adjust user points (with Zod validation)
- `useGetUserPointsHistoryQuery` - Get user points history
- `useGetLoyaltyStatisticsQuery` - Get loyalty statistics

#### Partner Management (`partners.ts`) - NEW!
- `useGetAllPartnersQuery` - Get paginated partners with status filter
- `useGetPartnerByIdQuery` - Get partner by ID
- `useApprovePartnerMutation` - Approve partner
- `useRejectPartnerMutation` - Reject partner (with Zod validation)
- `useSuspendPartnerMutation` - Suspend partner

#### Dashboard (`dashboard.ts`)
- `useGetDashboardOverviewQuery` - Get dashboard overview metrics

## Usage Example

```typescript
import {
  useGetAllUsersQuery,
  useCreateUserMutation,
  useUpdateUserStatusMutation,
  useGetUserLoyaltySummaryQuery,
  useApprovePartnerMutation,
} from '@/stores/apis/admin';

// Query example
const { data: users, isLoading } = useGetAllUsersQuery({ page: 0, size: 20 });

// Mutation example with automatic validation
const [createUser] = useCreateUserMutation();
const handleCreate = async (userData) => {
  try {
    const result = await createUser(userData).unwrap();
    // Success - data was validated by Zod before sending
  } catch (error) {
    // Error - either validation failed or API error
  }
};
```

## Enums (from api.md)

All enums are defined in `types/admin.enum.ts`:

- `AuthProvider` - LOCAL, GOOGLE, FACEBOOK, GITHUB, ZALO, PHONE, EMAIL
- `RoleName` - USER, STAFF, ADMIN, MODERATOR, PARTNER
- `LockerStatus` - ACTIVE, INACTIVE, MAINTENANCE, DISCONNECTED
- `BoxStatus` - AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE
- `OrderType` - LAUNDRY, DRY_CLEAN, STORAGE
- `OrderStatus` - INITIALIZED, RESERVED, WAITING, COLLECTED, PROCESSING, READY, RETURNED, COMPLETED, CANCELED
- `PaymentMethod` - CASH, WALLET, BANK_TRANSFER, MOMO, VNPAY, ZALOPAY
- `PaymentStatus` - PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELED
- `PartnerStatus` - PENDING, APPROVED, REJECTED, SUSPENDED
- `PointTransactionType` - EARN, REDEEM, EXPIRE, ADJUST, BONUS, REFUND
- `StampType` - BOX, SERVICE

## Validation Utilities

Located in `lib/validation.ts`:

```typescript
import { validateWithZod, createValidator } from '@/lib/validation';
import { CreateUserRequestSchema } from '@/schemas';

// Validate data
const result = validateWithZod(CreateUserRequestSchema, userData);

// Create reusable validator
const userValidator = createValidator(CreateUserRequestSchema);
const { validate, getFieldErrors } = userValidator;
```

## Schemas

Located in `schemas/`:

- `admin.schemas.ts` - All admin request schemas
- `common.schemas.ts` - Common/pagination schemas
- `index.ts` - Re-exports all schemas
