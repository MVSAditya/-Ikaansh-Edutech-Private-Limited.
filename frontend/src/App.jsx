import { useEffect, useState } from 'react';
import {
  ArrowDownUp,
  ArrowLeft,
  ArrowRight,
  Check,
  ChevronDown,
  CircleAlert,
  ClipboardList,
  Clock3,
  LogOut,
  Plus,
  ShieldCheck,
  Sparkles,
  X,
} from 'lucide-react';
import { apiRequest } from './api.js';

const PAGE_SIZE = 8;
const STATUS_OPTIONS = ['TODO', 'IN_PROGRESS', 'DONE'];

function readSession() {
  return {
    token: localStorage.getItem('accessToken'),
    email: localStorage.getItem('accountEmail'),
    role: localStorage.getItem('accountRole'),
  };
}

function formatStatus(status) {
  return status.replace('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatDate(value) {
  if (!value) return 'No due date';
  return new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', year: 'numeric' })
    .format(new Date(`${value}T00:00:00`));
}

function App() {
  const [session, setSession] = useState(readSession);
  const [authMode, setAuthMode] = useState('login');
  const [authEmail, setAuthEmail] = useState('');
  const [authName, setAuthName] = useState('');
  const [authDepartment, setAuthDepartment] = useState('');
  const [authPassword, setAuthPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [authBusy, setAuthBusy] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [sort, setSort] = useState('createdAt,desc');
  const [page, setPage] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);
  const [taskPage, setTaskPage] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pageError, setPageError] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [actionError, setActionError] = useState('');

  const isManager = session.role === 'ADMIN' || session.role === 'MANAGER';

  useEffect(() => {
    if (!session.token) return undefined;
    let active = true;
    const query = new URLSearchParams({ page, size: PAGE_SIZE, sort });
    if (statusFilter) query.set('status', statusFilter);
    if (priorityFilter) query.set('priority', priorityFilter);

    setLoading(true);
    setPageError('');
    apiRequest(`/api/tasks?${query}`, { token: session.token })
      .then((result) => {
        if (active) setTaskPage(result);
      })
      .catch((error) => {
        if (active) setPageError(error.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => { active = false; };
  }, [session.token, page, sort, statusFilter, priorityFilter, refreshKey]);

  useEffect(() => {
    if (!session.token || !isManager) return undefined;
    let active = true;
    apiRequest('/api/employees?sort=name,asc', { token: session.token })
      .then((result) => {
        if (active) setEmployees(result);
      })
      .catch((error) => {
        if (active) setActionError(error.message);
      });
    return () => { active = false; };
  }, [session.token, isManager]);

  async function submitAuth(event) {
    event.preventDefault();
    setAuthBusy(true);
    setAuthError('');
    try {
      const endpoint = authMode === 'login' ? '/api/auth/login' : '/api/auth/register';
      const response = await apiRequest(endpoint, {
        method: 'POST',
        body: JSON.stringify(authMode === 'login'
          ? { email: authEmail, password: authPassword }
          : { name: authName, email: authEmail, department: authDepartment, password: authPassword }),
      });
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('accountEmail', authEmail);
      localStorage.setItem('accountRole', response.role);
      setSession({ token: response.accessToken, email: authEmail, role: response.role });
      setAuthPassword('');
    } catch (error) {
      setAuthError(error.message);
    } finally {
      setAuthBusy(false);
    }
  }

  function signOut() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('accountEmail');
    localStorage.removeItem('accountRole');
    setSession({ token: null, email: null, role: null });
    setTaskPage({ content: [], totalElements: 0, totalPages: 0 });
    setEmployees([]);
    setPage(0);
  }

  async function changeTaskStatus(task, status) {
    setActionError('');
    try {
      await apiRequest(`/api/tasks/${task.id}`, {
        method: 'PUT',
        token: session.token,
        body: JSON.stringify({
          title: task.title,
          description: task.description,
          status,
          priority: task.priority,
          dueDate: task.dueDate,
          assignedEmployeeId: task.assignedEmployeeId,
        }),
      });
      setRefreshKey((key) => key + 1);
    } catch (error) {
      setActionError(error.message);
      throw error;
    }
  }

  async function deleteTask(task) {
    if (!window.confirm(`Delete “${task.title}”? This cannot be undone.`)) return;
    setActionError('');
    try {
      await apiRequest(`/api/tasks/${task.id}`, { method: 'DELETE', token: session.token });
      setRefreshKey((key) => key + 1);
    } catch (error) {
      setActionError(error.message);
    }
  }

  async function createTask(payload) {
    setActionError('');
    try {
      await apiRequest('/api/tasks', {
        method: 'POST',
        token: session.token,
        body: JSON.stringify(payload),
      });
      setFormOpen(false);
      setPage(0);
      setRefreshKey((key) => key + 1);
    } catch (error) {
      setActionError(error.message);
    }
  }

  if (!session.token) {
    return (
      <AuthScreen
        mode={authMode}
        setMode={(mode) => { setAuthMode(mode); setAuthError(''); }}
        name={authName}
        setName={setAuthName}
        email={authEmail}
        setEmail={setAuthEmail}
        department={authDepartment}
        setDepartment={setAuthDepartment}
        password={authPassword}
        setPassword={setAuthPassword}
        onSubmit={submitAuth}
        error={authError}
        busy={authBusy}
      />
    );
  }

  const visibleTasks = taskPage.content ?? [];
  const activeCount = visibleTasks.filter((task) => task.status === 'IN_PROGRESS').length;
  const doneCount = visibleTasks.filter((task) => task.status === 'DONE').length;

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#tasks" aria-label="Fieldwork home">
          <span className="brand-mark"><ClipboardList size={19} strokeWidth={2.2} /></span>
          <span>fieldwork<span className="brand-period">.</span></span>
        </a>
        <div className="workspace-switcher">
          <span className="workspace-icon">I</span>
          <span className="workspace-copy"><strong>IKAANSH</strong><small>Product team</small></span>
          <ChevronDown size={15} />
        </div>
        <p className="nav-caption">WORKSPACE</p>
        <nav className="primary-nav" aria-label="Main navigation">
          <a className="nav-link active" href="#tasks"><ClipboardList size={17} />Tasks<span className="nav-count">{taskPage.totalElements}</span></a>
        </nav>
        <div className="sidebar-bottom">
          <div className="security-note"><ShieldCheck size={17} /><span><strong>Secure workspace</strong><small>JWT protected</small></span></div>
          <button className="profile-row" onClick={signOut} title="Sign out">
            <span className="avatar">{session.email?.slice(0, 1).toUpperCase()}</span>
            <span className="profile-copy"><strong>{session.email}</strong><small>{session.role?.toLowerCase()}</small></span>
            <LogOut size={16} />
          </button>
        </div>
      </aside>

      <main className="main-panel" id="tasks">
        <header className="topbar">
          <div className="breadcrumb"><span>Workspace</span><span className="crumb-slash">/</span><strong>Tasks</strong></div>
          <div className="topbar-right"><span className="live-dot" />Workspace synced<span className="topbar-divider" /><span className="topbar-role">{session.role}</span></div>
        </header>

        <section className="page-content">
          <div className="page-heading">
            <div>
              <div className="eyebrow"><span className="eyebrow-line" />TASK MANAGEMENT</div>
              <h1>{isManager ? 'Task overview' : 'Your tasks'}</h1>
              <p>{isManager ? 'A clear view of work in motion across your team.' : 'The work assigned to you, all in one place.'}</p>
            </div>
            {isManager && <button className="button button-primary" onClick={() => { setActionError(''); setFormOpen(true); }}><Plus size={17} />New task</button>}
          </div>

          <div className="summary-grid" aria-label="Task summary">
            <SummaryTile label="Total tasks" value={taskPage.totalElements} note="Matching your filters" icon={<ClipboardList size={17} />} tone="green" />
            <SummaryTile label="In progress" value={activeCount} note="On this page" icon={<Clock3 size={17} />} tone="amber" />
            <SummaryTile label="Completed" value={doneCount} note="On this page" icon={<Check size={17} />} tone="blue" />
            <SummaryTile label="Current view" value={`${taskPage.totalPages ? page + 1 : 0} / ${taskPage.totalPages}`} note={`${PAGE_SIZE} tasks per page`} icon={<Sparkles size={17} />} tone="rose" />
          </div>

          <section className="task-section" aria-labelledby="task-list-title">
            <div className="section-heading">
              <div><h2 id="task-list-title">All tasks</h2><span className="result-count">{taskPage.totalElements} records</span></div>
              <div className="filters">
                <label className="filter-control"><span className="sr-only">Filter by status</span><select value={statusFilter} onChange={(event) => { setStatusFilter(event.target.value); setPage(0); }}><option value="">All statuses</option>{STATUS_OPTIONS.map((status) => <option key={status} value={status}>{formatStatus(status)}</option>)}</select></label>
                <label className="filter-control"><span className="sr-only">Filter by priority</span><select value={priorityFilter} onChange={(event) => { setPriorityFilter(event.target.value); setPage(0); }}><option value="">All priorities</option><option value="HIGH">High priority</option><option value="MEDIUM">Medium priority</option><option value="LOW">Low priority</option></select></label>
                <label className="filter-control sort-control"><ArrowDownUp size={15} /><span className="sr-only">Sort tasks</span><select value={sort} onChange={(event) => { setSort(event.target.value); setPage(0); }}><option value="createdAt,desc">Newest first</option><option value="dueDate,asc">Due date</option><option value="title,asc">Title A–Z</option></select></label>
              </div>
            </div>

            {pageError && <div className="notice notice-error"><CircleAlert size={17} />{pageError}<button onClick={() => setRefreshKey((key) => key + 1)}>Retry</button></div>}
            {actionError && <div className="notice notice-error"><CircleAlert size={17} />{actionError}<button className="notice-dismiss" onClick={() => setActionError('')} aria-label="Dismiss"><X size={15} /></button></div>}

            <div className="table-wrap">
              <table className="task-table">
                <thead><tr><th>Task</th><th>Assignee</th><th>Due date</th><th>Priority</th><th>Status</th>{isManager && <th aria-label="Actions" />}</tr></thead>
                <tbody>
                  {!loading && visibleTasks.map((task) => <TaskRow key={task.id} task={task} isManager={isManager} onStatusChange={changeTaskStatus} onDelete={deleteTask} />)}
                </tbody>
              </table>
              {loading && <div className="list-state"><span className="spinner" />Loading tasks</div>}
              {!loading && visibleTasks.length === 0 && !pageError && <div className="empty-state"><span className="empty-icon"><ClipboardList size={22} /></span><strong>No tasks in this view</strong><span>Try changing a filter, or create a task to get started.</span>{isManager && <button className="button button-secondary" onClick={() => setFormOpen(true)}><Plus size={16} />Create a task</button>}</div>}
            </div>

            <div className="pagination">
              <span>Showing {taskPage.totalElements === 0 ? 0 : page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, taskPage.totalElements)} of {taskPage.totalElements}</span>
              <div className="page-buttons"><button aria-label="Previous page" disabled={page === 0 || loading} onClick={() => setPage((current) => Math.max(0, current - 1))}><ArrowLeft size={16} /></button><span>Page <strong>{taskPage.totalPages ? page + 1 : 0}</strong> of {taskPage.totalPages}</span><button aria-label="Next page" disabled={page + 1 >= taskPage.totalPages || loading} onClick={() => setPage((current) => current + 1)}><ArrowRight size={16} /></button></div>
            </div>
          </section>
          <footer className="page-footer"><span>FIELDWORK <span className="brand-period">/</span> TEAM OPERATIONS</span><span>Authenticated as {session.email}</span></footer>
        </section>
      </main>

      {formOpen && <TaskDialog employees={employees} onClose={() => setFormOpen(false)} onSubmit={createTask} />}
    </div>
  );
}

