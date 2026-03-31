import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { usePosts } from '../../hooks/usePosts';
import { formatDate, truncateText } from '../../utils/constants';
import Button from '../common/Button';
import Spinner from '../common/Spinner';
import ErrorMessage from '../common/ErrorMessage';

const PostList = () => {
  const { posts, loading, error, loadPosts, removePost } = usePosts();

  useEffect(() => {
    loadPosts({ page: 0, size: 20 });
  }, [loadPosts]);

  const handleDelete = async (id, title) => {
    if (window.confirm(`Are you sure you want to delete "${title}"?`)) {
      try {
        await removePost(id);
      } catch (err) {
        // Error handled by Redux
      }
    }
  };

  if (loading && posts.length === 0) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="large" />
      </div>
    );
  }

  if (error) {
    return (
      <ErrorMessage
        message={error}
        className="mb-4"
      />
    );
  }

  if (posts.length === 0) {
    return (
      <div className="text-center py-12">
        <h3 className="text-lg font-medium text-gray-900 mb-2">No posts yet</h3>
        <p className="text-gray-600 mb-4">Get started by creating your first blog post.</p>
        <Link to="/posts/new">
          <Button variant="primary">Create Post</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">My Posts</h2>
        <Link to="/posts/new">
          <Button variant="primary">Create New Post</Button>
        </Link>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {posts.map((post) => (
          <div
            key={post.id}
            className="card hover:shadow-lg transition-shadow"
          >
            <div className="mb-4">
              <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
                {post.title}
              </h3>
              <p className="text-sm text-gray-600 mb-3">
                {truncateText(post.content, 120)}
              </p>
              <div className="flex items-center text-xs text-gray-500 space-x-3">
                <span>By {post.author?.username}</span>
                <span>•</span>
                <span>{formatDate(post.createdAt)}</span>
              </div>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-gray-200">
              <div className="flex items-center space-x-2">
                <span
                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    post.published
                      ? 'bg-green-100 text-green-800'
                      : 'bg-yellow-100 text-yellow-800'
                  }`}
                >
                  {post.published ? 'Published' : 'Draft'}
                </span>
              </div>
              <div className="flex items-center space-x-2">
                <Link
                  to={`/posts/${post.id}`}
                  className="text-primary-600 hover:text-primary-800 text-sm font-medium"
                >
                  View
                </Link>
                <Link
                  to={`/posts/${post.id}/edit`}
                  className="text-gray-600 hover:text-gray-800 text-sm font-medium"
                >
                  Edit
                </Link>
                <button
                  onClick={() => handleDelete(post.id, post.title)}
                  className="text-red-600 hover:text-red-800 text-sm font-medium"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination placeholder - can be implemented if needed */}
      {posts.length >= 20 && (
        <div className="flex justify-center mt-8">
          <p className="text-sm text-gray-500">
            Showing {posts.length} posts. Pagination can be added for more.
          </p>
        </div>
      )}
    </div>
  );
};

export default PostList;