import api from '@/shared/services/api.service';

const adminReportService = {
  getReports(params) {
    return api.get('/admin/reports', { params });
  },
  resolveReport(id, payload) {
    return api.put(`/admin/reports/${id}/resolve`, payload);
  }
};

export default adminReportService;
