<template>
  <div class="page-content">
    <div class="card p-4">
      <h2 class="mb-4">Cấu hình Hệ thống</h2>
      
      <div v-if="loading" class="loading-state my-5 text-center">
        <span>Đang tải cấu hình...</span>
      </div>

      <form v-else @submit.prevent="saveSettings">
        <div class="form-group mb-4">
          <label for="thread_edit_limit" class="form-label fw-bold mb-2" style="font-size: 1.05rem;">
            Giới hạn thời gian sửa bài đăng gốc (phút)
          </label>
          <div class="input-group" style="max-width: 300px;">
            <input 
              type="number" 
              id="thread_edit_limit" 
              v-model.number="settings.thread_edit_limit_minutes" 
              class="form-control" 
              :min="noLimitValue" 
              required 
            />
            <span class="input-group-text">phút</span>
          </div>
          <div class="form-text mt-2 text-muted" style="font-size: 0.9rem; line-height: 1.4;">
            Nhập số phút tối đa cho phép tác giả tự chỉnh sửa bài đăng của mình sau khi đăng. <br/>
            * Nhập <strong>{{ noLimitValue }}</strong> nếu muốn cho phép chỉnh sửa <strong>không giới hạn thời gian</strong>.
          </div>
        </div>

        <div class="form-group mb-4">
          <label for="post_edit_limit" class="form-label fw-bold mb-2" style="font-size: 1.05rem;">
            Giới hạn thời gian sửa bình luận/phản hồi (phút)
          </label>
          <div class="input-group" style="max-width: 300px;">
            <input 
              type="number" 
              id="post_edit_limit" 
              v-model.number="settings.post_edit_limit_minutes" 
              class="form-control" 
              :min="noLimitValue" 
              required 
            />
            <span class="input-group-text">phút</span>
          </div>
          <div class="form-text mt-2 text-muted" style="font-size: 0.9rem; line-height: 1.4;">
            Nhập số phút tối đa cho phép tác giả tự chỉnh sửa bình luận/phản hồi của mình sau khi đăng. <br/>
            * Nhập <strong>{{ noLimitValue }}</strong> nếu muốn cho phép chỉnh sửa <strong>không giới hạn thời gian</strong>.
          </div>
        </div>

        <div class="form-group mb-4">
          <label for="conversation_edit_limit" class="form-label fw-bold mb-2" style="font-size: 1.05rem;">
            Giới hạn thời gian sửa nội dung bắt đầu đối thoại (phút)
          </label>
          <div class="input-group" style="max-width: 300px;">
            <input 
              type="number" 
              id="conversation_edit_limit" 
              v-model.number="settings.conversation_edit_limit_minutes" 
              class="form-control" 
              :min="noLimitValue" 
              required 
            />
            <span class="input-group-text">phút</span>
          </div>
          <div class="form-text mt-2 text-muted" style="font-size: 0.9rem; line-height: 1.4;">
            Nhập số phút tối đa cho phép tác giả tự chỉnh sửa nội dung bắt đầu đối thoại của mình sau khi tạo. <br/>
            * Nhập <strong>{{ noLimitValue }}</strong> nếu muốn cho phép chỉnh sửa <strong>không giới hạn thời gian</strong>.
          </div>
        </div>

        <div class="form-group mb-4">
          <label for="conversation_reply_edit_limit" class="form-label fw-bold mb-2" style="font-size: 1.05rem;">
            Giới hạn thời gian sửa phản hồi đối thoại (phút)
          </label>
          <div class="input-group" style="max-width: 300px;">
            <input 
              type="number" 
              id="conversation_reply_edit_limit" 
              v-model.number="settings.conversation_reply_edit_limit_minutes" 
              class="form-control" 
              :min="noLimitValue" 
              required 
            />
            <span class="input-group-text">phút</span>
          </div>
          <div class="form-text mt-2 text-muted" style="font-size: 0.9rem; line-height: 1.4;">
            Nhập số phút tối đa cho phép tác giả tự chỉnh sửa tin nhắn phản hồi đối thoại của mình sau khi gửi. <br/>
            * Nhập <strong>{{ noLimitValue }}</strong> nếu muốn cho phép chỉnh sửa <strong>không giới hạn thời gian</strong>.
          </div>
        </div>

        <hr class="my-4" style="border-top: 1px solid #eee;" />

        <h4 class="mb-3" style="color: #1a507a; font-size: 1.15rem;">Cấu hình hiển thị nút Đăng bài theo màn hình</h4>
        <p class="text-muted mb-4" style="font-size: 0.9rem;">
          Tùy chỉnh bật/tắt hoặc giới hạn quyền hiển thị nút <strong>"Đăng bài..."</strong> trên từng giao diện cụ thể của diễn đàn:
        </p>

        <div class="row g-3 mb-4">
          <div class="col-md-6">
            <div class="card p-3" style="background-color: #fcfcfc; border: 1px solid #e2e8f0;">
              <label class="form-label fw-bold mb-1">Màn hình Trang chủ (Home)</label>
              <div class="form-text text-muted mb-2" style="font-size: 0.8rem;">Đường dẫn: <code>/</code></div>
              <select v-model="settings.post_button_home" class="form-select">
                <option value="ALL">Mọi thành viên đăng nhập (Mặc định)</option>
                <option value="ADMIN_ONLY">Chỉ Quản trị viên (BQT)</option>
                <option value="DISABLED">Ẩn hoàn toàn nút đăng bài</option>
              </select>
            </div>
          </div>

          <div class="col-md-6">
            <div class="card p-3" style="background-color: #fcfcfc; border: 1px solid #e2e8f0;">
              <label class="form-label fw-bold mb-1">Màn hình Mới ra lò (Latest)</label>
              <div class="form-text text-muted mb-2" style="font-size: 0.8rem;">Đường dẫn: <code>/latest</code></div>
              <select v-model="settings.post_button_latest" class="form-select">
                <option value="ALL">Mọi thành viên đăng nhập (Mặc định)</option>
                <option value="ADMIN_ONLY">Chỉ Quản trị viên (BQT)</option>
                <option value="DISABLED">Ẩn hoàn toàn nút đăng bài</option>
              </select>
            </div>
          </div>

          <div class="col-md-6">
            <div class="card p-3" style="background-color: #fcfcfc; border: 1px solid #e2e8f0;">
              <label class="form-label fw-bold mb-1">Màn hình Chú ý (Pinned)</label>
              <div class="form-text text-muted mb-2" style="font-size: 0.8rem;">Đường dẫn: <code>/pinned</code> (Danh sách bài ghim)</div>
              <select v-model="settings.post_button_pinned" class="form-select">
                <option value="ADMIN_ONLY">Chỉ Quản trị viên (BQT - Mặc định)</option>
                <option value="ALL">Mọi thành viên đăng nhập</option>
                <option value="DISABLED">Ẩn hoàn toàn nút đăng bài</option>
              </select>
            </div>
          </div>

          <div class="col-md-6">
            <div class="card p-3" style="background-color: #fcfcfc; border: 1px solid #e2e8f0;">
              <label class="form-label fw-bold mb-1">Màn hình Chuyên mục (Category)</label>
              <div class="form-text text-muted mb-2" style="font-size: 0.8rem;">Đường dẫn: <code>/category/:id</code></div>
              <select v-model="settings.post_button_category" class="form-select">
                <option value="ALL">Mọi thành viên đăng nhập (Mặc định)</option>
                <option value="ADMIN_ONLY">Chỉ Quản trị viên (BQT)</option>
                <option value="DISABLED">Ẩn hoàn toàn nút đăng bài</option>
              </select>
            </div>
          </div>
        </div>

        <hr class="my-4" style="border-top: 1px solid #eee;" />

        <div class="d-flex gap-2">
          <button type="submit" class="btn btn-primary px-4" :disabled="saving">
            {{ saving ? 'Đang lưu...' : 'Lưu cấu hình' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import settingService from '@/shared/services/setting.service'
import { alertSuccess, alertError } from '@/shared/utils/swal'
import { SETTINGS } from '@/shared/utils/constants'

export default {
  name: 'SystemSettingConfig',
  data() {
    return {
      settings: {
        thread_edit_limit_minutes: SETTINGS.DEFAULT_THREAD_EDIT_LIMIT_MINUTES,
        post_edit_limit_minutes: SETTINGS.DEFAULT_POST_EDIT_LIMIT_MINUTES,
        conversation_edit_limit_minutes: SETTINGS.DEFAULT_CONVERSATION_EDIT_LIMIT_MINUTES,
        conversation_reply_edit_limit_minutes: SETTINGS.DEFAULT_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES,
        post_button_home: 'ALL',
        post_button_latest: 'ALL',
        post_button_pinned: 'ADMIN_ONLY',
        post_button_category: 'ALL'
      },
      loading: true,
      saving: false,
      noLimitValue: SETTINGS.NO_LIMIT_VALUE
    }
  },
  async mounted() {
    await this.loadSettings()
  },
  methods: {
    async loadSettings() {
      this.loading = true
      try {
        const res = await settingService.getSettings()
        if (res && res.data) {
          const val = res.data[SETTINGS.THREAD_EDIT_LIMIT_MINUTES_KEY]
          if (val !== undefined) {
            this.settings.thread_edit_limit_minutes = Number(val)
          }
          const postVal = res.data[SETTINGS.POST_EDIT_LIMIT_MINUTES_KEY]
          if (postVal !== undefined) {
            this.settings.post_edit_limit_minutes = Number(postVal)
          }
          const convoVal = res.data[SETTINGS.CONVERSATION_EDIT_LIMIT_MINUTES_KEY]
          if (convoVal !== undefined) {
            this.settings.conversation_edit_limit_minutes = Number(convoVal)
          }
          const convoReplyVal = res.data[SETTINGS.CONVERSATION_REPLY_EDIT_LIMIT_MINUTES_KEY]
          if (convoReplyVal !== undefined) {
            this.settings.conversation_reply_edit_limit_minutes = Number(convoReplyVal)
          }
          if (res.data.post_button_home) this.settings.post_button_home = res.data.post_button_home
          if (res.data.post_button_latest) this.settings.post_button_latest = res.data.post_button_latest
          if (res.data.post_button_pinned) this.settings.post_button_pinned = res.data.post_button_pinned
          if (res.data.post_button_category) this.settings.post_button_category = res.data.post_button_category
        }
      } catch (err) {
        console.error('Không tải được cấu hình hệ thống:', err)
        alertError('Lỗi khi tải cấu hình hệ thống')
      } finally {
        this.loading = false
      }
    },
    async saveSettings() {
      this.saving = true
      try {
        const payload = {
          [SETTINGS.THREAD_EDIT_LIMIT_MINUTES_KEY]: String(this.settings.thread_edit_limit_minutes),
          [SETTINGS.POST_EDIT_LIMIT_MINUTES_KEY]: String(this.settings.post_edit_limit_minutes),
          [SETTINGS.CONVERSATION_EDIT_LIMIT_MINUTES_KEY]: String(this.settings.conversation_edit_limit_minutes),
          [SETTINGS.CONVERSATION_REPLY_EDIT_LIMIT_MINUTES_KEY]: String(this.settings.conversation_reply_edit_limit_minutes),
          post_button_home: this.settings.post_button_home,
          post_button_latest: this.settings.post_button_latest,
          post_button_pinned: this.settings.post_button_pinned,
          post_button_category: this.settings.post_button_category
        }
        await settingService.updateSettings(payload)
        alertSuccess('Lưu cấu hình hệ thống thành công')
      } catch (err) {
        console.error('Không lưu được cấu hình hệ thống:', err)
        alertError('Lỗi khi lưu cấu hình hệ thống')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.page-content {
  padding: 20px;
}
.card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.form-label {
  color: #2c3e50;
}
.btn-primary {
  background-color: #1a507a;
  border-color: #1a507a;
}
.btn-primary:hover {
  background-color: #123856;
  border-color: #123856;
}
.input-group-text {
  background-color: #f8f9fa;
  border-color: #ced4da;
  color: #495057;
}
</style>
