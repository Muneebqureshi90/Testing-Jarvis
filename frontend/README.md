# Secure Blog - Frontend

A React.js dashboard for the Secure Blog REST API. Built with Vite, Tailwind CSS, and Redux Toolkit.

## Features

- 🔐 **JWT Authentication** - Login, registration, token refresh, and protected routes
- 📝 **Blog Management** - Create, read, update, and delete blog posts
- 🎨 **Modern UI** - Responsive design with Tailwind CSS
- ⚡ **Fast Development** - Vite for instant hot module reloading
- 🧪 **Tested** - Unit tests with Jest and React Testing Library
- 📱 **Mobile-First** - Fully responsive layout

## Tech Stack

- **React 18** - UI library
- **Redux Toolkit** - State management (auth & posts)
- **React Router** - Client-side routing
- **Axios** - HTTP client with interceptors
- **Tailwind CSS** - Utility-first CSS framework
- **Vite** - Fast build tool
- **Jest** - Testing framework
- **React Testing Library** - Component testing

## Prerequisites

- Node.js 18+
- npm or yarn
- Backend API running (Spring Boot) - see [Backend README](../README.md)

## Quick Start

### 1. Install Dependencies

```bash
cd frontend
npm install
```

### 2. Configure Environment

Copy the example environment file and update the API URL:

```bash
cp .env.example .env
```

Edit `.env` to point to your backend:

```
REACT_APP_API_URL=http://localhost:8080/api/v1
```

### 3. Run Development Server

```bash
npm run dev
```

The app will be available at: http://localhost:3000

### 4. Build for Production

```bash
npm run build
```

Build output goes to `dist/` directory.

## Project Structure

```
src/
├── api/                    # API clients
│   ├── axiosConfig.js      # Axios instance with interceptors
│   └── index.js            # API endpoint definitions
├── components/             # Reusable components
│   ├── common/            # Button, Input, Spinner, Modal, etc.
│   ├── auth/              # LoginForm, RegisterForm
│   ├── dashboard/         # Layout
│   └── features/
│       └── posts/         # PostList, PostForm
├── hooks/                  # Custom hooks
│   ├── useAuth.js         # Auth state and actions
│   └── usePosts.js        # Posts state and actions
├── pages/                  # Page components
│   ├── LoginPage.jsx
│   ├── RegisterPage.jsx
│   ├── DashboardPage.jsx
│   ├── PostDetailPage.jsx
│   ├── CreatePostPage.jsx
│   └── EditPostPage.jsx
├── routes/                 # Routing configuration
│   ├── AppRouter.jsx
│   └── PrivateRoute.jsx   # Auth guards
├── store/                  # Redux store
│   ├── index.js
│   └── slices/
│       ├── authSlice.js   # Auth state
│       └── postsSlice.js  # Posts state
├── styles/                 # Global styles
│   └── index.css          # Tailwind imports + custom styles
├── utils/                  # Utility functions
│   └── constants.js       # Constants, formatters, storage
├── test/                   # Test setup
└── App.jsx                 # Root component
```

## API Integration

The frontend connects to the Spring Boot backend via the following endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/auth/register` | POST | Register new user |
| `/auth/login` | POST | Login with credentials |
| `/auth/refresh` | POST | Refresh JWT token |
| `/posts` | GET | List all posts (public) |
| `/posts` | POST | Create new post (auth required) |
| `/posts/:id` | GET | Get single post (public) |
| `/posts/:id` | PUT | Update post (auth + ownership required) |
| `/posts/:id` | DELETE | Delete post (auth + ownership required) |

### Authentication Flow

1. User registers or logs in
2. Backend returns `accessToken` and `refreshToken`
3. Tokens stored in `localStorage`
4. `accessToken` attached to all subsequent requests via axios interceptor
5. When `accessToken` expires (401), axios automatically calls `/auth/refresh`
6. If refresh fails, user is redirected to login

### Protected Routes

Routes requiring authentication are wrapped in `ProtectedRoute` component:

- `/dashboard`
- `/posts/new`
- `/posts/:id/edit`

Public routes:
- `/login`
- `/register`
- `/posts/:id` (view-only)

## State Management

### Auth Slice

- `user` - Current user data
- `token` - JWT access token
- `loading` - API call loading state
- `error` - Error message
- `initialized` - Auth state initialization complete

Actions:
- `login()` - Authenticate user
- `register()` - Create new account
- `logout()` - Clear tokens and user
- `initializeAuth()` - Check localStorage for existing token

### Posts Slice

- `posts` - Array of post objects
- `currentPost` - Single post for detail/edit view
- `loading` - API loading state
- `error` - Error message
- `pagination` - Pagination metadata

Actions:
- `fetchPosts()` - Get list of posts
- `fetchPostById()` - Get single post
- `createPost()` - Create new post
- `updatePost()` - Update existing post
- `deletePost()` - Delete post

## Available Scripts

```bash
# Development
npm run dev              # Start dev server with HMR
npm run build           # Production build
npm run preview         # Preview production build

