import api from '@/shared/services/api.service';

const adminReportService = {
  getReports(params) {
    return api.get('/admin/reports', { params });
  },
  getReportDetails(params) {
    return api.get('/admin/reports/detail', { params });
  },
  resolveReport(id, payload) {
    return api.put(`/admin/reports/${id}/resolve`, payload);
  },
  resolveReportGroup(payload) {
    return api.put('/admin/reports/resolve-group', payload);
  }
};

export default adminReportService;
