import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import type { RootState } from '../store';
import api from '../api';
import { StatCard } from '../components/StatCard';
import {
  Grid,
  Typography,
  Box,
  Card,
  CardContent,
  CircularProgress,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Paper,
  ButtonBase,
} from '@mui/material';
import {
  People as PeopleIcon,
  Work as WorkIcon,
  Description as AppIcon,
  EventNote as EventNoteIcon,
  CheckCircle as PresentIcon,
  Warning as LateIcon,
  Error as AbsentIcon,
  TrendingUp as RateIcon,
  ListAlt as ApplyIcon,
  HomeWork as DeptIcon,
} from '@mui/icons-material';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Legend, Cell, PieChart, Pie } from 'recharts';
import { useNavigate } from 'react-router-dom';

interface StatsData {
  totalEmployees: number;
  activeEmployees: number;
  leaveEmployees: number;
  totalDepartments: number;
  openJobs: number;
  totalApplications: number;
  applicationsStatusDistribution: Record<string, number>;
  pendingLeaves: number;
  presentToday: number;
  lateToday: number;
  absentToday: number;
  attendanceRateToday: number;
}

export const Dashboard: React.FC = () => {
  const { user } = useSelector((state: RootState) => state.auth);
  const navigate = useNavigate();
  const [stats, setStats] = useState<StatsData | null>(null);
  const [loading, setLoading] = useState(false);

  const isAdminOrHr = user?.roles.some((r) => r === 'ROLE_ADMIN' || r === 'ROLE_HR');
  const isEmployee = user?.roles.some((r) => r === 'ROLE_EMPLOYEE');
  const isCandidate = user?.roles.some((r) => r === 'ROLE_CANDIDATE');

  useEffect(() => {
    if (isAdminOrHr) {
      setLoading(true);
      api.get('/api/v1/dashboard/stats')
        .then((res) => {
          setStats(res.data.data);
        })
        .catch((err) => {
          console.error('Failed to load dashboard metrics', err);
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [isAdminOrHr]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  // HR/ADMIN DASHBOARD
  if (isAdminOrHr && stats) {
    const barData = Object.entries(stats.applicationsStatusDistribution || {}).map(([key, val]) => ({
      name: key,
      Applications: val,
    }));

    const attendancePieData = [
      { name: 'Present', value: stats.presentToday, color: '#10b981' },
      { name: 'Late', value: stats.lateToday, color: '#f59e0b' },
      { name: 'Absent', value: stats.absentToday, color: '#ef4444' },
    ].filter(d => d.value > 0);

    return (
      <Box>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: 800 }}>
          Placement & HR Insights
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          Analyze recruitment pipelines, student applications, and employee attendance metrics.
        </Typography>

        <Grid container spacing={3} sx={{ mb: 4 }}>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title="Total Employees" value={stats.totalEmployees} icon={<PeopleIcon />} color="primary.main" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title="Open Positions" value={stats.openJobs} icon={<WorkIcon />} color="secondary.main" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title="Applications Received" value={stats.totalApplications} icon={<AppIcon />} color="info.main" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title="Attendance Rate" value={`${stats.attendanceRateToday}%`} icon={<RateIcon />} color="success.main" />
          </Grid>
        </Grid>

        <Grid container spacing={3}>
          {/* Recruitment Chart */}
          <Grid item xs={12} md={7}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                  Application Status Pipeline
                </Typography>
                <Box sx={{ width: '100%', height: 300 }}>
                  {barData.length > 0 ? (
                    <ResponsiveContainer>
                      <BarChart data={barData}>
                        <XAxis dataKey="name" />
                        <YAxis allowDecimals={false} />
                        <Tooltip />
                        <Bar dataKey="Applications" fill="#6366f1" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                      <Typography color="text.secondary">No applications data available</Typography>
                    </Box>
                  )}
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Today's Attendance Pie */}
          <Grid item xs={12} md={5}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
                  Today's Attendance Status
                </Typography>
                <Grid container alignItems="center">
                  <Grid item xs={12} sm={6} sx={{ height: 200 }}>
                    {attendancePieData.length > 0 ? (
                      <ResponsiveContainer>
                        <PieChart>
                          <Pie
                            data={attendancePieData}
                            cx="50%"
                            cy="50%"
                            innerRadius={60}
                            outerRadius={80}
                            paddingAngle={5}
                            dataKey="value"
                          >
                            {attendancePieData.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={entry.color} />
                            ))}
                          </Pie>
                        </PieChart>
                      </ResponsiveContainer>
                    ) : (
                      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
                        <Typography color="text.secondary">No attendance logs logged yet</Typography>
                      </Box>
                    )}
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <PresentIcon color="success" />
                        <Typography variant="body2">Present: {stats.presentToday}</Typography>
                      </Box>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <LateIcon color="warning" />
                        <Typography variant="body2">Late: {stats.lateToday}</Typography>
                      </Box>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <AbsentIcon color="error" />
                        <Typography variant="body2">Absent: {stats.absentToday}</Typography>
                      </Box>
                    </Box>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>
    );
  }

  // EMPLOYEE DASHBOARD
  if (isEmployee) {
    return (
      <Box>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: 800 }}>
          Welcome back, {user?.username}!
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          Here is your workplace summary for today. Use the actions below to register attendance and request leaves.
        </Typography>

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Attendance Portal
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Log your daily attendance checks here. Note: check-in is required before starting your workday.
                </Typography>
                <Box sx={{ display: 'flex', gap: 2 }}>
                  <Button variant="contained" color="success" size="large" onClick={() => navigate('/leaves-attendance')}>
                    Daily Check-In
                  </Button>
                  <Button variant="outlined" color="primary" size="large" onClick={() => navigate('/leaves-attendance')}>
                    Log Check-Out
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Time-Off Requests
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Submit sick leave, casual leave, or check approvals.
                </Typography>
                <Button variant="contained" color="primary" size="large" onClick={() => navigate('/leaves-attendance')}>
                  Request Leave
                </Button>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>
    );
  }

  // CANDIDATE DASHBOARD
  if (isCandidate) {
    return (
      <Box>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: 800 }}>
          Placement Portal
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          Browse postings, upload your CV, track scheduled interviews, and secure placement offers.
        </Typography>

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Job Listings
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Explore active job vacancies from companies currently visiting the campus.
                </Typography>
                <Button variant="contained" color="primary" size="large" onClick={() => navigate('/recruitment')}>
                  View Open Positions
                </Button>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={6}>
            <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Application Status
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Check status of your submitted resumes and interview schedules.
                </Typography>
                <Button variant="outlined" color="primary" size="large" onClick={() => navigate('/recruitment')}>
                  Track My Applications
                </Button>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>
    );
  }

  return null;
};
export default Dashboard;
