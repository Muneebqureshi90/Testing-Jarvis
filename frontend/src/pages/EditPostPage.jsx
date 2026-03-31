import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { usePosts } from '../../hooks/usePosts';
import PostForm from '../../components/features/posts/PostForm';

const EditPostPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { loadPost, updatePost, loading, error, currentPost } = usePosts();
  const [initialData, setInitialData] = useState(null);
  const [fetchError, setFetchError] = useState(null);

  useEffect(() => {
    const fetchPost = async () => {
      try {
        const data = await loadPost(id);
        setInitialData(data);
        setFetchError(null);
      } catch (err) {
        setFetchError(err.message || 'Failed to load post');
      }
    };
    fetchPost();
  }, [id, loadPost]);

  const handleSubmit = async (formData) => {
    try {
      const updatedPost = await updatePost({ id, ...formData });
      navigate(`/posts/${updatedPost.id}`);
    } catch (err) {
      // Error handled in component state
    }
  };

  if (fetchError) {
    return (
      <div className="max-w-4xl mx-auto text-center py-12">
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Error Loading Post</h2>
        <p className="text-gray-600 mb-6">{fetchError}</p>
        <button
          onClick={() => navigate('/dashboard')}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
        >
          Back to Dashboard
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Edit Post</h1>
        <p className="text-gray-600 mt-1">
          Update your blog post
        </p>
      </div>

      <div className="card">
        {initialData && (
          <PostForm
            initialData={initialData}
            onSubmit={handleSubmit}
            loading={loading}
            error={error}
            mode="edit"
          />
        )}
      </div>
    </div>
  );
};

export default EditPostPage;