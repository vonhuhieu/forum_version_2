<template>
  <div class="page-content">
    <DataTable
      title="Quản lý bài viết diễn đàn"
      placeholder="Tìm kiếm tiêu đề, tác giả, chuyên mục..."
      addButtonLabel="Đăng bài mới"
      :headers="headers"
      :items="displayThreads"
      :totalItems="filteredThreads.length"
      v-model:pageSize="pageSize"
      v-model:currentPage="currentPage"
      :loading="loading"
      @search="handleSearch"
      @add="$router.push({ name: 'AdminThreadCreate' })"
      @edit="editThread"
      @delete="deleteThread"
      @view="viewThread"
      @sort="handleSort"
    >
      <template #extra-filters>
        <div class="filter-item-mini">
          <select v-model="filter.groupId" @change="onGroupChange" class="mini-select">
            <option value="">Tất cả nhóm</option>
            <option v-for="group in categoryGroups" :key="group.id" :value="group.id">{{ group.name }}</option>
          </select>
          <select v-model="filter.categoryId" @change="fetchThreads" class="mini-select" :disabled="!filter.groupId">
            <option value="">{{ filter.groupId ? 'Chọn chuyên mục' : 'Vui lòng chọn nhóm chuyên mục' }}</option>
            <option v-for="cat in filteredCategories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
        </div>
      </template>

      <template #item-title="{ item }">
        <div class="title-cell-wrapper">
          <span v-if="item.label" class="label-tag" :style="{ backgroundColor: item.label.colorCode, color: item.label.textColor, borderColor: item.label.borderColor || 'transparent' }">{{ item.label.name }}</span>
          <strong class="thread-title-text">{{ item.title }}</strong>
          <span v-if="item.pinned" title="Đã ghim" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 6px;">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="icon-pin" style="display: block; pointer-events: none;"><line x1="12" y1="17" x2="12" y2="22"></line><path d="M5 17h14v-1.76a2 2 0 0 0-.44-1.24l-2.78-3.5A2 2 0 0 1 15 9.26V5a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4.26a2 2 0 0 1-.78 1.24l-2.78 3.5a2 2 0 0 0-.44 1.24z"></path></svg>
          </span>
          <span v-if="item.locked" title="Đã khóa" style="display: inline-flex; align-items: center; vertical-align: middle; margin-left: 6px;">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-lock" style="display: block; pointer-events: none;"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
          </span>
        </div>
      </template>

      <template #item-author="{ item }">
        {{ item.author ? (item.author.displayName || item.author.username) : 'Ẩn danh' }}
      </template>

      <template #item-category="{ item }">
        <span class="category-tag">{{ item.category ? item.category.name : 'N/A' }}</span>
      </template>

      <template #item-createdAt="{ item }">
        {{ formatDate(item.createdAt) }}
      </template>
    </DataTable>
  </div>
</template>

