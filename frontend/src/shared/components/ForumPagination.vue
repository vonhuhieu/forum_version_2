<template>
  <div class="xf-pagination" v-if="totalPages > 1">
    <!-- Desktop Layout -->
    <div class="pagination-desktop">
      <button 
        class="page-btn page-prev" 
        :disabled="currentPage === 1" 
        @click="changePage(currentPage - 1)"
      >
        <span class="icon">◂</span> Trước
      </button>
      
      <template v-for="(page, index) in pages" :key="index">
        <div v-if="page === '...'" class="jump-wrapper" v-click-outside="closeJump">
          <button 
            class="page-btn page-ellipsis" 
            @click="toggleJump(index)"
          >
            ...
          </button>
          
          <div v-if="activeJumpIndex === index" class="jump-dropdown">
            <div class="jump-header">Đi tới trang</div>
            <div class="jump-body">
              <div class="jump-controls">
                <input 
                  type="number" 
                  v-model.number="jumpPageInput" 
                  min="1" 
                  :max="totalPages" 
                  class="jump-input" 
                  @keyup.enter="submitJump"
                />
                <button class="btn-step" @click="stepJump(1)">+</button>
                <button class="btn-step" @click="stepJump(-1)">-</button>
              </div>
              <button class="btn-submit" @click="submitJump">Tới</button>
            </div>
          </div>
        </div>
        
        <button 
          v-else 
          class="page-btn" 
          :class="{ active: page === currentPage }" 
          @click="changePage(page)"
        >
          {{ page }}
        </button>
      </template>
      
      <button 
        class="page-btn page-next" 
        :disabled="currentPage === totalPages" 
        @click="changePage(currentPage + 1)"
      >
        Tiếp <span class="icon">▸</span>
      </button>
    </div>

    <!-- Mobile Layout -->
    <div class="pagination-mobile">
      <button 
        v-if="currentPage > 1" 
        class="page-btn page-double-arrow" 
        @click="changePage(1)"
        title="Trang đầu"
      >
        <span class="icon">«</span>
      </button>
      
      <button 
        v-if="currentPage > 1" 
        class="page-btn page-prev" 
        @click="changePage(currentPage - 1)"
      >
        <span class="icon">◂</span> Trước
      </button>

      <div class="jump-wrapper" v-click-outside="closeMobileJump">
        <button 
          class="page-btn page-of" 
          @click="toggleMobileJump"
        >
          {{ currentPage }} of {{ totalPages }}
        </button>
        
        <div v-if="showMobileJump" class="jump-dropdown">
          <div class="jump-header">Đi tới trang</div>
          <div class="jump-body">
            <div class="jump-controls">
              <input 
                type="number" 
                v-model.number="jumpPageInput" 
                min="1" 
                :max="totalPages" 
                class="jump-input" 
                @keyup.enter="submitMobileJump"
              />
              <button class="btn-step" @click="stepMobileJump(1)">+</button>
              <button class="btn-step" @click="stepMobileJump(-1)">-</button>
            </div>
            <button class="btn-submit" @click="submitMobileJump">Tới</button>
          </div>
        </div>
      </div>

      <button 
        v-if="currentPage < totalPages" 
        class="page-btn page-next" 
        @click="changePage(currentPage + 1)"
      >
        Tiếp <span class="icon">▸</span>
      </button>

      <button 
        v-if="currentPage < totalPages" 
        class="page-btn page-double-arrow" 
        @click="changePage(totalPages)"
        title="Trang cuối"
      >
        <span class="icon">»</span>
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ForumPagination',
  props: {
    currentPage: {
      required: true
    },
    totalPages: {
      required: true
    }
  },
  data() {
    return {
      activeJumpIndex: null,
      jumpPageInput: 1,
      showMobileJump: false
    }
  },
  computed: {
    pages() {
      const pages = [];
      let left = Math.max(2, this.currentPage - 1);
      let right = Math.min(this.totalPages - 1, this.currentPage + 1);
      
      if (this.currentPage <= 2) {
         right = Math.min(this.totalPages - 1, 4);
      }
      if (this.currentPage >= this.totalPages - 1) {
         left = Math.max(2, this.totalPages - 3);
      }
      
      pages.push(1);
      
      if (left > 2) {
         pages.push('...');
      }
      
      for (let i = left; i <= right; i++) {
         pages.push(i);
      }
      
      if (right < this.totalPages - 1) {
         pages.push('...');
      }
      
      if (this.totalPages > 1) {
         pages.push(this.totalPages);
      }
      
      return pages;
    }
  },
  methods: {
    changePage(page) {
      if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
        this.closeJump();
        this.closeMobileJump();
        this.$emit('page-changed', page);
      }
    },
    toggleJump(index) {
      if (this.activeJumpIndex === index) {
        this.closeJump();
      } else {
        this.activeJumpIndex = index;
        this.jumpPageInput = this.currentPage;
      }
    },
    closeJump(event) {
      if (event && event.target && event.target.closest('.jump-wrapper')) {
        return;
      }
      this.activeJumpIndex = null;
    },
    stepJump(step) {
      let newVal = (this.jumpPageInput || this.currentPage) + step;
      if (newVal >= 1 && newVal <= this.totalPages) {
        this.jumpPageInput = newVal;
      }
    },
    submitJump() {
      if (this.jumpPageInput >= 1 && this.jumpPageInput <= this.totalPages) {
        this.changePage(this.jumpPageInput);
      } else {
        this.jumpPageInput = this.currentPage;
      }
    },
    toggleMobileJump() {
      if (this.showMobileJump) {
        this.closeMobileJump();
      } else {
        this.showMobileJump = true;
        this.jumpPageInput = this.currentPage;
      }
    },
    closeMobileJump() {
      this.showMobileJump = false;
    },
    stepMobileJump(step) {
      let newVal = (this.jumpPageInput || this.currentPage) + step;
      if (newVal >= 1 && newVal <= this.totalPages) {
        this.jumpPageInput = newVal;
      }
    },
    submitMobileJump() {
      if (this.jumpPageInput >= 1 && this.jumpPageInput <= this.totalPages) {
        this.changePage(this.jumpPageInput);
        this.closeMobileJump();
      } else {
        this.jumpPageInput = this.currentPage;
      }
    }
  },
  directives: {
    'click-outside': {
      mounted(el, binding) {
        el.clickOutsideEvent = function(event) {
          if (!(el === event.target || el.contains(event.target))) {
            binding.value(event);
          }
        };
        document.body.addEventListener('click', el.clickOutsideEvent);
      },
      unmounted(el) {
        document.body.removeEventListener('click', el.clickOutsideEvent);
      }
    }
  }
}
</script>

