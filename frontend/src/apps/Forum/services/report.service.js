import api from '@/shared/services/api.service';

const reportService = {
  create(payload) {
    return api.post('/api/reports', payload);
  }
};

export default reportService;