<script>
import threadService from '@/apps/Forum/services/thread.service'
import AdminService from '@/apps/Admin/services/admin.service'
import DataTable from '@/shared/components/DataTable.vue'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'ThreadManagement',
  components: { DataTable },
  data() {
    return {
      threads: [],
      categoryGroups: [],
      loading: false,
      filter: { groupId: '', categoryId: '', keyword: '' },
      pageSize: 10,
      currentPage: 1,
      headers: [
        { text: 'Tiêu đề', value: 'title', sortable: true, width: '40%' },
        { text: 'Tác giả', value: 'author.username', sortable: true, width: '15%' },
        { text: 'Chuyên mục', value: 'category.name', sortable: true, width: '15%' },
        { text: 'Ngày đăng', value: 'createdAt', sortable: true, width: '150px' },
        { text: 'Lượt xem', value: 'viewCount', sortable: true, width: '80px' }
      ],
      sortField: '',
      sortOrder: 'asc'
    }
  },
  computed: {
    filteredThreads() {
      let result = this.threads
      
      if (this.filter.keyword) {
        const k = this.filter.keyword.trim().toLowerCase()
        result = result.filter(t => 
          (t.title && t.title.toLowerCase().includes(k)) ||
          (t.author && ((t.author.username && t.author.username.toLowerCase().includes(k)) || (t.author.displayName && t.author.displayName.toLowerCase().includes(k)))) ||
          (t.category && t.category.name && t.category.name.toLowerCase().includes(k))
        )
      }
      
      if (this.sortField) {
        result = [...result].sort((a, b) => {
          let valA = this.getNestedValue(a, this.sortField)
          let valB = this.getNestedValue(b, this.sortField)
          
          if (typeof valA === 'string') valA = valA.toLowerCase()
          if (typeof valB === 'string') valB = valB.toLowerCase()

          if (valA < valB) return this.sortOrder === 'asc' ? -1 : 1
          if (valA > valB) return this.sortOrder === 'asc' ? 1 : -1
          return 0
        })
      }
      
      if (this.filter.groupId && !this.filter.categoryId) {
        const groupCatIds = this.filteredCategories.map(c => c.id)
        result = result.filter(t => t.category && groupCatIds.includes(t.category.id))
      }
      
      return result
    },
    allCategories() {
      if (!this.categoryGroups) return []
      const cats = []
      this.categoryGroups.forEach(group => {
        if (group.categories) {
          group.categories.forEach(cat => {
            cats.push({ ...cat, categoryGroupId: group.id })
          })
        }
      })
      return cats
    },
    filteredCategories() {
      let catsToFormat = [];
      if (!this.filter.groupId) {
        catsToFormat = this.allCategories;
      } else {
        const selectedGroup = this.categoryGroups.find(g => String(g.id) === String(this.filter.groupId));
        catsToFormat = selectedGroup ? selectedGroup.categories : [];
      }
      return this.formatCategoriesHierarchy(catsToFormat);
    },
    displayThreads() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredThreads.slice(start, end)
    }
  },
  mounted() {
    this.fetchCategoryGroups()
    this.fetchCategories()
    this.fetchThreads()
  },
  methods: {
    async fetchCategoryGroups() {
      const response = await AdminService.getCategoryGroups()
      this.categoryGroups = response.data
    },
    async fetchCategories() {
      // We use fetchCategoryGroups instead
    },
    onGroupChange() {
      this.filter.categoryId = ''
      this.fetchThreads()
    },
    formatCategoriesHierarchy(flatCategories) {
      if (!flatCategories || flatCategories.length === 0) return [];
      
      const categoryMap = {};
      const roots = [];
      
      // Clone and initialize map
      flatCategories.forEach(cat => {
        categoryMap[cat.id] = { ...cat, children: [] };
      });
      
      // Build tree
      flatCategories.forEach(cat => {
        if (cat.parentCategoryId && categoryMap[cat.parentCategoryId]) {
          categoryMap[cat.parentCategoryId].children.push(categoryMap[cat.id]);
        } else {
          roots.push(categoryMap[cat.id]);
        }
      });
      
      // Flatten with prefix
      const result = [];
      const flatten = (nodes, prefix = '') => {
        nodes.forEach(node => {
          result.push({
            ...node,
            name: prefix + node.name
          });
          if (node.children && node.children.length > 0) {
            flatten(node.children, prefix + '\u00A0\u00A0\u00A0\u00A0└─ ');
          }
        });
      };
      
      flatten(roots);
      return result;
    },
    async fetchThreads() {
      this.loading = true
      const params = {}
      if (this.filter.categoryId) params.categoryId = this.filter.categoryId
      const response = await threadService.getAll(params)
      this.threads = response.data
      this.loading = false
      this.currentPage = 1 // Reset to first page on fetch
    },
    handleSearch(keyword) {
      this.filter.keyword = keyword
      this.currentPage = 1
    },
    handleSort({ field, order }) {
      this.sortField = field
      this.sortOrder = order
    },
    getNestedValue(obj, path) {
      if (!path) return ''
      return path.split('.').reduce((acc, part) => acc && acc[part], obj)
    },
    editThread(item) {
      this.$router.push({ name: 'AdminThreadEdit', params: { id: item.id } })
    },
    viewThread(item) {
      this.$router.push({ name: 'AdminThreadView', params: { id: item.id } })
    },
    async deleteThread(item) {
      const result = await alertConfirm('Xóa bài viết', `Bạn có chắc chắn muốn xóa bài viết "${item.title}"?`)
      if (result.isConfirmed) {
        try {
          await threadService.delete(item.id)
          toastSuccess('Đã xóa bài viết')
          this.fetchThreads()
        } catch (error) {
          toastError('Lỗi khi xóa bài viết')
        }
      }
    },
    formatDate(dateStr) {
      return new Date(dateStr).toLocaleString('vi-VN')
    }
  }
}
</script>

<style scoped>
.filter-item-mini {
  display: flex;
  align-items: center;
}

.mini-select {
  padding: 0.6rem;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: white;
  outline: none;
  min-width: 200px;
}

.badge-pinned {
  background: #fff3cd;
  color: #856404;
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid #ffeeba;
  white-space: nowrap;
  display: inline-block;
  vertical-align: middle;
  line-height: 1;
}

.category-tag {
  background: #e8f4fd;
  color: #007bff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
}

.label-tag {
  padding: 2px 6px;
  font-size: 0.75rem;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
  border: 1px solid transparent;
  white-space: nowrap;
  vertical-align: middle;
  line-height: 1;
}

.filter-item-mini {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mini-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  color: #333;
  font-size: 14px;
  min-width: 200px;
  outline: none;
  cursor: pointer;
}

.mini-select:focus {
  border-color: var(--primary);
}

.title-cell-wrapper {
  display: block;
  line-height: 1.5;
}

.thread-title-text {
  line-height: 1.2;
}
</style>

