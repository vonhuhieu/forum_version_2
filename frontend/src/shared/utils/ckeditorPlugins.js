import uploadService from '@/apps/Forum/services/upload.service'
import { ButtonView } from 'ckeditor5'
import { convertHeicToJpegIfNeeded, processFilesForUpload } from '@/shared/utils/heicUtils'
import { getBackendBaseUrl } from '@/shared/services/api.service'

// Custom Upload Adapter cho hình ảnh (khi paste ảnh hoặc dùng nút imageUpload)
class MyUploadAdapter {
  constructor(loader, editor) {
    this.loader = loader
    this.editor = editor
  }

  upload() {
    if (this.editor) {
      this.editor.fire('uploadMultipleStart');
    }
    return this.loader.file.then(file => new Promise((resolve, reject) => {
      const formData = new FormData()
      formData.append('file', file)
      
      uploadService.upload(formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then(res => {
        const url = res.data.url;
        let fullUrl = url;
        if (fullUrl && !fullUrl.startsWith('http://') && !fullUrl.startsWith('https://')) {
          fullUrl = `${getBackendBaseUrl()}${fullUrl.startsWith('/') ? '' : '/'}${fullUrl}`;
        }
        if (this.editor) {
          this.editor.fire('imageUploaded', { url: url, name: file.name, type: res.data.type || 'image/jpeg' });
        }
        resolve({ default: fullUrl })
      })
      .catch(err => {
        reject(err)
      })
      .finally(() => {
        if (this.editor) {
          this.editor.fire('uploadMultipleEnd');
        }
      })
    })).catch(err => {
      if (this.editor) {
        this.editor.fire('uploadMultipleEnd');
      }
      throw err;
    })
  }

  abort() {}
}

export function MyCustomUploadAdapterPlugin(editor) {
  editor.plugins.get('FileRepository').createUploadAdapter = (loader) => {
    return new MyUploadAdapter(loader, editor)
  }
}

// Plugin Tải Tệp Đa Năng
export function CustomUploadPlugin(editor) {
  editor.ui.componentFactory.add('customUpload', locale => {
    const view = new ButtonView(locale);

    // Icon hình ghim kẹp giấy (paperclip)
    const uploadIcon = '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5a2.5 2.5 0 0 1 5 0v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5a2.5 2.5 0 0 0 5 0V5c0-1.38-1.12-2.5-2.5-2.5S10 3.62 10 5v12.5a4 4 0 0 0 8 0V6h-1.5z"/></svg>';

    view.set({
      label: 'Tải lên tệp đính kèm (Video, Ảnh, Tài liệu)',
      icon: uploadIcon,
      tooltip: true,
      class: 'ck-btn-custom-upload'
    });

    view.extendTemplate({
      attributes: {
        class: ['ck-btn-custom-upload']
      }
    });

    view.on('execute', () => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = true;
      input.accept = 'video/*,image/jpeg,image/png,image/webp,image/gif,.pdf,.doc,.docx,.xls,.xlsx';

      input.onchange = async (e) => {
        const files = Array.from(e.target.files);
        if (files.length === 0) return;

        // Kiểm tra giới hạn video trước khi tải lên (Tối đa 200MB)
        const MAX_VIDEO_SIZE = 200 * 1024 * 1024;
        for (const file of files) {
          const isVideo = (file.type && file.type.startsWith('video/')) ||
            /\.(mp4|webm|mov|avi|mkv|m4v)$/i.test(file.name);
          if (isVideo && file.size > MAX_VIDEO_SIZE) {
            alert(`Tệp video "${file.name}" vượt quá giới hạn 200MB của hệ thống. Vui lòng nén nhỏ video hoặc chia sẻ qua liên kết bên ngoài.`);
            return;
          }
        }

        // Thông báo bắt đầu upload để Vue cha có thể hiển thị Loading overlay
        editor.fire('uploadMultipleStart');

        try {
          const formData = new FormData();
          files.forEach(file => formData.append('files', file));

          const res = await uploadService.uploadMultiple(formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          });
          
          const validResults = res.data || [];
          
          if (validResults.length > 0) {
            if (editor) {
              validResults.forEach(r => {
                if (r.type && r.type.startsWith('image/')) {
                  editor.fire('imageUploaded', { url: r.url, name: r.name, type: r.type });
                }
              });
            }
            editor.model.change(writer => {
              const selection = editor.model.document.selection;
              let insertPosition = selection.getFirstPosition();

              validResults.forEach((result, index) => {
                let elementToInsert;
                const fileType = result.type || '';
                const fileUrl = result.url;
                const fileName = result.name;

                let fullUrl = fileUrl;
                if (fullUrl && !fullUrl.startsWith('http://') && !fullUrl.startsWith('https://')) {
                  fullUrl = `${getBackendBaseUrl()}${fullUrl.startsWith('/') ? '' : '/'}${fullUrl}`;
                }

                if (fileType.startsWith('image/')) {
                  elementToInsert = writer.createElement('imageBlock', { src: fullUrl });
                } else if (fileType.startsWith('video/')) {
                  elementToInsert = writer.createElement('media', { url: fullUrl });
                } else {
                  // Đối với tài liệu (.docx, .pdf...), tạo một đoạn văn chứa icon và link
                  elementToInsert = writer.createElement('paragraph');
                  const linkedText = writer.createText(`📎 ${fileName}`, { linkHref: fullUrl });
                  writer.insert(linkedText, elementToInsert, 'end');
                }

                // Chèn phần tử vào vị trí hiện tại
                writer.insert(elementToInsert, insertPosition);
                
                // Vị trí tiếp theo là ngay sau phần tử vừa chèn
                insertPosition = writer.createPositionAfter(elementToInsert);

                // Nếu là phần tử cuối cùng, thêm một dòng trống để dễ soạn thảo tiếp
                if (index === validResults.length - 1) {
                  const spacer = writer.createElement('paragraph');
                  writer.insert(spacer, insertPosition);
                  insertPosition = writer.createPositionAt(spacer, 0);
                }
              });
              
              // Đặt con trỏ vào vị trí mới cuối cùng
              writer.setSelection(insertPosition);
            });
          }
        } catch (err) {
          console.error('Error uploading multiple files:', err);
          const errorMsg = err.response?.data?.message || err.message || 'Không thể tải lên tệp đính kèm. Vui lòng thử lại sau.';
          alert(errorMsg);
        } finally {
          // Thông báo kết thúc upload (dù thành công hay thất bại) để ẩn Loading overlay
          editor.fire('uploadMultipleEnd');
        }
      };

      input.click();
    });

    return view;
  });
}

