<template>
  <span class="tier" :style="{ '--tier': tierColor(tier) }">
    <span class="gem" aria-hidden="true" />
    <span class="label">{{ tierLabel(tier) }}</span>
    <span v-if="lp != null && tier !== 'UNRANKED' && showLp" class="lp num">{{ lp }}LP</span>
  </span>
</template>

<script setup lang="ts">
import { tierColor, tierLabel } from '~/utils/lol'

withDefaults(defineProps<{ tier: string; lp?: number | null; showLp?: boolean }>(), { showLp: true, lp: null })
</script>

<style scoped>
.tier {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--tier);
  white-space: nowrap;
}

.gem {
  width: 8px;
  height: 8px;
  background: var(--tier);
  transform: rotate(45deg);
  box-shadow: 0 0 8px var(--tier);
}

.lp {
  color: var(--muted);
  font-weight: 600;
  font-size: 0.95em;
}
</style>
