<template>
  <div class="page-content">
    <DataTable
      title="Quản lý Báo cáo Vi phạm"
      placeholder="Tìm kiếm lý do, người báo cáo..."
      :headers="headers"
      :items="reports"
      :totalItems="totalElements"
      v-model:pageSize="pageSize"
      v-model:currentPage="currentPage"
      :loading="loading"
      :showAddButton="false"
      :showAction="false"
      @search="handleSearch"
    >
      <!-- Filter by status -->
      <template #extra-filters>
        <div class="filter-item-mini">
          <select v-model="selectedStatus" @change="fetchReports" class="mini-select">
            <option value="">Tất cả trạng thái</option>
            <option value="PENDING">Chờ xử lý (PENDING)</option>
            <option value="RESOLVED">Đã giải quyết (RESOLVED)</option>
            <option value="REJECTED">Đã bác bỏ (REJECTED)</option>
          </select>
        </div>
      </template>

      <!-- Column targetType -->
      <template #item-targetType="{ item }">
        <span :class="['badge-type', item.targetType === 'THREAD' ? 'badge-info' : 'badge-warning']">
          {{ item.targetType === 'THREAD' ? 'Thread' : 'Bình luận' }}
        </span>
      </template>

      <!-- Column createdAt -->
      <template #item-createdAt="{ item }">
        {{ formatDate(item.createdAt) }}
      </template>

      <!-- Column status -->
      <template #item-status="{ item }">
        <span :class="['badge-status', getStatusBadgeClass(item.status)]">
          {{ getStatusText(item.status) }}
        </span>
      </template>

      <!-- Column actions -->
      <template #item-actions="{ item }">
        <div class="report-actions">
          <button class="action-btn-custom btn-view" @click="viewReportTarget(item)" title="Xem bài viết vi phạm">
            👁 Xem
          </button>
          <template v-if="item.status === 'PENDING'">
            <button class="action-btn-custom btn-resolve" @click="handleResolve(item, true)" title="Xác nhận vi phạm & Xóa nội dung">
              ✓ Xóa
            </button>
            <button class="action-btn-custom btn-reject" @click="handleResolve(item, false)" title="Bác bỏ báo cáo">
              ✗ Bỏ qua
            </button>
          </template>
          <span v-else class="resolved-by-text" :title="`Xử lý bởi ${item.resolvedByUsername}`">
            {{ item.status === 'RESOLVED' ? 'Đã xóa' : 'Bác bỏ' }}
          </span>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script>
import DataTable from '@/shared/components/DataTable.vue'
import adminReportService from '@/apps/Admin/services/adminReport.service'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'
import { formatForumDate } from '@/shared/utils/date'

export default {
  name: 'ReportManagement',
  components: {
    DataTable
  },
  data() {
    return {
      headers: [
        { text: 'Loại', value: 'targetType', width: '100px' },
        { text: 'Nội dung trích dẫn', value: 'targetContentSnippet', width: '250px' },
        { text: 'Tác giả', value: 'targetAuthorUsername', width: '120px' },
        { text: 'Người báo cáo', value: 'reporterUsername', width: '120px' },
        { text: 'Lý do báo cáo', value: 'reason', width: '200px' },
        { text: 'Thời gian', value: 'createdAt', width: '160px' },
        { text: 'Trạng thái', value: 'status', width: '120px' },
        { text: 'Hành động', value: 'actions', width: '180px' }
      ],
      reports: [],
      totalElements: 0,
      pageSize: 10,
      currentPage: 1,
      loading: false,
      selectedStatus: 'PENDING',
      searchKeyword: ''
    }
  },
  watch: {
    currentPage() {
      this.fetchReports()
    },
    pageSize() {
      this.currentPage = 1
      this.fetchReports()
    }
  },
  mounted() {
    this.fetchReports()
  },
  methods: {
    async fetchReports() {
      this.loading = true
      try {
        const params = {
          status: this.selectedStatus,
          page: this.currentPage - 1,
          size: this.pageSize,
          keyword: this.searchKeyword
        }
        const res = await adminReportService.getReports(params)
        if (res.data) {
          this.reports = res.data.content || []
          this.totalElements = res.data.totalElements || 0
        }
      } catch (e) {
        console.error(e)
        toastError('Lỗi khi tải danh sách báo cáo vi phạm')
      } finally {
        this.loading = false
      }
    },
    handleSearch(keyword) {
      this.searchKeyword = keyword
      this.currentPage = 1
      this.fetchReports()
    },
    async handleResolve(item, deleteContent) {
      const actionText = deleteContent ? 'XÓA NỘI DUNG vi phạm' : 'BÁC BỎ (Bỏ qua) báo cáo';
      const confirmRes = await alertConfirm(
        'Xác nhận',
        `Bạn có chắc chắn muốn ${actionText} này không?`
      );
      if (confirmRes.isConfirmed) {
        this.loading = true
        try {
          const payload = {
            status: deleteContent ? 'RESOLVED' : 'REJECTED',
            deleteContent
          }
          await adminReportService.resolveReport(item.id, payload)
          toastSuccess('Xử lý báo cáo thành công!')
          this.fetchReports()
        } catch (e) {
          console.error(e)
          const errMsg = e.response?.data?.message || 'Có lỗi xảy ra khi xử lý.'
          toastError(errMsg)
        } finally {
          this.loading = false
        }
      }
    },
    viewReportTarget(item) {
      if (!item.threadId) {
        toastError('Không tìm thấy ID bài viết chứa nội dung này')
        return
      }
      
      const routeQuery = item.targetType === 'POST' ? { postId: item.targetId } : {}
      const route = this.$router.resolve({
        name: 'ThreadDetail',
        params: { id: item.threadId },
        query: routeQuery
      })
      window.open(route.href, '_blank')
    },
    formatDate(dateStr) {
      return formatForumDate(dateStr)
    },
    getStatusText(status) {
      switch (status) {
        case 'PENDING': return 'Chờ xử lý'
        case 'RESOLVED': return 'Đã giải quyết'
        case 'REJECTED': return 'Đã bác bỏ'
        default: return status
      }
    },
    getStatusBadgeClass(status) {
      switch (status) {
        case 'PENDING': return 'badge-status-pending'
        case 'RESOLVED': return 'badge-status-resolved'
        case 'REJECTED': return 'badge-status-rejected'
        default: return ''
      }
    }
  }
}
</script>

<style scoped>
.page-content {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.mini-select {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  outline: none;
  background: white;
}

.badge-type, .badge-status {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 500;
  text-align: center;
}

.badge-info {
  background-color: #e3f2fd;
  color: #1e88e5;
}

.badge-warning {
  background-color: #fff3e0;
  color: #fb8c00;
}

.badge-status-pending {
  background-color: #fff3e0;
  color: #fb8c00;
}

.badge-status-resolved {
  background-color: #e8f5e9;
  color: #43a047;
}

.badge-status-rejected {
  background-color: #eceff1;
  color: #78909c;
}

.report-actions {
  display: flex;
  gap: 6px;
  justify-content: center;
  align-items: center;
}

.action-btn-custom {
  border: none;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.8rem;
  cursor: pointer;
  font-weight: 500;
  transition: opacity 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.action-btn-custom:hover {
  opacity: 0.85;
}

.btn-view {
  background-color: #eceff1;
  color: #37474f;
}

.btn-resolve {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.btn-reject {
  background-color: #ffebee;
  color: #c62828;
}

.resolved-by-text {
  font-size: 0.85rem;
  color: #78909c;
  font-style: italic;
}

.filter-item-mini {
  display: flex;
  gap: 10px;
}
</style>
