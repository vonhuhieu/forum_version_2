import api from '@/shared/services/api.service';

const adminReportService = {
  getReports(params) {
    return api.get('/api/admin/reports', { params });
  },
  resolveReport(id, payload) {
    return api.put(`/api/admin/reports/${id}/resolve`, payload);
  }
};

export default adminReportService;
