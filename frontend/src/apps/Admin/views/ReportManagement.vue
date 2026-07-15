<template>
  <div class="page-content">
    <!-- Back button and Target info for Detail Mode -->
    <div v-if="isDetailMode" class="detail-header mb-3">
      <button class="btn-back-custom" @click="goBackToList">
        ⬅ Quay lại danh sách nhóm
      </button>
      <div class="target-info-header mt-2">
        <strong>Đang xem báo cáo của:</strong> 
        <span :class="['badge-type', $route.query.targetType === 'THREAD' ? 'badge-info' : 'badge-warning']">
          {{ $route.query.targetType === 'THREAD' ? 'Thread' : 'Bình luận' }}
        </span>
        <span class="target-id-label">ID: {{ $route.query.targetId }}</span>
        <span class="target-summary-text" v-if="reports.length > 0">
          - Tác giả: <strong>{{ reports[0].targetAuthorUsername }}</strong>
        </span>
      </div>
    </div>

    <DataTable
      :title="isDetailMode ? 'Chi tiết Báo cáo Vi phạm' : 'Quản lý Báo cáo Vi phạm'"
      :placeholder="isDetailMode ? 'Tìm kiếm trong chi tiết...' : 'Tìm kiếm lý do, người báo cáo...'"
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

      <!-- Column targetType (Grouped Mode) -->
      <template #item-targetType="{ item }">
        <span :class="['badge-type', item.targetType === 'THREAD' ? 'badge-info' : 'badge-warning']">
          {{ item.targetType === 'THREAD' ? 'Thread' : 'Bình luận' }}
        </span>
      </template>

      <!-- Column createdAt (Detail Mode) -->
      <template #item-createdAt="{ item }">
        {{ formatDate(item.createdAt) }}
      </template>

      <!-- Column latestReportedAt (Grouped Mode) -->
      <template #item-latestReportedAt="{ item }">
        {{ formatDate(item.latestReportedAt) }}
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
          
          <!-- View details button: only in Grouped Mode -->
          <button v-if="!isDetailMode" class="action-btn-custom btn-detail" @click="goToDetails(item)" title="Xem danh sách báo cáo chi tiết">
            📋 Chi tiết
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
      reports: [],
      totalElements: 0,
      pageSize: 10,
      currentPage: 1,
      loading: false,
      selectedStatus: this.$route.query.status || 'PENDING',
      searchKeyword: ''
    }
  },
  computed: {
    isDetailMode() {
      return !!this.$route.query.targetType && !!this.$route.query.targetId
    },
    headers() {
      if (this.isDetailMode) {
        return [
          { text: 'Người báo cáo', value: 'reporterUsername', width: '150px' },
          { text: 'Lý do báo cáo', value: 'reason', width: '350px' },
          { text: 'Thời gian báo cáo', value: 'createdAt', width: '180px' },
          { text: 'Trạng thái', value: 'status', width: '120px' },
          { text: 'Hành động', value: 'actions', width: '180px' }
        ]
      } else {
        return [
          { text: 'Loại', value: 'targetType', width: '100px' },
          { text: 'Nội dung trích dẫn', value: 'targetContentSnippet', width: '250px' },
          { text: 'Tác giả', value: 'targetAuthorUsername', width: '120px' },
          { text: 'Số lượt báo cáo', value: 'reportCount', width: '120px' },
          { text: 'Thời gian gần nhất', value: 'latestReportedAt', width: '160px' },
          { text: 'Trạng thái', value: 'status', width: '120px' },
          { text: 'Hành động', value: 'actions', width: '180px' }
        ]
      }
    }
  },
  watch: {
    currentPage() {
      this.fetchReports()
    },
    pageSize() {
      this.currentPage = 1
      this.fetchReports()
    },
    '$route'(to, from) {
      this.selectedStatus = to.query.status || 'PENDING'
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
        if (this.isDetailMode) {
          const params = {
            targetType: this.$route.query.targetType,
            targetId: this.$route.query.targetId,
            status: this.selectedStatus,
            page: this.currentPage - 1,
            size: this.pageSize
          }
          const res = await adminReportService.getReportDetails(params)
          if (res.data) {
            this.reports = res.data.content || []
            this.totalElements = res.data.totalElements || 0
          }
        } else {
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
            targetType: item.targetType,
            targetId: item.targetId,
            status: deleteContent ? 'RESOLVED' : 'REJECTED',
            deleteContent
          }
          await adminReportService.resolveReportGroup(payload)
          toastSuccess('Xử lý báo cáo thành công!')
          
          if (this.isDetailMode) {
            this.goBackToList()
          } else {
            this.fetchReports()
          }
        } catch (e) {
          console.error(e)
          const errMsg = e.response?.data?.message || 'Có lỗi xảy ra khi xử lý.'
          toastError(errMsg)
        } finally {
          this.loading = false
        }
      }
    },
    goToDetails(item) {
      this.$router.push({
        name: 'AdminReports',
        query: {
          targetType: item.targetType,
          targetId: item.targetId,
          status: this.selectedStatus
        }
      })
    },
    goBackToList() {
      this.$router.push({
        name: 'AdminReports'
      })
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

.detail-header {
  background-color: #f8f9fa;
  padding: 1rem;
  border-radius: 6px;
  border-left: 4px solid var(--primary);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-back-custom {
  align-self: flex-start;
  background-color: #eceff1;
  color: #37474f;
  border: 1px solid #cfd8dc;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-back-custom:hover {
  background-color: #cfd8dc;
}

.target-info-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  flex-wrap: wrap;
}

.target-id-label {
  color: #607d8b;
  font-weight: 600;
}

.target-summary-text {
  color: #37474f;
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

.btn-detail {
  background-color: #e0f2fe;
  color: #0284c7;
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