// Plugin hỗ trợ bấm phím Tab để thụt đầu dòng (insert 4 spaces)
export function TabIndentPlugin(editor) {
  editor.keystrokes.set('Tab', (keyEvtData, cancel) => {
    const position = editor.model.document.selection.getFirstPosition();
    if (!position) return;
    
    // Kiểm tra xem con trỏ đang ở trong danh sách hoặc bảng không
    const ancestors = position.getAncestors();
    const isList = ancestors.some(a => a.name === 'listItem');
    const isTable = ancestors.some(a => a.name === 'tableCell');
    
    // Nếu đang ở trong danh sách hoặc bảng, để CKEditor tự xử lý (thụt lề list hoặc nhảy cell)
    if (isList || isTable) {
      return;
    }
    
    // Ngăn chặn hành vi mặc định (chuyển focus ra khỏi editor hoặc nhảy widget)
    cancel();
    
    // Chèn 4 dấu cách không ngắt dòng (Non-breaking space)
    editor.model.change(writer => {
      writer.insertText('\u00A0\u00A0\u00A0\u00A0', editor.model.document.selection.getAttributes(), position);
    });
  }, { priority: 'highest' });
}

// Plugin tự động xóa kích thước cứng (resizedWidth) của ảnh khi vừa được dán/chèn vào editor
// Giúp mọi ảnh copy từ nguồn khác luôn ở trạng thái mặc định (bung full 100% chiều rộng)
export function ClearPastedImageWidthPlugin(editor) {
  editor.model.document.on('change', () => {
    const changes = editor.model.document.differ.getChanges();
    
    editor.model.change(writer => {
      for (const entry of changes) {
        // Chỉ bắt sự kiện khi có một phần tử mới được chèn vào
        if (entry.type === 'insert' && (entry.name === 'imageBlock' || entry.name === 'imageInline')) {
          const item = entry.position.nodeAfter;
          // Nếu ảnh có mang theo kích thước từ trang web cũ, xóa nó đi để ảnh bung 100%, trừ thumbnail
          if (item && item.hasAttribute('resizedWidth')) {
            const currentWidth = item.getAttribute('resizedWidth');
            // Không xóa chiều rộng nếu là ảnh thumbnail (150px) hoặc icon/sticker (24px)
            if (currentWidth !== '150px' && currentWidth !== '24px') {
              writer.removeAttribute('resizedWidth', item);
            }
          }
        }
      }
    });
  });
}
// Plugin Mở Hộp Chọn Emoji/Sticker
export function EmojiPickerPlugin(editor) {
  editor.ui.componentFactory.add('emojiPicker', locale => {
    const view = new ButtonView(locale);

    // Icon mặt cười
    const emojiIcon = '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 22C6.477 22 2 17.523 2 12S6.477 2 12 2s10 4.477 10 10-4.477 10-10 10zm0-2a8 8 0 1 0 0-16 8 8 0 0 0 0 16zm-4-8a2 2 0 1 1 0-4 2 2 0 0 1 0 4zm8 0a2 2 0 1 1 0-4 2 2 0 0 1 0 4zm-4 5c-2.33 0-4.31-1.46-5.11-3.5h10.22c-.8 2.04-2.78 3.5-5.11 3.5z"/></svg>';

    view.set({
      label: 'Chèn biểu tượng cảm xúc (Emoji)',
      icon: emojiIcon,
      tooltip: true
    });

    view.on('execute', (evt) => {
      // Lấy phần tử DOM thực tế của button (đã được render ra toolbar)
      const domTarget = view.element || null;
      // Bắn sự kiện custom để Vue cha bắt và hiển thị Picker
      editor.fire('openEmojiPicker', { domTarget });
    });

    return view;
  });
}

