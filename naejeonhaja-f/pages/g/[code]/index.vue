<template>
  <!-- 페이지 전환(Transition)에는 항상 요소 하나가 있어야 해서 감싸 둔다 -->
  <div class="route-root">
    <DraftPage v-if="ready" :code="code" />
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const session = useSession()
const code = computed(() => String(route.params.code).toUpperCase())
const ready = ref(false)

useHead(() => ({ title: `${session.get(code.value)?.name ?? code.value} | 내전하자` }))

onMounted(() => {
  // 입장 토큰이 없으면 입장 화면으로
  if (!session.get(code.value)) {
    navigateTo(`/?join=${code.value}`, { replace: true })
    return
  }
  ready.value = true
})
</script>
