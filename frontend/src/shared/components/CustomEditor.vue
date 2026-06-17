<template>
  <div class="custom-editor-wrapper" :style="{ '--editor-min-height': minHeight }">
    <ckeditor 
      :editor="editor" 
      :model-value="modelValue" 
      @update:model-value="$emit('update:modelValue', $event)"
      :config="editorConfig"
      :disabled="disabled"
      @ready="onEditorReady"
    ></ckeditor>

    <!-- Reusable Emoji & Sticker Picker -->
    <EmojiPicker
      :visible="showEmojiPicker"
      :target-element="emojiPickerTarget"
      @close="showEmojiPicker = false"
      @select="handleEmojiSelect"
    />

    <!-- Tagging Autocomplete Popup -->
    <div 
      v-if="isTagging && filteredUsers.length > 0" 
      class="tagging-popup" 
      :style="popupStyle"
      @mousedown.prevent
    >
      <div class="tagging-list">
        <div 
          v-for="(user, idx) in filteredUsers" 
          :key="user.id || user.username" 
          class="tagging-item"
          :class="{ 'active': idx === activeIndex }"
          @click="selectUser(user)"
        >
          <div class="tagging-avatar" :style="{ backgroundColor: getAvatarColor(user) }">
            {{ (user.displayName || user.username || '?').charAt(0).toUpperCase() }}
          </div>
          <div class="tagging-name">{{ user.displayName || user.username }}</div>
        </div>
      </div>
      <div class="tagging-pagination" v-if="totalPages > 1">
        <ForumPagination 
          :current-page="currentPage" 
          :total-pages="totalPages" 
          @page-changed="onPageChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import { Ckeditor } from '@ckeditor/ckeditor5-vue'
import translations from 'ckeditor5/translations/vi.js'
import userService from '@/apps/Forum/services/user.service'
import ForumPagination from '@/shared/components/ForumPagination.vue'
import { downloadFileAsBlob, extractAttachmentFilename } from '@/shared/utils/downloadUtils'
import {
  ClassicEditor,
  Plugin,
  Essentials,
  Paragraph,
  Heading,
  Bold,
  Italic,
  Underline,
  Strikethrough,
  Font,
  Alignment,
  Link,
  List,
  Indent,
  IndentBlock,
  Image,
  ImageUpload,
  ImageInsert,
  ImageResize,
  ImageStyle,
  ImageToolbar,
  ImageCaption,
  ImageTextAlternative,
  Table,
  MediaEmbed,
  BlockQuote,
  FileRepository,
  TableToolbar,
  TableColumnResize,
  Undo,
  TextTransformation
} from 'ckeditor5'
import 'ckeditor5/ckeditor5.css'
import { MyCustomUploadAdapterPlugin, CustomUploadPlugin, TabIndentPlugin, ClearPastedImageWidthPlugin, EmojiPickerPlugin } from '@/shared/utils/ckeditorPlugins'
import EmojiPicker from '@/shared/components/EmojiPicker.vue'
 
class QuoteSourcePlugin extends Plugin {
  static get requires() {
    return [BlockQuote];
  }
  init() {
    const editor = this.editor;
    editor.model.schema.extend('blockQuote', { allowAttributes: 'data-source' });
    editor.conversion.attributeToAttribute({ model: 'data-source', view: 'data-source' });
  }
}

