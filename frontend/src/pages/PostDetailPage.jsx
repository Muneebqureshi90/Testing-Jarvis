import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { usePosts } from '../../hooks/usePosts';
import { useAuth } from '../../hooks/useAuth';
import Spinner from '../../components/common/Spinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import { formatDate } from '../../utils/constants';

const PostDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { loadPost, loading, error } = usePosts();
  const { user } = useAuth();
  const [post, setPost] = useState(null);

  useEffect(() => {
    const fetchPost = async () => {
      try {
        const data = await loadPost(id);
        setPost(data);
      } catch (err) {
        // Error handled by Redux
      }
    };
    fetchPost();
  }, [id, loadPost]);

  const isAuthor = post?.author?.id === user?.id;

  if (loading && !post) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="large" />
      </div>
    );
  }

  if (error && !post) {
    return (
      <div className="max-w-4xl mx-auto">
        <ErrorMessage
          message={error}
          className="mb-4"
        />
        <Link to="/dashboard">
          <Button variant="secondary">Back to Dashboard</Button>
        </Link>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="max-w-4xl mx-auto text-center py-12">
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Post Not Found</h2>
        <p className="text-gray-600 mb-6">The post you're looking for doesn't exist.</p>
        <Link to="/dashboard">
          <Button variant="primary">Back to Dashboard</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="card">
        <div className="mb-6">
          <div className="flex justify-between items-start mb-4">
            <h1 className="text-3xl font-bold text-gray-900">{post.title}</h1>
            <div className="flex items-center space-x-2">
              <span
                className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${
                  post.published
                    ? 'bg-green-100 text-green-800'
                    : 'bg-yellow-100 text-yellow-800'
                }`}
              >
                {post.published ? 'Published' : 'Draft'}
              </span>
              {isAuthor && (
                <Link to={`/posts/${post.id}/edit`}>
                  <Button variant="outline" size="sm">
                    Edit
                  </Button>
                </Link>
              )}
            </div>
          </div>

          <div className="flex items-center text-sm text-gray-500 space-x-4 mb-6 pb-6 border-b border-gray-200">
            <span>By {post.author?.firstName} {post.author?.lastName} (@{post.author?.username})</span>
            <span>•</span>
            <span>Created: {formatDate(post.createdAt)}</span>
            <span>•</span>
            <span>Updated: {formatDate(post.updatedAt)}</span>
          </div>

          <div className="prose max-w-none">
            <div className="whitespace-pre-wrap text-gray-800 leading-relaxed">
              {post.content}
            </div>
          </div>
        </div>

        <div className="flex justify-between items-center pt-6 border-t border-gray-200">
          <Link to="/dashboard">
            <Button variant="secondary">Back to Dashboard</Button>
          </Link>
          {isAuthor && (
            <div className="flex space-x-2">
              <Link to={`/posts/${post.id}/edit`}>
                <Button variant="primary">Edit Post</Button>
              </Link>
              <button
                onClick={() => {
                  if (window.confirm('Are you sure you want to delete this post?')) {
                    // We'll handle deletion in the parent component or via a modal
                    // For now, navigate back and let Dashboard handle state refresh
                    navigate('/dashboard');
                    // Could dispatch delete action here
                  }
                }}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 font-medium transition-colors"
              >
                Delete
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PostDetailPage;