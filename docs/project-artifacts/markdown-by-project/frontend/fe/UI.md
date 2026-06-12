# UI Design System - Laundry Locker

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Hệ thống thiết kế giao diện cho Laundry Locker Frontend.

---

## Màu sắc (Colors)

### Primary Palette (Xanh dương)
| Token | Mã màu | Usage |
|-------|--------|-------|
| `primary-50` | `#eff6ff` | Background nhẹ |
| `primary-100` | `#dbeafe` | Hover states |
| `primary-200` | `#bfdbfe` | Borders |
| `primary-500` | `#3b82f6` | Interactive elements |
| `primary-600` | `#2563eb` | **Primary color** |
| `primary-700` | `#1d4ed8` | Hover primary |
| `primary-900` | `#1e3a8a` | Text headings |
| `primary-950` | `#172554` | **Sidebar background** |

### Status Colors
| Status | Màu | Token | Usage |
|--------|-----|-------|-------|
| Success | Xanh lá | `green-500` | Active, completed |
| Warning | Vàng | `yellow-500` | Pending, waiting |
| Error | Đỏ | `red-500` | Inactive, cancelled |
| Info | Xanh dương | `blue-500` | Processing |
| Neutral | Xám | `gray-500` | Default |

### Background Colors
| Token | Mã màu | Usage |
|-------|--------|-------|
| `bg-page` | `#f9fafb` (gray-50) | Page background |
| `bg-card` | `#ffffff` | Card background |
| `bg-sidebar` | `#172554` (blue-950) | Sidebar |
| `bg-hover` | `#f3f4f6` (gray-100) | Hover state |

### Text Colors
| Token | Mã màu | Usage |
|-------|--------|-------|
| `text-primary` | `#111827` (gray-900) | Headings |
| `text-secondary` | `#6b7280` (gray-500) | Descriptions |
| `text-muted` | `#9ca3af` (gray-400) | Placeholders |
| `text-white` | `#ffffff` | On dark backgrounds |

---

## Typography

### Font Stack
```css
font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
```

### Type Scale
| Level | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| H1 | 2rem (32px) | 700 | 1.2 | Page title |
| H2 | 1.5rem (24px) | 600 | 1.3 | Section title |
| H3 | 1.25rem (20px) | 600 | 1.4 | Card title |
| Body | 0.875rem (14px) | 400 | 1.5 | Body text |
| Small | 0.75rem (12px) | 400 | 1.5 | Captions, labels |

---

## Spacing System

### Base Unit: 4px
| Token | Value | Usage |
|-------|-------|-------|
| `space-1` | 4px | Tight spacing |
| `space-2` | 8px | Default gap |
| `space-3` | 12px | Button padding |
| `space-4` | 16px | Card padding |
| `space-6` | 24px | Section gap |
| `space-8` | 32px | Large sections |

### Layout Spacing
| Context | Value |
|---------|-------|
| Page padding | `p-6` (24px) |
| Card padding | `p-6` (24px) |
| Section gap | `space-y-6` (24px) |
| Element gap | `gap-4` (16px) |

---

## Components

### 1. Sidebar

#### Structure
```
┌─────────────────────────────┐
│  [Logo]         [<|] Toggle │  ← Header (h-16)
├─────────────────────────────┤
│                             │
│  [Icon]  Dashboard          │  ← Nav Item (h-10)
│  [Icon]  Users              │
│  [Icon]  Orders             │
│       ...                   │
│                             │
├─────────────────────────────┤
│  [Icon]  Settings           │  ← Footer Actions
│  [Icon]  Logout             │
│  ┌────┐  User Name          │  ← User Profile
│  │ 👤 │  Role               │
│  └────┘                     │
└─────────────────────────────┘
```

