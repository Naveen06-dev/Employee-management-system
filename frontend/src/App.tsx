import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { store } from './store';
import { lightTheme, darkTheme } from './theme';

import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';

import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { EmployeeList } from './pages/EmployeeList';
import { JobBoard } from './pages/JobBoard';
import { CandidateTracker } from './pages/CandidateTracker';
import { LeaveAttendance } from './pages/LeaveAttendance';

function App() {
  const [darkMode, setDarkMode] = useState(() => {
    const saved = localStorage.getItem('darkMode');
    return saved ? JSON.parse(saved) : false;
  });

  const toggleDarkMode = () => {
    setDarkMode((prev: boolean) => {
      const next = !prev;
      localStorage.setItem('darkMode', JSON.stringify(next));
      return next;
    });
  };

  const theme = darkMode ? darkTheme : lightTheme;

  return (
    <Provider store={store}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <BrowserRouter>
          <Routes>
            {/* Public Auth Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Protected Workspace Layout Wrapper */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout darkMode={darkMode} toggleDarkMode={toggleDarkMode} />
                </ProtectedRoute>
              }
            >
              {/* Dashboard */}
              <Route index element={<Dashboard />} />

              {/* Employee Management CRUD */}
              <Route
                path="employees"
                element={
                  <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_HR']}>
                    <EmployeeList />
                  </ProtectedRoute>
                }
              />

              {/* Careers / Job Board */}
              <Route path="recruitment" element={<JobBoard />} />

              {/* HR Candidate Tracker */}
              <Route
                path="candidate-tracker"
                element={
                  <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_HR']}>
                    <CandidateTracker />
                  </ProtectedRoute>
                }
              />

              {/* Leaves & Attendance checks */}
              <Route path="leaves-attendance" element={<LeaveAttendance />} />
            </Route>

            {/* Fallback wildcard */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </ThemeProvider>
    </Provider>
  );
}

export default App;