// Plugin Nút Chuyển Đổi Mở Rộng / Thu Hẹp Thanh Công Cụ Trên Mobile
export function MobileToolbarTogglePlugin(editor) {
  editor.ui.componentFactory.add('mobileToolbarToggle', locale => {
    const view = new ButtonView(locale);

    view.set({
      label: 'Mở rộng',
      withText: true,
      tooltip: false,
      class: 'ck-btn-mobile-toggle'
    });

    view.extendTemplate({
      attributes: {
        class: ['ck-btn-mobile-toggle']
      }
    });

    view.on('execute', () => {
      const editorElement = editor.ui.view.element;
      if (editorElement) {
        const isExpanded = editorElement.classList.toggle('mobile-toolbar-expanded');
        view.set({
          label: isExpanded ? 'Thu hẹp' : 'Mở rộng'
        });
      }
    });

    return view;
  });
}

// Plugin Mở Hộp Thoại Chèn / Sửa Mã Nguồn HTML
export function HtmlSourcePlugin(editor) {
  editor.ui.componentFactory.add('htmlSource', locale => {
    const view = new ButtonView(locale);

    // Icon Code </>
    const codeIcon = '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>';

    view.set({
      label: 'Dán mã HTML / Nguồn (Source Code)',
      icon: codeIcon,
      tooltip: true,
      class: 'ck-btn-html-source'
    });

    view.on('execute', () => {
      editor.fire('openHtmlSource');
    });

    return view;
  });
}