function SummaryTile({ label, value, note, icon, tone }) {
  return <article className="summary-tile"><span className={`summary-icon ${tone}`}>{icon}</span><div className="summary-value">{value}</div><div className="summary-label">{label}</div><div className="summary-note">{note}</div></article>;
}

function TaskRow({ task, isManager, onStatusChange, onDelete }) {
  return (
    <tr>
      <td><div className="task-title-cell"><span className={`task-symbol ${task.status.toLowerCase()}`}><ClipboardList size={15} /></span><span><strong>{task.title}</strong><small>{task.description || 'No description'}</small></span></div></td>
      <td><span className="assignee"><span className="assignee-avatar">{task.assignedEmployeeName?.slice(0, 1).toUpperCase()}</span>{task.assignedEmployeeName}</span></td>
      <td><span className={`due-date ${task.dueDate && task.dueDate < new Date().toISOString().slice(0, 10) && task.status !== 'DONE' ? 'overdue' : ''}`}>{formatDate(task.dueDate)}</span></td>
      <td><span className={`priority priority-${task.priority.toLowerCase()}`}><span />{task.priority.toLowerCase()}</span></td>
      <td>{isManager ? <label className={`status-select status-${task.status.toLowerCase()}`}><span className="sr-only">Change status for {task.title}</span><select value={task.status} onChange={(event) => onStatusChange(task, event.target.value)}>{STATUS_OPTIONS.map((status) => <option key={status} value={status}>{formatStatus(status)}</option>)}</select><ChevronDown size={13} /></label> : <span className={`status-badge status-${task.status.toLowerCase()}`}>{formatStatus(task.status)}</span>}</td>
      {isManager && <td><button className="icon-button delete-button" title="Delete task" aria-label={`Delete ${task.title}`} onClick={() => onDelete(task)}><X size={16} /></button></td>}
    </tr>
  );
}

