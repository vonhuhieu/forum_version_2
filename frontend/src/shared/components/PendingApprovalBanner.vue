<template>
  <div class="pending-approval-banner-wrapper">
    <div class="container">
      <div class="pending-approval-banner">
        Tài khoản của bạn hiện đang chờ được quản trị viên phê duyệt. Bạn sẽ nhận được email khi quyết định được đưa ra.
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PendingApprovalBanner'
}
</script>

<style scoped>
.pending-approval-banner-wrapper {
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
  width: 100%;
}

.pending-approval-banner {
  background-color: #f4f9fc;
  border: 1px solid #d3e6f2;
  color: #1a507a;
  padding: 12px 20px;
  border-radius: 4px;
  font-size: 0.95rem;
  text-align: left;
  line-height: 1.5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}
</style>