# Code Quality
npm run lint            # Run ESLint

# Testing
npm test                # Run tests once
npm run test:watch      # Run tests in watch mode
```

## Environment Variables

| Variable | Required | Description | Default |
|----------|----------|-------------|---------|
| `REACT_APP_API_URL` | Yes | Backend API base URL | `http://localhost:8080/api/v1` |
| `REACT_APP_ENV` | No | Environment name | `development` |
| `REACT_APP_TITLE` | No | App title | `Secure Blog` |

## Testing

### Run Tests

```bash
npm test
```

### Test Coverage

- Component rendering tests
- Form validation tests
- User interaction tests

Test files live alongside components with `.test.jsx` extension.

## Styling

This project uses **Tailwind CSS** for utility-first styling.

### Custom Classes

Defined in `src/styles/index.css`:

- `.btn` - Base button styles
- `.btn-primary` - Primary action button
- `.btn-secondary` - Secondary button
- `.btn-danger` - Destructive action button
- `.input` - Form input styling
- `.card` - Card container with shadow
- `.nav-link` - Navigation link styles
- `.nav-link-active` - Active navigation state

### Responsive Breakpoints

We follow Tailwind's defaults:

- `sm`: 640px
- `md`: 768px
- `lg`: 1024px
- `xl`: 1280px

The layout is mobile-first by default.

## Deployment

### Build

```bash
npm run build
```

This creates an optimized production build in `dist/`.

### Docker

```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Deploy to Static Host

The `dist/` folder can be deployed to any static hosting service:

- **Vercel** - Zero-config deployment
- **Netlify** - Drag and drop `dist/`
- **AWS S3** - Configure as static website
- **GitHub Pages** - Use `gh-pages` branch

Make sure to set `REACT_APP_API_URL` environment variable in your hosting platform to point to the backend.

## Development Tips

### Hot Module Replacement

Vite provides instant HMR - changes reflect immediately without page reload.

### Redux DevTools

Install Redux DevTools browser extension to inspect state changes.

### API Debugging

Check browser DevTools Network tab to see API requests. Ensure backend is running on the URL specified in `.env`.

### Common Issues

**403 Forbidden on POST/PUT/DELETE**

- Ensure JWT token is being sent (`Authorization: Bearer <token>`)
- Verify you're the author of the post you're trying to modify

**401 Unauthorized on app load**

- Clear localStorage: `localStorage.clear()` in browser console
- Check that backend is running
- Verify `REACT_APP_API_URL` is correct

**CORS errors**

- Backend must have CORS enabled for your frontend origin
- Development: `http://localhost:3000`
- Production: your deployed domain

## Extending the Application

### Adding a New API Endpoint

1. Define API function in `src/api/index.js`
2. Create Redux async thunk in appropriate slice (or new slice)
3. Add UI components and pages
4. Add route in `src/routes/AppRouter.jsx`

### Adding a New Page

1. Create component in `src/pages/`
2. Add route in `AppRouter.jsx`
3. If auth required, wrap in `ProtectedRoute`

### Adding a New Component

- Reusable UI → `src/components/common/`
- Feature-specific → `src/components/features/[feature-name]/`

## Contributing

Follow the project conventions:

- Use functional components with hooks
- Keep components small and focused
- Use Redux for global state, local state for component-specific
- Write tests for new components and logic
- Follow existing code style

## License

This project is part of the Secure Blog system. See repository for license details.

---

**Need Help?** Check the backend repository for API specifications and deployment guides.