import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import type { RootState } from '../store';
import api from '../api';
import {
  Box,
  Typography,
  Paper,
  Tabs,
  Tab,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  TextField,
  MenuItem,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
  CircularProgress,
  List,
  ListItem,
  ListItemText,
  Chip,
  Divider,
} from '@mui/material';
import {
  Add as AddIcon,
  Search as SearchIcon,
  CloudUpload as UploadIcon,
  CheckCircle as SuccessIcon,
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

const VisuallyHiddenInput = styled('input')({
  clip: 'rect(0 0 0 0)',
  clipPath: 'inset(50%)',
  height: 1,
  overflow: 'hidden',
  position: 'absolute',
  bottom: 0,
  left: 0,
  whiteSpace: 'nowrap',
  width: 1,
});

interface Job {
  id: number;
  title: string;
  description: string;
  requirements: string;
  location: string;
  salaryRange: string;
  departmentId: number;
  departmentName: string;
  status: string;
}

interface Application {
  id: number;
  jobId: number;
  jobTitle: string;
  companyName: string;
  resumeUrl: string;
  coverLetter: string;
  status: string;
  createdAt: string;
}

interface Department {
  id: number;
  name: string;
}

export const JobBoard: React.FC = () => {
  const { user } = useSelector((state: RootState) => state.auth);
  const isCandidate = user?.roles.some((r) => r === 'ROLE_CANDIDATE');
  const isAdminOrHr = user?.roles.some((r) => r === 'ROLE_ADMIN' || r === 'ROLE_HR');

  const [tabIndex, setTabIndex] = useState(0);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [myApplications, setMyApplications] = useState<Application[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  
  // Filters
  const [search, setSearch] = useState('');
  const [deptFilter, setDeptFilter] = useState<number | string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Dialog State
  const [openJobDialog, setOpenJobDialog] = useState(false);
  const [openApplyDialog, setOpenApplyDialog] = useState(false);
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);

  // Job Form Fields (Admin/HR)
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [requirements, setRequirements] = useState('');
  const [location, setLocation] = useState('');
  const [salaryRange, setSalaryRange] = useState('');
  const [departmentId, setDepartmentId] = useState<number | string>('');

  // Application Form Fields (Candidate)
  const [coverLetter, setCoverLetter] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [uploadedResumeUrl, setUploadedResumeUrl] = useState('');

  const fetchJobs = () => {
    setLoading(true);
    setError(null);
    const params: any = {
      search,
      status: 'OPEN',
    };
    if (deptFilter) params.departmentId = deptFilter;

    api.get('/api/v1/jobs', { params })
      .then((res) => {
        setJobs(res.data.data.content);
      })
      .catch((err) => {
        setError('Failed to fetch job postings');
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const fetchMyApplications = () => {
    if (!isCandidate) return;
    setLoading(true);
    api.get('/api/v1/recruitment/applications/my')
      .then((res) => {
        setMyApplications(res.data.data);
      })
      .catch((err) => {
        setError('Failed to load your applications history');
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const fetchDepartments = () => {
    api.get('/api/v1/departments')
      .then((res) => {
        setDepartments(res.data.data);
      })
      .catch((err) => {
        console.error('Failed to load departments list', err);
      });
  };

  useEffect(() => {
    if (tabIndex === 0) {
      fetchJobs();
    } else {
      fetchMyApplications();
    }
  }, [tabIndex, search, deptFilter]);

  useEffect(() => {
    fetchDepartments();
  }, []);

  const handleOpenCreateJob = () => {
    setTitle('');
    setDescription('');
    setRequirements('');
    setLocation('');
    setSalaryRange('');
    setDepartmentId('');
    setOpenJobDialog(true);
  };

  const handleCreateJobSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const payload = {
      title,
      description,
      requirements,
      location,
      salaryRange,
      departmentId: Number(departmentId),
    };

    api.post('/api/v1/jobs', payload)
      .then(() => {
        fetchJobs();
        setOpenJobDialog(false);
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Failed to create job posting');
      });
  };

  const handleOpenApply = (job: Job) => {
    setSelectedJob(job);
    setCoverLetter('');
    setSelectedFile(null);
    setUploadSuccess(false);
    setUploadedResumeUrl('');
    setOpenApplyDialog(true);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
      setUploadSuccess(false);
    }
  };

  const handleUploadResume = () => {
    if (!selectedFile) return;
    setUploading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', selectedFile);

    api.post('/api/v1/recruitment/upload-resume', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
      .then((res) => {
        setUploadedResumeUrl(res.data.data);
        setUploadSuccess(true);
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Resume upload failed. Please ensure file is under 5MB and format is PDF/DOCX.');
      })
      .finally(() => {
        setUploading(false);
      });
  };

  const handleApplySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadedResumeUrl) {
      setError('Please upload your resume before submitting application');
      return;
    }

    api.post('/api/v1/recruitment/apply', {
      jobId: selectedJob?.id,
      resumeUrl: uploadedResumeUrl,
      coverLetter,
    })
      .then(() => {
        setOpenApplyDialog(false);
        setTabIndex(1); // Redirect to My Applications tab
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Failed to submit job application');
      });
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 800 }}>
          Recruitment & Careers
        </Typography>
        {isAdminOrHr && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreateJob}>
            Post a Job
          </Button>
        )}
      </Box>

      {isCandidate && (
        <Tabs value={tabIndex} onChange={(_, idx) => setTabIndex(idx)} sx={{ mb: 3 }}>
          <Tab label="Explore Placements" />
          <Tab label="My Application Logs" />
        </Tabs>
      )}

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {tabIndex === 0 ? (
        <>
          {/* Filters */}
          <Paper sx={{ p: 2, mb: 3 }}>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  size="small"
                  label="Search roles, skills, description"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  InputProps={{
                    endAdornment: <SearchIcon color="action" fontSize="small" />,
                  }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  select
                  size="small"
                  label="Department Filter"
                  value={deptFilter}
                  onChange={(e) => setDeptFilter(e.target.value)}
                >
                  <MenuItem value="">All Departments</MenuItem>
                  {departments.map((dept) => (
                    <MenuItem key={dept.id} value={dept.id}>
                      {dept.name}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
            </Grid>
          </Paper>

          {/* Job Postings Grid */}
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}><CircularProgress /></Box>
          ) : (
            <Grid container spacing={3}>
              {jobs.map((job) => (
                <Grid item xs={12} md={6} key={job.id}>
                  <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="h6" sx={{ fontWeight: 700 }}>{job.title}</Typography>
                        <Chip label={job.departmentName} color="primary" size="small" variant="outlined" />
                      </Box>
                      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                        {job.location} | {job.salaryRange || 'Competitive Salary'}
                      </Typography>
                      <Divider sx={{ my: 1.5 }} />
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        {job.description}
                      </Typography>
                      <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>Key Requirements:</Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-line' }}>
                        {job.requirements}
                      </Typography>
                    </CardContent>
                    <CardActions sx={{ p: 2 }}>
                      {isCandidate && (
                        <Button variant="contained" fullWidth onClick={() => handleOpenApply(job)}>
                          Apply Now
                        </Button>
                      )}
                    </CardActions>
                  </Card>
                </Grid>
              ))}
              {jobs.length === 0 && (
                <Grid item xs={12}>
                  <Paper sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
                    No active job vacancies were found. Check back later!
                  </Paper>
                </Grid>
              )}
            </Grid>
          )}
        </>
      ) : (
        /* Candidates Application logs */
        loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}><CircularProgress /></Box>
        ) : (
          <Paper>
            <List>
              {myApplications.map((app) => (
                <ListItem key={app.id} divider>
                  <ListItemText
                    primary={app.jobTitle}
                    secondary={`Applied on: ${new Date(app.createdAt).toLocaleDateString()}`}
                  />
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Button size="small" href={app.resumeUrl} target="_blank">View Submitted CV</Button>
                    <Chip
                      label={app.status}
                      color={
                        app.status === 'OFFERED' ? 'success' :
                        app.status === 'REJECTED' ? 'error' :
                        app.status === 'INTERVIEWING' ? 'info' :
                        'default'
                      }
                    />
                  </Box>
                </ListItem>
              ))}
              {myApplications.length === 0 && (
                <ListItem sx={{ py: 3, justifySelf: 'center' }}>
                  <Typography color="text.secondary">You haven't submitted any job applications yet.</Typography>
                </ListItem>
              )}
            </List>
          </Paper>
        )
      )}

      {/* Post a Job Dialog (Admin/HR) */}
      <Dialog open={openJobDialog} onClose={() => setOpenJobDialog(false)} maxWidth="sm" fullWidth>
        <Box component="form" onSubmit={handleCreateJobSubmit}>
          <DialogTitle sx={{ fontWeight: 800 }}>Create Placement Job Posting</DialogTitle>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12}>
                <TextField required fullWidth label="Role/Job Title" value={title} onChange={(e) => setTitle(e.target.value)} />
              </Grid>
              <Grid item xs={12}>
                <TextField required fullWidth select label="Department" value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
                  {departments.map((d) => (
                    <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth label="Work Location" value={location} onChange={(e) => setLocation(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Salary Range" placeholder="e.g. $80k - $100k" value={salaryRange} onChange={(e) => setSalaryRange(e.target.value)} />
              </Grid>
              <Grid item xs={12}>
                <TextField required fullWidth multiline rows={4} label="Role Description" value={description} onChange={(e) => setDescription(e.target.value)} />
              </Grid>
              <Grid item xs={12}>
                <TextField required fullWidth multiline rows={4} label="Key Candidate Requirements" placeholder="Enter list of requirements" value={requirements} onChange={(e) => setRequirements(e.target.value)} />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 3 }}>
            <Button onClick={() => setOpenJobDialog(false)} color="inherit">Cancel</Button>
            <Button type="submit" variant="contained">Post Listing</Button>
          </DialogActions>
        </Box>
      </Dialog>

      {/* Apply for Job Dialog (Candidate) */}
      <Dialog open={openApplyDialog} onClose={() => setOpenApplyDialog(false)} maxWidth="sm" fullWidth>
        <Box component="form" onSubmit={handleApplySubmit}>
          <DialogTitle sx={{ fontWeight: 800 }}>
            Apply for {selectedJob?.title}
          </DialogTitle>
          <DialogContent>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Upload your resume and attach a cover letter below to submit your application.
            </Typography>

            <Box sx={{ my: 3, p: 2, border: '2px dashed #e2e8f0', borderRadius: 2, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
              <Button component="label" variant="outlined" startIcon={<UploadIcon />}>
                Choose Resume File
                <VisuallyHiddenInput
                  type="file"
                  accept=".pdf,.docx"
                  onChange={handleFileChange}
                />
              </Button>
              
              {selectedFile && (
                <Typography variant="body2">{selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)</Typography>
              )}

              {selectedFile && !uploadSuccess && (
                <Button
                  size="small"
                  variant="contained"
                  color="secondary"
                  disabled={uploading}
                  onClick={handleUploadResume}
                >
                  {uploading ? <CircularProgress size={20} color="inherit" /> : 'Confirm Upload'}
                </Button>
              )}

              {uploadSuccess && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'success.main' }}>
                  <SuccessIcon fontSize="small" />
                  <Typography variant="body2">Resume uploaded successfully!</Typography>
                </Box>
              )}
            </Box>

            <TextField
              fullWidth
              multiline
              rows={4}
              label="Cover Letter"
              value={coverLetter}
              onChange={(e) => setCoverLetter(e.target.value)}
              placeholder="Explain why you are a good fit for this placement role..."
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 3 }}>
            <Button onClick={() => setOpenApplyDialog(false)} color="inherit">Cancel</Button>
            <Button type="submit" variant="contained" disabled={!uploadSuccess}>Submit Application</Button>
          </DialogActions>
        </Box>
      </Dialog>
    </Box>
  );
};
export default JobBoard;
