import { Provider } from 'react-redux';
import { store } from './store';
import AppRouter from './routes/AppRouter';

function App() {
  return (
    <Provider store={store}>
      <AppRouter />
    </Provider>
  );
}

export default App;