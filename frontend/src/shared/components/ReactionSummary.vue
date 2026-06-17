<template>
  <div v-if="hasReactions" class="reactionsBar js-reactionsList is-active">
    <ul class="reactionSummary">
      <li v-for="item in topReactionIcons" :key="item.reactionIcon.id">
        <span class="reaction reaction--small" :class="`reaction--${item.reactionIcon.id}`" :data-reaction-id="item.reactionIcon.id">
          <i aria-hidden="true"></i>
          <ReactionIcon 
            :code="item.reactionIcon.icon"
            :color="item.reactionIcon.color"
            class="reaction-sprite js-reaction"
            :title="item.reactionIcon.tooltip"
            size="16px"
          />
        </span>
      </li>
    </ul>

    <span class="u-srOnly" style="display: none;">Reactions:</span>
    <a class="reactionsBar-link" href="#" @click.prevent="$emit('open-popup')">
      <template v-if="processedReactors.length > 0">
        <template v-for="(reactor, idx) in processedReactors" :key="reactor.id">
          <bdi :class="{ 'is-me': reactor.isMe }">{{ reactor.displayName }}</bdi><template v-if="idx < processedReactors.length - 1"><template v-if="totalCount > 3">{{ ', ' }}</template><template v-else><template v-if="idx === processedReactors.length - 2">{{ ' và ' }}</template><template v-else>{{ ', ' }}</template></template></template>
        </template>
        <template v-if="totalCount > 3">{{ ' và ' }}{{ totalCount - 3 }}{{ ' người khác' }}</template>
      </template>
      <template v-else>
        {{ totalCount }} người
      </template>
    </a>
  </div>
</template>

<script>
import ReactionIcon from './ReactionIcon.vue'

export default {
  name: 'ReactionSummary',
  components: {
    ReactionIcon
  },
  props: {
    summary: {
      default: () => []
    },
    recentReactors: {
      default: () => []
    }
  },
  computed: {
    hasReactions() {
      return this.summary && this.summary.length > 0 && this.totalCount > 0
    },
    totalCount() {
      if (!this.summary) return 0
      return this.summary.reduce((acc, curr) => acc + curr.count, 0)
    },
    topReactionIcons() {
      if (!this.summary) return []
      
      return [...this.summary]
        .sort((a, b) => {
          // 1. Ưu tiên số lượt thả nhiều nhất
          if (b.count !== a.count) {
            return b.count - a.count
          }
          
          // 2. Nếu bằng nhau, ưu tiên icon có thời gian tương tác mới nhất
          const timeA = a.latestInteractionTime ? new Date(a.latestInteractionTime).getTime() : 0
          const timeB = b.latestInteractionTime ? new Date(b.latestInteractionTime).getTime() : 0
          return timeB - timeA
        })
        .slice(0, 3)
    },
    currentUser() {
      const userStr = localStorage.getItem('user')
      if (!userStr) return null
      try {
        return JSON.parse(userStr)
      } catch (e) {
        return null
      }
    },
    processedReactors() {
      if (!this.recentReactors) return []
      
      const currentUserId = this.currentUser ? this.currentUser.id : null
      const mapped = this.recentReactors.map(user => {
        const isMe = currentUserId && String(user.id) === String(currentUserId)
        return {
          id: user.id,
          displayName: isMe ? 'Bạn' : (user.displayName || user.username),
          isMe
        }
      })

      // Đưa "Bạn" lên đầu danh sách nếu có để hợp logic tiếng Việt
      const meIndex = mapped.findIndex(r => r.isMe)
      if (meIndex > -1) {
        const [me] = mapped.splice(meIndex, 1)
        mapped.unshift(me)
      }

      return mapped.slice(0, 3)
    }
  }
}
</script>

<style scoped>
.reactionsBar {
  background: #f5f5f5;
  border: 1px solid #dfdfdf;
  border-left: 2px solid #47a7eb;
  padding: 6px 10px;
  font-size: 13px;
  margin: 10px 0; /* Khoảng cách lề trái phải bằng nhau */
  display: flex;
  align-items: center; /* Căn giữa icon và text theo chiều dọc */
  width: 100% !important;
}

.reactionSummary {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  align-items: center;
}

.reactionSummary li {
  display: inline-flex;
  align-items: center;
  margin-right: -4px; /* Thu hẹp khoảng cách để xếp chồng lên nhau tự nhiên hơn */
  position: relative;
  z-index: 1;
}

/* Thêm z-index để các icon xếp chồng đúng thứ tự */
.reactionSummary li:nth-child(1) { z-index: 3; }
.reactionSummary li:nth-child(2) { z-index: 2; }
.reactionSummary li:nth-child(3) { z-index: 1; }

.reaction {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  /* Đã loại bỏ border và background màu trắng gây lỗi UI */
}

.reactionsBar-link {
  color: #1a507a;
  text-decoration: none;
  margin-left: 8px;
  cursor: pointer;
}

.reactionsBar-link:hover {
  text-decoration: underline;
}

.reactionsBar-link bdi {
  color: inherit;
}
</style>
