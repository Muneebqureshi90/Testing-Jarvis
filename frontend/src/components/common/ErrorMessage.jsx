const ErrorMessage = ({ message, className = '' }) => {
  if (!message) return null;

  return (
    <div className={`bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg ${className}`}>
      <p className="font-medium">Error</p>
      <p className="text-sm">{message}</p>
    </div>
  );
};

export default ErrorMessage;