export default {
  name: 'CustomEditor',
  components: {
    ckeditor: Ckeditor,
    EmojiPicker,
    ForumPagination
  },
  props: {
    modelValue: {
      default: ''
    },
    disabled: {
      default: false
    },
    minHeight: {
      default: '400px'
    },
    allowedUsers: {
      type: Array,
      default: null
    }
  },
  emits: ['update:modelValue', 'ready', 'image-uploaded', 'upload-loading-start', 'upload-loading-end'],
  data() {
    return {
      editorInstance: null,
      showEmojiPicker: false,
      emojiPickerTarget: null,
      editor: ClassicEditor,
      isTagging: false,
      searchQuery: '',
      filteredUsers: [],
      currentPage: 1,
      totalPages: 1,
      activeIndex: 0,
      popupStyle: {
        position: 'absolute',
        left: '0px',
        top: '0px',
        zIndex: 10000
      },
      hasClosedManually: false,
      manualCloseQuery: '',
      editorConfig: {
        licenseKey: 'GPL',
        mediaEmbed: {
          extraProviders: [
            {
              name: 'uploaded-video',
              url: /^.*\.(mp4|webm|ogg|avi|mov)(\?.*)?$/,
              html: match => {
                let url = match[0];
                const uploadsIndex = url.indexOf('/uploads/');
                if (uploadsIndex !== -1) {
                  url = url.substring(uploadsIndex);
                }
                return `<div data-cke-ignore-events="true" style="width: 100%;"><video controls data-cke-ignore-events="true" style="width: 100%; max-height: 500px; object-fit: contain; background: #000;" src="${url}"></video></div>`;
              }
            }
          ]
        },
        fontSize: {
          options: [
            9, 10, 11, 12, 13, 'default', 15, 16, 18, 20, 22, 24, 28, 32, 36
          ]
        },
        image: {
          resizeOptions: [
            { name: 'resizeImage:original', value: null, label: 'Original' },
            { name: 'resizeImage:50', value: '50', label: '50%' },
            { name: 'resizeImage:75', value: '75', label: '75%' }
          ],
          styles: [
            'alignLeft',
            'alignCenter',
            'alignRight'
          ],
          toolbar: [
            'imageStyle:alignLeft',
            'imageStyle:alignCenter',
            'imageStyle:alignRight',
            '|',
            'toggleImageCaption',
            'imageTextAlternative',
            '|',
            'resizeImage'
          ]
        },
        plugins: [
          Essentials, Paragraph, Heading, Bold, Italic, Underline, Strikethrough,
          Font, Alignment, Link, List, Indent, IndentBlock, Image, ImageUpload, ImageInsert, ImageResize, ImageStyle, ImageToolbar, ImageCaption, ImageTextAlternative, Table,
          MediaEmbed, BlockQuote, FileRepository, TableToolbar, TableColumnResize, Undo, TextTransformation,
          MyCustomUploadAdapterPlugin, CustomUploadPlugin, TabIndentPlugin, ClearPastedImageWidthPlugin, EmojiPickerPlugin, QuoteSourcePlugin
        ],
        toolbar: {
          items: [
            'heading',
            '|',
            'bold', 'italic', 'underline', 'strikethrough',
            '|',
            'fontSize', 'fontFamily', 'fontColor', 'fontBackgroundColor',
            '|',
            'alignment',
            '|',
            'bulletedList', 'numberedList',
            '|',
            'outdent', 'indent',
            '|',
            'link', 'insertImage', 'customUpload', 'emojiPicker', 'insertTable', 'mediaEmbed', 'blockQuote',
            '|',
            'undo', 'redo'
          ]
        },
        typing: {
          transformations: {
            include: [ 'symbols', 'mathematical', 'typography', 'quotes' ],
            extra: [
              { from: ':)', to: '😊' },
              { from: ';)', to: '😉' },
              { from: ':D', to: '😀' },
              { from: ':(', to: '☹️' },
              { from: '<3', to: '❤️' },
              { from: ':*', to: '😘' }
            ]
          }
        },
        table: {
          contentToolbar: [
            'tableColumn', 'tableRow', 'mergeTableCells'
          ]
        },
        link: {
          decorators: {
            openInNewTab: {
              mode: 'automatic',
              callback: url => !url.startsWith('#'),
              attributes: {
                target: '_blank',
                rel: 'noopener noreferrer'
              }
            },
            downloadable: {
              mode: 'automatic',
              callback: url => url.match(/\.(pdf|docx|xls|xlsx|doc|txt|zip|rar)$/i),
              attributes: {
                download: 'file'
              }
            }
          }
        },
        placeholder: "Sử dụng '@' với keyword để tìm kiếm và tag người khác",
        language: 'vi',
        translations: [
          translations,
          {
            language: 'vi',
            dictionary: {
              'Insert image': 'Chèn ảnh'
            }
          }
        ]
      }
    }
  },
  watch: {
    searchQuery(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.currentPage = 1;
        this.activeIndex = 0;
      }
    }
  },
  mounted() {
    document.addEventListener('click', this.handleClickOutside)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside)
  },
  methods: {
    onEditorReady(editor) {
      this.editorInstance = editor;
      
      editor.on('imageUploaded', (evt, data) => {
        this.$emit('image-uploaded', data);
      });

      // Lắng nghe sự kiện click nút Picker từ Plugin
      editor.on('openEmojiPicker', (evt, data) => {
        this.emojiPickerTarget = markRaw(data.domTarget);
        this.showEmojiPicker = !this.showEmojiPicker;
      });

      // Relay sự kiện upload tài liệu ra component cha để hiển thị/ẩn Loading overlay
      editor.on('uploadMultipleStart', () => {
        this.$emit('upload-loading-start');
      });
      editor.on('uploadMultipleEnd', () => {
        this.$emit('upload-loading-end');
      });

      // Lắng nghe thay đổi dữ liệu để bắt cú pháp tag @
      editor.model.document.on('change:data', () => {
        this.checkMentionTrigger();
      });

      // Intercept click vào link tài liệu đính kèm (📎) bên trong editor để trigger download thay vì dời con trỏ
      editor.editing.view.document.on('click', (evt, data) => {
        const domEvent = data.domEvent;
        if (!domEvent) return;
        const target = domEvent.target;
        if (!target) return;
        const link = (typeof target.closest === 'function') ? target.closest('a') : null;
        if (link) {
          const filename = extractAttachmentFilename(link);
          if (filename) {
            domEvent.preventDefault();
            evt.stop();
            downloadFileAsBlob(link.href, filename);
          }
        }
      }, { priority: 'high' });

      // Bắt sự kiện keydown mức độ ưu tiên cao nhất để kiểm soát di chuyển dòng và Enter/Escape
      editor.editing.view.document.on('keydown', (evt, data) => {
        if (this.isTagging && this.filteredUsers.length > 0) {
          const keyCode = data.keyCode;
          
          if (keyCode === 40) { // Arrow Down
            evt.stop();
            data.preventDefault();
            this.activeIndex = (this.activeIndex + 1) % this.filteredUsers.length;
          } else if (keyCode === 38) { // Arrow Up
            evt.stop();
            data.preventDefault();
            this.activeIndex = (this.activeIndex - 1 + this.filteredUsers.length) % this.filteredUsers.length;
          } else if (keyCode === 13) { // Enter
            evt.stop();
            data.preventDefault();
            this.selectUser(this.filteredUsers[this.activeIndex]);
          } else if (keyCode === 27) { // Escape
            evt.stop();
            data.preventDefault();
            this.isTagging = false;
            this.hasClosedManually = true;
            this.manualCloseQuery = this.searchQuery;
          }
        }
      }, { priority: 'highest' });

      this.$emit('ready', editor);
    },
    handleClickOutside(e) {
      if (this.$el && !this.$el.contains(e.target)) {
        this.isTagging = false;
      }
    },
    checkMentionTrigger() {
      if (!this.editorInstance) return;
      
      const selection = this.editorInstance.model.document.selection;
      const position = selection.getFirstPosition();
      if (!position) {
        this.isTagging = false;
        return;
      }
      
      const parent = position.parent;
      const range = this.editorInstance.model.createRange(
        this.editorInstance.model.createPositionAt(parent, 0),
        position
      );
      
      let text = '';
      for (const item of range.getItems()) {
        if (item.is('textProxy') || item.is('text')) {
          text += item.data;
        }
      }
      
      // Match @ followed by non-space characters (excluding other @ symbols)
      const match = text.match(/(?:^|\s)@([^@\s][^@]*)$/);
      
      if (match) {
        const query = match[1];
        
        if (this.hasClosedManually && this.manualCloseQuery === query) {
          return;
        } else {
          this.hasClosedManually = false;
          this.manualCloseQuery = '';
        }
        
        this.isTagging = true;
        this.searchQuery = query;
        this.updatePopupPosition();
        this.fetchUsers();
      } else {
        this.isTagging = false;
        this.searchQuery = '';
        this.hasClosedManually = false;
      }
    },
    async fetchUsers() {
      if (!this.editorInstance) return;

      const root = this.editorInstance.model.document.getRoot();
      const selection = this.editorInstance.model.document.selection;
      const position = selection.getFirstPosition();

      let textToCheck = '';
      if (position) {
        const textRange = this.editorInstance.model.createRange(
          this.editorInstance.model.createPositionAt(root, 0),
          position
        );
        let textBefore = '';
        for (const item of textRange.getItems()) {
          if (item.is('textProxy') || item.is('text')) {
            textBefore += item.data;
          }
        }
        const lastAtIndex = textBefore.lastIndexOf('@');
        textToCheck = lastAtIndex === -1 ? textBefore : textBefore.substring(0, lastAtIndex);
      }

      const taggedSet = new Set();
      try {
        const startPosition = this.editorInstance.model.createPositionAt(root, 0);
        const endPosition = this.editorInstance.model.createPositionAt(root, 'end');
        const range = this.editorInstance.model.createRange(startPosition, endPosition);
        for (const item of range.getItems()) {
          if (item.is('textProxy') || item.is('text')) {
            if (item.getAttribute('fontColor') === '#2577b1') {
              const tagText = item.data;
              if (tagText.startsWith('@')) {
                const name = tagText.substring(1).trim().toLowerCase();
                if (name) {
                  taggedSet.add(name);
                }
              }
            }
          }
        }
      } catch (err) {
        console.error('Error getting tagged users:', err);
      }

      // Nếu có allowedUsers (ví dụ: participants của cuộc đối thoại), filter local thay vì search API
      if (this.allowedUsers !== null) {
        const query = (this.searchQuery || '').toLowerCase().trim()
        const filtered = this.allowedUsers.filter(u => {
          if (this.isUserAlreadyTagged(u, taggedSet, textToCheck)) {
            return false;
          }
          if (!query) return true;
          const name = (u.displayName || u.username || '').toLowerCase()
          const uname = (u.username || '').toLowerCase()
          return name.includes(query) || uname.includes(query)
        })
        this.filteredUsers = filtered.slice(0, 10)
        this.totalPages = 1
        if (this.activeIndex >= this.filteredUsers.length) {
          this.activeIndex = Math.max(0, this.filteredUsers.length - 1)
        }
        return
      }

      // Fallback: search API toàn bộ user
      try {
        const response = await userService.search({
          keyword: this.searchQuery,
          page: this.currentPage - 1, // backend is 0-indexed
          size: 10
        })
        
        if (response.data) {
          const pageData = response.data
          const rawContent = pageData.content || []
          const filtered = rawContent.filter(u => !this.isUserAlreadyTagged(u, taggedSet, textToCheck))
          this.filteredUsers = filtered
          this.totalPages = pageData.totalPages || 1
          
          if (this.activeIndex >= this.filteredUsers.length) {
            this.activeIndex = Math.max(0, this.filteredUsers.length - 1)
          }
        }
      } catch (error) {
        console.error('Error fetching users for mention autocomplete:', error)
      }
    },
    escapeRegExp(string) {
      return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    },
    isUserAlreadyTagged(user, taggedSet, textToCheck) {
      const username = (user.username || '').toLowerCase();
      const displayName = (user.displayName || '').toLowerCase();
      
      if (username && taggedSet.has(username)) return true;
      if (displayName && taggedSet.has(displayName)) return true;
      
      if (textToCheck) {
        if (username) {
          const regexU = new RegExp('@' + this.escapeRegExp(username) + '(\\b|\\s|$)', 'i');
          if (regexU.test(textToCheck)) return true;
        }
        if (displayName) {
          const regexD = new RegExp('@' + this.escapeRegExp(displayName) + '(\\b|\\s|$)', 'i');
          if (regexD.test(textToCheck)) return true;
        }
      }
      
      return false;
    },
    updatePopupPosition() {
      this.$nextTick(() => {
        const domSelection = window.getSelection();
        if (domSelection && domSelection.rangeCount > 0) {
          const range = domSelection.getRangeAt(0);
          const rect = range.getBoundingClientRect();
          
          const wrapper = this.$el;
          if (!wrapper) return;
          const wrapperRect = wrapper.getBoundingClientRect();
          
          let relLeft = rect.left - wrapperRect.left;
          let relTop = rect.bottom - wrapperRect.top;
          
          if (relLeft + 320 > wrapperRect.width) {
            relLeft = Math.max(0, wrapperRect.width - 330);
          }
          
          this.popupStyle = {
            position: 'absolute',
            left: `${relLeft}px`,
            top: `${relTop + 5}px`,
            zIndex: 10000
          };
        }
      });
    },
    onPageChange(page) {
      this.currentPage = page;
      this.fetchUsers();
      this.activeIndex = 0;
      if (this.editorInstance) {
        this.editorInstance.editing.view.focus();
      }
    },
    getAvatarColor(user) {
      if (user.avatar && user.avatar.startsWith('#')) {
        return user.avatar;
      }
      const name = user.displayName || user.username || '?';
      let hash = 0;
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash);
      }
      const h = Math.abs(hash % 360);
      return `hsl(${h}, 60%, 50%)`;
    },
    selectUser(user) {
      if (!this.editorInstance) return;
      
      const selection = this.editorInstance.model.document.selection;
      const position = selection.getFirstPosition();
      if (!position) return;
      
      const parent = position.parent;
      const range = this.editorInstance.model.createRange(
        this.editorInstance.model.createPositionAt(parent, 0),
        position
      );
      
      let text = '';
      for (const item of range.getItems()) {
        if (item.is('textProxy') || item.is('text')) {
          text += item.data;
        }
      }
      
      const match = text.match(/(?:^|\s)@([^@\s][^@]*)$/);
      if (!match) return;
      
      const mentionText = match[0];
      const startOffset = text.length - mentionText.length;
      
      const startPosition = this.editorInstance.model.createPositionAt(parent, startOffset);
      const replaceRange = this.editorInstance.model.createRange(startPosition, position);
      
      this.editorInstance.model.change(writer => {
        writer.remove(replaceRange);
        
        const displayTag = '@' + (user.displayName || user.username);
        const startPos = writer.createPositionAt(parent, startOffset);
        let currentPos = startPos;
        
        if (mentionText.startsWith(' ')) {
          writer.insertText(' ', currentPos);
          currentPos = writer.createPositionAt(parent, startOffset + 1);
        }
        
        const attributes = {
          fontColor: '#2577b1',
          bold: true
        };
        
        writer.insertText(displayTag, attributes, currentPos);
        
        const spacePos = writer.createPositionAt(parent, currentPos.offset + displayTag.length);
        writer.insertText(' ', {}, spacePos);
        
        const finalPos = writer.createPositionAt(parent, spacePos.offset + 1);
        writer.setSelection(finalPos);
      });
      
      this.isTagging = false;
      this.searchQuery = '';
      this.filteredUsers = [];
      this.currentPage = 1;
      this.activeIndex = 0;
      
      this.editorInstance.editing.view.focus();
    },
    handleEmojiSelect(item) {
      if (!this.editorInstance) return;

      this.editorInstance.model.change(writer => {
        const selection = this.editorInstance.model.document.selection;
        let insertPosition = selection.getFirstPosition();

        if (item.type === 'unicode') {
          // Chèn chữ thuần (emoji text)
          writer.insertText(item.value, selection.getAttributes(), insertPosition);
        } else if (item.type === 'image') {
          // Chèn Inline Image cho stickers
          const imageElement = writer.createElement('imageInline', { 
            src: item.value, 
            alt: item.name,
            resizedWidth: '24px' // Kích thước cố định nhỏ gọn cho sticker trong dòng văn bản
          });
          writer.insert(imageElement, insertPosition);
        }
      });

      // Focus trả lại editor để gõ tiếp
      this.editorInstance.editing.view.focus();
    },
    insertImages(urls, type = 'full') {
      if (!this.editorInstance) return;
      
      this.editorInstance.model.change(writer => {
        const selection = this.editorInstance.model.document.selection;
        let insertPosition = selection.getFirstPosition();

        if (type === 'thumbnail') {
          const paragraph = writer.createElement('paragraph');
          writer.setAttribute('alignment', 'center', paragraph);
          writer.insert(paragraph, insertPosition);
          
          let currentPos = writer.createPositionAt(paragraph, 0);

          urls.forEach((url, index) => {
            const imageElement = writer.createElement('imageInline', { src: url, resizedWidth: '200px' });
            writer.insert(imageElement, currentPos);
            currentPos = writer.createPositionAfter(imageElement);
            
            if (index < urls.length - 1) {
              const space = writer.createText(' ');
              writer.insert(space, currentPos);
              currentPos = writer.createPositionAfter(space);
            }
          });

          const spacer = writer.createElement('paragraph');
          writer.insert(spacer, writer.createPositionAfter(paragraph));
          insertPosition = writer.createPositionAt(spacer, 0);
        } else {
          urls.forEach((url, index) => {
            const attributes = { src: url };
            const imageElement = writer.createElement('imageBlock', attributes);
            writer.insert(imageElement, insertPosition);
            
            insertPosition = writer.createPositionAfter(imageElement);

            if (index === urls.length - 1) {
              const spacer = writer.createElement('paragraph');
              writer.insert(spacer, insertPosition);
              insertPosition = writer.createPositionAt(spacer, 0);
            }
          });
        }
        
        writer.setSelection(insertPosition);
      });
    }
  }
}
</script>

