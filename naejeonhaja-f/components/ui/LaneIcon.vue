<template>
  <!-- 라인 아이콘 (직접 그린 단순 도형: 맵 위치를 사각형 안의 굵은 선으로 표현) -->
  <svg :width="size" :height="size" viewBox="0 0 20 20" fill="none" aria-hidden="true" class="lane-icon">
    <template v-if="line === 'TOP'">
      <rect x="2.5" y="2.5" width="15" height="15" rx="1.5" stroke="currentColor" stroke-opacity=".35" stroke-width="1.4" />
      <path d="M3 13V3h10" stroke="currentColor" stroke-width="3" stroke-linecap="square" />
    </template>
    <template v-else-if="line === 'JUNGLE'">
      <path d="M10 2.5c-1.2 3-3.8 4.5-3.8 8.3 0 2.6 1.7 4.6 3.8 6.7 2.1-2.1 3.8-4.1 3.8-6.7 0-3.8-2.6-5.3-3.8-8.3Z" fill="currentColor" />
      <path d="M4.2 7.5c.2 2.5 1 4.5 2.4 6M15.8 7.5c-.2 2.5-1 4.5-2.4 6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
    </template>
    <template v-else-if="line === 'MID'">
      <rect x="2.5" y="2.5" width="15" height="15" rx="1.5" stroke="currentColor" stroke-opacity=".35" stroke-width="1.4" />
      <path d="M4.5 15.5l11-11" stroke="currentColor" stroke-width="3" stroke-linecap="square" />
    </template>
    <template v-else-if="line === 'AD'">
      <rect x="2.5" y="2.5" width="15" height="15" rx="1.5" stroke="currentColor" stroke-opacity=".35" stroke-width="1.4" />
      <path d="M17 7v10H7" stroke="currentColor" stroke-width="3" stroke-linecap="square" />
    </template>
    <template v-else-if="line === 'SUPPORT'">
      <path d="M10 2.8 16 5v4.6c0 3.6-2.5 6.3-6 7.6-3.5-1.3-6-4-6-7.6V5l6-2.2Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
      <path d="M10 7v6M7 10h6" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
    </template>
    <template v-else>
      <circle cx="10" cy="10" r="6.5" stroke="currentColor" stroke-width="1.6" stroke-dasharray="2.5 2.5" />
    </template>
  </svg>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ line?: string | null; size?: number }>(), { size: 18, line: null })
</script>

<style scoped>
.lane-icon {
  flex: none;
}
</style>
