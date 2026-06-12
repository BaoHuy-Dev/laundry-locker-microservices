# Landing Page - Frame Animation Guide

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

## Tổng quan

Landing page này sử dụng 241 frame images để tạo hiệu ứng animation khi scroll, thay thế các 3D objects.

## Cấu trúc Components

### 1. FrameSequence.tsx

Component chịu trách nhiệm load và render các frame dựa trên scroll progress.

**Props:**

- `scrollProgress`: 0-1, điều khiển frame nào được hiển thị
- `startFrame`: Frame bắt đầu (default: 1)
- `endFrame`: Frame kết thúc (default: 241)
- `className`: CSS classes

### 2. ScrollSection.tsx

Component tạo section với frame animation và nội dung.

**Props:**

- `title`: Tiêu đề section
- `description`: Mô tả
- `badge`: Badge text
- `icon`: Emoji icon
- `features`: Mảng các tính năng
- `reverse`: Layout đảo ngược (trái/phải)
- `frameStart`: Frame bắt đầu cho section này
- `frameEnd`: Frame kết thúc cho section này

## Phân chia Frames cho các Section

### Section 1 - Tự động hóa (Frames 1-60)

- 60 frames đầu tiên
- Animation: AI và automation effects

### Section 2 - Bảo mật (Frames 61-120)

- 60 frames tiếp theo
- Animation: Security và lock effects

### Section 3 - Truy cập 24/7 (Frames 121-180)

- 60 frames tiếp theo
- Animation: Mobile app và accessibility

### Section 4 - Tiết kiệm (Frames 181-241)

- 61 frames cuối
- Animation: Efficiency và analytics

## Cách thức hoạt động

1. **Preload**: Tất cả frames trong range được preload khi component mount
2. **Scroll Detection**: useScroll hook từ Framer Motion theo dõi scroll position
3. **Frame Calculation**: Scroll progress (0-1) được map sang frame index
4. **Canvas Rendering**: Frame tương ứng được vẽ lên canvas
5. **Smooth Animation**: Khi scroll, frames thay đổi tạo hiệu ứng animation mượt mà

## Performance Optimization

- Sử dụng Vite's glob import để bundle images
- Canvas rendering thay vì DOM manipulation
- Lazy loading với loading state
- Error handling cho missing frames

## Customization

### Thay đổi frame range cho section:

```tsx
{
  frameStart: 1,
  frameEnd: 60,
  // ...
}
```

### Điều chỉnh animation speed:

Trong ScrollSection.tsx, thay đổi công thức normalize progress:

```tsx
const normalizedProgress = Math.max(0, Math.min(1, (latest - 0.3) / 0.4));
```

### Thêm/bớt sections:

Chỉnh sửa mảng `sections` trong LandingPage.tsx

## Troubleshooting

### Frames không load:

- Kiểm tra path trong `src/assets/`
- Đảm bảo tên file đúng format: `ezgif-frame-001.jpg` đến `ezgif-frame-241.jpg`
- Check console warnings

### Animation không mượt:

- Giảm số lượng frames per section
- Tăng performance với image compression
- Optimize canvas rendering

### Loading quá lâu:

- Giảm số frames được load cùng lúc
- Implement progressive loading
- Use WebP format thay vì JPG

## Browser Support

- Modern browsers with Canvas API support
- Chrome/Edge: ✅
- Firefox: ✅
- Safari: ✅
- IE11: ❌ (không hỗ trợ)
