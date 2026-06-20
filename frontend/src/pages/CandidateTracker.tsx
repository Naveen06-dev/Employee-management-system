import React, { useState, useEffect } from 'react';
import api from '../api';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  TextField,
  MenuItem,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  Alert,
  CircularProgress,
  Chip,
  Tabs,
  Tab,
} from '@mui/material';
import {
  CalendarMonth as CalendarIcon,
  CheckCircle as SuccessIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';

interface Application {
  id: number;
  jobId: number;
  jobTitle: string;
  candidateId: string;
  candidateName: string;
  candidateEmail: string;
  resumeUrl: string;
  coverLetter: string;
  status: string;
}

interface Employee {
  id: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
}

export const CandidateTracker: React.FC = () => {
  const [tabIndex, setTabIndex] = useState(0);
  const [applications, setApplications] = useState<Application[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Dialog state
  const [openInterviewDialog, setOpenInterviewDialog] = useState(false);
  const [selectedApp, setSelectedApp] = useState<Application | null>(null);

  // Form Fields
  const [interviewerId, setInterviewerId] = useState('');
  const [scheduledAt, setScheduledAt] = useState('');
  const [durationMinutes, setDurationMinutes] = useState('45');
  const [meetLink, setMeetLink] = useState('');

  const fetchApplications = () => {
    setLoading(true);
    setError(null);
    api.get('/api/v1/recruitment/applications')
      .then((res) => {
        setApplications(res.data.data);
      })
      .catch((err) => {
        setError('Failed to fetch candidate applications list');
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const fetchEmployees = () => {
    // Fetch all active employees to act as potential interviewers
    api.get('/api/v1/employees', { params: { size: 100 } })
      .then((res) => {
        setEmployees(res.data.data.content);
      })
      .catch((err) => {
        console.error('Failed to fetch interviewers list', err);
      });
  };

  useEffect(() => {
    fetchApplications();
    fetchEmployees();
  }, []);

  const handleStatusChange = (appId: number, newStatus: string) => {
    setError(null);
    api.patch(`/api/v1/recruitment/applications/${appId}/status`, null, {
      params: { status: newStatus },
    })
      .then(() => {
        fetchApplications();
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Failed to update candidate status');
      });
  };

  const handleOpenInterview = (app: Application) => {
    setSelectedApp(app);
    setInterviewerId('');
    setScheduledAt('');
    setDurationMinutes('45');
    setMeetLink('https://meet.google.com/abc-defg-hij');
    setOpenInterviewDialog(true);
  };

  const handleScheduleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!interviewerId || !scheduledAt) {
      setError('Please select an interviewer and date-time');
      return;
    }

    const payload = {
      applicationId: selectedApp?.id,
      interviewerId,
      scheduledAt: scheduledAt + ':00', // Format to LocalDateTime ISO-8601
      durationMinutes: Number(durationMinutes),
      meetLink,
    };

    api.post('/api/v1/recruitment/interviews', payload)
      .then(() => {
        fetchApplications();
        setOpenInterviewDialog(false);
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Failed to schedule interview session');
      });
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 800, mb: 1 }}>
        Candidate Application Tracker
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Track placements, verify student resumes, schedule panels, and manage candidate workflows.
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}><CircularProgress /></Box>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Job Position</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Student Name</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Email Address</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Resume / CV</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Cover Letter</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                <TableCell sx={{ fontWeight: 700 }} align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {applications.map((app) => (
                <TableRow key={app.id} hover>
                  <TableCell sx={{ fontWeight: 600 }}>{app.jobTitle}</TableCell>
                  <TableCell>{app.candidateName}</TableCell>
                  <TableCell>{app.candidateEmail}</TableCell>
                  <TableCell>
                    <Button size="small" href={app.resumeUrl} target="_blank">Download CV</Button>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {app.coverLetter || 'No cover letter submitted'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={app.status}
                      color={
                        app.status === 'OFFERED' ? 'success' :
                        app.status === 'REJECTED' ? 'error' :
                        app.status === 'INTERVIEWING' ? 'info' :
                        'default'
                      }
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                      <TextField
                        select
                        size="small"
                        value={app.status}
                        onChange={(e) => handleStatusChange(app.id, e.target.value)}
                        sx={{ width: 150 }}
                      >
                        <MenuItem value="SUBMITTED">Submitted</MenuItem>
                        <MenuItem value="SCREENING">Screening</MenuItem>
                        <MenuItem value="INTERVIEWING">Interviewing</MenuItem>
                        <MenuItem value="OFFERED">Offered</MenuItem>
                        <MenuItem value="REJECTED">Rejected</MenuItem>
                      </TextField>
                      {app.status !== 'REJECTED' && (
                        <IconButton color="primary" onClick={() => handleOpenInterview(app)}>
                          <CalendarIcon />
                        </IconButton>
                      )}
                    </Box>
                  </TableCell>
                </TableRow>
              ))}
              {applications.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                    No student applications have been logged in the system yet.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Schedule Interview Dialog */}
      <Dialog open={openInterviewDialog} onClose={() => setOpenInterviewDialog(false)} maxWidth="sm" fullWidth>
        <Box component="form" onSubmit={handleScheduleSubmit}>
          <DialogTitle sx={{ fontWeight: 800 }}>Schedule Selection Interview</DialogTitle>
          <DialogContent>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Candidate: {selectedApp?.candidateName} for position "{selectedApp?.jobTitle}"
            </Typography>

            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  select
                  label="Select Interviewer"
                  value={interviewerId}
                  onChange={(e) => setInterviewerId(e.target.value)}
                >
                  {employees.map((emp) => (
                    <MenuItem key={emp.id} value={emp.id}>
                      {`${emp.firstName} ${emp.lastName} (${emp.jobTitle})`}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  required
                  fullWidth
                  type="datetime-local"
                  label="Scheduled Date & Time"
                  InputLabelProps={{ shrink: true }}
                  value={scheduledAt}
                  onChange={(e) => setScheduledAt(e.target.value)}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  type="number"
                  label="Duration (Minutes)"
                  value={durationMinutes}
                  onChange={(e) => setDurationMinutes(e.target.value)}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Meeting Link"
                  value={meetLink}
                  onChange={(e) => setMeetLink(e.target.value)}
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 3 }}>
            <Button onClick={() => setOpenInterviewDialog(false)} color="inherit">Cancel</Button>
            <Button type="submit" variant="contained">Schedule Interview</Button>
          </DialogActions>
        </Box>
      </Dialog>
    </Box>
  );
};
export default CandidateTracker;
