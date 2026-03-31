import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import configureStore from 'redux-mock-store';
import PostList from '../components/features/posts/PostList';

const mockStore = configureStore([]);

describe('PostList', () => {
  let store;
  const mockPosts = [
    {
      id: 1,
      title: 'Test Post 1',
      content: 'This is test content for post 1',
      author: { id: 1, username: 'testuser' },
      published: true,
      createdAt: '2025-01-15T10:30:00Z',
      updatedAt: '2025-01-15T10:30:00Z',
    },
    {
      id: 2,
      title: 'Test Post 2',
      content: 'This is test content for post 2',
      author: { id: 1, username: 'testuser' },
      published: false,
      createdAt: '2025-01-16T10:30:00Z',
      updatedAt: '2025-01-16T10:30:00Z',
    },
  ];

  beforeEach(() => {
    store = mockStore({
      posts: {
        posts: mockPosts,
        loading: false,
        error: null,
        pagination: { total: 2, page: 0, size: 20 },
      },
    });
  });

  it('renders post list with posts', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <PostList />
        </BrowserRouter>
      </Provider>
    );

    expect(screen.getByText('My Posts')).toBeInTheDocument();
    expect(screen.getByText('Test Post 1')).toBeInTheDocument();
    expect(screen.getByText('Test Post 2')).toBeInTheDocument();
  });

  it('shows empty state when no posts', () => {
    store = mockStore({
      posts: {
        posts: [],
        loading: false,
        error: null,
        pagination: { total: 0, page: 0, size: 20 },
      },
    });

    render(
      <Provider store={store}>
        <BrowserRouter>
          <PostList />
        </BrowserRouter>
      </Provider>
    );

    expect(screen.getByText('No posts yet')).toBeInTheDocument();
    expect(screen.getByText('Get started by creating your first blog post.')).toBeInTheDocument();
  });

  it('displays published status badges', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <PostList />
        </BrowserRouter>
      </Provider>
    );

    expect(screen.getByText('Published')).toBeInTheDocument();
    expect(screen.getByText('Draft')).toBeInTheDocument();
  });

  it('shows action buttons for each post', () => {
    render(
      <Provider store={store}>
        <BrowserRouter>
          <PostList />
        </BrowserRouter>
      </Provider>
    );

    const viewButtons = screen.getAllByText('View');
    const editButtons = screen.getAllByText('Edit');
    const deleteButtons = screen.getAllByText('Delete');

    expect(viewButtons).toHaveLength(2);
    expect(editButtons).toHaveLength(2);
    expect(deleteButtons).toHaveLength(2);
  });
});