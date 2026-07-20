# Laundry Locker Mobile App 🧺

<!-- CURRENT_STATUS_START -->
> **Cập nhật 2026-06-13:** Tài liệu này đã được rà soát để bám theo trạng thái hiện tại của dự án. Backend Phase 2 cho
> locker flow đã triển khai SEND / RENTAL / QR / RBAC / maintenance; FE admin build pass; Flutter mobile đã có luồng
> Customer, Manager và Maintenance. Nguồn trạng thái chuẩn: `laundry-locker-microservices/docs/CURRENT_PROJECT_STATUS.md`,
`RUN_RESULT.md`, `LOCKER_FLOW_PLAN.md`.
<!-- CURRENT_STATUS_END -->

This is an [Expo](https://expo.dev) project created with [
`create-expo-app`](https://www.npmjs.com/package/create-expo-app) for managing laundry locker operations.

## Project Structure

### Current Folders

- **`app/`** - Main application screens and routing

    - Contains file-based routing structure using Expo Router
    - `_layout.tsx`: Root layout configuration
    - `(tabs)/`: Tab-based navigation screens
    - Main screens for the application

- **`assets/`** - Static resources

    - `images/`: Image files, icons, logos
    - Fonts, audio files, and other static resources

- **`components/`** - Reusable UI components

    - Custom React components used across multiple screens
    - `ui/`: Base UI components (buttons, inputs, cards, etc.)
    - Presentational and container components

- **`constants/`** - Application constants

    - `theme.ts`: Color themes, typography, spacing
    - Configuration values that don't change at runtime

- **`hooks/`** - Custom React hooks

    - Reusable logic and state management hooks
    - `use-color-scheme.ts`: Theme management
    - `use-theme-color.ts`: Color utilities

- **`scripts/`** - Build and development scripts
    - `reset-project.js`: Project reset utility
    - Automation scripts for development workflow

### Recommended Folders to Add

- **`services/`** - API and external service integrations

    - API client configuration
    - Backend communication layer
    - Authentication services
    - Payment gateway integration

- **`utils/`** - Utility functions and helpers

    - Common helper functions
    - Formatters, validators, converters
    - Pure functions used across the app

- **`types/`** - TypeScript type definitions

    - Interface definitions
    - Type declarations
    - Shared types across the application

- **`context/`** or **`store/`** - State management

    - React Context providers
    - Global state management (Redux/Zustand/etc.)
    - App-wide state and actions

- **`config/`** - Configuration files

    - Environment-specific configurations
    - Feature flags
    - API endpoints

- **`lib/`** - Third-party library configurations

    - Custom library wrappers
    - SDK initializations
    - External package configurations

- **`models/`** - Data models and schemas

    - Business logic models
    - Data structures
    - Entity definitions

- **`navigation/`** - Navigation configuration (optional if using Expo Router extensively)
    - Custom navigation helpers
    - Navigation type definitions

## Get started

1. Install dependencies

   ```bash
   npm install
   ```

2. Start the app

   ```bash
   npx expo start
   ```

In the output, you'll find options to open the app in a

- [development build](https://docs.expo.dev/develop/development-builds/introduction/)
- [Android emulator](https://docs.expo.dev/workflow/android-studio-emulator/)
- [iOS simulator](https://docs.expo.dev/workflow/ios-simulator/)
- [Expo Go](https://expo.dev/go), a limited sandbox for trying out app development with Expo

You can start developing by editing the files inside the **app** directory. This project
uses [file-based routing](https://docs.expo.dev/router/introduction).

## Get a fresh project

When you're ready, run:

```bash
npm run reset-project
```

This command will move the starter code to the **app-example** directory and create a blank **app** directory where you
can start developing.

## Learn more

To learn more about developing your project with Expo, look at the following resources:

- [Expo documentation](https://docs.expo.dev/): Learn fundamentals, or go into advanced topics with
  our [guides](https://docs.expo.dev/guides).
- [Learn Expo tutorial](https://docs.expo.dev/tutorial/introduction/): Follow a step-by-step tutorial where you'll
  create a project that runs on Android, iOS, and the web.

## Join the community

Join our community of developers creating universal apps.

- [Expo on GitHub](https://github.com/expo/expo): View our open source platform and contribute.
- [Discord community](https://chat.expo.dev): Chat with Expo users and ask questions.
