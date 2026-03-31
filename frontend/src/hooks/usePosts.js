import { useDispatch, useSelector } from 'react-redux';
import {
  fetchPosts,
  fetchPostById,
  createPost,
  updatePost,
  deletePost,
  clearCurrentPost,
  clearPosts,
} from '../store/slices/postsSlice';

export const usePosts = () => {
  const dispatch = useDispatch();
  const { posts, currentPost, loading, error, pagination } = useSelector((state) => state.posts);

  const loadPosts = (params) => dispatch(fetchPosts(params));
  const loadPost = (id) => dispatch(fetchPostById(id));
  const addPost = (postData) => dispatch(createPost(postData));
  const editPost = (id, postData) => dispatch(updatePost({ id, ...postData }));
  const removePost = (id) => dispatch(deletePost(id));
  const resetCurrentPost = () => dispatch(clearCurrentPost());
  const resetPosts = () => dispatch(clearPosts());

  return {
    posts,
    currentPost,
    loading,
    error,
    pagination,
    loadPosts,
    loadPost,
    addPost,
    editPost,
    removePost,
    resetCurrentPost,
    resetPosts,
  };
};