<style scoped>
.custom-editor-wrapper {
  background: white;
  min-height: var(--editor-min-height, 400px);
  position: relative;
}

.tagging-popup {
  position: absolute;
  background: white;
  border: 1px solid #dfdfdf;
  border-radius: 6px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
  width: 320px;
  z-index: 10000;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.tagging-list {
  max-height: 250px;
  overflow-y: auto;
}

.tagging-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.tagging-item:hover, .tagging-item.active {
  background: #f0f7fb;
}

.tagging-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: white;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.tagging-name {
  color: #2c3e50;
  font-size: 14px;
  font-weight: 500;
}

.tagging-pagination {
  padding: 8px 12px;
  border-top: 1px solid #eee;
  background: #f8f9fa;
  display: flex;
  justify-content: center;
}

:deep(.ck-editor__editable) {
  min-height: var(--editor-min-height, 400px);
  font-size: 16px;
  line-height: 1.6;
}

:deep(.ck-toolbar__items) {
  flex-wrap: wrap !important;
}

:deep(.ck-content table) {
  width: 100%;
  border-collapse: collapse;
}

/* Ghi đè các lớp bảo vệ của CKEditor để cho phép click vào video trong lúc soạn thảo */
:deep(.ck-editor__editable .ck-widget[data-widget="media"]) {
  pointer-events: auto !important;
}
:deep(.ck-editor__editable .ck-media__wrapper) {
  pointer-events: auto !important;
}
:deep(.ck-editor__editable .ck-media__wrapper::after) {
  display: none !important;
  pointer-events: none !important;
}
:deep(.ck-editor__editable .ck-media__wrapper video),
:deep(.ck-editor__editable .ck-media__wrapper iframe) {
  pointer-events: auto !important;
  z-index: 1000 !important;
}

:deep(.ck-content td), :deep(.ck-content th) {
  border: 1px solid #bfbfbf;
  padding: 0.4em;
}

/* Image alignment styles */
:deep(.ck-content .image-style-align-center) {
  margin-left: auto !important;
  margin-right: auto !important;
  display: block !important;
  text-align: center !important;
}

:deep(.ck-content .image-style-align-left) {
  float: left !important;
  margin-right: 1.5em !important;
}

:deep(.ck-content .image-style-align-right) {
  float: right !important;
  margin-left: 1.5em !important;
}

/* Image inline spacing for thumbnails and emojis */
:deep(.ck-content .image-inline) {
  margin: 0 2px !important;
  display: inline-block !important;
  vertical-align: middle;
}

:deep(.ck-content .image-inline img) {
  max-width: 100%;
  height: auto;
  object-fit: contain;
}

/* Ensure inserted stickers don't blow up but preserve their intrinsic aspect ratio */
:deep(.ck-content .image-inline[src*="twemoji"]),
:deep(.ck-content img[src*="twemoji"]) {
   max-width: 24px !important;
   width: 24px !important;
   height: 24px !important;
   display: inline-block !important;
}

</style>