#### Specs
- **Width Expanded**: 280px
- **Width Collapsed**: 80px
- **Background**: `blue-950` (#172554)
- **Active Item**: `blue-900` + orange indicator dot
- **Text**: `amber-100` (hover: white)
- **Transition**: 300ms ease-in-out

---

### 2. Status Tabs

#### Structure
```
[ All (42) ]  [ Active (35) ]  [ Pending (7) ]
```

#### Variants
| Variant | Background | Text | Active |
|---------|------------|------|--------|
| Blue | `blue-100` | `blue-700` | `blue-600` bg |
| Green | `green-100` | `green-700` | `green-600` bg |
| Yellow | `yellow-100` | `yellow-700` | `yellow-600` bg |
| Red | `red-100` | `red-700` | `red-600` bg |
| Gray | `gray-100` | `gray-700` | `gray-600` bg |
| Purple | `purple-100` | `purple-700` | `purple-600` bg |

#### Specs
- **Height**: 36px
- **Padding**: px-4 py-2
- **Border Radius**: 8px (rounded-lg)
- **Gap**: 8px between tabs
- **Shadow on Active**: `shadow-md`
- **Transform on Active**: `-translate-y-0.5`

---

### 3. Data Table

#### Structure
```
┌─────────────────────────────────────────────┐
│ Column 1 │ Column 2 │ Column 3 │ Actions    │  ← Header (bg: blue-950)
├──────────┼──────────┼──────────┼────────────┤
│ Data     │ Data     │ Badge    │ [•••]      │  ← Row (hover: gray-50)
│ Data     │ Data     │ Badge    │ [•••]      │
├──────────┼──────────┼──────────┼────────────┤
│ Rows per page: [10 ▼]    [<] 1 2 3 [>]     │  ← Pagination
└─────────────────────────────────────────────┘
```

#### Specs
- **Header Background**: `blue-950`
- **Header Text**: `amber-100`, font-semibold
- **Row Hover**: `gray-50`
- **Cell Padding**: py-3
- **Border**: `gray-200`
- **Border Radius**: 12px (rounded-xl)
- **Shadow**: `shadow-sm`

---

### 4. Cards

#### Standard Card
```
┌─────────────────────────────┐
│                             │  ← Border (gray-200)
│  Content                    │  ← Padding: 24px
│                             │  ← Background: white
│                             │  ← Shadow: shadow-sm
└─────────────────────────────┘
```

#### Stats Card
```
┌─────────────────────────────┐
│ [Icon]             [Badge]  │  ← Gradient background
│                             │
│  Title                      │
│  Description                │
│                             │
│  ████████████░░░░░░░░       │  ← Progress bar
└─────────────────────────────┘
```

#### Specs
- **Border Radius**: 12px (rounded-xl)
- **Padding**: 24px
- **Background**: White or gradient
- **Border**: 1px `gray-200`

---

### 5. Buttons

#### Variants
| Variant | Background | Text | Hover |
|---------|------------|------|-------|
| Primary | `blue-600` | White | `blue-700` |
| Secondary | `gray-100` | `gray-900` | `gray-200` |
| Ghost | Transparent | `gray-700` | `gray-100` |
| Danger | `red-600` | White | `red-700` |

#### Sizes
| Size | Height | Padding | Font |
|------|--------|---------|------|
| Small | 32px | px-3 | 14px |
| Default | 40px | px-4 | 14px |
| Large | 48px | px-6 | 16px |

#### With Icon
```
[ + ]  Add New
Icon   Label (ml-2)
```

---

### 6. Badges

#### Status Badges
```
[• Active]     [• Inactive]
 Green dot     Red dot
```

#### Specs
- **Height**: 24px
- **Padding**: px-2.5
- **Font Size**: 12px
- **Border Radius**: 9999px (rounded-full)
- **With Dot**: w-1.5 h-1.5 rounded-full mr-1.5

---

## Icons

### Library
- **Lucide React** (duy nhất)
- Không dùng Ant Design icons

### Size Scale
| Size | Value | Usage |
|------|-------|-------|
| sm | 14px | Inline text |
| md | 16px | Buttons |
| lg | 18px | Navigation |
| xl | 20px | Feature icons |

### Common Icons
```tsx
import { 
  User, Users, Settings, LogOut,
  Plus, Search, Filter, MoreHorizontal,
  ChevronLeft, ChevronRight,
  Package, ShoppingCart, Store,
  Check, X, AlertCircle, Info
} from "lucide-react";
```

---

## Layout Principles

### Page Structure
```
┌─────────────────────────────────────────────────────┐
│  [Mock Banner] (if enabled)                         │
├─────────────────────────────────────────────────────┤
│  ┌──────┐                                           │
│  │      │  Title                          [Action]  │
│  │ Side │  Description                              │
│  │ bar  │                                           │
│  │      │  ┌─────────────────────────────────────┐  │
│  │      │  │ [Tab] [Tab] [Tab]    [🔍 Search]  │  │
│  │      │  ├─────────────────────────────────────┤  │
│  │      │  │                                     │  │
│  │      │  │        Data Table                   │  │
│  │      │  │                                     │  │
│  └──────┘  └─────────────────────────────────────┘  │
│                                                     │
│  [Mock Indicator] (floating)                        │
└─────────────────────────────────────────────────────┘
```

### Responsive Breakpoints
| Breakpoint | Width | Sidebar |
|------------|-------|---------|
| Mobile | < 768px | Collapsed + overlay |
| Tablet | 768-1024px | Collapsible |
| Desktop | > 1024px | Expanded |

---

## Animations

### Sidebar
- **Duration**: 300ms
- **Easing**: ease-in-out
- **Properties**: width, opacity, transform

### Status Tabs
- **Hover**: `hover:-translate-y-0.5 hover:shadow-md`
- **Active**: `shadow-md -translate-y-0.5`
- **Transition**: all 200ms

### Loading States
```tsx
// Spinner
<div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />

// Skeleton
<div className="animate-pulse bg-gray-200 rounded" />

// Mock Indicator
<div className="animate-pulse" />
```

---

## Mock Data UI

### Mock Banner
```
┌─────────────────────────────────────────────────────┐
│  ⚠️  Đang sử dụng dữ liệu giả (Mock Data)           │
└─────────────────────────────────────────────────────┘
Background: amber-500
Text: white
```

### Mock Indicator (Floating)
```
┌─────────────────┐
│ 🗄️  Mock Data ● │
└─────────────────┘
Background: amber-500
Position: fixed bottom-4 right-4
Animation: pulse
```

---

## File Structure

```
components/
├── ui/                     # Shadcn components
│   ├── button.tsx
│   ├── card.tsx
│   └── ...
└── shared/                 # Custom shared components
    ├── layout/
    │   ├── Sidebar.tsx
    │   └── Layout.tsx
    ├── data-table/
    │   ├── DataTable.tsx
    │   └── DataTablePagination.tsx
    ├── status-tabs/
    │   └── StatusTabs.tsx
    ├── page-header/
    │   └── PageHeader.tsx
    └── mock-indicator/
        └── MockIndicator.tsx
```

---

## Best Practices

### 1. Consistency
- Dùng `~/` cho tất cả imports
- Lucide icons duy nhất
- Tailwind classes, không inline styles

### 2. Component Size
- Max 150 lines per component
- Tách ra components nhỏ hơn nếu vượt quá
- Custom hooks cho logic phức tạp

### 3. Accessibility
- `aria-label` cho icon buttons
- `sr-only` cho text ẩn
- Focus states rõ ràng

### 4. Performance
- `useMemo` cho computed values
- `useCallback` cho event handlers
- Lazy loading cho heavy components

---

*Last updated: 2026-02-27*