function TaskDialog({ employees, onClose, onSubmit }) {
  const [form, setForm] = useState({ title: '', description: '', status: 'TODO', priority: 'MEDIUM', dueDate: '', assignedEmployeeId: employees[0]?.id ?? '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    if (!form.assignedEmployeeId) {
      setError('Add an employee before assigning a task.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await onSubmit({ ...form, assignedEmployeeId: Number(form.assignedEmployeeId), dueDate: form.dueDate || null });
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="task-dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title">
        <div className="dialog-heading"><div><span className="dialog-kicker">WORK ITEM</span><h2 id="dialog-title">Create a task</h2></div><button className="icon-button" onClick={onClose} aria-label="Close dialog"><X size={19} /></button></div>
        <form onSubmit={submit} className="task-form">
          <label>Task title<input autoFocus required maxLength="180" value={form.title} onChange={(event) => update('title', event.target.value)} placeholder="e.g. Prepare sprint review" /></label>
          <label>Description <span className="optional">Optional</span><textarea maxLength="5000" rows="3" value={form.description} onChange={(event) => update('description', event.target.value)} placeholder="Add context and expected outcome" /></label>
          <div className="form-two-col">
            <label>Assignee<select required value={form.assignedEmployeeId} onChange={(event) => update('assignedEmployeeId', event.target.value)}><option value="">Choose a teammate</option>{employees.map((employee) => <option key={employee.id} value={employee.id}>{employee.name} · {employee.department}</option>)}</select></label>
            <label>Due date<input type="date" value={form.dueDate} onChange={(event) => update('dueDate', event.target.value)} /></label>
            <label>Priority<select value={form.priority} onChange={(event) => update('priority', event.target.value)}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></label>
            <label>Status<select value={form.status} onChange={(event) => update('status', event.target.value)}>{STATUS_OPTIONS.map((status) => <option key={status} value={status}>{formatStatus(status)}</option>)}</select></label>
          </div>
          {error && <div className="inline-error"><CircleAlert size={16} />{error}</div>}
          <div className="dialog-actions"><button type="button" className="button button-quiet" onClick={onClose}>Cancel</button><button type="submit" className="button button-primary" disabled={busy || employees.length === 0}>{busy ? 'Creating…' : <><Plus size={16} />Create task</>}</button></div>
        </form>
      </section>
    </div>
  );
}

function AuthScreen({ mode, setMode, name, setName, email, setEmail, department, setDepartment,
  password, setPassword, onSubmit, error, busy }) {
  const isLogin = mode === 'login';
  return (
    <main className="auth-screen">
      <div className="auth-visual">
        <div className="visual-top"><a className="brand brand-light" href="#home"><span className="brand-mark"><ClipboardList size={19} /></span><span>fieldwork<span className="brand-period">.</span></span></a><span className="visual-edition">TEAM OPERATIONS / 01</span></div>
        <div className="visual-copy"><span className="visual-eyebrow"><span />MAKE ROOM FOR GOOD WORK</span><h1>Work moves<br />when teams<br /><em>move together.</em></h1><p>A calmer view of the work that matters, from first assignment to done.</p></div>
        <div className="visual-bottom"><span>IKAANSH EDUTECH</span><span>PEOPLE · PROJECTS · PROGRESS</span></div>
      </div>
      <section className="auth-panel">
        <div className="auth-panel-top"><span>TEAM WORKSPACE</span><span className="secure-label"><ShieldCheck size={14} />SECURE ACCESS</span></div>
        <div className="auth-form-wrap">
          <span className="auth-step">{isLogin ? 'WELCOME BACK' : 'GET STARTED'}</span>
          <h2>{isLogin ? 'Sign in to your workspace' : 'Create your account'}</h2>
          <p className="auth-subtitle">{isLogin ? 'Enter your details to continue to Fieldwork.' : 'Your account starts with employee-level access.'}</p>
          <div className="auth-tabs" role="tablist" aria-label="Authentication mode"><button className={isLogin ? 'selected' : ''} onClick={() => { setMode('login'); }} role="tab" aria-selected={isLogin}>Sign in</button><button className={!isLogin ? 'selected' : ''} onClick={() => { setMode('register'); }} role="tab" aria-selected={!isLogin}>Create account</button></div>
          <form className="auth-form" onSubmit={onSubmit}>
            {!isLogin && <label>Full name<input type="text" autoComplete="name" maxLength="120" required value={name} onChange={(event) => setName(event.target.value)} placeholder="Your name" /></label>}
            <label>Email address<input type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@company.com" /></label>
            {!isLogin && <label>Department<input type="text" maxLength="120" required value={department} onChange={(event) => setDepartment(event.target.value)} placeholder="e.g. Engineering" /></label>}
            <label>Password<input type="password" autoComplete={isLogin ? 'current-password' : 'new-password'} minLength="8" maxLength="72" required value={password} onChange={(event) => setPassword(event.target.value)} placeholder={isLogin ? 'Enter your password' : 'At least 8 characters'} /></label>
            {error && <div className="inline-error"><CircleAlert size={16} />{error}</div>}
            <button type="submit" className="button button-primary auth-submit" disabled={busy}>{busy ? 'Please wait…' : <>{isLogin ? 'Continue to workspace' : 'Create account'}<ArrowRight size={17} /></>}</button>
          </form>
          <div className="auth-hint"><ShieldCheck size={16} /><span>Protected with signed, role-based access tokens.</span></div>
        </div>
        <div className="auth-panel-footer"><span>© 2026 Fieldwork</span><span>BUILT FOR THE WORK AHEAD</span></div>
      </section>
    </main>
  );
}

export default App;