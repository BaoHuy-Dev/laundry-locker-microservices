# AGENTS.md - Laundry Locker Frontend

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

File này chứa thông tin quan trọng dành cho AI coding agents làm việc với dự án Laundry Locker Frontend.

---

## Project Overview

**Tên dự án:** Laundry Locker Frontend (fe)  
**Mô tả:** Frontend cho hệ thống tủ giặt thông minh (Smart Laundry Lockers)  
**Ngôn ngữ chính:** TypeScript, Vietnamese (UI text)

Dự án là Single Page Application (SPA) với 2 loại người dùng:
- **Admin**: Quản lý toàn hệ thống
- **Partner**: Chủ cửa hàng/quản lý đối tác

---

## Technology Stack

### Core
| Tech | Version |
|------|---------|
| React | ^19.2.0 |
| TypeScript | ~5.9.3 |
| Vite | ^7.2.4 |
| React Router DOM | ^7.12.0 |

### State Management
- **Redux Toolkit** + RTK Query cho data fetching

### UI
- **Tailwind CSS** ^4.1.18
- **Shadcn/ui** components (50+ components)
- **Radix UI** primitives
- **Lucide React** ^0.562.0 - Icon duy nhất được sử dụng

### Data Table
- **TanStack Table** (@tanstack/react-table)

### Animation
- **Framer Motion** cho sidebar và transitions

### Forms & Validation
- **React Hook Form** ^7.71.1
- **Zod** ^4.3.6

### Charts
- **Recharts** ^3.6.0

---

## Project Structure

```
src/
├── components/
│   ├── ui/                    # Shadcn/Radix UI components
│   └── shared/                # Global shared components
│       ├── layout/            # Sidebar, Layout
│       ├── data-table/        # DataTable, Pagination
│       ├── status-tabs/       # Status filter tabs
│       └── page-header/       # Page header component
├── constants/                 # Constants & config
├── context/
│   └── auth-context.tsx
├── hooks/                     # Custom React hooks
├── lib/                       # Utilities
│   ├── utils.ts
│   ├── i18n.tsx
│   └── validation.ts
├── pages/
│   ├── auth/
│   ├── Admin/
│   │   ├── layout.tsx
│   │   ├── users/             # Each page is a folder
│   │   │   ├── index.tsx
│   │   │   ├── components/
│   │   │   └── hooks/
│   │   ├── orders/
│   │   ├── stores/
│   │   └── ...
│   └── Partner/
├── routes/
├── schemas/                   # Zod validation
├── stores/
│   └── apis/                  # RTK Query APIs
└── types/                     # TypeScript types
```

---

## Coding Standards

### Component Rules
- Max **150 lines** per component
- Max **7 props** per component
- Max **3 levels** nesting JSX
- Use **named exports**
- **Lucide icons only** - không dùng Ant Design icons

### Folder Structure per Page
```
pages/Admin/feature-name/
├── index.tsx              # Page component (default export)
├── components/            # Page-specific components
│   ├── FeatureTable.tsx
│   └── FeatureFilters.tsx
└── hooks/                 # Page-specific hooks
    └── useFeature.ts
```

### Import Order
```typescript
import { useState } from 'react';                    // 1. React
import { useQuery } from '@tanstack/react-query';     // 2. Libraries
import { User } from 'lucide-react';                  // 3. Icons
import { DataTable } from '~/components/shared';      // 4. Shared components
import { Button } from '~/components/ui';             // 5. UI components
import { useUsers } from './hooks/useUsers';          // 6. Local imports
import type { User } from '~/types';                  // 7. Types
```

### Path Aliases
- `~/` → `./src/` (khuyến nghị cho tất cả imports)

### Icons (Lucide Only)
```tsx
import { User, Settings, Plus, Search, MoreHorizontal } from 'lucide-react';

// Size: 16 (sm), 18 (md), 20 (lg), 24 (xl)
<User size={18} />
```

---

## Shared Components

### 1. Sidebar
```tsx
import { Sidebar } from '~/components/shared';

<Sidebar
  items={navItems}
  onSettingsClick={() => setIsSettingsOpen(true)}
/>
```

### 2. StatusTabs
```tsx
import { StatusTabs } from '~/components/shared';

const tabs = [
  { value: 'ALL', label: 'Tất cả', color: 'blue', count: 100 },
  { value: 'ACTIVE', label: 'Hoạt động', color: 'green', count: 80 },
];

<StatusTabs
  tabs={tabs}
  activeTab={status}
  onTabChange={setStatus}
/>
```

### 3. DataTable
```tsx
import { DataTable } from '~/components/shared';
import { createColumnHelper } from '@tanstack/react-table';

const columnHelper = createColumnHelper<DataType>();

const columns = [
  columnHelper.accessor('field', {
    header: 'Header',
    cell: ({ row }) => <span>{row.original.field}</span>,
  }),
];

<DataTable
  columns={columns}
  data={data}
  isLoading={isLoading}
/>
```

### 4. PageHeader
```tsx
import { PageHeader } from '~/components/shared';

<PageHeader
  title="Quản lý người dùng"
  description="Mô tả page"
  action={{
    label: 'Thêm mới',
    onClick: handleAdd,
    icon: Plus,
  }}
/>
```

---

## State Management

### RTK Query Pattern
```typescript
// stores/apis/admin/feature.ts
export const featureApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getItems: builder.query<ApiResponse<Page<Item>>, PageableRequest>({
      query: (params) => ({ url: ENDPOINTS.ITEMS, params }),
      providesTags: ['Items'],
    }),
  }),
});
```

### Custom Hooks Pattern
```typescript
// pages/Admin/feature/hooks/useFeature.ts
export function useFeature() {
  const [status, setStatus] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  
  const { data, isLoading } = useGetItemsQuery({ ... });
  
  const filteredData = useMemo(() => {
    // Filter logic
  }, [data, status, searchQuery]);
  
  return {
    data: filteredData,
    isLoading,
    status,
    setStatus,
    // ...
  };
}
```

---

## Design System

### Colors
- Primary: `blue-600`
- Secondary: `blue-950`
- Background: `gray-50`
- Card: `white`

### Status Colors
- Active: `green`
- Pending: `yellow`
- Processing: `purple`
- Inactive/Error: `red`

### Spacing
- Page: `p-6`
- Card: `p-6`
- Section gap: `space-y-6`

---

## Build Commands

```bash
npm run dev      # Dev server (port 3000)
npm run build    # Production build
npm run lint     # ESLint
```

---

## Refactoring Guide

Khi tạo page mới, follow this pattern:

1. **Tạo folder** `pages/Admin/feature-name/`
2. **Tạo hook** `hooks/useFeature.ts` - logic và data fetching
3. **Tạo components** trong `components/`
4. **Tạo page** `index.tsx` - compose components
5. **Update routes** - thêm vào `routes-config.tsx`

Xem `refactor-todo.md` để biết tiến độ refactor.

---

*Last updated: 2026-02-27*
