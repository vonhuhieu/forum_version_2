<template>
  <div class="container-fluid py-4 dataset-config-page">
    <div class="container">
      <!-- Loading chuẩn theo Rule 3 -->
      <Loading :visible="loading" text="Đang nạp cấu hình dữ liệu..." />

      <!-- Top Header -->
      <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2" v-if="product">
        <div>
          <router-link :to="LAB_ROUTES.MY_PRODUCTS" class="text-decoration-none small text-muted mb-1 d-inline-block">
            <i class="bi bi-arrow-left me-1"></i> Trở về Sản phẩm của tôi
          </router-link>
          <h1 class="page-title mb-1">
            <i class="bi bi-table text-success me-2"></i>Cấu hình dữ liệu: {{ product.title }}
          </h1>
          <div class="d-flex align-items-center gap-2">
            <span class="badge bg-light text-dark border">
              {{ getTemplateIcon(product.templateType) }} {{ getTemplateName(product.templateType) }}
            </span>
            <span class="text-muted small">Tổng số dòng: <strong>{{ rows.length }}</strong></span>
          </div>
        </div>

        <div class="d-flex gap-2 flex-wrap align-items-center">
          <input
            type="file"
            ref="csvFileInput"
            accept=".csv"
            @change="handleCsvFileSelected"
            style="display: none"
          />
          <button
            class="btn btn-outline-info btn-sm d-flex align-items-center gap-1"
            :disabled="generatingAi"
            @click="handleGenerateAiRows"
            title="Sử dụng AI sinh tự động 5 dòng câu hỏi/dữ liệu mẫu"
          >
            <span v-if="generatingAi" class="spinner-border spinner-border-sm me-1"></span>
            <span v-else>✨</span>
            {{ generatingAi ? 'Đang tạo...' : 'AI sinh 5 dòng' }}
          </button>
          <button
            class="btn btn-outline-secondary btn-sm"
            @click="exportCsv"
            title="Tải toàn bộ dữ liệu bảng về máy dưới dạng CSV"
          >
            <i class="bi bi-download me-1"></i> Xuất CSV
          </button>
          <button
            class="btn btn-outline-primary btn-sm"
            @click="triggerImportCsv"
            title="Đọc dữ liệu từ file CSV để nạp nhanh"
          >
            <i class="bi bi-upload me-1"></i> Nhập CSV
          </button>
          <a :href="`${LAB_ROUTES.PLAYER_PREFIX}${product.publicId}`" target="_blank" class="btn btn-outline-primary btn-sm">
            <i class="bi bi-box-arrow-up-right me-1"></i> Chơi thử
          </a>
          <button class="btn btn-success btn-sm" @click="openAddRowModal">
            <i class="bi bi-plus-lg me-1"></i> Thêm dòng
          </button>
        </div>
      </div>

      <!-- Bảng dữ liệu động -->
      <div class="card shadow-sm border-0 rounded-3 mb-4" v-if="product">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th width="60px" class="text-center">STT</th>
                  <th
                    v-for="col in product.columns"
                    :key="col.code"
                    :class="`text-${col.align || 'left'}`"
                  >
                    {{ col.name }} <code class="small text-muted">({{ col.code }})</code>
                  </th>
                  <th width="120px" class="text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in rows" :key="row.id || idx">
                  <td class="text-center fw-bold text-muted">{{ idx + 1 }}</td>
                  <td
                    v-for="col in product.columns"
                    :key="col.code"
                    :class="`text-${col.align || 'left'}`"
                  >
                    <!-- Render đặc biệt nếu là cột màu -->
                    <span
                      v-if="col.code === 'color' && getRowValue(row, col.code)"
                      class="color-pill"
                      :style="{ backgroundColor: getRowValue(row, col.code) }"
                    >
                      {{ getRowValue(row, col.code) }}
                    </span>
                    <span v-else>
                      {{ getRowValue(row, col.code) !== '' ? getRowValue(row, col.code) : '-' }}
                    </span>
                  </td>
                  <td class="text-center">
                    <button class="btn btn-sm btn-light text-primary me-1" @click="openEditRowModal(row)" title="Sửa dòng">
                      ✏️
                    </button>
                    <button class="btn btn-sm btn-light text-danger" @click="handleDeleteRow(row)" title="Xoá dòng">
                      🗑️
                    </button>
                  </td>
                </tr>

                <tr v-if="rows.length === 0">
                  <td :colspan="(product.columns?.length || 0) + 2" class="text-center py-5 text-muted">
                    Chưa có dòng dữ liệu nào. Nhấn <strong>"Thêm dòng dữ liệu"</strong> để bắt đầu!
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Thêm / Sửa Dòng dữ liệu -->
    <div v-if="showRowModal" class="modal-backdrop-custom" @click="closeRowModal">
      <div class="modal-card-custom" @click.stop>
        <div class="modal-header-custom">
          <h5>{{ isEditingRow ? 'Sửa dòng dữ liệu' : 'Thêm dòng dữ liệu mới' }}</h5>
          <button class="btn-close-custom" @click="closeRowModal">&times;</button>
        </div>
        <div class="modal-body-custom">
          <div v-for="col in product.columns" :key="col.code" class="mb-3">
            <label class="form-label fw-bold">
              {{ col.name }}
              <span v-if="col.required" class="text-danger">*</span>
            </label>

            <!-- Input số nếu type = number -->
            <input
              v-if="col.type === 'number'"
              type="number"
              class="form-control"
              v-model.number="rowForm[col.code]"
              :placeholder="`Nhập ${col.name}...`"
            />
            <!-- Input màu nếu code = color -->
            <div v-else-if="col.code === 'color'" class="d-flex gap-2">
              <input type="color" class="form-control form-control-color" v-model="rowForm[col.code]" />
              <input type="text" class="form-control" v-model="rowForm[col.code]" placeholder="#3b82f6" />
            </div>
            <!-- Textarea nếu là câu hỏi, nội dung dài -->
            <textarea
              v-else-if="col.code === 'question' || col.code === 'front' || col.code === 'back'"
              class="form-control"
              rows="2"
              v-model="rowForm[col.code]"
              :placeholder="`Nhập ${col.name}...`"
            ></textarea>
            <!-- Text input bình thường -->
            <input
              v-else
              type="text"
              class="form-control"
              v-model="rowForm[col.code]"
              :placeholder="`Nhập ${col.name}...`"
            />
          </div>
        </div>
        <div class="modal-footer-custom">
          <button class="btn btn-secondary btn-sm" @click="closeRowModal">Hủy</button>
          <button class="btn btn-success btn-sm" :disabled="savingRow" @click="submitRow">
            {{ savingRow ? 'Đang lưu...' : (isEditingRow ? 'Lưu thay đổi' : 'Thêm dòng') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Loading from '@/shared/components/Loading.vue'
import { labService } from '../services/lab.service'
import { LAB_TEMPLATE_META, LAB_ROUTES } from '@/shared/utils/constants'
import { alertConfirm, alertError, alertSuccess, toastSuccess } from '@/shared/utils/swal'

export default {
  name: 'ProductDatasetConfigView',
  components: { Loading },
  data() {
    return {
      LAB_ROUTES,
      product: null,
      rows: [],
      loading: true,
      generatingAi: false,
      showRowModal: false,
      isEditingRow: false,
      editingRowId: null,
      savingRow: false,
      rowForm: {}
    }
  },
  async mounted() {
    await this.fetchData()
  },
  methods: {
    getRowValue(row, colCode) {
      if (!row || !row.data) return ''
      return row.data[colCode] !== undefined ? row.data[colCode] : ''
    },
    getTemplateIcon(type) {
      return LAB_TEMPLATE_META[type]?.icon || '📦'
    },
    getTemplateName(type) {
      return LAB_TEMPLATE_META[type]?.name || type
    },
    async fetchData() {
      const publicId = this.$route.params.publicId
      if (!publicId) return
      this.loading = true
      try {
        const [prodRes, rowsRes] = await Promise.all([
          labService.getPublicProduct(publicId),
          labService.getPublicProductRows(publicId)
        ])
        this.product = prodRes.data
        this.rows = rowsRes.data || []
      } catch (err) {
        alertError('Lỗi tải dữ liệu: ' + (err.response?.data?.message || err.message))
      } finally {
        this.loading = false
      }
    },
    openAddRowModal() {
      this.isEditingRow = false
      this.editingRowId = null
      this.rowForm = {}
      // Điền giá trị mặc định cho từng cột
      this.product.columns.forEach(col => {
        this.rowForm[col.code] = col.type === 'number' ? 1 : (col.code === 'color' ? '#3b82f6' : '')
      })
      this.showRowModal = true
    },
    openEditRowModal(row) {
      this.isEditingRow = true
      this.editingRowId = row.id
      this.rowForm = { ...(row.data || {}) }
      this.showRowModal = true
    },
    closeRowModal() {
      this.showRowModal = false
    },
    async submitRow() {
      // Validate cột bắt buộc
      for (const col of this.product.columns) {
        if (col.required && (!this.rowForm[col.code] || String(this.rowForm[col.code]).trim() === '')) {
          alertError(`Cột "${col.name}" là bắt buộc, không được để trống!`)
          return
        }
      }

      this.savingRow = true
      try {
        if (this.isEditingRow) {
          await labService.updateDatasetRow(this.product.publicId, this.editingRowId, this.rowForm)
          alertSuccess('Đã cập nhật dòng dữ liệu thành công!')
        } else {
          await labService.addDatasetRow(this.product.publicId, this.rowForm)
          alertSuccess('Đã thêm dòng dữ liệu mới!')
        }
        this.closeRowModal()
        await this.fetchData()
      } catch (err) {
        alertError(err.response?.data?.message || 'Lỗi khi lưu dữ liệu')
      } finally {
        this.savingRow = false
      }
    },
    async handleDeleteRow(row) {
      const confirm = await alertConfirm('Xác nhận xoá dòng', 'Bạn có chắc chắn muốn xoá dòng dữ liệu này?')
      if (!confirm.isConfirmed) return

      try {
        await labService.deleteDatasetRow(this.product.publicId, row.id)
        alertSuccess('Đã xoá dòng dữ liệu thành công!')
        await this.fetchData()
      } catch (err) {
        alertError(err.response?.data?.message || 'Không thể xoá dòng dữ liệu')
      }
    },
    exportCsv() {
      if (!this.product || !this.product.columns) return
      const cols = this.product.columns
      const headerRow = cols.map(c => `"${c.name}"`).join(',')
      const dataRows = this.rows.map(row => {
        return cols.map(c => {
          const raw = this.getRowValue(row, c.code)
          const sanitized = (raw !== undefined && raw !== null ? String(raw) : '').replace(/"/g, '""')
          return `"${sanitized}"`
        }).join(',')
      })
      const csvContent = '\uFEFF' + [headerRow, ...dataRows].join('\r\n')
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.setAttribute('href', url)
      link.setAttribute('download', `${this.product.title || 'dataset'}_${new Date().toISOString().slice(0, 10)}.csv`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      toastSuccess('Đã xuất file CSV thành công!')
    },
    triggerImportCsv() {
      if (this.$refs.csvFileInput) {
        this.$refs.csvFileInput.click()
      }
    },
    parseCsv(text) {
      const lines = text.split(/\r?\n/).filter(line => line.trim() !== '')
      if (lines.length < 2) return []

      const parseLine = (line) => {
        const result = []
        let current = ''
        let inQuotes = false
        for (let i = 0; i < line.length; i++) {
          const char = line[i]
          if (char === '"') {
            if (inQuotes && line[i + 1] === '"') {
              current += '"'
              i++
            } else {
              inQuotes = !inQuotes
            }
          } else if (char === ',' && !inQuotes) {
            result.push(current.trim())
            current = ''
          } else {
            current += char
          }
        }
        result.push(current.trim())
        return result
      }

      const rawHeaders = parseLine(lines[0])
      const headerMap = {}
      rawHeaders.forEach((h, idx) => {
        const cleanH = h.replace(/^"|"$/g, '').trim().toLowerCase()
        const col = this.product.columns.find(
          c => c.code.toLowerCase() === cleanH || c.name.toLowerCase() === cleanH
        )
        if (col) {
          headerMap[idx] = col.code
        }
      })

      const rows = []
      for (let i = 1; i < lines.length; i++) {
        const values = parseLine(lines[i])
        const rowData = {}
        let hasValue = false
        Object.keys(headerMap).forEach(idx => {
          const colCode = headerMap[idx]
          const val = values[idx] !== undefined ? values[idx].replace(/^"|"$/g, '').trim() : ''
          if (val !== '') hasValue = true
          rowData[colCode] = val
        })
        if (hasValue) rows.push(rowData)
      }
      return rows
    },
    async handleCsvFileSelected(event) {
      const file = event.target.files?.[0]
      if (!file) return
      event.target.value = ''
      const reader = new FileReader()
      reader.onload = async (e) => {
        try {
          const text = e.target.result
          const parsedRows = this.parseCsv(text)
          if (parsedRows.length === 0) {
            alertError('Không tìm thấy dữ liệu hợp lệ trong file CSV! Vui lòng kiểm tra lại tiêu đề các cột.')
            return
          }
          this.loading = true
          await labService.addRowsBatch(this.product.publicId, parsedRows)
          alertSuccess(`Đã nhập thành công ${parsedRows.length} dòng dữ liệu từ file CSV!`)
          await this.fetchData()
        } catch (err) {
          alertError(err.response?.data?.message || 'Lỗi khi xử lý file CSV')
        } finally {
          this.loading = false
        }
      }
      reader.readAsText(file, 'UTF-8')
    },
    async handleGenerateAiRows() {
      this.generatingAi = true
      try {
        await labService.generateAiRows(this.product.publicId, 5)
        toastSuccess('✨ AI đã sinh thêm 5 dòng dữ liệu mẫu!')
        await this.fetchData()
      } catch (err) {
        alertError(err.response?.data?.message || 'Lỗi khi gọi AI sinh dòng dữ liệu')
      } finally {
        this.generatingAi = false
      }
    }
  }
}
</script>

<style scoped>
.dataset-config-page {
  background: var(--bg-body, #eceef1);
  min-height: calc(100vh - 120px);
}

.page-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #1a507a;
}

.color-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.4);
}

/* Modal Styling */
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  padding: 1rem;
}

.modal-card-custom {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 520px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.modal-header-custom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e2e8f0;
}

.btn-close-custom {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  color: #64748b;
  cursor: pointer;
}

.modal-body-custom {
  padding: 1.25rem;
  overflow-y: auto;
}

.modal-footer-custom {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 1rem 1.25rem;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}
</style>
