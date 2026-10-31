<template>
  <div class="page">
    <UiTopBar :code="code" :guest="!code" />

    <div class="search-wrap">
      <DraftSearchStrip @open-members="membersOpen = true" />
    </div>

    <div class="flow">
      <DraftStage class="stage-block" />
      <div class="bench-wrap">
        <DraftBench @open="sheetId = $event" />
      </div>
    </div>

    <footer class="legal">
      내전하자는 Riot Games 의 공식 서비스가 아니며 Riot Games 또는 리그 오브 레전드 제작·관리에 관여한 누구의 견해도 대변하지 않습니다.
      챔피언 일러스트와 아이콘은 Riot Games 의 Data Dragon 에서 불러옵니다.
    </footer>

    <DraftPlayerSheet v-model="sheetId" />
    <DraftMemberDrawer v-if="code" v-model="membersOpen" />
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ code: string | null }>()

const roster = useRoster()
const draft = useDraft()
const sheetId = ref<string | null>(null)
const membersOpen = ref(false)

onMounted(() => {
  // 저장된 명단은 첫 렌더 이후에 복원 (Hydration mismatch 방지)
  roster.restore(props.code ?? 'guest')
  draft.reset()
  if (props.code) roster.loadMembers()
})

// 결과를 본 뒤 명단(사람·라인·점수)을 바꾸면 그 결과는 더 이상 맞지 않으므로 치운다.
// 이미 기록으로 저장한 내전은 그대로 둔다 (승패 기록 뒤 내전 보정치가 바뀌어도 화면 유지).
watch(
  () => JSON.stringify(roster.toRequest()),
  (now, before) => {
    if (draft.result.value && !draft.saved.value && before !== undefined && now !== before) draft.reset()
  }
)
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.search-wrap {
  padding: 20px var(--gutter) 18px;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
}

.flow {
  display: flex;
  flex-direction: column;
}

.bench-wrap {
  padding: 24px var(--gutter) 40px;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
}

.legal {
  margin-top: auto;
  padding: 18px var(--gutter) 26px;
  border-top: 1px solid var(--line);
  color: var(--faint);
  font-size: var(--fs-xs);
  line-height: 1.6;
}

/* 휴대폰: 대기열 먼저, 팀 무대는 그 아래 */
@media (max-width: 640px) {
  .bench-wrap {
    order: -1;
    padding-top: 4px;
    padding-bottom: 20px;
  }
}
</style>
