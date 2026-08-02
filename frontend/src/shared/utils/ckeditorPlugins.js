import uploadService from '@/apps/Forum/services/upload.service'
import { ButtonView } from 'ckeditor5'
import { convertHeicToJpegIfNeeded, processFilesForUpload } from '@/shared/utils/heicUtils'

// Custom Upload Adapter cho hình ảnh (khi paste ảnh hoặc dùng nút imageUpload)
class MyUploadAdapter {
  constructor(loader, editor) {
    this.loader = loader
    this.editor = editor
  }

  upload() {
    return this.loader.file.then(file => new Promise((resolve, reject) => {
      const formData = new FormData()
      formData.append('file', file)
      
      uploadService.upload(formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then(res => {
        const url = res.data.url;
        if (this.editor) {
          this.editor.fire('imageUploaded', { url: url, name: file.name, type: res.data.type || 'image/jpeg' });
        }
        resolve({ default: url })
      })
      .catch(err => {
        reject(err)
      })
    }))
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
      tooltip: true
    });

    view.on('execute', () => {
      const input = document.createElement('input');
      input.type = 'file';
      input.multiple = true;
      input.accept = 'video/*,image/*,.heic,.heif,.pdf,.doc,.docx,.xls,.xlsx';

      input.onchange = async (e) => {
        const files = Array.from(e.target.files);
        if (files.length === 0) return;

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

                if (fileType.startsWith('image/')) {
                  elementToInsert = writer.createElement('imageBlock', { src: fileUrl });
                } else if (fileType.startsWith('video/')) {
                  elementToInsert = writer.createElement('media', { url: fileUrl });
                } else {
                  // Đối với tài liệu (.docx, .pdf...), tạo một đoạn văn chứa icon và link
                  elementToInsert = writer.createElement('paragraph');
                  const linkedText = writer.createText(`📎 ${fileName}`, { linkHref: fileUrl });
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
          alert('Không thể tải lên tệp đính kèm. Vui lòng thử lại sau.');
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
