import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { usePosts } from '../../hooks/usePosts';
import PostList from '../../components/features/posts/PostList';
import Button from '../../components/common/Button';

const DashboardPage = () => {
  const { user, initializeAuthState } = useAuth();
  const { posts } = usePosts();

  useEffect(() => {
    // Initialize auth state if not already done
    if (!user) {
      initializeAuthState();
    }
  }, [user, initializeAuthState]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Dashboard</h2>
          <p className="mt-1 text-sm text-gray-600">
            Manage your blog posts and account
          </p>
        </div>
        <Link to="/posts/new">
          <Button variant="primary">Create New Post</Button>
        </Link>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Total Posts</h3>
          <p className="text-3xl font-bold text-primary-600">{posts.length}</p>
        </div>
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Account</h3>
          <p className="text-lg font-medium text-gray-900">{user?.username}</p>
          <p className="text-sm text-gray-600">{user?.email}</p>
        </div>
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-2">Quick Actions</h3>
          <div className="space-y-2">
            <Link to="/posts/new" className="block">
              <Button variant="secondary" className="w-full">Write a Post</Button>
            </Link>
            <Link to="/dashboard" className="block">
              <Button variant="outline" className="w-full" onClick={() => window.location.reload()}>
                Refresh
              </Button>
            </Link>
          </div>
        </div>
      </div>

      <div className="mt-8">
        <h3 className="text-xl font-bold text-gray-900 mb-4">Recent Posts</h3>
        <PostList />
      </div>
    </div>
  );
};

export default DashboardPage;