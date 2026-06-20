import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import type { RootState } from '../store';
import api from '../api';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Button,
  TextField,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  CircularProgress,
  Chip,
  Tabs,
  Tab,
} from '@mui/material';
import {
  CheckCircle as SuccessIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';

interface Leave {
  id: number;
  employeeId: string;
  employeeName: string;
  startDate: string;
  endDate: string;
  leaveType: string;
  reason: string;
  status: string;
}

interface Attendance {
  id: number;
  employeeId: string;
  employeeName: string;
  workDate: string;
  checkIn: string;
  checkOut: string;
  status: string;
}

export const LeaveAttendance: React.FC = () => {
  const { user } = useSelector((state: RootState) => state.auth);
  const isEmployee = user?.roles.some((r) => r === 'ROLE_EMPLOYEE');
  const isAdminOrHr = user?.roles.some((r) => r === 'ROLE_ADMIN' || r === 'ROLE_HR');

  const [tabIndex, setTabIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Employee State
  const [myLeaves, setMyLeaves] = useState<Leave[]>([]);
  const [myAttendance, setMyAttendance] = useState<Attendance[]>([]);
  const [employeeProfileId, setEmployeeProfileId] = useState<string | null>(null);
  
  // Leave Form
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [leaveType, setLeaveType] = useState('SICK');
  const [reason, setReason] = useState('');

  // HR/Admin State
  const [pendingLeaves, setPendingLeaves] = useState<Leave[]>([]);
  const [dailyAttendance, setDailyAttendance] = useState<Attendance[]>([]);

  // Fetch Current Employee Profile (needed for attendance report querying)
  const fetchEmployeeProfile = async () => {
    if (!isEmployee) return null;
    try {
      const res = await api.get('/api/v1/employees/profile');
      setEmployeeProfileId(res.data.data.id);
      return res.data.data.id;
    } catch (err) {
      console.error('Failed to resolve employee profile', err);
      return null;
    }
  };

  const fetchEmployeeData = async () => {
    setLoading(true);
    setError(null);
    try {
      const empId = employeeProfileId || await fetchEmployeeProfile();
      
      // Fetch Leaves
      const leavesRes = await api.get('/api/v1/leaves/my');
      setMyLeaves(leavesRes.data.data);

      if (empId) {
        // Fetch past 30 days attendance
        const today = new Date();
        const start = new Date();
        start.setDate(today.getDate() - 30);
        const startStr = start.toISOString().split('T')[0];
        const endStr = today.toISOString().split('T')[0];

        const attRes = await api.get('/api/v1/attendance/report', {
          params: { employeeId: empId, startDate: startStr, endDate: endStr }
        });
        setMyAttendance(attRes.data.data);
      }
    } catch (err) {
      setError('Failed to retrieve your workspace logs');
    } finally {
      setLoading(false);
    }
  };

  const fetchAdminData = async () => {
    setLoading(true);
    setError(null);
    try {
      // Fetch Pending Leaves
      const leavesRes = await api.get('/api/v1/leaves', { params: { status: 'PENDING' } });
      setPendingLeaves(leavesRes.data.data);

      // Fetch Today's Attendance
      const todayStr = new Date().toISOString().split('T')[0];
      const attRes = await api.get('/api/v1/attendance/daily', { params: { date: todayStr } });
      setDailyAttendance(attRes.data.data);
    } catch (err) {
      setError('Failed to fetch HR workflow logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isEmployee) {
      fetchEmployeeData();
    } else if (isAdminOrHr) {
      fetchAdminData();
    }
  }, [tabIndex]);

  // Attendance Actions (Employee)
  const handleCheckIn = () => {
    setError(null);
    setSuccessMsg(null);
    api.post('/api/v1/attendance/check-in')
      .then((res) => {
        setSuccessMsg('Check-In recorded successfully!');
        fetchEmployeeData();
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Check-in failed');
      });
  };

  const handleCheckOut = () => {
    setError(null);
    setSuccessMsg(null);
    api.post('/api/v1/attendance/check-out')
      .then((res) => {
        setSuccessMsg('Check-Out recorded successfully!');
        fetchEmployeeData();
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Check-out failed');
      });
  };

  // Leave Form Submit (Employee)
  const handleRequestLeave = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    const payload = {
      startDate,
      endDate,
      leaveType,
      reason,
    };

    api.post('/api/v1/leaves', payload)
      .then(() => {
        setSuccessMsg('Time-off request submitted successfully!');
        setStartDate('');
        setEndDate('');
        setReason('');
        fetchEmployeeData();
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Failed to submit leave request');
      });
  };

  // Approve / Reject Leave Actions (HR/Admin)
  const handleReviewLeave = (id: number, status: 'APPROVED' | 'REJECTED') => {
    setError(null);
    api.patch(`/api/v1/leaves/${id}/approve`, null, { params: { status } })
      .then(() => {
        fetchAdminData();
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Failed to submit review');
      });
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 800, mb: 1 }}>
        Leaves & Attendance
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Clock in your working hours and manage leaves.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {successMsg && <Alert severity="success" sx={{ mb: 3 }}>{successMsg}</Alert>}

      {isEmployee && (
        <>
          <Tabs value={tabIndex} onChange={(_, idx) => setTabIndex(idx)} sx={{ mb: 3 }}>
            <Tab label="Attendance Logging" />
            <Tab label="Leave Requests" />
          </Tabs>

          {tabIndex === 0 ? (
            /* Attendance Tab */
            <Grid container spacing={3}>
              <Grid item xs={12} md={4}>
                <Paper sx={{ p: 3, textAlign: 'center' }}>
                  <Typography variant="h6" sx={{ mb: 1, fontWeight: 700 }}>Record Attendance</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                    Record your check-in when arriving and check-out before leaving.
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Button variant="contained" color="success" size="large" onClick={handleCheckIn}>
                      Clock In
                    </Button>
                    <Button variant="outlined" color="primary" size="large" onClick={handleCheckOut}>
                      Clock Out
                    </Button>
                  </Box>
                </Paper>
              </Grid>
              <Grid item xs={12} md={8}>
                <TableContainer component={Paper}>
                  <Box sx={{ p: 2 }}>
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>Recent Clock logs (30 Days)</Typography>
                  </Box>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 700 }}>Date</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Clock In</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Clock Out</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {myAttendance.map((att) => (
                        <TableRow key={att.id}>
                          <TableCell>{att.workDate}</TableCell>
                          <TableCell>{att.checkIn || '--:--'}</TableCell>
                          <TableCell>{att.checkOut || '--:--'}</TableCell>
                          <TableCell>
                            <Chip
                              label={att.status}
                              color={att.status === 'PRESENT' ? 'success' : att.status === 'LATE' ? 'warning' : 'error'}
                              size="small"
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                      {myAttendance.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={4} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                            No clock logs recorded in the last 30 days.
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Grid>
            </Grid>
          ) : (
            /* Leaves Tab */
            <Grid container spacing={3}>
              <Grid item xs={12} md={5}>
                <Paper sx={{ p: 3 }} component="form" onSubmit={handleRequestLeave}>
                  <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>Submit Time-Off Request</Typography>
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}>
                      <TextField required fullWidth type="date" label="Start Date" InputLabelProps={{ shrink: true }} value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField required fullWidth type="date" label="End Date" InputLabelProps={{ shrink: true }} value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        required
                        fullWidth
                        select
                        label="Leave Category"
                        value={leaveType}
                        onChange={(e) => setLeaveType(e.target.value)}
                      >
                        <MenuItem value="SICK">Sick Leave</MenuItem>
                        <MenuItem value="CASUAL">Casual Leave</MenuItem>
                        <MenuItem value="UNPAID">Unpaid Leave</MenuItem>
                        <MenuItem value="MATERNITY">Maternity</MenuItem>
                        <MenuItem value="PATERNITY">Paternity</MenuItem>
                      </TextField>
                    </Grid>
                    <Grid item xs={12}>
                      <TextField required fullWidth multiline rows={4} label="Leave Purpose / Reason" value={reason} onChange={(e) => setReason(e.target.value)} />
                    </Grid>
                    <Grid item xs={12}>
                      <Button type="submit" variant="contained" fullWidth size="large">Submit Request</Button>
                    </Grid>
                  </Grid>
                </Paper>
              </Grid>
              <Grid item xs={12} md={7}>
                <TableContainer component={Paper}>
                  <Box sx={{ p: 2 }}>
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>My Leave Logs</Typography>
                  </Box>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 700 }}>Duration</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Category</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Purpose</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {myLeaves.map((l) => (
                        <TableRow key={l.id}>
                          <TableCell>{`${l.startDate} to ${l.endDate}`}</TableCell>
                          <TableCell sx={{ textTransform: 'capitalize' }}>{l.leaveType.toLowerCase()}</TableCell>
                          <TableCell>{l.reason}</TableCell>
                          <TableCell>
                            <Chip
                              label={l.status}
                              color={l.status === 'APPROVED' ? 'success' : l.status === 'PENDING' ? 'warning' : 'error'}
                              size="small"
                            />
                          </TableCell>
                        </TableRow>
                      ))}
                      {myLeaves.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={4} align="center" sx={{ color: 'text.secondary', py: 3 }}>
                            You have no leave history recorded.
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Grid>
            </Grid>
          )}
        </>
      )}

      {isAdminOrHr && (
        <>
          <Tabs value={tabIndex} onChange={(_, idx) => setTabIndex(idx)} sx={{ mb: 3 }}>
            <Tab label="Leaves Approval Queue" />
            <Tab label="Today's Attendance Registry" />
          </Tabs>

          {tabIndex === 0 ? (
            /* Leaves approval queue for HR/Admin */
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 700 }}>Employee Name</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Duration</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Category</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Reason</TableCell>
                    <TableCell sx={{ fontWeight: 700 }} align="right">Decision</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {pendingLeaves.map((l) => (
                    <TableRow key={l.id}>
                      <TableCell sx={{ fontWeight: 600 }}>{l.employeeName}</TableCell>
                      <TableCell>{`${l.startDate} to ${l.endDate}`}</TableCell>
                      <TableCell sx={{ textTransform: 'capitalize' }}>{l.leaveType.toLowerCase()}</TableCell>
                      <TableCell>{l.reason}</TableCell>
                      <TableCell align="right">
                        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                          <Button
                            size="small"
                            variant="contained"
                            color="success"
                            onClick={() => handleReviewLeave(l.id, 'APPROVED')}
                          >
                            Approve
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            onClick={() => handleReviewLeave(l.id, 'REJECTED')}
                          >
                            Reject
                          </Button>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                  {pendingLeaves.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                        No pending time-off approvals inside the queue.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            /* Today's Daily check ins */
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 700 }}>Employee Name</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Clock In Time</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Clock Out Time</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {dailyAttendance.map((att) => (
                    <TableRow key={att.id}>
                      <TableCell sx={{ fontWeight: 600 }}>{att.employeeName}</TableCell>
                      <TableCell>{att.checkIn || '--:--'}</TableCell>
                      <TableCell>{att.checkOut || '--:--'}</TableCell>
                      <TableCell>
                        <Chip
                          label={att.status}
                          color={att.status === 'PRESENT' ? 'success' : att.status === 'LATE' ? 'warning' : 'error'}
                          size="small"
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                  {dailyAttendance.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                        No attendance registry logged for today.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </>
      )}
    </Box>
  );
};
export default LeaveAttendance;
