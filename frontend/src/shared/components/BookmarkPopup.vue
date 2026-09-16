<template>
  <!-- Khi có anchorEl: Dùng Teleport to body để định vị tuyệt đối theo nút bấm mà không bị overflow/clipping -->
  <teleport to="body" v-if="show && anchorEl">
    <!-- Overlay trong suốt để bắt outside-click mà KHÔNG làm tối/mờ trang web như hình 1 -->
    <div class="bookmark-backdrop-transparent" @click.stop="$emit('close')"></div>

    <!-- Popup container định vị ngay tại button bookmark -->
    <div
      class="bookmark-anchor-popup"
      :class="{ 'display-below': displayBelow }"
      :style="popupStyle"
      @click.stop
    >
      <!-- Mũi tên chỉ vào nút bookmark (viền và nền) -->
      <div class="popup-arrow-border" :style="arrowStyle"></div>
      <div class="popup-arrow-fill" :style="arrowStyle"></div>

      <!-- Nội dung popup -->
      <div class="bookmark-content">
        <div class="bookmark-header">
          {{ headerTitle }}
        </div>

        <div class="bookmark-body">
          <div class="bookmark-field-group">
            <label class="bookmark-field-label">Nội dung: <span class="optional-text">Optional</span></label>
            <input
              type="text"
              v-model="form.note"
              class="bookmark-input"
              placeholder=""
              @keydown.enter.prevent="handleSave"
            />
          </div>

          <div class="bookmark-field-group">
            <label class="bookmark-field-label">Labels: <span class="optional-text">Optional</span></label>
            <div class="bookmark-labels-combobox" ref="labelsCombobox" @click="focusLabelInput">
              <!-- Selected labels tags -->
              <div class="bookmark-selected-tags" v-if="form.labels.length > 0">
                <span 
                  v-for="(label, idx) in form.labels" 
                  :key="idx" 
                  class="bookmark-label-tag"
                >
                  {{ label }}
                  <span class="bookmark-tag-remove" @click.stop="removeLabel(idx)">&times;</span>
                </span>
              </div>
              <!-- Input -->
              <input
                type="text"
                v-model="labelInput"
                class="bookmark-label-input"
                @focus="onLabelFocus"
                @input="onLabelInput"
                @keydown.down.prevent="navigateLabelDropdown('down')"
                @keydown.up.prevent="navigateLabelDropdown('up')"
                @keydown.enter.prevent="confirmLabelSelection"
                @keydown.esc="labelDropdownOpen = false"
                ref="labelInputRef"
                placeholder=""
              />
              <!-- Dropdown -->
              <div class="bookmark-label-dropdown" v-if="labelDropdownOpen && filteredLabelOptions.length > 0">
                <div
                  v-for="(option, idx) in filteredLabelOptions"
                  :key="idx"
                  :class="['bookmark-label-option', { 'active': idx === selectedLabelIndex }]"
                  @click.stop="selectLabelOption(option)"
                  @mouseenter="selectedLabelIndex = idx"
                >
                  {{ option }}
                </div>
              </div>
            </div>
            <div class="bookmark-label-hint">Multiple labels may be separated by commas.</div>
          </div>
        </div>

        <!-- Footer với 2 nút xanh dương bên trái chuẩn theo hình 1 -->
        <div class="bookmark-footer">
          <button class="btn-bookmark-action btn-save" @click="handleSave" :disabled="saving">
            <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg>
            Lưu
          </button>
          <button class="btn-bookmark-action btn-delete" @click="handleDelete" :disabled="saving">
            <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            Xóa
          </button>
        </div>
      </div>
    </div>
  </teleport>

  <!-- Fallback khi không có anchorEl (dùng cho các màn hình khác như Header hoặc Bookmark List) -->
  <teleport to="body" v-else-if="show">
    <div class="bookmark-modal-backdrop" @click.self="$emit('close')">
      <div class="bookmark-anchor-popup bookmark-modal-box" @click.stop>
        <div class="bookmark-content">
          <div class="bookmark-header">
            {{ headerTitle }}
          </div>

          <div class="bookmark-body">
            <div class="bookmark-field-group">
              <label class="bookmark-field-label">Nội dung: <span class="optional-text">Optional</span></label>
              <input
                type="text"
                v-model="form.note"
                class="bookmark-input"
                placeholder=""
                @keydown.enter.prevent="handleSave"
              />
            </div>

            <div class="bookmark-field-group">
              <label class="bookmark-field-label">Labels: <span class="optional-text">Optional</span></label>
              <div class="bookmark-labels-combobox" ref="labelsCombobox" @click="focusLabelInput">
                <div class="bookmark-selected-tags" v-if="form.labels.length > 0">
                  <span 
                    v-for="(label, idx) in form.labels" 
                    :key="idx" 
                    class="bookmark-label-tag"
                  >
                    {{ label }}
                    <span class="bookmark-tag-remove" @click.stop="removeLabel(idx)">&times;</span>
                  </span>
                </div>
                <input
                  type="text"
                  v-model="labelInput"
                  class="bookmark-label-input"
                  @focus="onLabelFocus"
                  @input="onLabelInput"
                  @keydown.down.prevent="navigateLabelDropdown('down')"
                  @keydown.up.prevent="navigateLabelDropdown('up')"
                  @keydown.enter.prevent="confirmLabelSelection"
                  @keydown.esc="labelDropdownOpen = false"
                  ref="labelInputRef"
                  placeholder=""
                />
                <div class="bookmark-label-dropdown" v-if="labelDropdownOpen && filteredLabelOptions.length > 0">
                  <div
                    v-for="(option, idx) in filteredLabelOptions"
                    :key="idx"
                    :class="['bookmark-label-option', { 'active': idx === selectedLabelIndex }]"
                    @click.stop="selectLabelOption(option)"
                    @mouseenter="selectedLabelIndex = idx"
                  >
                    {{ option }}
                  </div>
                </div>
              </div>
              <div class="bookmark-label-hint">Multiple labels may be separated by commas.</div>
            </div>
          </div>

          <div class="bookmark-footer">
            <button class="btn-bookmark-action btn-save" @click="handleSave" :disabled="saving">
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg>
              Lưu
            </button>
            <button class="btn-bookmark-action btn-delete" @click="handleDelete" :disabled="saving">
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
              Xóa
            </button>
          </div>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script>
