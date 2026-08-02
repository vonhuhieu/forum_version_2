<template>
  <div class="custom-editor-wrapper" :class="{ 'is-edit': isEdit }" :style="{ '--editor-min-height': minHeight }">
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
      <div class="tagging-list" ref="taggingListRef">
        <div 
          v-for="(user, idx) in filteredUsers" 
          :key="user.id || user.username || idx" 
          class="tagging-item"
          :class="{ 'active': idx === activeIndex }"
          @mouseenter="activeIndex = idx"
          @click="selectUser(user)"
        >
          <div class="tagging-avatar" :style="!isAvatarUrl(user.avatar) ? { backgroundColor: getAvatarColor(user) } : {}">
            <img v-if="isAvatarUrl(user.avatar)" :src="user.avatar" alt="avatar" />
            <span v-else>{{ (user.displayName || user.username || '?').charAt(0).toUpperCase() }}</span>
          </div>
          <div class="tagging-name-wrapper">
            <span class="tagging-name">{{ user.displayName || user.username }}</span>
            <VerifiedBadge :user="user" size="16px" />
          </div>
        </div>
      </div>
      <!-- <div class="tagging-pagination" v-if="totalPages > 1">
        <ForumPagination 
          :current-page="currentPage" 
          :total-pages="totalPages" 
          @page-changed="onPageChange"
        />
      </div> -->
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
import VerifiedBadge from '@/shared/components/VerifiedBadge.vue'
import { isAvatarUrl, getVerifiedBadgeSvgHtml } from '@/shared/utils/utils'
 
class QuoteSourcePlugin extends Plugin {
  static get requires() {
    return [BlockQuote];
  }
  init() {
    const editor = this.editor;
    editor.model.schema.extend('blockQuote', { allowAttributes: ['data-source', 'data-verified'] });
    editor.conversion.attributeToAttribute({ model: 'data-source', view: 'data-source' });
    editor.conversion.attributeToAttribute({ model: 'data-verified', view: 'data-verified' });
  }
}

