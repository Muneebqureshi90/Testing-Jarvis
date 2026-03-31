import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

const PrivateRoute = ({ children }) => {
  const { token, initialized } = useSelector((state) => state.auth);

  // Show loading state while initializing
  if (!initialized) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  // If not authenticated, redirect to login
  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

export const ProtectedRoute = ({ children }) => {
  return <PrivateRoute>{children}</PrivateRoute>;
};

// Role-based route guard (if we had roles)
export const AuthorOnlyRoute = ({ children, postId }) => {
  const { user } = useSelector((state) => state.auth);
  const { currentPost, posts } = useSelector((state) => state.posts);

  // Determine author ID
  let postAuthorId = null;
  if (postId && currentPost && currentPost.id === postId) {
    postAuthorId = currentPost.author?.id;
  } else {
    const post = posts.find((p) => p.id === postId);
    postAuthorId = post?.author?.id;
  }

  // If user is not the author, show access denied
  if (postAuthorId && user && user.id !== postAuthorId) {
    return <div className="p-8 text-center">
      <h2 className="text-2xl font-bold text-red-600 mb-4">Access Denied</h2>
      <p className="text-gray-600">You do not have permission to edit or delete this post.</p>
    </div>;
  }

  return children;
};