import bookmarkService from '@/apps/Forum/services/bookmark.service'
import { toastSuccess, alertConfirm } from '@/shared/utils/swal'

export default {
  name: 'BookmarkPopup',
  props: {
    show: { type: Boolean, default: false },
    mode: { type: String, default: 'create' }, // 'create' | 'edit'
    bookmarkId: { type: Number, default: null },
    threadId: { type: Number, default: null },
    postId: { type: Number, default: null },
    initialNote: { type: String, default: '' },
    initialLabels: { type: Array, default: () => [] },
    anchorEl: { type: [Object, null], default: null }
  },
  emits: ['close', 'saved', 'deleted'],
  data() {
    return {
      form: {
        note: '',
        labels: []
      },
      labelInput: '',
      allLabels: [],
      labelDropdownOpen: false,
      selectedLabelIndex: -1,
      saving: false,
      displayBelow: false,
      popupStyle: {},
      arrowStyle: {}
    }
  },
  computed: {
    headerTitle() {
      return this.mode === 'edit' ? 'Bookmark added' : 'Bookmark added'
    },
    filteredLabelOptions() {
      const keyword = this.labelInput.trim().toLowerCase()
      let options = this.allLabels.filter(l => !this.form.labels.includes(l))
      if (keyword) {
        options = options.filter(l => l.toLowerCase().includes(keyword))
        if (options.length === 0 && keyword && !this.form.labels.includes(this.labelInput.trim())) {
          options = [this.labelInput.trim()]
        }
      }
      return options
    }
  },
  watch: {
    show(val) {
      if (val) {
        this.form.note = this.initialNote || ''
        this.form.labels = this.initialLabels ? [...this.initialLabels] : []
        this.labelInput = ''
        this.labelDropdownOpen = false
        this.selectedLabelIndex = -1
        this.fetchLabels()
        this.$nextTick(() => {
          this.computePosition()
        })
      }
    },
    anchorEl() {
      if (this.show) {
        this.$nextTick(() => {
          this.computePosition()
        })
      }
    }
  },
  mounted() {
    window.addEventListener('scroll', this.handleScrollOrResize, { passive: true })
    window.addEventListener('resize', this.handleScrollOrResize, { passive: true })
    document.addEventListener('click', this._handleClickOutside = (e) => {
      if (this.$refs.labelsCombobox && !this.$refs.labelsCombobox.contains(e.target)) {
        this.labelDropdownOpen = false
      }
    })
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScrollOrResize)
    window.removeEventListener('resize', this.handleScrollOrResize)
    document.removeEventListener('click', this._handleClickOutside)
  },
  methods: {
    handleScrollOrResize() {
      if (this.show && this.anchorEl) {
        this.computePosition()
      }
    },
    computePosition() {
      if (!this.anchorEl) {
        this.popupStyle = {}
        this.arrowStyle = {}
        this.displayBelow = false
        return
      }

      const rect = this.anchorEl.getBoundingClientRect()
      const viewportWidth = window.innerWidth
      const viewportHeight = window.innerHeight
      const popupWidth = 320

      // Nếu khoảng cách phía trên nút không đủ (>240px) thì hiển thị bên dưới, ngược lại ưu tiên hiển thị phía trên như hình 1
      this.displayBelow = rect.top < 240

      const anchorCenterX = rect.left + rect.width / 2

      // Tính toạ độ lề trái sao cho mũi tên chỉ thẳng vào nút bookmark và nút bookmark nằm gần góc phải (cách góc phải ~ 26px)
      let popupLeft = anchorCenterX - popupWidth + 26

      // Giới hạn để popup không bị tràn mép màn hình
      if (popupLeft < 10) {
        popupLeft = 10
      } else if (popupLeft + popupWidth > viewportWidth - 10) {
        popupLeft = viewportWidth - 10 - popupWidth
      }

      const styles = {
        position: 'fixed',
        left: `${popupLeft}px`,
        width: `${popupWidth}px`,
        zIndex: 10001
      }

      if (this.displayBelow) {
        styles.top = `${rect.bottom + 8}px`
        styles.bottom = 'auto'
      } else {
        styles.bottom = `${viewportHeight - rect.top + 8}px`
        styles.top = 'auto'
      }

      this.popupStyle = styles
      this.arrowStyle = {
        left: `${anchorCenterX - popupLeft}px`
      }
    },
    async fetchLabels() {
      try {
        const res = await bookmarkService.getLabels()
        this.allLabels = res.data || []
      } catch (e) {
        console.error('Error fetching bookmark labels:', e)
        this.allLabels = []
      }
    },
    focusLabelInput() {
      this.$refs.labelInputRef?.focus()
    },
    onLabelFocus() {
      this.labelDropdownOpen = true
      this.selectedLabelIndex = -1
    },
    onLabelInput() {
      this.labelDropdownOpen = true
      this.selectedLabelIndex = this.filteredLabelOptions.length > 0 ? 0 : -1
      if (this.labelInput.includes(',')) {
        const parts = this.labelInput.split(',')
        for (const part of parts) {
          const trimmed = part.trim()
          if (trimmed && !this.form.labels.includes(trimmed)) {
            this.form.labels.push(trimmed)
          }
        }
        this.labelInput = ''
      }
    },
    navigateLabelDropdown(direction) {
      const options = this.filteredLabelOptions
      if (options.length === 0) return
      if (direction === 'down') {
        this.selectedLabelIndex = (this.selectedLabelIndex + 1) % options.length
      } else {
        this.selectedLabelIndex = this.selectedLabelIndex <= 0 ? options.length - 1 : this.selectedLabelIndex - 1
      }
    },
    confirmLabelSelection() {
      const options = this.filteredLabelOptions
      if (this.selectedLabelIndex >= 0 && this.selectedLabelIndex < options.length) {
        this.selectLabelOption(options[this.selectedLabelIndex])
      } else if (this.labelInput.trim()) {
        const val = this.labelInput.trim()
        if (!this.form.labels.includes(val)) {
          this.form.labels.push(val)
        }
        this.labelInput = ''
        this.labelDropdownOpen = false
      }
    },
    selectLabelOption(option) {
      if (!this.form.labels.includes(option)) {
        this.form.labels.push(option)
      }
      this.labelInput = ''
      this.labelDropdownOpen = false
      this.selectedLabelIndex = -1
      this.$nextTick(() => {
        this.$refs.labelInputRef?.focus()
      })
    },
    removeLabel(idx) {
      this.form.labels.splice(idx, 1)
    },
    async handleSave() {
      this.saving = true
      try {
        if (this.bookmarkId) {
          await bookmarkService.update(this.bookmarkId, {
            threadId: this.threadId,
            postId: this.postId,
            note: this.form.note,
            labels: this.form.labels
          })
          toastSuccess('Lưu bookmark thành công')
        } else {
          await bookmarkService.create({
            threadId: this.threadId,
            postId: this.postId,
            note: this.form.note,
            labels: this.form.labels
          })
          toastSuccess('Lưu bookmark thành công')
        }
        this.$emit('saved')
        this.$emit('close')
      } catch (e) {
        console.error('Error saving bookmark:', e)
      } finally {
        this.saving = false
      }
    },
    async handleDelete() {
      if (!this.bookmarkId) {
        this.$emit('close')
        return
      }
      const result = await alertConfirm('Xóa bookmark', 'Bạn có chắc chắn muốn xóa bookmark này?')
      if (!result.isConfirmed) return

      this.saving = true
      try {
        await bookmarkService.delete(this.bookmarkId)
        toastSuccess('Xóa bookmark thành công')
        this.$emit('deleted')
        this.$emit('close')
      } catch (e) {
        console.error('Error deleting bookmark:', e)
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
/* Backdrop hoàn toàn trong suốt cho trường hợp neo vào button (không làm tối hay mờ trang web) */
.bookmark-backdrop-transparent {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: transparent;
  z-index: 10000;
}

/* Backdrop nhẹ cho modal fallback nếu không có anchor element */
.bookmark-modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.2);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bookmark-modal-box {
  position: relative !important;
  left: auto !important;
  top: auto !important;
  bottom: auto !important;
  transform: none !important;
}

/* Card Popup chính */
.bookmark-anchor-popup {
  background: #ffffff;
  border: 1px solid #d2d6dc;
  border-radius: 4px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12);
  box-sizing: border-box;
  text-align: left;
  font-family: Arial, sans-serif;
}

.bookmark-content {
  padding: 14px 16px 16px;
}

/* Mũi tên khi popup hiển thị PHÍA TRÊN nút bookmark (chĩa xuống dưới) */
.popup-arrow-border {
  position: absolute;
  top: 100%;
  transform: translateX(-50%);
  border-width: 8px;
  border-style: solid;
  border-color: #d2d6dc transparent transparent transparent;
  z-index: 10002;
  margin-top: 0;
}

.popup-arrow-fill {
  position: absolute;
  top: 100%;
  transform: translateX(-50%);
  border-width: 7px;
  border-style: solid;
  border-color: #ffffff transparent transparent transparent;
  margin-top: -2px;
  z-index: 10003;
}

/* Mũi tên khi popup hiển thị PHÍA DƯỚI nút bookmark (chĩa lên trên) */
.bookmark-anchor-popup.display-below .popup-arrow-border {
  top: auto;
  bottom: 100%;
  border-color: transparent transparent #d2d6dc transparent;
  margin-top: 0;
  margin-bottom: 0;
}

.bookmark-anchor-popup.display-below .popup-arrow-fill {
  top: auto;
  bottom: 100%;
  border-color: transparent transparent #ffffff transparent;
  margin-top: 0;
  margin-bottom: -2px;
}

/* Header tiêu đề chữ xanh dịu mắt giống XenForo ở hình 1 */
.bookmark-header {
  font-size: 1.05rem;
  font-weight: 500;
  color: #2b96cc;
  margin-bottom: 12px;
  letter-spacing: -0.01em;
}

.bookmark-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bookmark-field-group {
  display: flex;
  flex-direction: column;
}

.bookmark-field-label {
  font-weight: 600;
  font-size: 0.88rem;
  color: #222;
  margin-bottom: 4px;
}

.optional-text {
  font-weight: normal;
  color: #8c8c8c;
  font-size: 0.8rem;
}

/* Ô input nhập nội dung */
.bookmark-input {
  width: 100%;
  border: 1px solid #d2d6dc;
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 0.85rem;
  background-color: #ffffff;
  color: #333;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
  height: 34px;
}

.bookmark-input:focus {
  border-color: #3b82f6;
}

/* Hộp labels combobox */
.bookmark-labels-combobox {
  position: relative;
  border: 1px solid #d2d6dc;
  border-radius: 4px;
  padding: 3px 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
  background: #ffffff;
  min-height: 34px;
  cursor: text;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.bookmark-labels-combobox:focus-within {
  border-color: #3b82f6;
}

.bookmark-selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.bookmark-label-tag {
  background: #e8f0f8;
  color: #1a507a;
  padding: 2px 7px;
  border-radius: 3px;
  font-size: 0.8rem;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.bookmark-tag-remove {
  cursor: pointer;
  font-size: 0.95rem;
  line-height: 1;
  color: #888;
  font-weight: bold;
}

.bookmark-tag-remove:hover {
  color: #d13838;
}

.bookmark-label-input {
  border: none;
  outline: none;
  flex: 1;
  min-width: 60px;
  font-size: 0.85rem;
  padding: 4px 2px;
  font-family: inherit;
  background: transparent;
}

.bookmark-label-dropdown {
  position: absolute;
  top: calc(100% + 2px);
  left: -1px;
  right: -1px;
  background: #ffffff;
  border: 1px solid #d2d6dc;
  border-radius: 4px;
  max-height: 150px;
  overflow-y: auto;
  z-index: 10010;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.bookmark-label-option {
  padding: 6px 10px;
  cursor: pointer;
  font-size: 0.85rem;
  color: #333;
}

.bookmark-label-option:hover,
.bookmark-label-option.active {
  background: #e8f0f8;
  color: #1a507a;
}

.bookmark-label-hint {
  font-size: 0.78rem;
  color: #7f8c8d;
  margin-top: 5px;
}

/* Footer nút Lưu & Xóa nằm bên TRÁI chuẩn theo hình 1 */
.bookmark-footer {
  margin-top: 14px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 8px;
}

/* Cả nút Lưu và Xóa đều dùng màu xanh dương XenForo theo mẫu hình 1 */
.btn-bookmark-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #2b82d9;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  font-weight: 500;
  transition: background 0.15s ease;
}

.btn-bookmark-action:hover {
  background: #1d70b8;
}

.btn-bookmark-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