export default {
  name: 'CustomEditor',
  components: {
    ckeditor: Ckeditor,
    EmojiPicker,
    // ForumPagination,
    VerifiedBadge
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
    },
    isEdit: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'ready', 'image-uploaded', 'upload-loading-start', 'upload-loading-end'],
  data() {
    return {
      editorInstance: null,
      decorateTimer: null,
      showEmojiPicker: false,
      emojiPickerTarget: null,
      editor: ClassicEditor,
      isTagging: false,
      searchQuery: '',
      filteredUsers: [],
      currentPage: 1,
      totalPages: 1,
      activeIndex: 0,
      searchTimeout: null,
      requestId: 0,
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
    },
    activeIndex() {
      this.scrollToHighlighted();
    },
    allowedUsers() {
      this.$nextTick(() => {
        this.decorateEditorQuotes();
      });
    }
  },
  mounted() {
    document.addEventListener('click', this.handleClickOutside)
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside)
    if (this.decorateTimer) {
      clearTimeout(this.decorateTimer)
      this.decorateTimer = null
    }
  },
  methods: {
    isAvatarUrl(avatar) {
      return isAvatarUrl(avatar)
    },
    scrollToHighlighted() {
      this.$nextTick(() => {
        if (!this.$refs.taggingListRef) return;
        const highlightedEl = this.$refs.taggingListRef.querySelector('.tagging-item.active');
        if (highlightedEl) {
          highlightedEl.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        }
      });
    },
    decorateEditorQuotes() {
      if (!this.editorInstance) return;
      const editor = this.editorInstance;
      const editableEl = editor.ui.getEditableElement();
      if (!editableEl) return;

      const blockquotes = editableEl.querySelectorAll('blockquote:not([data-verified])');
      if (blockquotes.length === 0) return;

      const modelElementsToVerify = [];

      blockquotes.forEach(bq => {
        const strongEl = bq.querySelector('p:first-child strong');
        if (!strongEl) return;

        let rawText = '';
        strongEl.childNodes.forEach(node => {
          if (node.nodeType === Node.TEXT_NODE) rawText += node.textContent;
        });
        const authorName = rawText.replace(/\s*đã\s*(?:viết|nói):\s*$/i, '').trim();
        if (!authorName) return;

        let targetUser = null;
        if (this.allowedUsers && Array.isArray(this.allowedUsers)) {
          targetUser = this.allowedUsers.find(u => u &&
            ((u.displayName && u.displayName.trim() === authorName) ||
             (u.username && u.username.trim() === authorName))
          );
        }
        if (!getVerifiedBadgeSvgHtml(targetUser)) return;

        try {
          const viewEl = editor.editing.view.domConverter.mapDomToView(bq);
          if (viewEl) {
            const modelEl = editor.editing.mapper.toModelElement(viewEl);
            if (modelEl) modelElementsToVerify.push(modelEl);
          }
        } catch (e) { /* ignore */ }
      });

      if (modelElementsToVerify.length === 0) return;

      editor.model.change(writer => {
        modelElementsToVerify.forEach(modelEl => {
          writer.setAttribute('data-verified', '1', modelEl);
        });
      });
    },
    onEditorReady(editor) {
      this.editorInstance = editor;

      editor.model.document.on('change:data', () => {
        if (this.decorateTimer) clearTimeout(this.decorateTimer);
        this.decorateTimer = setTimeout(() => {
          this.decorateEditorQuotes();
          this.decorateTimer = null;
        }, 80);
      });

      this.$nextTick(() => {
        this.decorateEditorQuotes();
      });

      // Hàm kiểm tra xem ký tự tại một offset có thuộc về tag @ hợp lệ hay không (màu xanh #2577b1, bold và bắt đầu bằng @)
      const isCharInsideMention = (parent, offset) => {
        if (offset < 0 || offset >= parent.maxOffset) return false;
        
        const testRange = editor.model.createRange(
          editor.model.createPositionAt(parent, offset),
          editor.model.createPositionAt(parent, offset + 1)
        );
        let isMentionChar = false;
        for (const item of testRange.getItems()) {
          if (item.is('textProxy') || item.is('text')) {
            if (item.getAttribute('fontColor') === '#2577b1' && item.getAttribute('bold') === true) {
              isMentionChar = true;
              break;
            }
          }
        }
        if (!isMentionChar) return false;

        // Quét ngược về trước để tìm startOffset của khối màu xanh liên tục
        let startOffset = offset;
        while (startOffset > 0) {
          const rangeBefore = editor.model.createRange(
            editor.model.createPositionAt(parent, startOffset - 1),
            editor.model.createPositionAt(parent, startOffset)
          );
          let isPrevMention = false;
          for (const item of rangeBefore.getItems()) {
            if (item.is('textProxy') || item.is('text')) {
              if (item.getAttribute('fontColor') === '#2577b1' && item.getAttribute('bold') === true) {
                isPrevMention = true;
                break;
              }
            }
          }
          if (isPrevMention) {
            startOffset--;
          } else {
            break;
          }
        }

        // Kiểm tra xem khối màu xanh này có bắt đầu bằng '@' không
        const firstCharRange = editor.model.createRange(
          editor.model.createPositionAt(parent, startOffset),
          editor.model.createPositionAt(parent, startOffset + 1)
        );
        let startsWithAt = false;
        for (const item of firstCharRange.getItems()) {
          if (item.is('textProxy') || item.is('text')) {
            if (item.data && item.data.startsWith('@')) {
              startsWithAt = true;
              break;
            }
          }
        }

        return startsWithAt;
      };

      // Hàm kiểm tra xem con trỏ có thực sự nằm ở giữa tag @ hợp lệ hay không (sau dấu @ và trước ký tự cuối)
      const checkIfSelectionIsInsideMention = (focus) => {
        if (!focus || !focus.parent) return false;
        const parent = focus.parent;
        const offset = focus.offset;
        return isCharInsideMention(parent, offset - 1) && isCharInsideMention(parent, offset);
      };

      // 1. Quản lý vùng chọn khi chỉnh sửa: Ngăn con trỏ đặt vào bên trong tag @ (mention)
      editor.model.document.selection.on('change:range', (evt, data) => {
        const selection = editor.model.document.selection;
        const focus = selection.focus;
        if (!focus || !focus.parent) return;

        const isAtMention = checkIfSelectionIsInsideMention(focus);

        // Chỉ thực hiện nhảy con trỏ ra ngoài tag khi ở chế độ CHỈNH SỬA (isEdit)
        if (isAtMention && this.isEdit) {
          editor.model.change(writer => {
            const parent = focus.parent;
            const offset = focus.offset;
            
            let endOffset = offset;
            while (endOffset < parent.maxOffset) {
              const nextPos = writer.createPositionAt(parent, endOffset + 1);
              const testRange = editor.model.createRange(
                writer.createPositionAt(parent, endOffset),
                nextPos
              );
              let isNextTagChar = false;
              for (const item of testRange.getItems()) {
                if (item.is('textProxy') || item.is('text')) {
                  if (item.getAttribute('fontColor') === '#2577b1' && item.getAttribute('bold') === true) {
                    isNextTagChar = true;
                    break;
                  }
                }
              }
              if (isNextTagChar) {
                endOffset++;
              } else {
                break;
              }
            }
            
            const targetPos = writer.createPositionAt(parent, endOffset);
            writer.setSelection(targetPos);
          });
        } else {
          // Bất kể chế độ nào (viết mới hay sửa), nếu đứng ngoài tag nhưng selection tự động mang style tag, hãy xóa nó
          const fontColor = selection.getAttribute('fontColor');
          const isBold = selection.getAttribute('bold');
          
          if (fontColor === '#2577b1' && isBold === true) {
            editor.model.change(writer => {
              writer.removeSelectionAttribute('fontColor');
              writer.removeSelectionAttribute('bold');
            });
          }
        }
      });

      // 2. Quản lý thuộc tính vùng chọn: Ngăn kế thừa style tag @ khi ở rìa tag @ (tránh lỗi gõ trước/sau tag)
      editor.model.document.selection.on('change:attribute', (evt, data) => {
        const selection = editor.model.document.selection;
        const focus = selection.focus;
        if (!focus || !focus.parent) return;

        const fontColor = selection.getAttribute('fontColor');
        const isBold = selection.getAttribute('bold');
        
        if (fontColor === '#2577b1' && isBold === true) {
          const isAtMention = checkIfSelectionIsInsideMention(focus);
          if (!isAtMention) {
            editor.model.change(writer => {
              writer.removeSelectionAttribute('fontColor');
              writer.removeSelectionAttribute('bold');
            });
          }
        }
      });

      // 3. Quản lý gõ phím khi chỉnh sửa: Chặn xóa hoặc chỉnh sửa đè tag @
      editor.editing.view.document.on('keydown', (evt, data) => {
        const selection = editor.model.document.selection;
        const focus = selection.focus;
        if (!focus) return;

        const keyCode = data.keyCode;

        // Bổ sung: Dọn dẹp style xanh của selection TRƯỚC KHI ký tự đầu tiên được chèn (áp dụng cho mọi chế độ)
        const isControlKey = [8, 46, 9, 13, 27, 37, 38, 39, 40, 33, 34, 35, 36, 16, 17, 18, 91, 92, 224].includes(keyCode) || data.ctrlKey || data.metaKey;
        if (!isControlKey) {
          const parent = focus.parent;
          if (parent) {
            // Nếu gõ sát rìa tag người dùng hợp lệ
            const isAtEdge = isCharInsideMention(parent, focus.offset - 1) !== isCharInsideMention(parent, focus.offset);
            if (isAtEdge) {
              editor.model.change(writer => {
                writer.removeSelectionAttribute('fontColor');
                writer.removeSelectionAttribute('bold');
              });
            }
          }
        }

        // CHỈ chặn các thao tác xóa/sửa tag khi ở chế độ CHỈNH SỬA (isEdit)
        if (!this.isEdit) return;

        const isSelectionContainingMention = () => {
          if (selection.isCollapsed) return false;
          for (const range of selection.getRanges()) {
            for (const item of range.getItems()) {
              if (item.is('textProxy') || item.is('text')) {
                if (item.getAttribute('fontColor') === '#2577b1' && item.getAttribute('bold') === true) {
                  // Chỉ coi là chứa mention nếu ký tự đó thực sự thuộc tag mention hợp lệ
                  const parent = item.parent;
                  const startPos = range.start;
                  if (parent && startPos) {
                    if (isCharInsideMention(parent, startPos.offset)) {
                      return true;
                    }
                  }
                }
              }
            }
          }
          return false;
        };

        const isMentionInSelection = isSelectionContainingMention();
        
        if (isMentionInSelection) {
          const allowedKeys = [
            37, 38, 39, 40, // Arrows
            35, 36,         // End, Home
            33, 34,         // PageUp, PageDown
            16, 17, 18, 91, 92, 224 // Shift, Ctrl, Alt, Meta
          ];
          
          const isCtrlOrMeta = data.ctrlKey || data.metaKey;
          if (isCtrlOrMeta && (keyCode === 67 || keyCode === 65)) {
            return;
          }
          
          evt.stop();
          data.preventDefault();
          return;
        }

        if (selection.isCollapsed) {
          if (keyCode === 8) { // Backspace
            if (focus.offset > 0) {
              const parent = focus.parent;
              // Chỉ chặn khi ký tự bị xóa đứng trước thực sự là một phần của tag @
              if (isCharInsideMention(parent, focus.offset - 1)) {
                evt.stop();
                data.preventDefault();
                return;
              }
            }
          }
          
          if (keyCode === 46) { // Delete
            if (focus.offset < focus.parent.maxOffset) {
              const parent = focus.parent;
              // Chỉ chặn khi ký tự bị xóa đứng sau thực sự là một phần của tag @
              if (isCharInsideMention(parent, focus.offset)) {
                evt.stop();
                data.preventDefault();
                return;
              }
            }
          }
        }
      }, { priority: 'highest' });
      
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

      // Lưu vị trí con trỏ an toàn gần nhất ngoài blockQuote
      let lastSafeSelection = null;

      // 1. Quản lý vùng chọn: Tự động nhảy con trỏ khi đi vào blockQuote hoặc ngăn bôi đen blockQuote
      editor.model.document.selection.on('change:range', (evt, data) => {
        const selection = editor.model.document.selection;
        const focus = selection.focus;
        const anchor = selection.anchor;
        
        if (!focus || !anchor) return;
        
        // Hàm kiểm tra xem một vị trí có nằm trong blockQuote không
        const getBlockQuoteAncestor = (position) => {
          return position.getAncestors().find(a => a.name === 'blockQuote');
        };
        
        const focusBQ = getBlockQuoteAncestor(focus);
        const anchorBQ = getBlockQuoteAncestor(anchor);
        
        // Kiểm tra xem vùng chọn có bao phủ/giao với blockQuote nào không
        let spanningBQ = null;
        if (!focusBQ && !anchorBQ) {
          for (const range of selection.getRanges()) {
            for (const item of range.getItems()) {
              if (item.is('element') && item.name === 'blockQuote') {
                spanningBQ = item;
                break;
              }
            }
            if (spanningBQ) break;
          }
        }
        
        // Tìm ra blockQuote bị vùng chọn chạm vào (nếu có)
        const blockQuoteElement = focusBQ || anchorBQ || spanningBQ;
        
        if (blockQuoteElement) {
          let targetPosition;
          let goBefore = false;
          
          if (lastSafeSelection) {
            try {
              editor.model.change(writer => {
                const beforePos = writer.createPositionBefore(blockQuoteElement);
                if (lastSafeSelection.compareWith(beforePos) === 'after') {
                  goBefore = true;
                }
              });
            } catch (err) {
              console.warn('Error comparing positions:', err);
            }
          }
          
          editor.model.change(writer => {
            if (goBefore) {
              let siblingBefore = blockQuoteElement.previousSibling;
              if (!siblingBefore || siblingBefore.name === 'blockQuote') {
                const paragraph = writer.createElement('paragraph');
                writer.insert(paragraph, blockQuoteElement, 'before');
                siblingBefore = paragraph;
              }
              targetPosition = writer.createPositionAt(siblingBefore, 'end');
            } else {
              let siblingAfter = blockQuoteElement.nextSibling;
              if (!siblingAfter || siblingAfter.name === 'blockQuote') {
                const paragraph = writer.createElement('paragraph');
                writer.insert(paragraph, blockQuoteElement, 'after');
                siblingAfter = paragraph;
              }
              targetPosition = writer.createPositionAt(siblingAfter, 0);
            }
            
            // Thu sập vùng chọn về vị trí an toàn ngoài blockQuote
            writer.setSelection(targetPosition);
          });
          return;
        }
        
        // Chỉ lưu vị trí an toàn khi con trỏ hoàn toàn nằm ngoài blockQuote
        lastSafeSelection = focus;
      });

      // 2. Xử lý xóa trích dẫn khi bấm Backspace ở đầu dòng tiếp theo hoặc Delete ở cuối dòng trước đó
      editor.editing.view.document.on('keydown', (evt, data) => {
        const keyCode = data.keyCode;
        
        if (keyCode === 8) { // Backspace
          const selection = editor.model.document.selection;
          const position = selection.getFirstPosition();
          if (position && position.offset === 0) {
            const parentBlock = position.parent;
            const blockQuoteElement = parentBlock.previousSibling;
            if (blockQuoteElement && blockQuoteElement.name === 'blockQuote') {
              evt.stop();
              data.preventDefault();
              
              // Chỉ thực hiện xóa quote khi KHÔNG phải chế độ Edit
              if (!this.isEdit) {
                editor.model.change(writer => {
                  writer.remove(blockQuoteElement);
                });
              }
              return;
            }
          }
        }
        
        if (keyCode === 46) { // Delete
          const selection = editor.model.document.selection;
          const position = selection.getFirstPosition();
          if (position && position.parent && position.offset === position.parent.maxOffset) {
            const parentBlock = position.parent;
            const blockQuoteElement = parentBlock.nextSibling;
            if (blockQuoteElement && blockQuoteElement.name === 'blockQuote') {
              evt.stop();
              data.preventDefault();
              
              // Chỉ thực hiện xóa quote khi KHÔNG phải chế độ Edit
              if (!this.isEdit) {
                editor.model.change(writer => {
                  writer.remove(blockQuoteElement);
                });
              }
              return;
            }
          }
        }
      }, { priority: 'high' });

      // 3. Xử lý hiển thị tooltip "Xóa quote" khi rê chuột vào góc trên bên phải của blockQuote
      editor.editing.view.document.on('mousemove', (evt, data) => {
        if (this.isEdit) return; // Không hiển thị tooltip khi đang sửa bài viết
        
        const domEvent = data.domEvent;
        if (!domEvent) return;
        const target = domEvent.target;
        if (!target) return;
        
        const blockquote = target.closest('blockquote');
        if (blockquote) {
          const rect = blockquote.getBoundingClientRect();
          const mouseX = domEvent.clientX - rect.left;
          const mouseY = domEvent.clientY - rect.top;
          const mouseXFromRight = rect.width - mouseX;
          
          // Nút xóa được thiết kế ở góc trên bên phải (phạm vi cách lề phải 36px và lề trên 34px)
          if (mouseXFromRight >= 0 && mouseXFromRight <= 36 && mouseY >= 0 && mouseY <= 34) {
            blockquote.setAttribute('title', 'Xóa quote');
          } else {
            blockquote.removeAttribute('title');
          }
        }
      });

      // 4. Xử lý click chuột trái vào nút "Xóa quote"
      editor.editing.view.document.on('mousedown', (evt, data) => {
        if (this.isEdit) return; // Không cho phép click xóa khi đang sửa bài viết
        
        const domEvent = data.domEvent;
        if (!domEvent) return;
        const target = domEvent.target;
        if (!target) return;
        
        const blockquote = target.closest('blockquote');
        if (blockquote) {
          const rect = blockquote.getBoundingClientRect();
          const clickX = domEvent.clientX - rect.left;
          const clickY = domEvent.clientY - rect.top;
          const clickXFromRight = rect.width - clickX;
          
          // Click vào nút xóa ở góc trên bên phải (phạm vi cách lề phải 36px và lề trên 34px)
          if (clickXFromRight >= 0 && clickXFromRight <= 36 && clickY >= 0 && clickY <= 34) {
            domEvent.preventDefault();
            evt.stop();
            
            const viewElement = editor.editing.view.domConverter.mapDomToView(blockquote);
            if (viewElement) {
              const modelElement = editor.editing.mapper.toModelElement(viewElement);
              if (modelElement) {
                editor.model.change(writer => {
                  writer.remove(modelElement);
                });
              }
            }
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
        
        clearTimeout(this.searchTimeout);
        this.searchTimeout = setTimeout(() => {
          this.fetchUsers();
        }, 300);
      } else {
        clearTimeout(this.searchTimeout);
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
      const currentReqId = ++this.requestId;
      try {
        const response = await userService.searchPublic({
          keyword: this.searchQuery,
          page: this.currentPage - 1, // backend is 0-indexed
          size: 10
        })
        
        if (currentReqId === this.requestId && response.data) {
          const pageData = response.data
          const rawContent = pageData.content || []
          const filtered = rawContent.filter(u => !this.isUserAlreadyTagged(u, taggedSet, textToCheck))
          this.filteredUsers = filtered
          this.totalPages = pageData.totalPages || 1
          
          if (this.activeIndex >= this.filteredUsers.length) {
            this.activeIndex = this.filteredUsers.length > 0 ? 0 : -1
          } else if (this.activeIndex < 0 && this.filteredUsers.length > 0) {
            this.activeIndex = 0
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
  flex-shrink: 0;
  overflow: hidden;
  background-color: #1a507a;
}

.tagging-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.tagging-name-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  white-space: nowrap;
}

.tagging-name {
  color: #2c3e50;
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  overflow: hidden;
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

/* Cấu hình thẻ blockquote trong editor */
:deep(.ck-editor__editable blockquote) {
  position: relative !important;
  cursor: not-allowed !important;
  user-select: none !important;
}

:deep(.ck-editor__editable blockquote *) {
  cursor: not-allowed !important;
  user-select: none !important;
}

/* Nút xóa trích dẫn (icon tròn đỏ chứa dấu nhân x) nằm ở góc trên bên phải blockquote, ngang hàng tiêu đề tác giả */
:deep(.ck-editor__editable blockquote::before) {
  content: "×" !important;
  position: absolute !important;
  top: 14px !important;
  right: 16px !important;
  width: 18px !important;
  height: 18px !important;
  background-color: #ff4d4f !important;
  color: #ffffff !important;
  border-radius: 50% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-family: Arial, sans-serif !important;
  font-size: 13px !important;
  font-weight: bold !important;
  line-height: 1 !important;
  cursor: pointer !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2) !important;
  transition: all 0.2s ease !important;
  z-index: 100 !important;
  user-select: none !important;
  pointer-events: auto !important;
}

/* Hiệu ứng zoom nhẹ và đổi màu khi hover */
:deep(.ck-editor__editable blockquote:hover::before) {
  background-color: #d9363e !important;
  transform: scale(1.1) !important;
}

/* Ẩn hoàn toàn nút xóa trích dẫn khi đang sửa bài viết (Edit mode) */
.is-edit :deep(.ck-editor__editable blockquote::before) {
  display: none !important;
}

/* Đồng bộ màu cam cho toàn bộ dòng tiêu đề quote (cả strong lẫn text "đã viết:" nằm ngoài strong) */
:deep(.ck-editor__editable blockquote p:first-child) {
  color: #e67e22 !important;
  font-size: 0.9rem !important;
  font-weight: bold !important;
}

/* Badge tích xanh uy tín trong Editor Composer (dùng CSS ::after trên data-verified) */
:deep(.ck-editor__editable blockquote[data-verified="1"] p:first-child strong::after) {
  content: '';
  display: inline-block;
  width: 15px;
  height: 15px;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 24 24' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Ccircle cx='12' cy='12' r='10' fill='%231877F2'/%3E%3Cpath d='M8.5 12.5L10.5 14.5L15.5 9.5' stroke='white' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  vertical-align: middle;
  margin-left: 4px;
  pointer-events: none;
}

</style>
