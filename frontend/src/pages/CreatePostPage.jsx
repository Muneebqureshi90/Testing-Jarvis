import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePosts } from '../../hooks/usePosts';
import PostForm from '../../components/features/posts/PostForm';
import { useAuth } from '../../hooks/useAuth';

const CreatePostPage = () => {
  const navigate = useNavigate();
  const { addPost, loading, error } = usePosts();
  const { user } = useAuth();

  const handleSubmit = async (formData) => {
    try {
      const newPost = await addPost(formData);
      navigate(`/posts/${newPost.id}`);
    } catch (err) {
      // Error handled in component state
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Create New Post</h1>
        <p className="text-gray-600 mt-1">
          Write and publish your thoughts
        </p>
      </div>

      <div className="card">
        <PostForm
          onSubmit={handleSubmit}
          loading={loading}
          error={error}
          mode="create"
        />
      </div>
    </div>
  );
};

export default CreatePostPage;