import React, { useState } from 'react';
import api from '../lib/axios';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  Typography,
  Box,
  CircularProgress,
  Grid,
  Paper,
} from '@mui/material';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

interface BacktestWizardProps {
  open: boolean;
  onClose: () => void;
  strategyId: string;
  strategyName: string;
}

interface BacktestResult {
  totalTrades: number;
  winningTrades: number;
  losingTrades: number;
  winRate: number;
  totalPnL: number;
  maxDrawdown: number;
  initialCapital: number;
  finalCapital: number;
  equityCurve: { timestamp: number; equity: number }[];
}

const BacktestWizard: React.FC<BacktestWizardProps> = ({ open, onClose, strategyId, strategyName }) => {
  const [startDate, setStartDate] = useState('2023-01-01');
  const [endDate, setEndDate] = useState(new Date().toISOString().split('T')[0]);
  const [interval, setInterval] = useState('1d');
  const [initialCapital, setInitialCapital] = useState(100000);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<BacktestResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleRun = async () => {
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const response = await api.post(`/strategies/${strategyId}/backtest`, {
        startDate: `${startDate}T00:00:00Z`,
        endDate: `${endDate}T23:59:59Z`,
        interval,
        initialCapital,
      });

      setResult(response.data);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Backtest Strategy: {strategyName}</DialogTitle>
      <DialogContent>
        {!result ? (
          <Box sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Grid container spacing={2}>
              <Grid size={{ xs: 6 }}>
                <TextField
                  label="Start Date"
                  type="date"
                  fullWidth
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  slotProps={{ inputLabel: { shrink: true } }}
                />
              </Grid>
              <Grid size={{ xs: 6 }}>
                <TextField
                  label="End Date"
                  type="date"
                  fullWidth
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  slotProps={{ inputLabel: { shrink: true } }}
                />
              </Grid>
              <Grid size={{ xs: 6 }}>
                <TextField
                  select
                  label="Interval"
                  fullWidth
                  value={interval}
                  onChange={(e) => setInterval(e.target.value)}
                >
                  <MenuItem value="1m">1 Minute</MenuItem>
                  <MenuItem value="5m">5 Minutes</MenuItem>
                  <MenuItem value="15m">15 Minutes</MenuItem>
                  <MenuItem value="1h">1 Hour</MenuItem>
                  <MenuItem value="1d">1 Day</MenuItem>
                </TextField>
              </Grid>
              <Grid size={{ xs: 6 }}>
                <TextField
                  label="Initial Capital (₹)"
                  type="number"
                  fullWidth
                  value={initialCapital}
                  onChange={(e) => setInitialCapital(Number(e.target.value))}
                />
              </Grid>
            </Grid>
            {error && <Typography color="error">{error}</Typography>}
          </Box>
        ) : (
          <Box sx={{ mt: 2 }}>
            <Grid container spacing={2} sx={{ mb: 4 }}>
              <Grid size={{ xs: 3 }}>
                <Paper sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="body2" color="text.secondary">Total PnL</Typography>
                  <Typography variant="h6" color={result.totalPnL >= 0 ? 'success.main' : 'error.main'}>
                    ₹{result.totalPnL.toFixed(2)}
                  </Typography>
                </Paper>
              </Grid>
              <Grid size={{ xs: 3 }}>
                <Paper sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="body2" color="text.secondary">Win Rate</Typography>
                  <Typography variant="h6">{result.winRate.toFixed(1)}%</Typography>
                </Paper>
              </Grid>
              <Grid size={{ xs: 3 }}>
                <Paper sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="body2" color="text.secondary">Max Drawdown</Typography>
                  <Typography variant="h6" color="error.main">₹{result.maxDrawdown.toFixed(2)}</Typography>
                </Paper>
              </Grid>
              <Grid size={{ xs: 3 }}>
                <Paper sx={{ p: 2, textAlign: 'center' }}>
                  <Typography variant="body2" color="text.secondary">Total Trades</Typography>
                  <Typography variant="h6">{result.totalTrades}</Typography>
                </Paper>
              </Grid>
            </Grid>
            
            <Typography variant="h6" sx={{ mb: 2 }}>Equity Curve</Typography>
            <Box sx={{ height: 300, width: '100%' }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={result.equityCurve}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis 
                    dataKey="timestamp" 
                    type="number"
                    domain={['dataMin', 'dataMax']}
                    tickFormatter={(unixTime) => new Date(unixTime).toLocaleDateString()}
                  />
                  <YAxis domain={['auto', 'auto']} />
                  <Tooltip 
                    labelFormatter={(label) => new Date(label).toLocaleString()}
                    formatter={(value: any) => [`₹${Number(value).toFixed(2)}`, 'Equity']}
                  />
                  <Line type="monotone" dataKey="equity" stroke="#8884d8" dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </Box>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={loading}>
          {result ? 'Close' : 'Cancel'}
        </Button>
        {!result && (
          <Button onClick={handleRun} variant="contained" disabled={loading}>
            {loading ? <CircularProgress size={24} /> : 'Run Backtest'}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default BacktestWizard;