<style scoped>
.xf-pagination {
  font-family: Arial, sans-serif;
  user-select: none;
}

.pagination-desktop {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-mobile {
  display: none;
}

.page-btn {
  background: white;
  border: 1px solid #dfdfdf;
  color: #185886;
  padding: 4px 8px;
  min-width: 32px;
  height: 32px;
  border-radius: 2px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: #f5f9fd;
  border-color: #a5cae4;
  color: #185886;
}

.page-btn.active {
  background: #185886;
  border-color: #185886;
  color: white;
  font-weight: bold;
}

.page-btn:disabled {
  color: #999;
  border-color: #eee;
  background: #fafafa;
  cursor: not-allowed;
}

.page-of {
  background: #fdf5ec;
  border-color: #fbe1c3;
  color: #e28a2a;
  font-weight: 500;
}

.page-of:hover {
  background: #fdf0e0 !important;
  border-color: #f7d4ae !important;
  color: #c46d16 !important;
}

.icon {
  font-size: 16px;
  margin: 0 2px;
  line-height: 1;
}

.jump-wrapper {
  position: relative;
}

.jump-dropdown {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 8px;
  background: white;
  border: 1px solid #b7cde1;
  border-radius: 3px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.15);
  width: 200px;
  z-index: 100;
}

.jump-dropdown::before {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: #b7cde1 transparent transparent transparent;
}
.jump-dropdown::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 5px;
  border-style: solid;
  border-color: white transparent transparent transparent;
}

.jump-header {
  background: #f5f9fd;
  padding: 8px 10px;
  font-size: 13px;
  font-weight: bold;
  color: #185886;
  border-bottom: 1px solid #dfdfdf;
  border-top-left-radius: 3px;
  border-top-right-radius: 3px;
}

.jump-body {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.jump-controls {
  display: flex;
  height: 30px;
}

.jump-input {
  flex: 1;
  width: 0;
  border: 1px solid #dfdfdf;
  padding: 0 8px;
  font-size: 13px;
  border-right: none;
  border-top-left-radius: 3px;
  border-bottom-left-radius: 3px;
  outline: none;
}

.jump-input:focus {
  border-color: #a5cae4;
}

.btn-step {
  width: 30px;
  background: #f5f9fd;
  border: 1px solid #dfdfdf;
  color: #185886;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
}
.btn-step:hover {
  background: #e6f0f9;
}
.btn-step:last-of-type {
  border-left: none;
}

.btn-submit {
  background: #185886;
  color: white;
  border: 1px solid #144970;
  border-radius: 3px;
  padding: 6px 0;
  cursor: pointer;
  font-size: 13px;
  font-weight: bold;
  transition: background 0.2s;
}

.btn-submit:hover {
  background: #144970;
}

@media (max-width: 767px) {
  .xf-pagination {
    display: flex;
    justify-content: center;
    width: 100%;
  }

  .pagination-desktop {
    display: none;
  }
  
  .pagination-mobile {
    display: flex;
    align-items: center;
    gap: 4px;
    justify-content: center;
  }
}
</style>
