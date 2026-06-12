# Laundry Locker Frontend

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`, `RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

Dự án frontend cho hệ thống Laundry Locker được xây dựng với React + TypeScript + Vite.

## Cấu trúc thư mục dự án

### 📁 Root Level

- **`index.html`** - File HTML chính, entry point của ứng dụng
- **`package.json`** - Quản lý dependencies và scripts của dự án
- **`vite.config.ts`** - Cấu hình Vite bundler
- **`tsconfig.json`** - Cấu hình TypeScript chung
- **`tsconfig.app.json`** - Cấu hình TypeScript cho source code ứng dụng
- **`tsconfig.node.json`** - Cấu hình TypeScript cho Node.js scripts
- **`eslint.config.js`** - Cấu hình ESLint cho code quality

### 📁 public/

Chứa các static assets sẽ được serve trực tiếp (images, icons, fonts, etc.)

### 📁 src/

Thư mục chính chứa source code của ứng dụng

#### 📄 Main Files

- **`main.tsx`** - Entry point của React application
- **`App.tsx`** - Root component của ứng dụng
- **`App.css`** - Styles cho App component
- **`index.css`** - Global styles

#### 📁 src/assets/

Chứa các tài nguyên tĩnh như images, icons, fonts được import vào code

#### 📁 src/components/

Chứa các React components có thể tái sử dụng

- **`components/ui/`** - UI components cơ bản (buttons, inputs, cards, dialogs, etc.)

  - Accordion, Alert, Avatar, Badge, Button, Card, Carousel
  - Chart, Checkbox, Calendar, Date Picker
  - Dialog, Drawer, Dropdown Menu
  - Form, Input, Label, Select, Textarea
  - Table, Tabs, Pagination
  - Toast, Notifications, Message
  - Progress, Slider, Switch
  - QR Code, Rate, Steps
  - Result pages (success, error, 403, 404, 500)
  - Và nhiều components khác...

- **`components/layout/`** - Components liên quan đến layout (Header, Footer, Sidebar, Navigation)

- **`components/shared/`** - Shared components được dùng chung ở nhiều nơi trong app

#### 📁 src/pages/

Chứa các page/screen components của ứng dụng (Home, Login, Dashboard, etc.)

#### 📁 src/features/

Chứa code theo từng feature/module của ứng dụng (feature-based architecture)

- Mỗi feature có thể chứa components, hooks, services riêng của feature đó

#### 📁 src/services/

Chứa các service functions để gọi API, xử lý business logic

- API calls, HTTP requests
- Data fetching và caching
- Third-party integrations

#### 📁 src/store/

Chứa state management code (Redux, Zustand, hoặc context API)

- Global state
- Actions và reducers
- Store configuration

#### 📁 src/hooks/

Chứa custom React hooks có thể tái sử dụng

- useAuth, useForm, useDebounce, etc.

#### 📁 src/utils/

Chứa các utility functions và helper functions

- Date formatting
- String manipulation
- Validation functions
- Common helpers

#### 📁 src/types/

Chứa TypeScript type definitions và interfaces

- API response types
- Component prop types
- Entity models

#### 📁 src/constants/

Chứa các hằng số, config values, enum

- API endpoints
- Route paths
- Static configurations

#### 📁 src/context/

Chứa React Context providers để share state giữa components

- Theme context
- Auth context
- App-level contexts

#### 📁 src/styles/

Chứa global styles, CSS modules, theme configuration

- Global CSS
- Theme variables
- Style utilities

## Tech Stack

- **React 18** - UI Library
- **TypeScript** - Type-safe JavaScript
- **Vite** - Fast build tool và dev server
- **Ant Design** - UI component library
- **Shadcn/ui** - Customizable component library

## Getting Started

### Prerequisites

- Node.js (v18 hoặc cao hơn)
- npm hoặc yarn

### Installation

```bash
npm install
```

### Development

```bash
npm run dev
```

### Build

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

---

## Vite Plugins

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) (or [oxc](https://oxc.rs) when used in [rolldown-vite](https://vite.dev/guide/rolldown)) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(["dist"]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ["./tsconfig.node.json", "./tsconfig.app.json"],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
]);
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from "eslint-plugin-react-x";
import reactDom from "eslint-plugin-react-dom";

export default defineConfig([
  globalIgnores(["dist"]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs["recommended-typescript"],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ["./tsconfig.node.json", "./tsconfig.app.json"],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
]);
```
