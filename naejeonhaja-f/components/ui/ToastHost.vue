<template>
  <div class="host" aria-live="polite">
    <TransitionGroup name="toast">
      <p v-for="t in toast.toasts.value" :key="t.id" class="toast" :class="t.tone" @click="toast.dismiss(t.id)">
        {{ t.text }}
      </p>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
const toast = useToast()
</script>

<style scoped>
.host {
  position: fixed;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  z-index: 80;
  display: grid;
  gap: 8px;
  width: min(520px, calc(100% - 32px));
  pointer-events: none;
}

.toast {
  pointer-events: auto;
  padding: 12px 16px;
  border-radius: var(--r-s);
  background: var(--panel-2);
  border: 1px solid var(--line-strong);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.45);
  font-size: var(--fs-sm);
  font-weight: 600;
  cursor: pointer;
}

.toast.ok {
  border-color: rgba(94, 227, 161, 0.5);
}

.toast.error {
  border-color: rgba(255, 78, 91, 0.6);
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s var(--ease-out);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
