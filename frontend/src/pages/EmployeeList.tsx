import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import type { RootState } from '../store';
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
  TablePagination,
  TextField,
  MenuItem,
  Button,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
  Search as SearchIcon,
} from '@mui/icons-material';

interface Employee {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  jobTitle: string;
  departmentId: number;
  departmentName: string;
  salary: number;
  hireDate: string;
  status: string;
}

interface Department {
  id: number;
  name: string;
  code: string;
}

export const EmployeeList: React.FC = () => {
  const { user } = useSelector((state: RootState) => state.auth);
  const isAdmin = user?.roles.some((r) => r === 'ROLE_ADMIN');
  const isAdminOrHr = user?.roles.some((r) => r === 'ROLE_ADMIN' || r === 'ROLE_HR');

  const [employees, setEmployees] = useState<Employee[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  
  // Search & Filter State
  const [search, setSearch] = useState('');
  const [deptFilter, setDeptFilter] = useState<number | string>('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Dialog Forms State
  const [openDialog, setOpenDialog] = useState(false);
  const [dialogMode, setDialogMode] = useState<'create' | 'edit'>('create');
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);
  
  // Form Fields
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [usernameField, setUsernameField] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [jobTitle, setJobTitle] = useState('');
  const [departmentId, setDepartmentId] = useState<number | string>('');
  const [salary, setSalary] = useState('');
  const [hireDate, setHireDate] = useState('');
  const [statusField, setStatusField] = useState('ACTIVE');

  // Load Employees and Departments
  const fetchEmployees = () => {
    setLoading(true);
    setError(null);
    
    const params: any = {
      search,
      page,
      size,
      sortBy: 'employeeId',
      sortDir: 'asc',
    };
    if (deptFilter) params.departmentId = deptFilter;
    if (statusFilter) params.status = statusFilter;

    api.get('/api/v1/employees', { params })
      .then((res) => {
        setEmployees(res.data.data.content);
        setTotalElements(res.data.data.totalElements);
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Failed to load employee list');
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
        console.error('Failed to load departments', err);
      });
  };

  useEffect(() => {
    fetchEmployees();
  }, [search, deptFilter, statusFilter, page, size]);

  useEffect(() => {
    fetchDepartments();
  }, []);

  const handleOpenCreate = () => {
    setDialogMode('create');
    setSelectedEmployee(null);
    setFirstName('');
    setLastName('');
    setEmail('');
    setUsernameField('');
    setEmployeeId('');
    setJobTitle('');
    setDepartmentId('');
    setSalary('');
    setHireDate(new Date().toISOString().split('T')[0]);
    setStatusField('ACTIVE');
    setOpenDialog(true);
  };

  const handleOpenEdit = (emp: Employee) => {
    setDialogMode('edit');
    setSelectedEmployee(emp);
    setFirstName(emp.firstName);
    setLastName(emp.lastName);
    setEmail(emp.email);
    setUsernameField(emp.username || '');
    setEmployeeId(emp.employeeId);
    setJobTitle(emp.jobTitle);
    setDepartmentId(emp.departmentId || '');
    setSalary(emp.salary.toString());
    setHireDate(emp.hireDate);
    setStatusField(emp.status);
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const payload = {
      firstName,
      lastName,
      email,
      username: usernameField || null,
      employeeId,
      jobTitle,
      departmentId: Number(departmentId),
      salary: Number(salary),
      hireDate,
      status: statusField,
    };

    const apiCall = dialogMode === 'create'
      ? api.post('/api/v1/employees', payload)
      : api.put(`/api/v1/employees/${selectedEmployee?.id}`, payload);

    apiCall
      .then(() => {
        fetchEmployees();
        setOpenDialog(false);
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Error occurred while saving profile');
      });
  };

  const handleDelete = (id: string) => {
    if (window.confirm('Are you sure you want to delete this employee?')) {
      api.delete(`/api/v1/employees/${id}`)
        .then(() => {
          fetchEmployees();
        })
        .catch((err) => {
          setError(err.response?.data?.message || 'Failed to delete employee profile');
        });
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 800 }}>
          Employee Directory
        </Typography>
        {isAdminOrHr && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate}>
            Add Employee
          </Button>
        )}
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {/* Filter Toolbar */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              size="small"
              label="Search name, email, or title"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              InputProps={{
                endAdornment: <SearchIcon color="action" fontSize="small" />,
              }}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
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
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              select
              size="small"
              label="Work Status Filter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <MenuItem value="">All Statuses</MenuItem>
              <MenuItem value="ACTIVE">Active</MenuItem>
              <MenuItem value="LEAVE">On Leave</MenuItem>
              <MenuItem value="TERMINATED">Terminated</MenuItem>
            </TextField>
          </Grid>
        </Grid>
      </Paper>

      {/* Employee List Table */}
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Emp ID</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Name</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Email</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Job Title</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Department</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                {isAdminOrHr && <TableCell sx={{ fontWeight: 700 }} align="right">Actions</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {employees.map((emp) => (
                <TableRow key={emp.id} hover>
                  <TableCell>{emp.employeeId}</TableCell>
                  <TableCell>{`${emp.firstName} ${emp.lastName}`}</TableCell>
                  <TableCell>{emp.email}</TableCell>
                  <TableCell>{emp.jobTitle}</TableCell>
                  <TableCell>{emp.departmentName}</TableCell>
                  <TableCell sx={{ textTransform: 'capitalize' }}>{emp.status.toLowerCase()}</TableCell>
                  {isAdminOrHr && (
                    <TableCell align="right">
                      <IconButton size="small" color="primary" onClick={() => handleOpenEdit(emp)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      {isAdmin && (
                        <IconButton size="small" color="error" onClick={() => handleDelete(emp.id)}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
              {employees.length === 0 && (
                <TableRow>
                  <TableCell colSpan={isAdminOrHr ? 7 : 6} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                    No employees matching the search filters were found.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            onPageChange={(_, newPage) => setPage(newPage)}
            rowsPerPage={size}
            onRowsPerPageChange={(e) => {
              setSize(parseInt(e.target.value, 10));
              setPage(0);
            }}
          />
        </TableContainer>
      )}

      {/* Create / Edit Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <Box component="form" onSubmit={handleSave}>
          <DialogTitle sx={{ fontWeight: 800 }}>
            {dialogMode === 'create' ? 'Add Employee Record' : 'Edit Employee Profile'}
          </DialogTitle>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth label="First Name" value={firstName} onChange={(e) => setFirstName(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth label="Last Name" value={lastName} onChange={(e) => setLastName(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth type="email" label="Email Address" value={email} onChange={(e) => setEmail(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField fullWidth label="Mapped Username" placeholder="Optional" value={usernameField} onChange={(e) => setUsernameField(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth label="Employee ID" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth label="Job Title" value={jobTitle} onChange={(e) => setJobTitle(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  select
                  label="Department"
                  value={departmentId}
                  onChange={(e) => setDepartmentId(e.target.value)}
                >
                  {departments.map((dept) => (
                    <MenuItem key={dept.id} value={dept.id}>
                      {dept.name}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth type="number" label="Salary ($)" value={salary} onChange={(e) => setSalary(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField required fullWidth type="date" label="Hire Date" InputLabelProps={{ shrink: true }} value={hireDate} onChange={(e) => setHireDate(e.target.value)} />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  required
                  fullWidth
                  select
                  label="Status"
                  value={statusField}
                  onChange={(e) => setStatusField(e.target.value)}
                >
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="LEAVE">On Leave</MenuItem>
                  <MenuItem value="TERMINATED">Terminated</MenuItem>
                </TextField>
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 3 }}>
            <Button onClick={handleCloseDialog} color="inherit">Cancel</Button>
            <Button type="submit" variant="contained">Save</Button>
          </DialogActions>
        </Box>
      </Dialog>
    </Box>
  );
};
export default EmployeeList;
