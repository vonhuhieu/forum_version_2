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
        conversation_reply_edit_limit_minutes: SETTINGS.DEFAULT_CONVERSATION_REPLY_EDIT_LIMIT_MINUTES
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
          [SETTINGS.CONVERSATION_REPLY_EDIT_LIMIT_MINUTES_KEY]: String(this.settings.conversation_reply_edit_limit_minutes)
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
