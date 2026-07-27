# AlgoTradeX - Phase 2: Frontend Foundation & Dashboard

This implementation plan outlines the architectural approach and execution steps for **Phase 2** of the AlgoTradeX platform, focusing on the React 19 frontend. Phase 1 (Core Backend & Security) is already complete.

## Proposed Changes

### Frontend Architecture & Dependencies
*   **Routing**: React Router DOM (v7 or v6 latest).
*   **Data Fetching**: React Query (TanStack Query) & Axios.
*   **State Management**: Zustand (for user session/auth state).
*   **Styling**: Vanilla CSS / CSS Modules with a robust `index.css` design system (CSS variables).
*   **Animations**: Framer Motion for smooth micro-interactions.
*   **Icons**: Lucide React.

### Execution Steps

1. **Setup Core Infrastructure**
   - Install `react-router-dom`, `@tanstack/react-query`, `axios`, `zustand`, `lucide-react`, `framer-motion`.
   - Configure Axios instance with base URL and JWT interceptors (to attach access tokens to requests).

2. **Design System & Foundation (`index.css`)**
   - Define CSS variables for a premium dark theme.
   - Implement global styles, typography (e.g., Inter or Roboto), and utility classes for glassmorphism panels.

3. **Core Reusable Components**
   - `Button`: Primary, secondary, ghost variants with hover effects.
   - `Input`: Form inputs with floating labels and validation states.
   - `Card`: Dashboard glass-paneled cards.

4. **Authentication Flow**
   - **Login Page**: UI for email/password and handling 2FA challenge.
   - **Register Page**: UI for user sign-up.
   - **Auth Service integration**: Connect forms to the backend `/api/auth/login` and `/api/auth/register` endpoints.

5. **Dashboard Layout**
   - **Sidebar Navigation**: Animated, collapsible side menu.
   - **Top Header**: User profile, notifications, and context menus.
   - **Protected Route Wrapper**: Ensure only authenticated users can access the dashboard.

6. **Dashboard Home (Mockup)**
   - Build a visually stunning, responsive grid for the main dashboard view.
   - Add placeholder widgets for Account Summary, Live P&L, and Active Orders.

## Verification Plan

### Manual Verification
- Start the Vite development server (`npm run dev`).
- Verify the Login and Registration flow successfully communicates with the Spring Boot backend.
- Ensure the JWT token is saved locally and used for subsequent routing into the protected Dashboard.
- Verify the UI aesthetics and responsive design on different screen sizes.
