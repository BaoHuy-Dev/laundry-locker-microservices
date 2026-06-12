# 🎨 Laundry Locker - Adaptive Color System

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Hệ thống màu **Adaptive** - tự động thay đổi tông màu theo chế độ:
- **Light Mode**: Tông ấm (Warm) - dễ chịu, thân thiện
- **Dark Mode**: Tông lạnh (Cool) - sang trọng, chuyên nghiệp

---

## 🌅 Light Mode - Tông Ấm (Warm Undertones)

### Background Layers
```
Background:    #FDFCFA (Warm white - trắng kem nhẹ)
Card:          #FFFFFF (Pure white)
Foreground:    #292524 (Warm charcoal - nâu đen ấm)
```

### Brand Colors - Warm Palette
| Token | Hex | Mô tả |
|-------|-----|-------|
| `brand-50` | `#FDFCFA` | Warm white |
| `brand-100` | `#F5F0E8` | Warm cream (kem) |
| `brand-200` | `#E8E2D9` | Warm gray (xám ấm) |
| `brand-300` | `#D4C4B0` | Beige (be nhạt) |
| `brand-400` | `#C4A882` | Sand/Golden (cát/vàng nhạt) |
| `brand-500` | `#B8956A` | Warm tan (nâu vàng) |
| `brand-600` | `#5B7A91` | **Warm blue-gray** (Primary) |
| `brand-700` | `#4A6275` | Darker warm blue-gray |
| `brand-800` | `#3D5262` | Deep warm blue-gray |
| `brand-900` | `#2F3F4A` | Very deep |

### Interactive Elements
```
Primary:       #5B7A91 (Warm blue-gray)
Secondary:     #C4A882 (Sand/Golden)
Muted:         #E8E2D9 (Warm gray)
Border:        #E7E5E4 (Stone-200)
Accent:        #F5F0E8 (Warm cream)
```

---

## 🌙 Dark Mode - Tông Lạnh (Cool Undertones - Deep Ocean)

### Background Layers
```
Background:    #0F172A (Slate-950) - Xanh đen sâu
Card:          #1E293B (Slate-900) - Xanh đen
Foreground:    #F8FAFC (Slate-50) - Trắng xanh
```

### Brand Colors - Cool Palette
| Token | Hex | Mô tả |
|-------|-----|-------|
| `cool-400` | `#7BAAD1` | **Light cool blue** (Primary) |
| `cool-600` | `#32689C` | Brand blue từ mẫu |
| `cool-700` | `#26547A` | Deep cool blue |
| `cool-800` | `#1E4062` | Very deep blue |
| `cool-900` | `#0F2030` | Blue black |
| `cool-950` | `#081018` | Deepest dark |

### Interactive Elements
```
Primary:       #7BAAD1 (Light cool blue - dễ nhìn trên nền tối)
Secondary:     #26547A (Deep cool blue)
Muted:         #1E293B (Slate-800)
Border:        #1E293B (Slate-800)
Accent:        #1A3D5C (Deep blue)
```

---

## 🚦 Status Colors (Adaptive)

| Status | Light Mode | Dark Mode |
|--------|------------|-----------|
| Active | `#22C55E` (Emerald) | `#22C55E` (Emerald) |
| Pending | `#F59E0B` (Amber) | `#F59E0B` (Amber) |
| Processing | `#5B7A91` (Warm blue) | `#7BAAD1` (Cool blue) |
| Inactive | `#9CA3AF` (Gray) | `#94A3B8` (Slate-400) |
| Error | `#DC2626` (Warm red) | `#991B1B` (Cool red) |

---

## 💡 Usage Examples

### Buttons
```tsx
// Primary button (adaptive)
<Button className="bg-primary hover:opacity-90 text-primary-foreground">
  Primary Action
</Button>

// Secondary button (adaptive)
<Button className="bg-secondary hover:opacity-90 text-secondary-foreground">
  Secondary Action
</Button>

// Brand colors (explicit)
<Button className="bg-brand-600 hover:bg-brand-700 text-white">
  Warm Blue (Light)
</Button>

<Button className="dark:bg-cool-400 dark:hover:bg-cool-300 dark:text-cool-950">
  Cool Blue (Dark)
</Button>
```

### Cards
```tsx
// Basic card (adaptive)
<Card className="bg-card text-card-foreground border-border">
  <CardContent>Content</CardContent>
</Card>

// Card với hover effect
<Card className="card-hover">
  <CardContent>Content</CardContent>
</Card>
```

### Status Badges (Adaptive)
```tsx
// Tự động đổi màu theo mode
<span className="badge-active">Hoạt động</span>
<span className="badge-pending">Chờ xử lý</span>
<span className="badge-processing">Đang xử lý</span>
<span className="badge-inactive">Không hoạt động</span>
```

### Backgrounds
```tsx
// Gradient (adaptive)
<div className="bg-gradient-brand">Brand gradient</div>
<div className="bg-gradient-subtle">Subtle gradient</div>

// Glassmorphism (adaptive)
<div className="glass">Glass effect</div>
```

---

## 🔄 Dark Mode Toggle

```tsx
import { useTheme } from "~/context/theme-context";

function MyComponent() {
  const { theme, setTheme, resolvedTheme } = useTheme();
  
  // theme: 'light' | 'dark' | 'system'
  // resolvedTheme: 'light' | 'dark' (giá trị thực tế đang dùng)
  
  return (
    <button onClick={() => setTheme(resolvedTheme === 'light' ? 'dark' : 'light')}>
      Toggle Theme
    </button>
  );
}
```

Hoặc dùng sẵn component:
```tsx
import { ThemeToggle } from "~/components/shared";

<ThemeToggle />
```

---

## 🎨 So sánh Light vs Dark

| Element | Light (Warm) | Dark (Cool) |
|---------|--------------|-------------|
| Background | `#FDFCFA` (Warm white) | `#0F172A` (Deep blue) |
| Card | `#FFFFFF` | `#1E293B` |
| Primary | `#5B7A91` (Warm blue-gray) | `#7BAAD1` (Light cool blue) |
| Secondary | `#C4A882` (Sand) | `#26547A` (Deep blue) |
| Muted | `#E8E2D9` (Warm gray) | `#1E293B` (Slate) |
| Border | `#E7E5E4` (Stone) | `#1E293B` (Slate) |
| Vibe | Friendly, Cozy | Professional, Sleek |

---

## 📝 Best Practices

1. **Luôn dùng semantic colors** cho consistency
   ```tsx
   ✅ bg-background text-foreground
   ❌ bg-white text-gray-900
   ```

2. **Status badges** dùng utility classes `badge-*`
   ```tsx
   ✅ <span className="badge-active">
   ❌ <span className="bg-green-100 text-green-700">
   ```

3. **Gradient và glass** dùng utility classes
   ```tsx
   ✅ <div className="bg-gradient-brand">
   ✅ <div className="glass">
   ```

4. **Test cả 2 mode** khi thêm component mới

5. **Khi cần explicit color**, dùng `brand-*` cho warm, `cool-*` cho cool

---

## 🔧 Migration từ màu cũ

| Cũ | Mới (Light) | Mới (Dark) |
|----|-------------|------------|
| `bg-blue-900` | `bg-brand-600` | `dark:bg-cool-400` |
| `bg-orange-200` | `bg-brand-300` | `dark:bg-cool-700` |
| `bg-gray-50` | `bg-background` | `dark:bg-background` |
| `bg-white` | `bg-card` | `dark:bg-card` |

---

*Last updated: 2026-02-28*