// Plugin Nút Tiện Ích Mở Catbox.moe và Gofile.io
export function QuickHostLinkPlugins(editor) {
  // Nút mở Catbox.moe
  editor.ui.componentFactory.add('openCatbox', locale => {
    const view = new ButtonView(locale);
    // Icon chú mèo Catbox
    const catIcon = '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2a9 9 0 0 0-9 9c0 3.2 1.7 6.1 4.3 7.7L6 22l3.4-1.5c.8.3 1.7.5 2.6.5 5 0 9-4 9-9s-4-9-9-9zm-3 8c.8 0 1.5.7 1.5 1.5S9.8 13 9 13s-1.5-.7-1.5-1.5S8.2 10 9 10zm6 0c.8 0 1.5.7 1.5 1.5s-.7 1.5-1.5 1.5-1.5-.7-1.5-1.5.7-1.5 1.5-1.5zm-3 6.5c-1.5 0-2.8-.8-3.4-2h6.8c-.6 1.2-1.9 2-3.4 2z"/></svg>';

    view.set({
      label: 'Catbox.moe (Tải video/ảnh lấy direct link .mp4)',
      icon: catIcon,
      tooltip: true,
      class: 'ck-btn-quick-catbox'
    });

    view.extendTemplate({
      attributes: {
        class: ['ck-btn-quick-catbox']
      }
    });

    view.on('execute', () => {
      window.open('https://catbox.moe', '_blank', 'noopener,noreferrer');
    });

    return view;
  });

  // Nút mở Gofile.io
  editor.ui.componentFactory.add('openGofile', locale => {
    const view = new ButtonView(locale);
    // Icon Cloud Upload Gofile
    const cloudIcon = '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/></svg>';

    view.set({
      label: 'Gofile.io (Tải file lớn / video nặng không giới hạn)',
      icon: cloudIcon,
      tooltip: true,
      class: 'ck-btn-quick-gofile'
    });

    view.extendTemplate({
      attributes: {
        class: ['ck-btn-quick-gofile']
      }
    });

    view.on('execute', () => {
      window.open('https://gofile.io', '_blank', 'noopener,noreferrer');
    });

    return view;
  });
}

// Plugin Tự Động Chuyển Đổi Mọi URL Thành Liên Kết (Hyperlink) Khi Dán (Auto-link on Paste)
export function AutoLinkOnPastePlugin(editor) {
  // Lắng nghe sự kiện clipboardInput trên editing view document
  editor.editing.view.document.on('clipboardInput', (evt, data) => {
    const dataTransfer = data.dataTransfer;
    if (!dataTransfer) return;

    // Lấy chuỗi văn bản thuần túy (plain text)
    const plainText = dataTransfer.getData('text/plain');
    if (!plainText || !plainText.trim()) return;

    // Nếu dữ liệu HTML từ clipboard đã có sẵn thẻ <a> với thuộc tính href thì để CKEditor xử lý mặc định
    const htmlData = dataTransfer.getData('text/html');
    if (htmlData && /<a\s+[^>]*href=/i.test(htmlData)) {
      return;
    }

    // Regex kiểm tra xem trong plainText có chứa URL bắt đầu bằng http:// hoặc https:// không
    const urlRegex = /(https?:\/\/[^\s<>"']+)/i;
    if (!urlRegex.test(plainText)) {
      return;
    }

    // Hàm escape ký tự đặc biệt HTML an toàn
    const escapeHtml = (str) => {
      return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    };

    // Tách các dòng để giữ nguyên cấu trúc dòng
    const lines = plainText.split(/\r?\n/);
    const convertedLines = lines.map(line => {
      const escaped = escapeHtml(line);
      return escaped.replace(/(https?:\/\/[^\s<>"']+)/gi, (rawUrl) => {
        // Tách các dấu câu dính ở cuối URL (nếu có: .,;:!?)
        let trailingPunct = '';
        const cleanUrl = rawUrl.replace(/[.,;:!?)]+$/, (punct) => {
          trailingPunct = punct;
          return '';
        });
        return `<a href="${cleanUrl}">${cleanUrl}</a>${trailingPunct}`;
      });
    });

    const finalHtml = convertedLines.length === 1
      ? convertedLines[0]
      : convertedLines.map(line => `<p>${line || '&nbsp;'}</p>`).join('');

    try {
      if (editor.data && editor.data.processor) {
        // Tạo ViewDocumentFragment và gán vào data.content để CKEditor chuyển thành Model Text có thuộc tính linkHref
        data.content = editor.data.processor.toView(finalHtml);
      }
    } catch (err) {
      console.error('[AutoLinkOnPastePlugin] Lỗi khi chuyển đổi URL sang liên kết:', err);
    }
  }, { priority: 'high' });
}


