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

        <!-- Khu vực quản lý bộ lọc từ cấm & kiểm duyệt tự động -->
        <div class="d-flex justify-content-between align-items-center mb-3">
          <div>
            <h4 class="mb-1" style="color: #1a507a; font-size: 1.15rem;">
              🛡️ Quản lý Bộ Lọc Từ Cấm & Kiểm Duyệt Tự Động (Profanity Filter)
            </h4>
            <p class="text-muted mb-0" style="font-size: 0.9rem;">
              Hệ thống lọc theo thuật toán đa chuỗi Aho-Corasick. Từ khóa cấm mặc định được nạp từ file JSON và Admin có toàn quyền cấu hình bổ sung động.
            </p>
          </div>
          <div class="form-check form-switch fs-5">
            <input 
              class="form-check-input" 
              type="checkbox" 
              id="profanity_filter_switch"
              v-model="moderation.enabled"
            />
            <label class="form-check-label fs-6 fw-bold" for="profanity_filter_switch">
              {{ moderation.enabled ? 'Đang BẬT' : 'Đang TẮT' }}
            </label>
          </div>
        </div>

        <!-- Banner trạng thái bộ lọc -->
        <div class="card p-3 mb-4" :style="moderation.enabled ? 'background: #f0fdf4; border-color: #bbf7d0;' : 'background: #fef2f2; border-color: #fecaca;'">
          <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
            <div>
              <span class="badge" :class="moderation.enabled ? 'bg-success' : 'bg-danger'" style="font-size: 0.85rem;">
                {{ moderation.enabled ? 'Hoạt động bình thường' : 'Đang tạm dừng' }}
              </span>
              <span class="ms-2 text-muted" style="font-size: 0.9rem;">
                Hiện đang nạp tổng cộng <strong>{{ moderation.totalKeywordsLoaded }}</strong> từ khóa cấm trong bộ nhớ đệm Aho-Corasick.
              </span>
            </div>
            <button 
              type="button" 
              class="btn btn-sm btn-outline-secondary"
              @click="showDefaultRules = !showDefaultRules"
            >
              {{ showDefaultRules ? 'Ẩn từ khóa mặc định' : 'Xem từ khóa mặc định (JSON)' }}
            </button>
          </div>

          <!-- Danh sách từ khóa mặc định từ JSON -->
          <div v-if="showDefaultRules" class="mt-3 pt-3 border-top">
            <p class="text-muted mb-2" style="font-size: 0.85rem;">
              <em>Từ khóa mặc định từ tệp <code>backend/src/main/resources/moderation/profanity-rules.json</code>:</em>
            </p>
            <div class="row g-2">
              <div class="col-md-6">
                <div class="p-2 border rounded bg-white">
                  <div class="fw-bold text-danger mb-1" style="font-size: 0.85rem;">
                    🛑 Cấp 1: Nghiêm trọng / Pháp luật (CRITICAL - Chặn ngay)
                  </div>
                  <div class="d-flex flex-wrap gap-1">
                    <span v-for="w in moderation.defaultRules.critical" :key="w" class="badge bg-light text-danger border">
                      {{ w }}
                    </span>
                  </div>
                </div>
              </div>
              <div class="col-md-6">
                <div class="p-2 border rounded bg-white">
                  <div class="fw-bold mb-1" style="font-size: 0.85rem; color: #b45309;">
                    🔞 Cấp 2: Khiêu dâm / Bạo lực (SEVERE - Chặn ngay)
                  </div>
                  <div class="d-flex flex-wrap gap-1">
                    <span v-for="w in moderation.defaultRules.severe" :key="w" class="badge bg-light border" style="color: #b45309;">
                      {{ w }}
                    </span>
                  </div>
                </div>
              </div>
              <div class="col-md-6">
                <div class="p-2 border rounded bg-white">
                  <div class="fw-bold mb-1" style="font-size: 0.85rem; color: #0284c7;">
                    🎰 Cấp 3: Cờ bạc / Lừa đảo (FRAUD - Chặn ngay)
                  </div>
                  <div class="d-flex flex-wrap gap-1">
                    <span v-for="w in moderation.defaultRules.fraud" :key="w" class="badge bg-light border" style="color: #0284c7;">
                      {{ w }}
                    </span>
                  </div>
                </div>
              </div>
              <div class="col-md-6">
                <div class="p-2 border rounded bg-white">
                  <div class="fw-bold text-secondary mb-1" style="font-size: 0.85rem;">
                    🤬 Cấp 4: Thô tục / Chửi thề (OFFENSIVE - Tự che ***)
                  </div>
                  <div class="d-flex flex-wrap gap-1">
                    <span v-for="w in moderation.defaultRules.offensive" :key="w" class="badge bg-light text-secondary border">
                      {{ w }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Cấu hình từ khóa cấm tùy biến động của Admin -->
        <h5 class="mb-2" style="font-size: 1rem; color: #334155;">Cấu hình từ khóa cấm tùy biến bổ sung (Dynamic Blacklist):</h5>
        <p class="text-muted mb-3" style="font-size: 0.85rem;">
          Nhập các từ khóa cấm bổ sung (ngăn cách bằng dấu phẩy <code>,</code> hoặc xuống dòng). Khi lưu, bộ lọc sẽ được nạp lại tức thì:
        </p>

        <div class="row g-3 mb-4">
          <div class="col-md-6">
            <div class="card p-3" style="background-color: #fff; border: 1px solid #e2e8f0;">
              <label class="form-label fw-bold mb-1 text-danger" style="font-size: 0.9rem;">
                🛑 Từ khóa Nghiêm trọng / Pháp luật (CRITICAL - Chặn ngay)
              </label>
              <textarea 
                v-model="moderation.customRules.critical" 
                class="form-control" 
                rows="2" 
                placeholder="Ví dụ: bạo động, đảo chính..."
              ></textarea>
              <div class="form-text text-muted" style="font-size: 0.78rem;">Bài viết chứa từ khóa này sẽ bị từ chối đăng lập tức.</div>
            </div>
          </div>

          <div class="col-md-6">
            <div class="card p-3" style="background-color: #fff; border: 1px solid #e2e8f0;">
              <label class="form-label fw-bold mb-1" style="font-size: 0.9rem; color: #b45309;">
                🔞 Từ khóa Khiêu dâm / Bạo lực (SEVERE - Chặn ngay)
              </label>
              <textarea 
                v-model="moderation.customRules.severe" 
                class="form-control" 
                rows="2" 
                placeholder="Ví dụ: clip sex, xxx..."
              ></textarea>
              <div class="form-text text-muted" style="font-size: 0.78rem;">Bài viết chứa từ khóa này sẽ bị chặn vì vi phạm tiêu chuẩn cộng đồng.</div>
            </div>
          </div>

          <div class="col-md-6">
            <div class="card p-3" style="background-color: #fff; border: 1px solid #e2e8f0;">
              <label class="form-label fw-bold mb-1 text-primary" style="font-size: 0.9rem;">
                🎰 Từ khóa Cờ bạc / Lừa đảo (FRAUD - Chặn ngay)
              </label>
              <textarea 
                v-model="moderation.customRules.fraud" 
                class="form-control" 
                rows="2" 
                placeholder="Ví dụ: tài xỉu online, nhận thưởng tiền triệu..."
              ></textarea>
              <div class="form-text text-muted" style="font-size: 0.78rem;">Nội dung chứa từ khóa cờ bạc, lừa đảo sẽ bị từ chối đăng.</div>
            </div>
          </div>

          <div class="col-md-6">
            <div class="card p-3" style="background-color: #fff; border: 1px solid #e2e8f0;">
              <label class="form-label fw-bold mb-1 text-secondary" style="font-size: 0.9rem;">
                🤬 Từ khóa Thô tục / Chửi thề (OFFENSIVE - Tự che ***)
              </label>
              <textarea 
                v-model="moderation.customRules.offensive" 
                class="form-control" 
                rows="2" 
                placeholder="Ví dụ: vcl, dkm, dcm..."
              ></textarea>
              <div class="form-text text-muted" style="font-size: 0.78rem;">Bài viết vẫn được đăng nhưng từ ngữ vi phạm sẽ tự động thay bằng <code>***</code>.</div>
            </div>
          </div>
        </div>

        <!-- Khung thử nghiệm bộ lọc -->
        <div class="card p-3 mb-4" style="background: #f8fafc; border: 1px dashed #94a3b8;">
          <h5 class="mb-2" style="font-size: 0.95rem; color: #1e293b;">
            🧪 Công cụ kiểm tra nhanh Bộ lọc Từ cấm (Profanity Tester)
          </h5>
          <p class="text-muted mb-2" style="font-size: 0.85rem;">
            Nhập nội dung bất kỳ để kiểm tra xem hệ thống sẽ chấp thuận, làm sạch bằng <code>***</code> hay từ chối:
          </p>
          <div class="input-group mb-2">
            <input 
              type="text" 
              class="form-control" 
              v-model="testInput" 
              placeholder="Ví dụ: Kêu gọi lật đổ chính quyền hoặc Xem clip sex tại đây..." 
              @keyup.enter.prevent="runModerationTest"
            />
            <button 
              type="button" 
              class="btn btn-outline-primary" 
              :disabled="testing || !testInput.trim()"
              @click="runModerationTest"
            >
              {{ testing ? 'Đang kiểm tra...' : 'Kiểm tra ngay' }}
            </button>
          </div>

          <!-- Kết quả thử nghiệm -->
          <div v-if="testResult" class="p-3 rounded mt-2 border bg-white">
            <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
              <span class="fw-bold">Kết quả xử lý:</span>
              <span v-if="testResult.blocked" class="badge bg-danger">BỊ TỪ CHỐI / CHẶN (BLOCKED)</span>
              <span v-else-if="testResult.maskedContent !== testInput" class="badge bg-warning text-dark">LÀM SẠCH (MASKED ***)</span>
              <span v-else class="badge bg-success">HỢP LỆ (PASSED)</span>
              <span v-if="testResult.highestSeverity" class="badge bg-dark">Mức độ: {{ testResult.highestSeverity }}</span>
            </div>
            <div v-if="testResult.detectedWords && testResult.detectedWords.length > 0" class="mb-2">
              <span class="text-muted" style="font-size: 0.85rem;">Từ khóa vi phạm phát hiện:</span>
              <span v-for="w in testResult.detectedWords" :key="w" class="badge bg-danger ms-1">
                {{ w }}
              </span>
            </div>
            <div v-if="testResult.violationMessage" class="alert alert-danger py-2 px-3 mb-2" style="font-size: 0.88rem;">
              {{ testResult.violationMessage }}
            </div>
            <div v-if="testResult.maskedContent" class="p-2 rounded bg-light border" style="font-size: 0.88rem;">
              <strong>Nội dung sau khi lọc:</strong> <code>{{ testResult.maskedContent }}</code>
            </div>
          </div>
        </div>

        <hr class="my-4" style="border-top: 1px solid #eee;" />

        <div class="d-flex gap-2">
          <button type="submit" class="btn btn-primary px-4" :disabled="saving">
            {{ saving ? 'Đang lưu...' : 'Lưu tất cả cấu hình' }}
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
      moderation: {
        enabled: true,
        defaultRules: {
          critical: [],
          severe: [],
          fraud: [],
          offensive: []
        },
        customRules: {
          critical: '',
          severe: '',
          fraud: '',
          offensive: '',
          legacy: ''
        },
        totalKeywordsLoaded: 0
      },
      showDefaultRules: false,
      testInput: '',
      testResult: null,
      testing: false,
      loading: true,
      saving: false,
      noLimitValue: SETTINGS.NO_LIMIT_VALUE
    }
  },
  async mounted() {
    await this.loadSettings()
    await this.loadModerationConfig()
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
    async loadModerationConfig() {
      try {
        const res = await settingService.getModerationConfig()
        const data = res.data?.data !== undefined ? res.data.data : res.data
        if (data) {
          this.moderation.enabled = data.enabled !== false
          if (data.defaultRules) this.moderation.defaultRules = data.defaultRules
          if (data.customRules) {
            this.moderation.customRules.critical = data.customRules.critical || ''
            this.moderation.customRules.severe = data.customRules.severe || ''
            this.moderation.customRules.fraud = data.customRules.fraud || ''
            this.moderation.customRules.offensive = data.customRules.offensive || ''
            this.moderation.customRules.legacy = data.customRules.legacy || ''
          }
          this.moderation.totalKeywordsLoaded = data.totalKeywordsLoaded || 0
        }
      } catch (e) {
        console.error('Không tải được cấu hình kiểm duyệt:', e)
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
          post_button_category: this.settings.post_button_category,
          profanity_filter_enabled: this.moderation.enabled ? 'true' : 'false',
          profanity_custom_critical: this.moderation.customRules.critical,
          profanity_custom_severe: this.moderation.customRules.severe,
          profanity_custom_fraud: this.moderation.customRules.fraud,
          profanity_custom_offensive: this.moderation.customRules.offensive
        }
        await settingService.updateSettings(payload)
        await this.loadModerationConfig()
        alertSuccess('Lưu cấu hình hệ thống & từ khóa kiểm duyệt thành công')
      } catch (err) {
        console.error('Không lưu được cấu hình hệ thống:', err)
        alertError('Lỗi khi lưu cấu hình hệ thống')
      } finally {
        this.saving = false
      }
    },
    async runModerationTest() {
      if (!this.testInput.trim()) return
      this.testing = true
      try {
        const res = await settingService.testModeration(this.testInput)
        const data = res.data?.data !== undefined ? res.data.data : res.data
        this.testResult = data
      } catch (e) {
        alertError('Lỗi khi kiểm tra bộ lọc: ' + (e.response?.data?.message || e.message))
      } finally {
        this.testing = false
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
